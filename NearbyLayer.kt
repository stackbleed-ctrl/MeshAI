package com.meshai.mesh

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.meshai.agent.AgentNode
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Google Nearby Connections layer — Bluetooth + Wi-Fi P2P.
 *
 * Used as a fallback / complement to Meshrabiya.
 * Nearby Connections handles:
 * - Initial peer discovery (Bluetooth advertising)
 * - Short-range data transfer (< ~100m)
 * - No internet required
 *
 * Strategy: CLUSTER (many-to-many, best for mesh use case)
 */
class NearbyLayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SERVICE_ID = "com.meshai.mesh.nearby"
    }

    var onMessage: ((ByteArray) -> Unit)? = null
    var onPeerChanged: ((List<AgentNode>) -> Unit)? = null

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val connectedEndpoints = mutableMapOf<String, String>() // endpointId -> nodeId
    private val discoveredEndpoints = mutableMapOf<String, AgentNode>()
    private var localNodeId: String = ""
    private var isRunning = false

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun start(localNode: AgentNode) {
        if (isRunning) return
        isRunning = true
        localNodeId = localNode.nodeId

        startAdvertising(localNode)
        startDiscovery()
        Timber.i("[Nearby] Started advertising + discovery as ${localNode.displayName}")
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        discoveredEndpoints.clear()
        Timber.i("[Nearby] Stopped")
    }

    // -----------------------------------------------------------------------
    // Advertising
    // -----------------------------------------------------------------------

    private fun startAdvertising(localNode: AgentNode) {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            localNode.displayName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Timber.d("[Nearby] Advertising started")
        }.addOnFailureListener { e ->
            Timber.w(e, "[Nearby] Advertising failed (may need Bluetooth permission)")
        }
    }

    // -----------------------------------------------------------------------
    // Discovery
    // -----------------------------------------------------------------------

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Timber.d("[Nearby] Discovery started") }
            .addOnFailureListener { e -> Timber.w(e, "[Nearby] Discovery failed") }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("[Nearby] Found endpoint: $endpointId (${info.endpointName})")
            // Auto-connect to discovered peers
            connectionsClient.requestConnection(
                localNodeId,
                endpointId,
                connectionLifecycleCallback
            ).addOnFailureListener { e ->
                Timber.w(e, "[Nearby] Connection request failed to $endpointId")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.d("[Nearby] Lost endpoint: $endpointId")
            val nodeId = connectedEndpoints.remove(endpointId)
            discoveredEndpoints.remove(nodeId)
            notifyPeerChanged()
        }
    }

    // -----------------------------------------------------------------------
    // Connection lifecycle
    // -----------------------------------------------------------------------

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Timber.d("[Nearby] Connection initiated with $endpointId")
            // Auto-accept connections from other MeshAI nodes
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Timber.i("[Nearby] Connected to $endpointId")
                    connectedEndpoints[endpointId] = endpointId
                    notifyPeerChanged()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Timber.w("[Nearby] Connection rejected by $endpointId")
                }
                else -> {
                    Timber.w("[Nearby] Connection error: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.i("[Nearby] Disconnected from $endpointId")
            connectedEndpoints.remove(endpointId)
            notifyPeerChanged()
        }
    }

    // -----------------------------------------------------------------------
    // Payload (data transfer)
    // -----------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                Timber.d("[Nearby] Received ${bytes.size} bytes from $endpointId")
                onMessage?.invoke(bytes)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Monitor transfer progress for large payloads if needed
        }
    }

    fun send(targetNodeId: String, payload: ByteArray) {
        val endpointId = connectedEndpoints.entries
            .firstOrNull { it.value == targetNodeId }?.key ?: return

        connectionsClient.sendPayload(endpointId, Payload.fromBytes(payload))
            .addOnFailureListener { e -> Timber.w(e, "[Nearby] Send failed to $targetNodeId") }
    }

    fun broadcast(payload: ByteArray) {
        val endpoints = connectedEndpoints.keys.toList()
        if (endpoints.isEmpty()) return
        connectionsClient.sendPayload(endpoints, Payload.fromBytes(payload))
            .addOnFailureListener { e -> Timber.w(e, "[Nearby] Broadcast failed") }
    }

    fun isConnected(nodeId: String): Boolean = connectedEndpoints.containsValue(nodeId)

    fun peerCount(): Int = connectedEndpoints.size

    private fun notifyPeerChanged() {
        val nodes = discoveredEndpoints.values.toList()
        onPeerChanged?.invoke(nodes)
    }
}
