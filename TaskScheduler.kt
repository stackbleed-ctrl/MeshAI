package com.meshai.agent

import timber.log.Timber
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskScheduler — priority queue with exponential backoff retry.
 *
 * The kernel dequeues via next(); failed tasks are re-queued via onFailure().
 * Retry backoff: 5s → 15s → 60s. After 3 failures the task is dropped (FAILED).
 *
 * SPEC_REF: SCHED-001
 */
@Singleton
class TaskScheduler @Inject constructor() {

    companion object {
        private const val MAX_RETRIES = 3
        private val BACKOFF_MS = longArrayOf(5_000L, 15_000L, 60_000L)
    }

    private val queue = PriorityQueue<ScheduledTask>(
        compareByDescending<ScheduledTask> { it.priority.ordinal }.thenBy { it.scheduledAtMs }
    )
    private val retryCounters = ConcurrentHashMap<String, Int>()

    /** Add a task to the queue — immediately eligible for dequeue. */
    fun submit(task: AgentTask) {
        synchronized(queue) { queue.add(ScheduledTask(task)) }
        Timber.d("[Scheduler] Submitted '${task.title}' [${task.priority}] — queue: ${queue.size}")
    }

    /**
     * Return the highest-priority ready task, or null if none are due yet.
     * MeshKernel sleeps briefly and retries when null is returned.
     */
    fun next(): AgentTask? {
        val now = System.currentTimeMillis()
        synchronized(queue) {
            val ready = queue.firstOrNull { it.scheduledAtMs <= now } ?: return null
            queue.remove(ready)
            return ready.task
        }
    }

    /**
     * Re-queue with exponential backoff. Returns false if retries exhausted
     * (caller should mark task FAILED in Room).
     */
    fun onFailure(task: AgentTask): Boolean {
        val attempt = retryCounters.merge(task.taskId, 1, Int::plus)!!
        return if (attempt <= MAX_RETRIES) {
            val backoff = BACKOFF_MS.getOrElse(attempt - 1) { BACKOFF_MS.last() }
            synchronized(queue) {
                queue.add(ScheduledTask(task, scheduledAtMs = System.currentTimeMillis() + backoff, retryAttempt = attempt))
            }
            Timber.w("[Scheduler] '${task.title}' retry $attempt/$MAX_RETRIES in ${backoff}ms")
            true
        } else {
            retryCounters.remove(task.taskId)
            Timber.e("[Scheduler] '${task.title}' exhausted retries")
            false
        }
    }

    fun onSuccess(task: AgentTask) { retryCounters.remove(task.taskId) }
    fun clear()    { synchronized(queue) { queue.clear() }; retryCounters.clear() }
    fun hasWork(): Boolean = queue.isNotEmpty()
    fun pendingCount(): Int = queue.size

    private data class ScheduledTask(
        val task: AgentTask,
        val scheduledAtMs: Long = System.currentTimeMillis(),
        val retryAttempt: Int   = 0,
        val priority: TaskPriority = task.priority
    )
}
