package com.meshai.core.model

import kotlinx.serialization.Serializable

/**
 * Capability — a single advertised skill of a mesh node.
 *
 * Nodes broadcast their Capability list inside NODE_ADVERTISE envelopes.
 * CapabilityRegistry uses this to make routing decisions.
 *
 * SPEC_REF: CAP-001
 */
@Serializable
data class Capability(
    /** Matches NodeCapability enum names for type-safe lookup. */
    val name: String,

    /** The node that owns this capability. */
    val deviceId: String,

    /** Estimated execution latency in milliseconds (rolling average). */
    val latencyMs: Int = 500,

    /** Estimated cost in USD per invocation (0 for local). */
    val costUsd: Double = 0.0,

    /** Whether this node is currently accepting new tasks for this capability. */
    val availability: Boolean = true,

    /** Battery level of the owning node at time of advertisement. */
    val batteryLevel: Int = 100,

    /** Epoch ms when this advertisement was created — used to expire stale entries. */
    val advertisedAtMs: Long = System.currentTimeMillis()
) {
    companion object {
        /** Capability entries older than this are considered stale. */
        const val STALE_THRESHOLD_MS = 30_000L
    }

    fun isStale(): Boolean =
        System.currentTimeMillis() - advertisedAtMs > STALE_THRESHOLD_MS

    /** Routing score — lower is better. Combines latency + cost + availability penalty. */
    fun routingScore(): Double {
        if (!availability || isStale()) return Double.MAX_VALUE
        val latencyWeight = latencyMs.toDouble() / 1_000.0   // normalise to seconds
        val costWeight    = costUsd * 1_000.0                 // normalise to milli-dollars
        val batteryPenalty = if (batteryLevel < 20) 5.0 else 0.0
        return latencyWeight + costWeight + batteryPenalty
    }
}
