package com.meshai.runtime

import com.meshai.control.DecisionEngine
import com.meshai.control.DecisionResult
import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskStatus
import com.meshai.data.repository.AgentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshKernel — the central execution authority.
 *
 * ## What this replaces (Option C — mini-Kubernetes for agents)
 *
 * Previously [AgentForegroundService] contained the execution loop directly.
 * The service was doing too many things: managing lifecycle, pulling tasks from
 * Room, deciding whether to execute, executing, handling failures, managing
 * wake locks. None of that was testable and all of it was entangled.
 *
 * [MeshKernel] owns exactly one responsibility: run the task execution loop.
 * Everything else is injected:
 * - [TaskScheduler] owns the queue and retry policy.
 * - [DecisionEngine] decides whether to execute now or defer.
 * - [MeshRouter] decides where to execute (local vs. remote).
 * - [RuntimeController] owns the RUNNING/PAUSED/STOPPED lifecycle state.
 *
 * [AgentForegroundService] now calls `kernel.loop()` inside a single coroutine
 * and handles nothing else. Service code shrinks to ~30 lines of Android
 * lifecycle boilerplate.
 *
 * ## Loop invariants
 *
 * 1. A task is dequeued only when the runtime is RUNNING.
 * 2. A task is executed only when [DecisionEngine.shouldExecute] returns Allow.
 * 3. A failed task is re-queued by [TaskScheduler.onFailure] if retries remain.
 * 4. The loop never blocks — if the queue is empty it sleeps [IDLE_POLL_MS].
 * 5. The loop is cancellable — callers cancel the coroutine to stop cleanly.
 *
 * SPEC_REF: KERN-001 / OPTION-C
 */
@Singleton
class MeshKernel @Inject constructor(
    private val scheduler: TaskScheduler,
    private val router: MeshRouter,
    private val decisionEngine: DecisionEngine,
    private val runtimeController: RuntimeController,
    private val telemetryCollector: TelemetryCollector,
    private val agentRepository: AgentRepository
) {
    companion object {
        /** Poll interval when queue is empty. Coarse-grained to save battery. */
        private const val IDLE_POLL_MS   = 5_000L

        /** Delay between tasks — gives mesh gossip time to propagate. */
        private const val INTER_TASK_MS  = 2_000L

        /** Delay when Deferred — short enough to catch state changes quickly. */
        private const val DEFER_POLL_MS  = 3_000L
    }

    private val _kernelState = MutableStateFlow<KernelState>(KernelState.Idle)
    val kernelState: StateFlow<KernelState> = _kernelState.asStateFlow()

    /**
     * Main execution loop. Run this inside a coroutine from [AgentForegroundService].
     * Cancel the coroutine to stop the loop cleanly.
     *
     * @param localNode  This device's node identity. Passed to [MeshRouter] for
     *                   local-vs-remote routing decisions.
     */
    suspend fun loop(localNode: AgentNode) {
        Timber.i("[MeshKernel] Loop started for node ${localNode.displayName}")
        _kernelState.value = KernelState.Running

        // Seed the scheduler with any tasks that were pending before this boot
        // (e.g., tasks persisted in Room from a previous session).
        seedFromRepository()

        while (true) {
            // ── Lifecycle gate ────────────────────────────────────────────
            if (!runtimeController.isRunning) {
                _kernelState.value = KernelState.Paused
                delay(IDLE_POLL_MS)
                continue
            }
            _kernelState.value = KernelState.Running

            // ── Queue check ───────────────────────────────────────────────
            val task = scheduler.next()
            if (task == null) {
                delay(IDLE_POLL_MS)
                continue
            }

            Timber.d("[MeshKernel] Task dequeued: '${task.title}' [${task.priority}]")

            // ── Decision gate ─────────────────────────────────────────────
            val batteryLevel = localNode.batteryLevel
            when (val decision = decisionEngine.shouldExecute(task, batteryLevel)) {
                is DecisionResult.Allow -> {
                    // Fall through to execution
                }
                is DecisionResult.Defer -> {
                    Timber.d("[MeshKernel] Deferred '${task.title}': ${decision.reason}")
                    // Re-submit to the back of the queue; don't count as failure
                    scheduler.submit(task)
                    delay(DEFER_POLL_MS)
                    continue
                }
                is DecisionResult.Deny -> {
                    Timber.w("[MeshKernel] Denied '${task.title}': ${decision.reason}")
                    agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
                    continue
                }
            }

            // ── Execute ───────────────────────────────────────────────────
            _kernelState.value = KernelState.Executing(task)
            agentRepository.updateTaskStatus(task.taskId, TaskStatus.IN_PROGRESS)

            try {
                val message = router.route(task, localNode)

                if (message.status == com.meshai.core.protocol.MessageStatus.DELIVERED) {
                    agentRepository.completeTask(task.taskId, message.payload)
                    scheduler.onSuccess(task)
                    Timber.i("[MeshKernel] Task '${task.title}' completed")
                } else {
                    handleFailure(task, "Router returned FAILED status: ${message.payload}")
                }
            } catch (e: Exception) {
                handleFailure(task, "Unhandled exception: ${e.message}")
            }

            _kernelState.value = KernelState.Running
            delay(INTER_TASK_MS)
        }
    }

    /**
     * Submit a new task to the kernel at runtime.
     * Called by [AgentForegroundService] when an incoming [MeshEnvelope]
     * with type TASK_DELEGATE arrives.
     */
    fun submit(task: AgentTask) {
        scheduler.submit(task)
        Timber.d("[MeshKernel] External submit: '${task.title}'")
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private suspend fun seedFromRepository() {
        val pending = agentRepository.getPendingTasks()
        pending.forEach { scheduler.submit(it) }
        if (pending.isNotEmpty()) {
            Timber.i("[MeshKernel] Seeded ${pending.size} pending tasks from repository")
        }
    }

    private suspend fun handleFailure(task: AgentTask, reason: String) {
        Timber.e("[MeshKernel] Task '${task.title}' failed: $reason")
        val willRetry = scheduler.onFailure(task)
        if (!willRetry) {
            agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
            Timber.e("[MeshKernel] Task '${task.title}' exhausted retries — FAILED permanently")
        }
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    sealed class KernelState {
        object Idle : KernelState()
        object Running : KernelState()
        object Paused : KernelState()
        data class Executing(val task: AgentTask) : KernelState()
    }
}
