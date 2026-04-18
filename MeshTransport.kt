package com.meshai.transport

import com.meshai.core.protocol.MeshEnvelope

/**
 * MeshTransport v2 — envelope-native transport abstraction.
 *
 * ## What changed from v1 (Option A — Protocol Boundary)
 *
 * The v1 interface was:
 * ```kotlin
 * interface MeshTransport {
 *     suspend fun send(task: AgentTask): MeshMessage
 * }
 * ```
 *
 * This made transport layers dependent on `AgentTask` — an agent-layer model.
 * Transport should be completely model-agnostic: it moves bytes between nodes.
 * The old `TransportManager.legacyTaskFromEnvelope()` bridge existed only
 * because the interface was wrong.
 *
 * The new interface:
 * - Takes a [MeshEnvelope] (the protocol boundary type) as input.
 * - Returns a [TransportResult] (success/failure + latency), not a MeshMessage.
 *   MeshMessage is a higher-level concept assembled by [TransportManager], not here.
 * - Transport layers are now fully ignorant of agents, tasks, and routing logic.
 *
 * SPEC_REF: TRANS-002 / OPTION-A
 */
interface MeshTransport {
    /**
     * Send [envelope] to its [MeshEnvelope.destinationNodeId].
     * If destinationNodeId is null, broadcast to all connected peers.
     *
     * @return [TransportResult] — never throws; failures are returned, not raised.
     */
    suspend fun send(envelope: MeshEnvelope): TransportResult

    /**
     * True if this transport has at least one connected peer capable of
     * receiving envelopes. Called by [TransportManager] before attempting send.
     */
    fun isConnected(): Boolean

    /**
     * Number of currently connected peer nodes via this transport.
     */
    fun peerCount(): Int

    /**
     * Estimated latency in ms for a typical send on this transport.
     * Used by [TransportManager] to select the lowest-latency available transport.
     * May be a static estimate (BLE: 200ms, Nearby: 80ms, Meshrabiya: 30ms)
     * or a rolling average updated on each successful send.
     */
    fun estimatedLatencyMs(): Long
}

/**
 * Result of a single transport send operation.
 */
data class TransportResult(
    val success: Boolean,
    val latencyMs: Long,
    val errorMessage: String? = null
) {
    companion object {
        fun ok(latencyMs: Long) = TransportResult(success = true, latencyMs = latencyMs)
        fun fail(reason: String, latencyMs: Long = 0L) =
            TransportResult(success = false, latencyMs = latencyMs, errorMessage = reason)
    }
}
