package com.meshai.runtime

import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskScheduler — the priority queue at the heart of [MeshKernel].
 *
 * ## Why this replaces the previous loop (Option C)
 *
 * The previous `AgentForegroundService.startAgentLoop()` called
 * `agentRepository.getPendingTasks().first()` — a naive FIFO pull from
 * Room with no concept of priority, retry policy, or backoff. A CRITICAL
 * task queued after a LOW task would sit and wait.
 *
 * [TaskScheduler] owns the queue. [MeshKernel] calls [next] — it never
 * touches the Room DB directly. This means:
 * - Priority is enforced at dequeue time (CRITICAL > HIGH > NORMAL > LOW).
 * - Failed tasks are re-queued with exponential backoff, not just FAILED.
 * - [MeshKernel] is a dumb loop: `while(true) { val t = scheduler.next(); kernel.route(t) }`.
 * - The scheduler is testable in isolation without an Android Context.
 *
 * ## Retry policy
 *
 * | Attempt | Backoff before retry |
 * |---------|---------------------|
 * | 1st     | 5 s                 |
 * | 2nd     | 15 s                |
 * | 3rd     | 60 s                |
 * | 4th+    | Task marked FAILED, not re-queued |
 *
 * SPEC_REF: SCHED-001 / OPTION-C
 */
@Singleton
class TaskScheduler @Inject constructor() {

    companion object {
        private const val MAX_RETRIES = 3
        private val BACKOFF_MS = longArrayOf(5_000L, 15_000L, 60_000L)
    }

    // Priority: higher numeric value = higher priority (CRITICAL=3, HIGH=2, NORMAL=1, LOW=0)
    private val queue = PriorityQueue<ScheduledTask>(
        compareByDescending<ScheduledTask> { it.priority.ordinal }
            .thenBy { it.scheduledAtMs }
    )

    private val retryCounters = ConcurrentHashMap<String, Int>()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    /**
     * Add a new task to the scheduler.
     * Tasks are immediately eligible for dequeue (no delay on first attempt).
     */
    fun submit(task: AgentTask) {
        synchronized(queue) {
            queue.add(ScheduledTask(task = task, scheduledAtMs = System.currentTimeMillis()))
            _queueSize.value = queue.size
        }
        Timber.d("[Scheduler] Submitted '${task.title}' [${task.priority}] — queue size: ${queue.size}")
    }

    /**
     * Return the highest-priority task that is ready to execute (i.e., its
     * [ScheduledTask.scheduledAtMs] is in the past), or null if none are ready.
     *
     * [MeshKernel] calls this in its main loop. If null is returned, the loop
     * sleeps briefly before trying again.
     */
    fun next(): AgentTask? {
        val now = System.currentTimeMillis()
        synchronized(queue) {
            val ready = queue.firstOrNull { it.scheduledAtMs <= now } ?: return null
            queue.remove(ready)
            _queueSize.value = queue.size
            Timber.d("[Scheduler] Dequeued '${ready.task.title}' [${ready.task.priority}]")
            return ready.task
        }
    }

    /**
     * Report a task failure. Re-queues with exponential backoff if retries remain;
     * returns false if the task has exhausted its retry budget (caller should mark FAILED).
     */
    fun onFailure(task: AgentTask): Boolean {
        val attempt = retryCounters.merge(task.taskId, 1, Int::plus)!!
        return if (attempt <= MAX_RETRIES) {
            val backoff = BACKOFF_MS.getOrElse(attempt - 1) { BACKOFF_MS.last() }
            val retryAt = System.currentTimeMillis() + backoff
            synchronized(queue) {
                queue.add(ScheduledTask(task = task, scheduledAtMs = retryAt, retryAttempt = attempt))
                _queueSize.value = queue.size
            }
            Timber.w("[Scheduler] '${task.title}' failed — retry $attempt/$MAX_RETRIES in ${backoff}ms")
            true
        } else {
            retryCounters.remove(task.taskId)
            Timber.e("[Scheduler] '${task.title}' exhausted retries — marking FAILED")
            false
        }
    }

    /**
     * Report successful completion. Clears the retry counter for this task.
     */
    fun onSuccess(task: AgentTask) {
        retryCounters.remove(task.taskId)
    }

    /** Cancel all pending tasks (e.g., on shutdown). */
    fun clear() {
        synchronized(queue) {
            queue.clear()
            _queueSize.value = 0
        }
        retryCounters.clear()
    }

    fun pendingCount(): Int = queue.size
    fun hasWork(): Boolean  = queue.isNotEmpty()

    private data class ScheduledTask(
        val task: AgentTask,
        val scheduledAtMs: Long,
        val retryAttempt: Int = 0,
        val priority: TaskPriority = task.priority
    )
}
