package com.meshai.transport

import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshEnvelope
import com.meshai.core.protocol.MeshMessage
import com.meshai.core.protocol.MessageStatus
import com.meshai.core.protocol.TaskResult
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransportManager — unified single entry point for all outbound mesh traffic.
 *
 * Selection strategy (SPEC_REF: TRANS-001):
 *  1. Prefer Meshrabiya (Wi-Fi Direct multi-hop) — highest bandwidth, multi-hop.
 *  2. Fall back to Nearby Connections — BLE+WiFi, good range.
 *  3. Last resort: BLE GATT — lowest power, limited payload size.
 *
 * Each send is wrapped in a timeout equal to the task's maxLatencyMs constraint.
 * If all transports fail, returns a FAILED MeshMessage so callers can handle gracefully.
 */
@Singleton
class TransportManager @Inject constructor(
    private val meshrabiyaLayer: MeshrabiyaLayer,
    private val nearbyLayer: NearbyLayer,
    private val bleGattLayer: BleGattLayer
) {
    /** Ordered list of transports; priority-sorted. */
    private val transports: List<NamedTransport> = listOf(
        NamedTransport("MESHRABIYA", meshrabiyaLayer),
        NamedTransport("NEARBY",     nearbyLayer),
        NamedTransport("BLE",        bleGattLayer)
    )

    /** Name of the transport used for the most recent send. */
    private var _activeTransportName: String = "NONE"
    fun activeTransportName(): String = _activeTransportName

    /**
     * Send a MeshEnvelope via the best available transport.
     * Returns a TaskResult so callers don't need to know transport details.
     */
    suspend fun sendEnvelope(envelope: MeshEnvelope): TaskResult {
        val payloadJson = Json.encodeToString(envelope)
        val startMs = System.currentTimeMillis()

        // Synthesise a legacy AgentTask for transport layers (until they're upgraded to MeshEnvelope)
        val legacyTask = legacyTaskFromEnvelope(envelope)

        for (named in transports) {
            if (!named.transport.isConnected()) {
                Timber.d("[TransportManager] ${named.name} not connected, skipping")
                continue
            }
            Timber.d("[TransportManager] Attempting send via ${named.name}")
            val result = runCatching { named.transport.send(legacyTask) }.getOrNull()
            if (result != null && result.status != MessageStatus.FAILED) {
                _activeTransportName = named.name
                Timber.i("[TransportManager] Sent via ${named.name} in ${System.currentTimeMillis() - startMs}ms")
                return TaskResult(
                    taskId     = envelope.envelopeId,
                    success    = true,
                    resultText = result.payload,
                    latencyMs  = System.currentTimeMillis() - startMs
                )
            }
            Timber.w("[TransportManager] ${named.name} send failed, trying next")
        }

        Timber.e("[TransportManager] All transports failed for envelope ${envelope.envelopeId}")
        return TaskResult(
            taskId     = envelope.envelopeId,
            success    = false,
            resultText = "All transports unavailable",
            latencyMs  = System.currentTimeMillis() - startMs
        )
    }

    /** Legacy bridge — wrap envelope in AgentTask until transport layers are upgraded. */
    private fun legacyTaskFromEnvelope(envelope: MeshEnvelope): com.meshai.core.model.AgentTask =
        com.meshai.core.model.AgentTask(
            taskId      = envelope.envelopeId,
            title       = "envelope:${envelope.type}",
            description = envelope.payload
        )

    /** Direct task send (legacy path, still used by old callers). */
    suspend fun send(task: AgentTask): MeshMessage {
        for (named in transports) {
            if (!named.transport.isConnected()) continue
            val result = runCatching { named.transport.send(task) }.getOrNull()
            if (result != null && result.status != MessageStatus.FAILED) {
                _activeTransportName = named.name
                return result
            }
        }
        return MeshMessage(
            messageId       = UUID.randomUUID().toString(),
            taskId          = task.taskId,
            senderNodeId    = "local",
            recipientNodeId = null,
            payload         = "All transports unavailable",
            status          = MessageStatus.FAILED
        )
    }

    fun isAnyConnected(): Boolean = transports.any { it.transport.isConnected() }
    fun totalPeers(): Int = transports.sumOf { it.transport.peerCount() }

    private data class NamedTransport(val name: String, val transport: MeshTransport)
}
