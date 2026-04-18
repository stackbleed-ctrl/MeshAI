package com.meshai.mesh

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshCodec — the ONLY path between raw bytes and [MeshEnvelope].
 *
 * Every transport layer (Nearby, BLE, Meshrabiya) calls:
 *   encode() before sending → gets ByteArray
 *   decode() on receipt    → gets MeshEnvelope or null (drop on failure)
 *
 * This interface is the hard protocol boundary enforced by Part A.
 * Transport layers are completely model-agnostic: they move ByteArrays.
 *
 * SPEC_REF: PROTO-004
 */
interface MeshCodec {
    fun encode(envelope: MeshEnvelope): ByteArray
    fun decode(bytes: ByteArray): MeshEnvelope?
}

@Singleton
class JsonMeshCodec @Inject constructor() : MeshCodec {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override fun encode(envelope: MeshEnvelope): ByteArray =
        json.encodeToString(envelope).toByteArray(Charsets.UTF_8)

    override fun decode(bytes: ByteArray): MeshEnvelope? {
        if (bytes.isEmpty()) return null
        return runCatching {
            json.decodeFromString<MeshEnvelope>(bytes.toString(Charsets.UTF_8))
        }.getOrElse {
            Timber.w("[MeshCodec] Decode failed: ${it.message?.take(80)}")
            null
        }
    }
}
