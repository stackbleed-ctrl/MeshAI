package com.meshai.agent

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// ReliabilityScorer
// ═══════════════════════════════════════════════════════════════════════════

/**
 * ReliabilityScorer — telemetry-driven node routing score.
 *
 * ## Why this exists (Option A — Resilience)
 *
 * The Codex review: "right now you accept remote capabilities blindly. Bad node
 * advertises fake capability → system routes critical tasks there."
 *
 * [CapabilityRegistry] currently scores nodes on battery level and agent mode.
 * Neither tells you whether the node actually *succeeds* at tasks.
 *
 * [ReliabilityScorer] maintains a rolling per-node reliability score based on
 * real execution outcomes reported by [TelemetryCollector]. A node with 90%
 * success rate and 300ms latency scores much higher than one with 50% success
 * at 100ms.
 *
 * ## Score formula
 *
 * score = (successRate × SUCCESS_WEIGHT) − (normalisedLatency × LATENCY_WEIGHT)
 *
 * Where normalisedLatency = min(avgLatencyMs / 5000, 1.0)
 * Score range: [0.0, 1.0]. Higher is better.
 *
 * Score degrades toward 0.5 (neutral) over [DECAY_WINDOW_MS] without
 * updates — prevents permanently penalising nodes that recover.
 *
 * SPEC_REF: REL-001 / OPTION-A
 */
@Singleton
class ReliabilityScorer @Inject constructor() {

    companion object {
        private const val SUCCESS_WEIGHT  = 0.7
        private const val LATENCY_WEIGHT  = 0.3
        private const val DECAY_WINDOW_MS = 10 * 60 * 1000L  // 10 minutes
        private const val MIN_SAMPLES     = 3               // min samples before scoring applies
        const val NEUTRAL_SCORE           = 0.5
    }

    private data class NodeStats(
        val successCount: AtomicInteger = AtomicInteger(0),
        val failureCount: AtomicInteger = AtomicInteger(0),
        val totalLatencyMs: AtomicLong  = AtomicLong(0),
        val sampleCount: AtomicInteger  = AtomicInteger(0),
        val lastUpdatedMs: AtomicLong   = AtomicLong(System.currentTimeMillis())
    )

    private val stats = ConcurrentHashMap<String, NodeStats>()

    /**
     * Record a task outcome for [nodeId].
     * Called by [TelemetryCollector] after every task completes or fails.
     */
    fun record(nodeId: String, success: Boolean, latencyMs: Long) {
        val s = stats.getOrPut(nodeId) { NodeStats() }
        if (success) s.successCount.incrementAndGet() else s.failureCount.incrementAndGet()
        s.totalLatencyMs.addAndGet(latencyMs)
        s.sampleCount.incrementAndGet()
        s.lastUpdatedMs.set(System.currentTimeMillis())
    }

    /**
     * Reliability score for [nodeId] in [0.0, 1.0]. Higher = more reliable.
     * Returns [NEUTRAL_SCORE] for unknown nodes or nodes with < [MIN_SAMPLES].
     */
    fun scoreOf(nodeId: String): Double {
        val s = stats[nodeId] ?: return NEUTRAL_SCORE
        val samples = s.sampleCount.get()
        if (samples < MIN_SAMPLES) return NEUTRAL_SCORE

        // Decay toward neutral over time
        val ageSec = (System.currentTimeMillis() - s.lastUpdatedMs.get()) / 1000.0
        val decayFactor = (1.0 - (ageSec * 1000 / DECAY_WINDOW_MS)).coerceAtLeast(0.0)

        val successRate        = s.successCount.get().toDouble() / samples
        val avgLatencyMs       = s.totalLatencyMs.get().toDouble() / samples
        val normalisedLatency  = (avgLatencyMs / 5_000.0).coerceAtMost(1.0)

        val rawScore = (successRate * SUCCESS_WEIGHT) - (normalisedLatency * LATENCY_WEIGHT)
        return NEUTRAL_SCORE + (rawScore - NEUTRAL_SCORE) * decayFactor
    }

    fun reset(nodeId: String) { stats.remove(nodeId) }

    fun allScores(): Map<String, Double> = stats.keys.associateWith { scoreOf(it) }
}

// ═══════════════════════════════════════════════════════════════════════════
// CognitionLimiter
// ═══════════════════════════════════════════════════════════════════════════

/**
 * CognitionLimiter — rate gate on LLM calls.
 *
 * ## Why this exists (Option A — Resilience)
 *
 * The Codex: "Cognition layer can still go rogue — flood kernel with tasks,
 * create loops." More concretely: if [GoalEngine] is decomposing a goal into
 * subtasks, and each subtask spawns more via ReActLoop reasoning, you can get
 * exponential LLM call growth that drains battery in minutes.
 *
 * [CognitionLimiter] is a token-bucket rate limiter:
 * - [maxCallsPerMinute] tokens refill every 60 seconds.
 * - Each LLM call consumes one token.
 * - When the bucket is empty, [tryAcquire] returns false and the call is
 *   deferred back to the scheduler with a backoff.
 *
 * The default limit (6 calls/min) allows continuous reasonable operation
 * while preventing runaway loops.
 *
 * SPEC_REF: COG-003 / OPTION-A
 */
@Singleton
class CognitionLimiter @Inject constructor() {

    companion object {
        const val DEFAULT_MAX_CALLS_PER_MINUTE = 6
        private const val WINDOW_MS = 60_000L
    }

    private val maxCallsPerMinute = DEFAULT_MAX_CALLS_PER_MINUTE
    private val callTimestamps    = ArrayDeque<Long>(maxCallsPerMinute * 2)

    /**
     * Attempt to acquire permission for one LLM call.
     *
     * @return true if the call is allowed; false if the rate limit is active.
     *         When false, [MeshKernel] re-queues the task with a short backoff
     *         instead of calling [ReActLoop].
     */
    @Synchronized
    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        // Evict timestamps outside the current window
        val cutoff = now - WINDOW_MS
        while (callTimestamps.isNotEmpty() && callTimestamps.first() < cutoff) {
            callTimestamps.removeFirst()
        }
        return if (callTimestamps.size < maxCallsPerMinute) {
            callTimestamps.addLast(now)
            true
        } else {
            Timber.w("[CognitionLimiter] Rate limit reached ($maxCallsPerMinute calls/min) — deferring")
            false
        }
    }

    /** Remaining calls allowed in the current window. */
    @Synchronized
    fun remaining(): Int {
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        callTimestamps.removeAll { it < cutoff }
        return (maxCallsPerMinute - callTimestamps.size).coerceAtLeast(0)
    }

    /** Ms until the oldest token in the bucket expires and a new call is allowed. */
    @Synchronized
    fun msUntilNextSlot(): Long {
        if (callTimestamps.isEmpty()) return 0L
        val oldestMs = callTimestamps.first()
        return ((oldestMs + WINDOW_MS) - System.currentTimeMillis()).coerceAtLeast(0L)
    }
}
