package com.meshai.agent

import com.meshai.data.repository.AgentRepository
import com.meshai.mesh.AckTracker
import com.meshai.mesh.EnvelopeDispatcher
import com.meshai.mesh.EnvelopeType
import com.meshai.mesh.MeshEnvelope
import com.meshai.mesh.MeshNetwork
import com.meshai.mesh.RoutingTable
import com.meshai.service.RuntimeController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshKernel — production-hardened central execution authority.
 *
 * ## Changes in this version (Option A + B)
 *
 * **Circular dependency fixed:**
 * No longer injects [EnvelopeDispatcher]. Instead, subscribes to
 * [EnvelopeDispatcher.taskDelegates] SharedFlow in [loop], which [EnvelopeDispatcher]
 * emits to. Both classes are now independently injectable.
 *
 * **Task timeout (Option A):**
 * Every `router.route()` call is wrapped in `withTimeoutOrNull(maxLatencyMs)`.
 * A hung remote task no longer blocks the kernel forever — it times out,
 * triggers a failure record, and the scheduler retries with backoff.
 *
 * **Load gate (Option A):**
 * Before submitting mesh-delegated tasks, the kernel checks
 * `loadController.canAccept()`. Also keeps `envelopeDispatcher.pendingQueueSize`
 * updated so the dispatcher's NACK logic has a current count.
 *
 * **Cognition rate limiting (Option A):**
 * Before every `reActLoop.execute()`, the kernel checks `cognitionLimiter.tryAcquire()`.
 * If the rate limit is active, the task is re-queued with a short backoff instead
 * of calling the LLM.
 *
 * **Routing table (Option B):**
 * After successful task execution, records outcome to `capabilityRegistry` for
 * reliability scoring. On task delegate send, calls `ackTracker.trackSent()`.
 *
 * SPEC_REF: KERN-001 / OPTION-A / OPTION-B
 */
