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
import com.meshai.security.MeshEncryption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NearbyLayer — Google Nearby Connections transport with ECDH encryption
 * and full [EnvelopeDispatcher] integration.
 *
 * ## Data flow after Part A + B
 *
 * **Inbound:**
 *   Peer sends bytes
 *       ↓ onPayloadReceived
 *       ↓ ECDH handshake frame? → registerPeer / send our key back
 *       ↓ Encrypted data frame? → decrypt → envelopeDispatcher.onBytesReceived()
 *       ↓ EnvelopeDispatcher → type dispatch → MeshKernel.submit() etc.
 *
 * **Outbound:**
 *   caller provides ByteArray (already a serialised MeshEnvelope via MeshCodec)
 *       ↓ encrypt via MeshEncryption.encrypt()
 *       ↓ buildDataFrame() → [MAGIC | COUNTER | CIPHERTEXT]
 *       ↓ Nearby.sendPayload()
 *
 * ## Capability exchange (Part B)
 *
 * On [STATUS_OK], this node immediately:
 * 1. Sends its EC public key (ECDH handshake — from previous work).
 * 2. After ECDH is confirmed, sends a NODE_ADVERTISE envelope so the peer
 *    can populate its [CapabilityRegistry] and make routing decisions.
 *
 * This is the Part B "discovery + capability exchange" that makes the
 * 2-device demo possible: Device A knows Device B has `vision.detect`,
 * so the kernel can delegate the task without hardcoded configuration.
 *
 * SPEC_REF: TRANS-003 / PART-B
 */
