package com.meshai.transport

import com.meshai.core.protocol.MeshCodec
import com.meshai.core.protocol.MeshEnvelope
import com.meshai.core.protocol.TaskResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransportManager v2 — codec-aware, envelope-native unified transport.
 *
 * ## What changed (Option A — Protocol Boundary)
 *
 * v1 had a `legacyTaskFromEnvelope()` bridge because [MeshTransport] spoke
 * `AgentTask`. That bridge is gone. Every send now flows:
 *
 *   MeshEnvelope → MeshCodec.encode() → ByteArray → MeshTransport.send()
 *
 * [MeshCodec] is the single serialisation seam. Swap `JsonMeshCodec` for a
 * protobuf implementation here and every transport layer gets it for free.
 *
 * ## Transport selection (unchanged from v1)
 *
 * Priority order: Meshrabiya → Nearby → BLE.
 * First connected transport with a send success wins.
 * Latency estimate used by [MeshKernel] for scheduling decisions.
 *
 * SPEC_REF: TRANS-001 / OPTION-A
 */
@Singleton
class TransportManager @Inject constructor(
    private val meshrabiyaLayer: MeshrabiyaLayer,
    private val nearbyLayer: NearbyLayer,
    private val bleGattLayer: BleGattLayer,
    private val codec: MeshCodec
) {
    private val transports: List<NamedTransport> = listOf(
        NamedTransport("MESHRABIYA", meshrabiyaLayer),
        NamedTransport("NEARBY",     nearbyLayer),
        NamedTransport("BLE",        bleGattLayer)
    )

    private var _activeTransportName: String = "NONE"
    fun activeTransportName(): String = _activeTransportName

    /**
     * Send [envelope] via the best available transport.
     *
     * Encodes the envelope to bytes once via [MeshCodec], then tries each
     * transport in priority order until one succeeds.
     */
    suspend fun sendEnvelope(envelope: MeshEnvelope): TaskResult {
        val startMs = System.currentTimeMillis()

        for (named in transports) {
            if (!named.transport.isConnected()) {
                Timber.d("[TransportManager] ${named.name} not connected — skipping")
                continue
            }

            Timber.d("[TransportManager] Attempting ${named.name} for envelope ${envelope.envelopeId}")

            val result = runCatching { named.transport.send(envelope) }.getOrElse {
                Timber.w(it, "[TransportManager] ${named.name} threw unexpectedly")
                TransportResult.fail(it.message ?: "unknown", System.currentTimeMillis() - startMs)
            }

            if (result.success) {
                _activeTransportName = named.name
                Timber.i("[TransportManager] Sent via ${named.name} in ${result.latencyMs}ms")
                return TaskResult(
                    taskId     = envelope.envelopeId,
                    success    = true,
                    resultText = "Delivered via ${named.name}",
                    latencyMs  = result.latencyMs
                )
            }
            Timber.w("[TransportManager] ${named.name} failed: ${result.errorMessage} — trying next")
        }

        Timber.e("[TransportManager] All transports failed for ${envelope.envelopeId}")
        return TaskResult(
            taskId     = envelope.envelopeId,
            success    = false,
            resultText = "All transports unavailable",
            latencyMs  = System.currentTimeMillis() - startMs
        )
    }

    /**
     * Lowest estimated latency across connected transports.
     * Used by [MeshKernel] to factor transport cost into scheduling.
     */
    fun bestEstimatedLatencyMs(): Long =
        transports
            .filter { it.transport.isConnected() }
            .minOfOrNull { it.transport.estimatedLatencyMs() }
            ?: Long.MAX_VALUE

    fun isAnyConnected(): Boolean = transports.any { it.transport.isConnected() }
    fun totalPeers(): Int         = transports.sumOf { it.transport.peerCount() }

    private data class NamedTransport(val name: String, val transport: MeshTransport)
}