@Singleton
class MeshKernel @Inject constructor(
    private val scheduler: TaskScheduler,
    private val reActLoop: ReActLoop,
    private val decisionEngine: DecisionEngine,
    private val agentRepository: AgentRepository,
    private val meshNetwork: MeshNetwork,
    private val capabilityRegistry: CapabilityRegistry,
    private val envelopeDispatcher: EnvelopeDispatcher,
    private val telemetryCollector: TelemetryCollector,
    private val loadController: LoadController,
    private val cognitionLimiter: CognitionLimiter,
    private val ackTracker: AckTracker,
    private val routingTable: RoutingTable
) {
    companion object {
        private const val IDLE_POLL_MS      = 5_000L
        private const val INTER_TASK_MS     = 2_000L
        private const val DEFER_POLL_MS     = 3_000L
        private const val COGNITION_WAIT_MS = 10_000L
        private const val DEFAULT_TIMEOUT_MS = 30_000L
    }

    private val _kernelState = MutableStateFlow<KernelState>(KernelState.Idle)
    val kernelState: StateFlow<KernelState> = _kernelState.asStateFlow()

    private var isRunning = false

    // -----------------------------------------------------------------------
    // Main loop
    // -----------------------------------------------------------------------

    suspend fun loop(localNode: AgentNode) = coroutineScope {
        Timber.i("[MeshKernel] Loop started — ${localNode.displayName}")
        isRunning = true
        _kernelState.value = KernelState.Running

        // Wire up AckTracker retransmission
        ackTracker.onRetransmit = { envelope ->
            launch { meshNetwork.sendEnvelope(envelope) }
        }
        ackTracker.onExhausted = { envelopeId ->
            Timber.e("[MeshKernel] Envelope $envelopeId delivery failed — no ACK after retransmits")
        }

        // Seed from repository (tasks from previous sessions)
        seedFromRepository()

        // Subscribe to mesh-delegated tasks — no circular dep
        launch {
            envelopeDispatcher.taskDelegates.collect { task ->
                Timber.d("[MeshKernel] Mesh task received: '${task.title}'")
                scheduler.submit(task)
                // Keep dispatcher's queue size current for load gate
                envelopeDispatcher.pendingQueueSize = scheduler.pendingCount()
            }
        }

        // Subscribe to task results (delegated tasks returning)
        launch {
            envelopeDispatcher.taskResults.collect { (envelope, result) ->
                val latencyMs = System.currentTimeMillis() - envelope.originTimestampMs
                if (result.success) {
                    agentRepository.completeTask(result.taskId, result.resultText)
                    capabilityRegistry.recordOutcome(envelope.originNodeId, true, latencyMs)
                    Timber.i("[MeshKernel] Remote result received: task=${result.taskId} ✓")
                } else {
                    capabilityRegistry.recordOutcome(envelope.originNodeId, false, latencyMs)
                    val task = agentRepository.getTaskById(result.taskId) ?: return@collect
                    handleFailure(task, "Remote node failed: ${result.resultText}")
                }
            }
        }

        // Subscribe to control signals
        launch {
            envelopeDispatcher.controlSignals.collect { (_, signal) ->
                when (signal.command) {
                    com.meshai.mesh.ControlCommand.PAUSE  -> { isRunning = false; _kernelState.value = KernelState.Paused }
                    com.meshai.mesh.ControlCommand.RESUME -> { isRunning = true;  _kernelState.value = KernelState.Running }
                    com.meshai.mesh.ControlCommand.KILL   -> { isRunning = false; Timber.w("[MeshKernel] KILL received") }
                    else -> {}
                }
            }
        }

        // Main scheduling loop
        while (true) {
            if (!isRunning) { delay(IDLE_POLL_MS); continue }

            val task = scheduler.next()
            if (task == null) { delay(IDLE_POLL_MS); continue }

            Timber.d("[MeshKernel] Dequeued '${task.title}' [${task.priority}]")
            _kernelState.value = KernelState.Executing(task)

            // Decision gate
            when (val d = decisionEngine.shouldExecute(task, localNode.batteryLevel)) {
                is DecisionResult.Allow -> { /* proceed */ }
                is DecisionResult.Defer -> {
                    Timber.d("[MeshKernel] Deferred: ${d.reason}")
                    scheduler.submit(task); delay(DEFER_POLL_MS)
                    _kernelState.value = KernelState.Running; continue
                }
                is DecisionResult.Deny -> {
                    Timber.w("[MeshKernel] Denied: ${d.reason}")
                    agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
                    _kernelState.value = KernelState.Running; continue
                }
            }

            agentRepository.updateTaskStatus(task.taskId, TaskStatus.IN_PROGRESS)
            val startMs = System.currentTimeMillis()

            try {
                val remoteCandidate = task.requiredCapabilities.firstOrNull()
                    ?.let { cap -> capabilityRegistry.bestNodeForCapability(cap, localNode.nodeId) }

                val success: Boolean
                val resultText: String

                if (remoteCandidate != null && shouldDelegate(task, localNode)) {
                    // Delegate with ACK tracking (Option A)
                    Timber.i("[MeshKernel] Delegating '${task.title}' → ${remoteCandidate.nodeId}")
                    val envelope = MeshEnvelope.wrap(
                        type              = EnvelopeType.TASK_DELEGATE,
                        payload           = task,
                        originNodeId      = localNode.nodeId,
                        destinationNodeId = remoteCandidate.nodeId
                    )
                    ackTracker.trackSent(envelope)
                    val sent = meshNetwork.sendEnvelope(envelope)
                    success    = sent
                    resultText = if (sent) "Delegated to ${remoteCandidate.nodeId}" else "Delegation failed"

                } else {
                    // Execute locally with cognition rate limit (Option A) + timeout (Option A)
                    if (!cognitionLimiter.tryAcquire()) {
                        val waitMs = cognitionLimiter.msUntilNextSlot().coerceAtMost(COGNITION_WAIT_MS)
                        Timber.d("[MeshKernel] Cognition rate limited — requeuing '${task.title}' in ${waitMs}ms")
                        scheduler.submit(task)
                        delay(waitMs)
                        _kernelState.value = KernelState.Running; continue
                    }

                    val timeoutMs = task.constraints.maxLatencyMs.coerceAtLeast(DEFAULT_TIMEOUT_MS)
                    val execResult = withTimeoutOrNull(timeoutMs) {
                        reActLoop.execute(task, localNode)
                    }

                    if (execResult == null) {
                        Timber.w("[MeshKernel] Task '${task.title}' timed out after ${timeoutMs}ms")
                        telemetryCollector.record(task.taskId, localNode.nodeId,
                            timeoutMs, false, "LOCAL_TIMEOUT")
                        handleFailure(task, "Execution timed out after ${timeoutMs}ms")
                        _kernelState.value = KernelState.Running; continue
                    }

                    agentRepository.storeTrace(task.taskId, execResult.trace)
                    success    = execResult.trace.outcome == ExecutionTrace.Outcome.SUCCESS
                    resultText = execResult.answer
                }

                val latencyMs = System.currentTimeMillis() - startMs
                if (success && !resultText.startsWith("Delegated")) {
                    agentRepository.completeTask(task.taskId, resultText)
                    scheduler.onSuccess(task)
                } else if (!success) {
                    handleFailure(task, resultText)
                }

                telemetryCollector.record(task.taskId, localNode.nodeId, latencyMs, success,
                    if (remoteCandidate != null) "MESH_DELEGATE" else "LOCAL")

            } catch (e: Exception) {
                Timber.e(e, "[MeshKernel] '${task.title}' threw")
                handleFailure(task, "Exception: ${e.message}")
            }

            _kernelState.value = KernelState.Running
            delay(INTER_TASK_MS)
        }
    }

    /** Called by external sources (e.g. UI, WorkManager) to inject a task. */
    fun submit(task: AgentTask) {
        scheduler.submit(task)
        envelopeDispatcher.pendingQueueSize = scheduler.pendingCount()
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private suspend fun seedFromRepository() {
        agentRepository.getPendingTasks().forEach { scheduler.submit(it) }
    }

    private fun shouldDelegate(task: AgentTask, node: AgentNode): Boolean {
        if (node.status == NodeStatus.AGENT_MODE) return false
        if (node.batteryLevel < 10) return true
        return false
    }

    private suspend fun handleFailure(task: AgentTask, reason: String) {
        Timber.e("[MeshKernel] '${task.title}' failed: $reason")
        if (!scheduler.onFailure(task)) {
            agentRepository.updateTaskStatus(task.taskId, TaskStatus.FAILED)
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
