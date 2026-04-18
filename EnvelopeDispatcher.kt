package com.meshai.mesh

import com.meshai.agent.AgentTask
import com.meshai.agent.CapabilityRegistry
import com.meshai.agent.LoadController
import com.meshai.mesh.MeshEnvelope.Companion.hasLoop
import com.meshai.mesh.MeshEnvelope.Companion.isVersionCompatible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EnvelopeDispatcher — inbound side of the Mesh Protocol stack.
 *
 * ## Circular dependency fix (Option A)
 *
 * The previous version injected [MeshKernel] directly, while [MeshKernel]
 * also injected [EnvelopeDispatcher] — a circular Hilt dependency that
 * would fail to compile.
 *
 * Fixed by removing [MeshKernel] from this class entirely.
 * [taskDelegates] is now a [SharedFlow] that [MeshKernel] subscribes to
 * in its `loop()`. No circular dependency.
 *
 * ## ACK protocol (Option A)
 *
 * On successful TASK_DELEGATE receipt, we immediately send an ACK envelope
 * back to the originating node. This confirms delivery to the sender's
 * [AckTracker] and stops retransmission.
 *
 * ## Multi-hop forwarding (Option B)
 *
 * When an envelope's [destinationNodeId] is not this node, [HopForwarder]
 * routes it toward the destination. If destinationNodeId is null (broadcast),
 * it floods to all direct peers.
 *
 * ## Load gate (Option A)
 *
 * [LoadController.canAccept] is checked before emitting task delegates.
 * If the queue is full, a NACK is sent back to the originating node.
 *
 * SPEC_REF: PROTO-002 / OPTION-A / OPTION-B
 */
