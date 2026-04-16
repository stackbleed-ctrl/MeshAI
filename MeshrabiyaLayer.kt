package com.meshai.mesh

import android.content.Context
import com.meshai.agent.AgentNode
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Wi-Fi mesh layer powered by Meshrabiya.
 *
 * Meshrabiya (github.com/ustadmobile/meshrabiya) creates a multi-hop
 * Wi-Fi mesh with virtual IP addresses (10.241.x.x/16), allowing full
 * TCP/UDP socket communication between Android devices without internet.
 *
 * Features:
 * - WPA3 encrypted Wi-Fi Direct connections
 * - Virtual IP routing (each node gets a stable 10.241.x.x address)
 * - Multi-hop: messages route through intermediate nodes
 * - True peer-to-peer: no central access point required
 *
 * Integration notes:
 * - Meshrabiya runs as a VirtualMeshNode that creates a local hotspot + VPN
 * - Other nodes connect as hotspot clients
 * - The library handles all routing internally
 * - Requires NEARBY_WIFI_DEVICES permission on Android 13+
 */
class MeshrabiyaLayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Callbacks set by MeshNetwork
    var onMessage: ((ByteArray) -> Unit)? = null
    var onPeerChanged: ((List<AgentNode>) -> Unit)? = null

    private val connectedPeers = mutableMapOf<String, AgentNode>()
    private var isRunning = false

    /**
     * Start the Meshrabiya mesh node.
     *
     * In production, this initializes VirtualMeshNode from the Meshrabiya
     * library and starts listening on a virtual network interface.
     *
     * Example Meshrabiya initialization:
     * ```kotlin
     * val meshNode = VirtualMeshNode(
     *     context = context,
     *     config = MeshrabiyaConfig(
     *         meshPrefix = InetAddress.getByName("10.241.0.0"),
     *         virtualNodeIpv4Address = generateVirtualIp(localNode.nodeId),
     *         hotspotConfig = MeshrabiyaHotspotConfig(
     *             ssid = "MeshAI-${localNode.nodeId.take(8)}",
     *             passphrase = generateNetworkPassphrase()
     *         )
     *     )
     * )
     * meshNode.start()
     * meshNode.meshPacketListener = { packet -> onMessage?.invoke(packet.data) }
     * meshNode.nodeStatusListener = { nodes -> onPeerChanged?.invoke(nodes.map { it.toAgentNode() }) }
     * ```
     */
    fun start(localNode: AgentNode) {
        if (isRunning) return
        isRunning = true
        Timber.i("[Meshrabiya] Starting Wi-Fi mesh node for ${localNode.displayName}")
        // TODO: Initialize actual Meshrabiya VirtualMeshNode here
        // Requires NEARBY_WIFI_DEVICES + ACCESS_FINE_LOCATION permissions
        Timber.d("[Meshrabiya] Wi-Fi Direct mesh active (stub — wire up VirtualMeshNode)")
    }

    fun stop() {
        isRunning = false
        connectedPeers.clear()
        Timber.i("[Meshrabiya] Mesh stopped")
    }

    fun send(targetNodeId: String, payload: ByteArray) {
        if (!isRunning) return
        Timber.d("[Meshrabiya] Sending ${payload.size} bytes to $targetNodeId")
        // TODO: meshNode.sendPacket(targetVirtualIp, payload)
    }

    fun broadcast(payload: ByteArray) {
        if (!isRunning) return
        Timber.d("[Meshrabiya] Broadcasting ${payload.size} bytes to all mesh nodes")
        // TODO: meshNode.broadcastPacket(payload)
    }

    fun isConnected(nodeId: String): Boolean = connectedPeers.containsKey(nodeId)

    fun peerCount(): Int = connectedPeers.size
}
