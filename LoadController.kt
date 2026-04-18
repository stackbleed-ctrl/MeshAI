package com.meshai.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LoadController — backpressure gate for the MeshKernel.
 *
 * ## Why this exists (Option A — Resilience)
 *
 * Without load control, a mesh storm (multiple devices simultaneously delegating
 * tasks) fills the scheduler queue, causing:
 * - Unbounded memory growth
 * - ANRs from coroutine pool exhaustion
 * - Battery drain from continuous wake locks
 * - Tasks dropped silently when the OS kills the process
 *
 * LoadController provides two gates:
 *
 * **1. Global queue gate** — [canAccept] returns false when the scheduler
 * queue exceeds [maxQueueSize]. New external tasks are NACK'd at the
 * [EnvelopeDispatcher] level rather than accepted and dropped later.
 *
 * **2. Per-node rate limiter** — [isNodeRateLimited] prevents any single
 * remote node from flooding us with more than [maxTasksPerNodePerMinute]
 * tasks/min. Protects against both bugs and malicious nodes.
 *
 * ## Integration
 *
 * MeshKernel calls [canAccept] before submitting mesh-delegated tasks.
 * Local tasks (from agentRepository) bypass the gate — owner's own tasks
 * are never dropped.
 *
 * SPEC_REF: LOAD-001 / OPTION-A
 */
@Singleton
class LoadController @Inject constructor() {

    companion object {
        /** Max tasks in scheduler before we start rejecting external tasks. */
        const val DEFAULT_MAX_QUEUE_SIZE = 100

        /** Max tasks any single remote node can submit per minute. */
        const val DEFAULT_MAX_TASKS_PER_NODE_PER_MIN = 10
    }

    private val maxQueueSize             = DEFAULT_MAX_QUEUE_SIZE
    private val maxTasksPerNodePerMinute = DEFAULT_MAX_TASKS_PER_NODE_PER_MIN

    /** Rolling per-node task counter with 60s window. */
    private data class NodeWindow(
        val count: AtomicInteger = AtomicInteger(0),
        val windowStartMs: Long  = System.currentTimeMillis()
    )
    private val nodeWindows = ConcurrentHashMap<String, NodeWindow>()

    private val _isOverloaded = MutableStateFlow(false)
    val isOverloaded: StateFlow<Boolean> = _isOverloaded.asStateFlow()

    /**
     * Returns true if the kernel can accept another task into the queue.
     *
     * @param currentQueueSize  Current [TaskScheduler.pendingCount].
     * @param fromNodeId        Origin node for rate-limit check; null for local tasks
     *                          (local tasks are never rejected).
     */
    fun canAccept(currentQueueSize: Int, fromNodeId: String? = null): Boolean {
        // Local tasks always accepted
        if (fromNodeId == null) return true

        // Global queue ceiling
        if (currentQueueSize >= maxQueueSize) {
            _isOverloaded.value = true
            Timber.w("[LoadController] Queue full ($currentQueueSize/$maxQueueSize) — rejecting from $fromNodeId")
            return false
        }
        _isOverloaded.value = currentQueueSize > maxQueueSize * 0.8  // 80% = soft warning

        // Per-node rate limit
        if (isNodeRateLimited(fromNodeId)) {
            Timber.w("[LoadController] Node $fromNodeId rate-limited (>${maxTasksPerNodePerMinute}/min)")
            return false
        }

        return true
    }

    /**
     * Record that a task was accepted from [nodeId].
     * Call this when a task is successfully enqueued (after [canAccept] returns true).
     */
    fun recordAccepted(nodeId: String) {
        val window = nodeWindows.getOrPut(nodeId) { NodeWindow() }
        val now = System.currentTimeMillis()

        // Reset window if it's older than 60s
        if (now - window.windowStartMs > 60_000L) {
            nodeWindows[nodeId] = NodeWindow()
        } else {
            window.count.incrementAndGet()
        }
    }

    /** Current queue load as a fraction [0.0, 1.0]. Used by dashboard. */
    fun loadFraction(currentQueueSize: Int): Float =
        (currentQueueSize.toFloat() / maxQueueSize).coerceIn(0f, 1f)

    fun snapshot(currentQueueSize: Int) = LoadSnapshot(
        queueSize    = currentQueueSize,
        maxQueueSize = maxQueueSize,
        isOverloaded = isOverloaded.value,
        loadFraction = loadFraction(currentQueueSize)
    )

    private fun isNodeRateLimited(nodeId: String): Boolean {
        val window = nodeWindows[nodeId] ?: return false
        val now = System.currentTimeMillis()
        if (now - window.windowStartMs > 60_000L) {
            nodeWindows[nodeId] = NodeWindow()
            return false
        }
        return window.count.get() >= maxTasksPerNodePerMinute
    }
}

data class LoadSnapshot(
    val queueSize: Int,
    val maxQueueSize: Int,
    val isOverloaded: Boolean,
    val loadFraction: Float
)