@Singleton
class EnvelopeDispatcher @Inject constructor(
    private val codec: MeshCodec,
    private val capabilityRegistry: CapabilityRegistry,
    private val routingTable: RoutingTable,
    private val hopForwarder: HopForwarder,
    private val ackTracker: AckTracker,
    private val loadController: LoadController,
    private val transportManager: TransportManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json  = Json { ignoreUnknownKeys = true }

    // ── Typed outbound flows for subscribers ──────────────────────────────
    // MeshKernel subscribes to taskDelegates (no circular dep)
    private val _taskDelegates  = MutableSharedFlow<AgentTask>(extraBufferCapacity = 128)
    private val _taskResults    = MutableSharedFlow<Pair<MeshEnvelope, TaskResultPayload>>(extraBufferCapacity = 64)
    private val _controlSignals = MutableSharedFlow<Pair<MeshEnvelope, ControlSignal>>(extraBufferCapacity = 32)
    private val _rawEnvelopes   = MutableSharedFlow<MeshEnvelope>(extraBufferCapacity = 256)

    val taskDelegates:  SharedFlow<AgentTask>                                 = _taskDelegates.asSharedFlow()
    val taskResults:    SharedFlow<Pair<MeshEnvelope, TaskResultPayload>>     = _taskResults.asSharedFlow()
    val controlSignals: SharedFlow<Pair<MeshEnvelope, ControlSignal>>         = _controlSignals.asSharedFlow()
    val rawEnvelopes:   SharedFlow<MeshEnvelope>                              = _rawEnvelopes.asSharedFlow()

    /**
     * Entry point: called by EVERY transport layer when bytes arrive.
     *
     * @param bytes        Raw bytes from the transport.
     * @param thisNodeId   This device's nodeId.
     */
    fun onBytesReceived(bytes: ByteArray, thisNodeId: String) {
        scope.launch {
            val envelope = codec.decode(bytes)
            if (envelope == null) {
                Timber.w("[Dispatcher] Failed to decode ${bytes.size} bytes — dropping")
                return@launch
            }
            processEnvelope(envelope, thisNodeId)
        }
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private suspend fun processEnvelope(envelope: MeshEnvelope, thisNodeId: String) {
        // ── Protocol validation ───────────────────────────────────────────
        with(MeshEnvelope.Companion) {
            if (!envelope.isVersionCompatible()) {
                Timber.w("[Dispatcher] Version ${envelope.version} > max — dropping")
                return
            }
        }
        if (envelope.ttl <= 0) {
            Timber.w("[Dispatcher] TTL=0 for ${envelope.envelopeId.take(8)} — dropping")
            return
        }
        with(MeshEnvelope.Companion) {
            if (envelope.hasLoop(thisNodeId)) {
                Timber.w("[Dispatcher] Loop detected — dropping ${envelope.envelopeId.take(8)}")
                return
            }
        }

        _rawEnvelopes.emit(envelope)

        // ── Multi-hop forward check (Option B) ────────────────────────────
        if (hopForwarder.shouldForward(envelope, thisNodeId)) {
            Timber.d("[Dispatcher] Forwarding ${envelope.type} → ${envelope.destinationNodeId}")
            hopForwarder.forward(envelope, thisNodeId)
            return  // don't process locally if we're just a relay
        }

        Timber.d("[Dispatcher] ${envelope.type} from ${envelope.originNodeId}")

        // ── Type dispatch ─────────────────────────────────────────────────
        when (envelope.type) {

            EnvelopeType.TASK_DELEGATE -> {
                runCatching { json.decodeFromString<AgentTask>(envelope.payload) }
                    .onSuccess { task ->
                        // Load gate (Option A)
                        if (!loadController.canAccept(
                                currentQueueSize = pendingQueueSize,
                                fromNodeId = envelope.originNodeId
                            )) {
                            Timber.w("[Dispatcher] Load limit — NACKing task from ${envelope.originNodeId}")
                            sendNack(envelope, thisNodeId, "Load limit exceeded")
                            return
                        }

                        // Send ACK before emitting (Option A)
                        sendAck(envelope, thisNodeId)

                        loadController.recordAccepted(envelope.originNodeId)
                        _taskDelegates.emit(task)
                        Timber.i("[Dispatcher] TASK_DELEGATE '${task.title}' accepted from ${envelope.originNodeId}")
                    }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad TASK_DELEGATE payload") }
            }

            EnvelopeType.TASK_RESULT -> {
                runCatching { json.decodeFromString<TaskResultPayload>(envelope.payload) }
                    .onSuccess { _taskResults.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad TASK_RESULT payload") }
            }

            EnvelopeType.NODE_ADVERTISE -> {
                runCatching { json.decodeFromString<NodeAdvertisement>(envelope.payload) }
                    .onSuccess { ad ->
                        capabilityRegistry.advertise(ad)
                        // Register multi-hop route (Option B)
                        if (envelope.hopTrace.isNotEmpty()) {
                            routingTable.addRouteViaPeer(
                                destinationNodeId = ad.nodeId,
                                viaPeer           = envelope.hopTrace.first(),
                                hopCount          = envelope.hopTrace.size + 1
                            )
                        }
                    }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad NODE_ADVERTISE payload") }
            }

            EnvelopeType.ACK -> {
                ackTracker.onAckReceived(envelope.payload)
                Timber.d("[Dispatcher] ACK for ${envelope.payload.take(8)}")
            }

            EnvelopeType.NACK -> {
                Timber.w("[Dispatcher] NACK from ${envelope.originNodeId}: ${envelope.payload.take(80)}")
            }

            EnvelopeType.CONTROL_SIGNAL -> {
                runCatching { json.decodeFromString<ControlSignal>(envelope.payload) }
                    .onSuccess { _controlSignals.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad CONTROL_SIGNAL payload") }
            }

            EnvelopeType.CAPABILITY_QUERY, EnvelopeType.CAPABILITY_RESPONSE ->
                Timber.d("[Dispatcher] ${envelope.type} from ${envelope.originNodeId} — not yet handled")
        }
    }

    // ── ACK / NACK senders ─────────────────────────────────────────────────

    private suspend fun sendAck(originalEnvelope: MeshEnvelope, thisNodeId: String) {
        val ack = ackTracker.buildAck(originalEnvelope, thisNodeId)
        transportManager.sendEnvelope(ack)
    }

    private suspend fun sendNack(originalEnvelope: MeshEnvelope, thisNodeId: String, reason: String) {
        val nack = MeshEnvelope(
            type              = EnvelopeType.NACK,
            originNodeId      = thisNodeId,
            destinationNodeId = originalEnvelope.originNodeId,
            payload           = reason
        )
        transportManager.sendEnvelope(nack)
    }

    /** Updated externally by MeshKernel so the load gate has a current queue size. */
    var pendingQueueSize: Int = 0
}