@Singleton
class NearbyLayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshEncryption: MeshEncryption,
    private val envelopeDispatcher: EnvelopeDispatcher,
    private val codec: MeshCodec
) {
    companion object {
        private const val SERVICE_ID    = "com.meshai.mesh.nearby"
        private val MAGIC_HANDSHAKE     = byteArrayOf(0xEC.toByte(), 0x44)
        private val MAGIC_DATA          = byteArrayOf(0x4D, 0x53)
        private const val MAGIC_LEN     = 2
        private const val COUNTER_LEN   = 8
        private const val HEADER_LEN    = MAGIC_LEN + COUNTER_LEN
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val connectedEndpoints = mutableMapOf<String, String>() // endpointId → nodeId

    // Set by MeshNetwork after start() — needed for NODE_ADVERTISE payloads
    var localNode: AgentNode? = null

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun start(node: AgentNode) {
        localNode = node
        startAdvertising(node)
        startDiscovery()
        Timber.i("[Nearby] Started — node: ${node.displayName}")
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectedEndpoints.keys.forEach { meshEncryption.evictPeer(it) }
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        Timber.i("[Nearby] Stopped")
    }

    // -----------------------------------------------------------------------
    // Advertising + Discovery
    // -----------------------------------------------------------------------

    private fun startAdvertising(node: AgentNode) {
        connectionsClient.startAdvertising(
            node.displayName, SERVICE_ID, connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnFailureListener { Timber.w(it, "[Nearby] Advertising failed") }
    }

    private fun startDiscovery() {
        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnFailureListener { Timber.w(it, "[Nearby] Discovery failed") }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("[Nearby] Found: $endpointId (${info.endpointName})")
            connectionsClient.requestConnection(
                localNode?.nodeId ?: "unknown", endpointId, connectionLifecycleCallback
            ).addOnFailureListener { Timber.w(it, "[Nearby] Connection request failed to $endpointId") }
        }
        override fun onEndpointLost(endpointId: String) {
            Timber.d("[Nearby] Lost: $endpointId")
            connectedEndpoints.remove(endpointId)
            meshEncryption.evictPeer(endpointId)
        }
    }

    // -----------------------------------------------------------------------
    // Connection lifecycle — ECDH handshake + NODE_ADVERTISE on connect
    // -----------------------------------------------------------------------

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                Timber.i("[Nearby] Connected to $endpointId")
                connectedEndpoints[endpointId] = endpointId

                // Step 1: ECDH key exchange — send our EC public key
                sendHandshake(endpointId)

                // Step 2 (Part B): send NODE_ADVERTISE so peer can update CapabilityRegistry
                // Sent after a short delay to let ECDH complete first
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    sendNodeAdvertisement(endpointId)
                }
            } else {
                Timber.w("[Nearby] Connection failed: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.i("[Nearby] Disconnected from $endpointId")
            connectedEndpoints.remove(endpointId)
            meshEncryption.evictPeer(endpointId)
        }
    }

    // -----------------------------------------------------------------------
    // Payload receive — demux handshake vs. encrypted data
    // -----------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (bytes.size < MAGIC_LEN) return

            val magic = bytes.copyOfRange(0, MAGIC_LEN)

            when {
                magic.contentEquals(MAGIC_HANDSHAKE) -> {
                    // ECDH: peer sent us their EC public key
                    val peerPubKeyBytes = bytes.copyOfRange(MAGIC_LEN, bytes.size)
                    try {
                        meshEncryption.registerPeer(endpointId, peerPubKeyBytes)
                        Timber.i("[Nearby] ECDH handshake complete with $endpointId")
                        // Now that we have a session key, send our NODE_ADVERTISE encrypted
                        scope.launch { sendNodeAdvertisement(endpointId) }
                    } catch (e: Exception) {
                        Timber.e(e, "[Nearby] ECDH failed with $endpointId — disconnecting")
                        connectionsClient.disconnectFromEndpoint(endpointId)
                    }
                }

                magic.contentEquals(MAGIC_DATA) -> {
                    if (bytes.size <= HEADER_LEN) return
                    if (!meshEncryption.hasPeerSessionKey(endpointId)) {
                        Timber.w("[Nearby] Data from $endpointId — no session key yet, dropping")
                        return
                    }
                    val counter    = ByteBuffer.wrap(bytes, MAGIC_LEN, COUNTER_LEN).order(ByteOrder.LITTLE_ENDIAN).long
                    val ciphertext = bytes.copyOfRange(HEADER_LEN, bytes.size)
                    try {
                        val plaintext = meshEncryption.decrypt(ciphertext, endpointId, endpointId, counter)
                        // Route ALL inbound data through EnvelopeDispatcher — the Part A fix
                        envelopeDispatcher.onBytesReceived(
                            plaintext.toByteArray(Charsets.UTF_8),
                            localNode?.nodeId ?: "unknown"
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "[Nearby] Decrypt failed from $endpointId")
                    }
                }

                else -> Timber.w("[Nearby] Unknown frame magic from $endpointId")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // -----------------------------------------------------------------------
    // Send API — used by MeshNetwork / MeshKernel
    // -----------------------------------------------------------------------

    /**
     * Send [envelopeBytes] (output of [MeshCodec.encode]) to [targetEndpointId],
     * encrypted with the ECDH session key.
     */
    fun send(targetEndpointId: String, envelopeBytes: ByteArray) {
        val frame = buildDataFrame(targetEndpointId, envelopeBytes) ?: return
        connectionsClient.sendPayload(targetEndpointId, Payload.fromBytes(frame))
            .addOnFailureListener { Timber.w(it, "[Nearby] Send failed to $targetEndpointId") }
    }

    fun broadcast(envelopeBytes: ByteArray) {
        connectedEndpoints.keys.forEach { send(it, envelopeBytes) }
    }

    fun isConnected(nodeId: String): Boolean  = connectedEndpoints.containsValue(nodeId)
    fun peerCount(): Int                       = connectedEndpoints.size
    fun estimatedLatencyMs(): Long             = 80L

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun sendHandshake(endpointId: String) {
        val pubKey = meshEncryption.exportPublicKeyBytes()
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(MAGIC_HANDSHAKE + pubKey))
            .addOnFailureListener { Timber.e(it, "[Nearby] Handshake send failed to $endpointId") }
        Timber.d("[Nearby] Handshake sent to $endpointId")
    }

    /**
     * Part B: send a NODE_ADVERTISE envelope to [endpointId] so it can populate
     * its [CapabilityRegistry] with this node's live capabilities.
     *
     * If ECDH is complete the envelope is encrypted; if not yet (called before
     * handshake finishes) it is dropped gracefully.
     */
    private fun sendNodeAdvertisement(endpointId: String) {
        val node = localNode ?: return
        if (!meshEncryption.hasPeerSessionKey(endpointId)) {
            Timber.d("[Nearby] Skipping NODE_ADVERTISE to $endpointId — ECDH not ready")
            return
        }
        val ad = NodeAdvertisement(
            nodeId         = node.nodeId,
            displayName    = node.displayName,
            capabilities   = node.capabilities.map { it.name },
            batteryLevel   = node.batteryLevel,
            isOwnerPresent = node.isOwnerPresent
        )
        val envelope = MeshEnvelope.wrap(
            type         = EnvelopeType.NODE_ADVERTISE,
            payload      = ad,
            originNodeId = node.nodeId,
            destinationNodeId = endpointId
        )
        val bytes = codec.encode(envelope)
        send(endpointId, bytes)
        Timber.i("[Nearby] NODE_ADVERTISE sent to $endpointId: caps=${node.capabilities.map { it.name }}")
    }

    private fun buildDataFrame(endpointId: String, plainBytes: ByteArray): ByteArray? {
        if (!meshEncryption.hasPeerSessionKey(endpointId)) {
            Timber.w("[Nearby] No session key for $endpointId — dropping payload")
            return null
        }
        return try {
            val ciphertext = meshEncryption.encrypt(
                plaintext    = plainBytes.toString(Charsets.UTF_8),
                peerNodeId   = endpointId,
                senderNodeId = localNode?.nodeId ?: "unknown"
            )
            val counter = ByteBuffer.allocate(COUNTER_LEN)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(meshEncryption.currentSendCounter(endpointId))
                .array()
            MAGIC_DATA + counter + ciphertext
        } catch (e: Exception) {
            Timber.e(e, "[Nearby] Encrypt failed for $endpointId")
            null
        }
    }
}
