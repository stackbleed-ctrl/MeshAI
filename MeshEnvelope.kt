package com.meshai.mesh

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * MeshEnvelope — the single versioned contract for ALL inter-node traffic.
 *
 * Every byte that crosses a transport layer is a serialised [MeshEnvelope].
 * Transport layers are completely model-agnostic: they encode/decode via
 * [MeshCodec] and route the result to [EnvelopeDispatcher]. Nothing else.
 *
 * ## Key design decisions
 *
 * - [type] drives the dispatch table in [EnvelopeDispatcher] without requiring
 *   payload deserialisation — receivers can make routing decisions cheaply.
 * - [hopTrace] enables loop detection on any hop without a central coordinator.
 * - [ttl] provides a self-expiring safety net for stale envelopes.
 * - [destinationNodeId] null = broadcast/flood to all connected peers.
 *
 * SPEC_REF: PROTO-001
 */
@Serializable
data class MeshEnvelope(
    val version: Int              = PROTOCOL_VERSION,
    val envelopeId: String        = UUID.randomUUID().toString(),
    val type: EnvelopeType,
    val originNodeId: String,
    val destinationNodeId: String? = null,
    val hopTrace: List<String>    = emptyList(),
    val ttl: Int                  = DEFAULT_TTL,
    val payload: String,
    val metadata: Map<String, String> = emptyMap(),
    val accumulatedCostUsd: Double = 0.0,
    val originTimestampMs: Long   = System.currentTimeMillis()
) {
    companion object {
        const val PROTOCOL_VERSION      = 1
        const val MAX_SUPPORTED_VERSION = 1
        const val DEFAULT_TTL           = 8

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        fun MeshEnvelope.isVersionCompatible(): Boolean = version <= MAX_SUPPORTED_VERSION
        fun MeshEnvelope.hasLoop(thisNodeId: String): Boolean = hopTrace.count { it == thisNodeId } > 0
        fun MeshEnvelope.forwarded(thisNodeId: String): MeshEnvelope =
            copy(hopTrace = hopTrace + thisNodeId, ttl = ttl - 1)

        inline fun <reified T> wrap(
            type: EnvelopeType,
            payload: T,
            originNodeId: String,
            destinationNodeId: String? = null,
            metadata: Map<String, String> = emptyMap()
        ): MeshEnvelope = MeshEnvelope(
            type              = type,
            originNodeId      = originNodeId,
            destinationNodeId = destinationNodeId,
            payload           = json.encodeToString(payload),
            metadata          = metadata
        )
    }
}

@Serializable
enum class EnvelopeType {
    TASK_DELEGATE,       // payload = AgentTask (JSON)
    TASK_RESULT,         // payload = TaskResultPayload (JSON)
    NODE_ADVERTISE,      // payload = NodeAdvertisement (JSON)
    CAPABILITY_QUERY,    // payload = CapabilityQuery (JSON)
    CAPABILITY_RESPONSE, // payload = List<Capability> (JSON)
    CONTROL_SIGNAL,      // payload = ControlSignal (JSON)
    ACK,                 // payload = envelopeId of acked envelope
    NACK                 // payload = ErrorPayload (JSON)
}

/** Minimal result payload for TASK_RESULT envelopes. */
@Serializable
data class TaskResultPayload(
    val taskId: String,
    val success: Boolean,
    val resultText: String,
    val latencyMs: Long = 0L
)

/** Payload for NODE_ADVERTISE envelopes — broadcasts this node's live capabilities. */
@Serializable
data class NodeAdvertisement(
    val nodeId: String,
    val displayName: String,
    val capabilities: List<String>,
    val batteryLevel: Int,
    val isOwnerPresent: Boolean,
    val advertisedAtMs: Long = System.currentTimeMillis()
)

/** Payload for CAPABILITY_QUERY envelopes. */
@Serializable
data class CapabilityQuery(
    val requiredCapabilities: List<String>,
    val maxLatencyMs: Long  = 5_000,
    val maxCostUsd: Double  = 0.05
)

/** Payload for CONTROL_SIGNAL envelopes. */
@Serializable
data class ControlSignal(
    val command: ControlCommand,
    val targetNodeId: String? = null
)

@Serializable
enum class ControlCommand { PAUSE, RESUME, KILL, RESET_TELEMETRY }

/** Error payload for NACK envelopes. */
@Serializable
data class ErrorPayload(
    val originalEnvelopeId: String,
    val code: String,
    val message: String
)
