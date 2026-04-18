package com.meshai.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * MeshEnvelope — Mesh Protocol v1
 *
 * The single versioned contract for ALL inter-node communication.
 * Every byte crossing a transport layer is a serialised MeshEnvelope.
 *
 * Design goals (SPEC_REF: PROTO-001):
 *  - Strict versioning so nodes can negotiate compatibility.
 *  - Type-tagged payload so receivers can dispatch without deserialising content.
 *  - Routing metadata embedded so any hop can re-route without decrypting payload.
 *  - Economy fields (cost, latency) enable real-time routing decisions.
 *  - Noise-protocol handshake ID reserved for future E2E encryption (SPEC_REF: SEC-001).
 */
@Serializable
data class MeshEnvelope(
    /** Protocol version — receivers MUST reject version > their max supported. */
    val version: Int = PROTOCOL_VERSION,

    /** Unique ID for this envelope; used for dedup / ack. */
    val envelopeId: String = UUID.randomUUID().toString(),

    /** Message class — drives dispatch table on receiver. */
    val type: EnvelopeType,

    /** Origin device nodeId. */
    val originNodeId: String,

    /** Final destination nodeId; null = broadcast / flood. */
    val destinationNodeId: String? = null,

    /**
     * Intermediate hop IDs in order (append-only as envelope is forwarded).
     * Used to detect loops (SPEC_REF: PROTO-003).
     */
    val hopTrace: List<String> = emptyList(),

    /** Max hops before envelope is dropped (default 8). */
    val ttl: Int = DEFAULT_TTL,

    /**
     * JSON-serialised inner payload, typed by [type].
     * Future: replaced by Noise-encrypted ciphertext when SEC-001 is implemented.
     */
    val payload: String,

    /** Arbitrary key-value metadata (transport hints, feature flags, etc.). */
    val metadata: Map<String, String> = emptyMap(),

    // ── Economy fields ─────────────────────────────────────────────────────
    /** Accumulated cost (USD) across all hops so far. */
    val accumulatedCostUsd: Double = 0.0,

    /** Wall-clock ms at origin — used by receivers to compute end-to-end latency. */
    val originTimestampMs: Long = System.currentTimeMillis(),

    // ── Security stubs (SPEC_REF: SEC-001) ────────────────────────────────
    /** Noise handshake session ID; empty until SEC-001 is implemented. */
    val noiseSessionId: String = "",

    /** HMAC-SHA256 of (envelopeId + type + payload) using shared mesh key; empty until SEC-001. */
    val payloadHmac: String = ""
) {
    companion object {
        const val PROTOCOL_VERSION = 1
        const val MAX_SUPPORTED_VERSION = 1
        const val DEFAULT_TTL = 8

        /** Reject envelopes that are too new for this node. */
        fun MeshEnvelope.isVersionCompatible(): Boolean =
            version <= MAX_SUPPORTED_VERSION

        /** Check for routing loops in hop trace. */
        fun MeshEnvelope.hasLoop(thisNodeId: String): Boolean =
            hopTrace.count { it == thisNodeId } > 0

        /** Stamp this node onto the hop trace and decrement TTL. */
        fun MeshEnvelope.forwarded(thisNodeId: String): MeshEnvelope =
            copy(hopTrace = hopTrace + thisNodeId, ttl = ttl - 1)

        /** Convenience: wrap a serialisable payload into an envelope. */
        inline fun <reified T> wrap(
            type: EnvelopeType,
            payload: T,
            originNodeId: String,
            destinationNodeId: String? = null,
            metadata: Map<String, String> = emptyMap()
        ): MeshEnvelope = MeshEnvelope(
            type               = type,
            originNodeId       = originNodeId,
            destinationNodeId  = destinationNodeId,
            payload            = Json.encodeToString(payload),
            metadata           = metadata
        )
    }
}

/**
 * Exhaustive set of envelope types.
 * Adding a new type here is the ONLY change needed to extend the protocol.
 */
@Serializable
enum class EnvelopeType {
    /** Task delegation: payload = AgentTask */
    TASK_DELEGATE,

    /** Task result back to origin: payload = TaskResult */
    TASK_RESULT,

    /** Node capability advertisement (broadcast): payload = AgentNode */
    NODE_ADVERTISE,

    /** Node capability query (unicast): payload = CapabilityQuery */
    CAPABILITY_QUERY,

    /** Response to capability query: payload = List<Capability> */
    CAPABILITY_RESPONSE,

    /** Telemetry event (unicast to coordinator): payload = MeshEvent */
    TELEMETRY_EVENT,

    /** Control signal (pause/resume/kill): payload = ControlSignal */
    CONTROL_SIGNAL,

    /** Acknowledgement for any envelope. payload = envelopeId of acked message. */
    ACK,

    /** Negative-ack / error: payload = ErrorPayload */
    NACK
}

/** Minimal result payload for TASK_RESULT envelopes. */
@Serializable
data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val resultText: String,
    val costUsd: Double = 0.0,
    val latencyMs: Long = 0L
)

/** Error payload for NACK envelopes. */
@Serializable
data class ErrorPayload(
    val originalEnvelopeId: String,
    val code: ErrorCode,
    val message: String
)

@Serializable
enum class ErrorCode {
    VERSION_MISMATCH, TTL_EXCEEDED, POLICY_DENIED,
    LOOP_DETECTED, UNKNOWN_TYPE, INTERNAL_ERROR
}

/** Control signal payload. */
@Serializable
data class ControlSignal(
    val command: ControlCommand,
    val targetNodeId: String? = null
)

@Serializable
enum class ControlCommand { PAUSE, RESUME, KILL, RESET_TELEMETRY }

/** Capability query payload. */
@Serializable
data class CapabilityQuery(
    val requiredCapabilities: List<String>,
    val maxLatencyMs: Long = 5_000,
    val maxCostUsd: Double = 0.05
)
