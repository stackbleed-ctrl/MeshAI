package com.meshai.transport

import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EnvelopeDispatcher — inbound side of the Mesh Protocol v1 stack.
 *
 * All transport layers deliver raw bytes here.
 * EnvelopeDispatcher:
 *  1. Deserialises into MeshEnvelope.
 *  2. Validates version + TTL + loop detection.
 *  3. Dispatches to the correct handler SharedFlow.
 *
 * Consumers (MeshRouter, CapabilityRegistry, DashboardViewModel) collect
 * the appropriate flow.
 *
 * SPEC_REF: PROTO-002 / OPTION-C
 */
@Singleton
class EnvelopeDispatcher @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _taskDelegates   = MutableSharedFlow<Pair<MeshEnvelope, AgentTask>>(extraBufferCapacity = 64)
    private val _taskResults     = MutableSharedFlow<Pair<MeshEnvelope, TaskResult>>(extraBufferCapacity = 64)
    private val _nodeAdvertise   = MutableSharedFlow<Pair<MeshEnvelope, AgentNode>>(extraBufferCapacity = 64)
    private val _controlSignals  = MutableSharedFlow<Pair<MeshEnvelope, ControlSignal>>(extraBufferCapacity = 32)
    private val _telemetryEvents = MutableSharedFlow<Pair<MeshEnvelope, MeshEvent>>(extraBufferCapacity = 128)

    val taskDelegates:   SharedFlow<Pair<MeshEnvelope, AgentTask>>     = _taskDelegates.asSharedFlow()
    val taskResults:     SharedFlow<Pair<MeshEnvelope, TaskResult>>     = _taskResults.asSharedFlow()
    val nodeAdvertise:   SharedFlow<Pair<MeshEnvelope, AgentNode>>     = _nodeAdvertise.asSharedFlow()
    val controlSignals:  SharedFlow<Pair<MeshEnvelope, ControlSignal>> = _controlSignals.asSharedFlow()
    val telemetryEvents: SharedFlow<Pair<MeshEnvelope, MeshEvent>>     = _telemetryEvents.asSharedFlow()

    /**
     * Entry point called by every transport layer when bytes arrive.
     * @param rawJson Serialised MeshEnvelope JSON.
     * @param thisNodeId ID of the receiving node (for loop detection).
     */
    suspend fun onBytesReceived(rawJson: String, thisNodeId: String) {
        val envelope = runCatching {
            Json.decodeFromString<MeshEnvelope>(rawJson)
        }.getOrElse {
            Timber.w("[Dispatcher] Failed to parse envelope: ${it.message}")
            return
        }

        // ── Protocol validation ───────────────────────────────────────────
        with(MeshEnvelope.Companion) {
            if (!envelope.isVersionCompatible()) {
                Timber.w("[Dispatcher] Version ${envelope.version} > max ${ MeshEnvelope.MAX_SUPPORTED_VERSION }, dropping")
                return
            }
            if (envelope.ttl <= 0) {
                Timber.w("[Dispatcher] TTL expired for envelope ${envelope.envelopeId}")
                return
            }
            if (envelope.hasLoop(thisNodeId)) {
                Timber.w("[Dispatcher] Loop detected for envelope ${envelope.envelopeId}")
                return
            }
        }

        Timber.d("[Dispatcher] Received ${envelope.type} from ${envelope.originNodeId}")

        // ── Type dispatch ─────────────────────────────────────────────────
        when (envelope.type) {
            EnvelopeType.TASK_DELEGATE -> {
                runCatching { Json.decodeFromString<AgentTask>(envelope.payload) }
                    .onSuccess { _taskDelegates.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad TASK_DELEGATE payload") }
            }
            EnvelopeType.TASK_RESULT -> {
                runCatching { Json.decodeFromString<TaskResult>(envelope.payload) }
                    .onSuccess { _taskResults.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad TASK_RESULT payload") }
            }
            EnvelopeType.NODE_ADVERTISE -> {
                runCatching { Json.decodeFromString<AgentNode>(envelope.payload) }
                    .onSuccess { _nodeAdvertise.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad NODE_ADVERTISE payload") }
            }
            EnvelopeType.CONTROL_SIGNAL -> {
                runCatching { Json.decodeFromString<ControlSignal>(envelope.payload) }
                    .onSuccess { _controlSignals.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad CONTROL_SIGNAL payload") }
            }
            EnvelopeType.TELEMETRY_EVENT -> {
                runCatching { Json.decodeFromString<MeshEvent>(envelope.payload) }
                    .onSuccess { _telemetryEvents.emit(envelope to it) }
                    .onFailure { Timber.e(it, "[Dispatcher] Bad TELEMETRY_EVENT payload") }
            }
            else -> Timber.d("[Dispatcher] Unhandled type ${envelope.type} — ignoring")
        }
    }
}
