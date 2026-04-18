package com.meshai.core.protocol

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * MeshCodec — the ONLY path between bytes and [MeshEnvelope].
 *
 * ## Why this exists (Option A — Protocol Boundary)
 *
 * Before this interface, transport layers called `send(AgentTask)` directly,
 * meaning the transport had to know about the agent model. `TransportManager`
 * contained a `legacyTaskFromEnvelope` bridge to paper over this. That bridge
 * is the symptom of a missing boundary.
 *
 * This interface enforces a hard contract:
 *   > NOTHING crosses a transport layer without going through MeshCodec.
 *
 * Consequences:
 * - Transport layers are now completely model-agnostic — they move [ByteArray],
 *   know nothing about tasks, agents, or routing.
 * - The protocol can be versioned, compressed, or swapped to protobuf by changing
 *   one implementation class, touching zero transport code.
 * - Unit tests for routing/dispatch never need a live transport.
 *
 * ## Usage
 *
 * ```kotlin
 * // Sender side (TransportManager):
 * val bytes = codec.encode(envelope)
 * transport.sendBytes(targetNodeId, bytes)
 *
 * // Receiver side (each transport layer on inbound bytes):
 * val envelope = codec.decode(incomingBytes) ?: return  // drop on parse failure
 * envelopeDispatcher.onEnvelopeReceived(envelope, thisNodeId)
 * ```
 *
 * SPEC_REF: PROTO-004 / OPTION-A
 */
interface MeshCodec {
    /**
     * Serialise [envelope] to bytes for transmission.
     * Must be the inverse of [decode] — `decode(encode(e)) == e` for all valid e.
     */
    fun encode(envelope: MeshEnvelope): ByteArray

    /**
     * Deserialise [bytes] into a [MeshEnvelope].
     * Returns null (and logs a warning) on any parse failure instead of throwing,
     * so the transport layer can simply drop malformed frames.
     */
    fun decode(bytes: ByteArray): MeshEnvelope?
}

/**
 * JSON implementation of [MeshCodec].
 *
 * Uses kotlinx.serialization for deterministic, human-readable encoding.
 * UTF-8 byte encoding is explicit so BLE and Nearby layers never have to
 * guess the charset.
 *
 * Production note: swap this for a protobuf or CBOR implementation to cut
 * per-message overhead by ~60% once the protocol is stable.
 */
class JsonMeshCodec : MeshCodec {

    private val json = Json {
        ignoreUnknownKeys = true   // forward-compatible: new fields in newer nodes are ignored
        encodeDefaults    = false  // don't bloat wire with default-value fields
    }

    override fun encode(envelope: MeshEnvelope): ByteArray {
        return json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
    }

    override fun decode(bytes: ByteArray): MeshEnvelope? {
        if (bytes.isEmpty()) return null
        return runCatching {
            json.decodeFromString<MeshEnvelope>(bytes.toString(Charsets.UTF_8))
        }.getOrElse {
            Timber.w("[MeshCodec] Decode failed: ${it.message?.take(100)}")
            null
        }
    }
}
