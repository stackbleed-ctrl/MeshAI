package com.meshai.mesh

import android.content.Context
import com.meshai.agent.AgentNode
import com.meshai.agent.NodeStatus
import com.meshai.security.MeshEncryption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central mesh networking coordinator.
 *
 * Manages three radio layers in priority order:
 *   1. [MeshrabiyaLayer]  — true multi-hop Wi-Fi mesh (highest bandwidth)
 *   2. [NearbyLayer]      — Google Nearby Connections (Bluetooth + Wi-Fi)
 *   3. [BleGattLayer]     — BLE GATT beaconing (lowest power, discovery only)
 *
 * Incoming messages from any layer are normalized to [MeshMessage] and
 * dispatched to registered [MessageHandler]s.
 *
 * Outgoing messages use the best available transport, falling back down
 * the stack automatically.
 */
@Singleton
class MeshNetwork @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshrabiyaLayer: MeshrabiyaLayer,
    private val nearbyLayer: NearbyLayer,
    private val bleGattLayer: BleGattLayer,
    private val encryption: MeshEncryption,
    private val coroutineScope: CoroutineScope
) {

    private val _connectedNodes = MutableStateFlow<List<AgentNode>>(emptyList())
    val connectedNodes: StateFlow<List<AgentNode>> = _connectedNodes

    private val _networkStatus = MutableStateFlow(NetworkStatus.OFFLINE)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus

    private val messageHandlers = mutableListOf<MessageHandler>()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun start(localNode: AgentNode) {
        Timber.i("[Mesh] Starting mesh network for node ${localNode.nodeId}")

        // Set up message reception callbacks before starting layers
        meshrabiyaLayer.onMessage = { bytes -> onRawMessage(bytes) }
        nearbyLayer.onMessage = { bytes -> onRawMessage(bytes) }
        bleGattLayer.onBeacon = { bytes -> onRawMessage(bytes) }

        meshrabiyaLayer.onPeerChanged = { peers -> updatePeers(peers) }
        nearbyLayer.onPeerChanged = { peers -> updatePeers(peers) }

        // Start each layer — each degrades gracefully if hardware is unavailable
        meshrabiyaLayer.start(localNode)
        nearbyLayer.start(localNode)
        bleGattLayer.start(localNode)

        _networkStatus.value = NetworkStatus.SEARCHING
        Timber.i("[Mesh] All layers started")
    }

    fun stop() {
        Timber.i("[Mesh] Stopping mesh network")
        meshrabiyaLayer.stop()
        nearbyLayer.stop()
        bleGattLayer.stop()
        _networkStatus.value = NetworkStatus.OFFLINE
    }

    // -----------------------------------------------------------------------
    // Sending
    // -----------------------------------------------------------------------

    /**
     * Send a [MeshMessage] to the given target node, using the best available transport.
     */
    suspend fun send(message: MeshMessage) {
        val payload = encodeAndEncrypt(message)
        val targetId = message.targetNodeId

        Timber.d("[Mesh] Sending ${message.type} to $targetId via best transport")

        when {
            meshrabiyaLayer.isConnected(targetId) -> meshrabiyaLayer.send(targetId, payload)
            nearbyLayer.isConnected(targetId) -> nearbyLayer.send(targetId, payload)
            else -> {
                // Broadcast on BLE if no direct link (limited to 512 bytes)
                val truncated = if (payload.size > 512) payload.take(512).toByteArray() else payload
                bleGattLayer.broadcast(truncated)
                Timber.w("[Mesh] Sent via BLE broadcast (truncated to 512 bytes)")
            }
        }
    }

    /**
     * Broadcast to all connected nodes on all transports.
     */
    suspend fun broadcast(message: MeshMessage) {
        val payload = encodeAndEncrypt(message)
        meshrabiyaLayer.broadcast(payload)
        nearbyLayer.broadcast(payload)
        bleGattLayer.broadcast(payload.take(512).toByteArray())
    }

    // -----------------------------------------------------------------------
    // Receiving
    // -----------------------------------------------------------------------

    fun registerHandler(handler: MessageHandler) {
        messageHandlers.add(handler)
    }

    fun unregisterHandler(handler: MessageHandler) {
        messageHandlers.remove(handler)
    }

    private fun onRawMessage(bytes: ByteArray) {
        try {
            val decrypted = encryption.decrypt(bytes)
            val message = Json.decodeFromString<MeshMessage>(decrypted)
            Timber.d("[Mesh] Received ${message.type} from ${message.originNodeId}")
            messageHandlers.forEach { it.onMessage(message) }
        } catch (e: Exception) {
            Timber.w(e, "[Mesh] Failed to decode incoming message")
        }
    }

    // -----------------------------------------------------------------------
    // Peer management
    // -----------------------------------------------------------------------

    private fun updatePeers(newPeers: List<AgentNode>) {
        val current = _connectedNodes.value.toMutableList()
        newPeers.forEach { peer ->
            val idx = current.indexOfFirst { it.nodeId == peer.nodeId }
            if (idx >= 0) current[idx] = peer else current.add(peer)
        }
        // Remove peers marked offline
        current.removeAll { it.status == NodeStatus.OFFLINE }
        _connectedNodes.value = current

        _networkStatus.value = if (current.isEmpty()) NetworkStatus.SEARCHING else NetworkStatus.CONNECTED
        Timber.d("[Mesh] Peer list updated: ${current.size} nodes")
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun encodeAndEncrypt(message: MeshMessage): ByteArray {
        val json = Json.encodeToString(message)
        return encryption.encrypt(json)
    }

    fun getStats(): MeshStats = MeshStats(
        meshrabiyaPeers = meshrabiyaLayer.peerCount(),
        nearbyPeers = nearbyLayer.peerCount(),
        bleAdvertising = bleGattLayer.isAdvertising()
    )
}

// -----------------------------------------------------------------------
// Supporting types
// -----------------------------------------------------------------------

enum class NetworkStatus { OFFLINE, SEARCHING, CONNECTED }

data class MeshStats(
    val meshrabiyaPeers: Int,
    val nearbyPeers: Int,
    val bleAdvertising: Boolean
)

fun interface MessageHandler {
    fun onMessage(message: MeshMessage)
}
