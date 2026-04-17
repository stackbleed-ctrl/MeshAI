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
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Google Nearby Connections layer — Bluetooth + Wi-Fi P2P.
 *
 * ## ECDH Handshake Protocol
 *
 * Nearby Connections authenticates the channel via its own token exchange
 * (visible to both users), but does not provide end-to-end encryption for
 * the payload bytes. We layer our own AES-256-GCM on top via ECDH-derived
 * per-peer session keys.
 *
 * On every successful connection ([STATUS_OK]):
 * 1. This node immediately sends its EC public key as a [FRAME_HANDSHAKE] payload.
 * 2. When the peer's [FRAME_HANDSHAKE] arrives, [MeshEncryption.registerPeer] is
 *    called, deriving the shared AES session key via ECDH + HKDF.
 * 3. All subsequent [send] / [broadcast] payloads are AES-256-GCM encrypted.
 *    The first 8 bytes of every data payload carry the monotonic send counter
 *    needed to reconstruct the GCM AAD on the receiving side.
 * 4. On disconnect, the session key is evicted via [MeshEncryption.evictPeer].
 *
 * ## Wire frame format
 *
 * Handshake:  [ MAGIC_HS(2) | EC_PUBLIC_KEY_BYTES(91) ]       = 93 bytes
 * Data:       [ MAGIC_DATA(2) | SEND_COUNTER(8 LE) | CIPHERTEXT(variable) ]
 *
 * Using distinct 2-byte magic prefixes avoids any ambiguity between frame types.
 */
class NearbyLayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshEncryption: MeshEncryption
) {

    companion object {
        private const val SERVICE_ID = "com.meshai.mesh.nearby"

        // Frame type magic bytes
        private val MAGIC_HANDSHAKE = byteArrayOf(0xEC.toByte(), 0x44)  // EC DH
        private val MAGIC_DATA      = byteArrayOf(0x4D, 0x53)           // MS = MeshAI

        private const val MAGIC_LEN   = 2
        private const val COUNTER_LEN = 8
        private const val DATA_HEADER_LEN = MAGIC_LEN + COUNTER_LEN
    }

    var onMessage: ((ByteArray) -> Unit)? = null
    var onPeerChanged: ((List<AgentNode>) -> Unit)? = null

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    /** endpointId → stable nodeId (currently same value until nodeId exchange is added) */
    private val connectedEndpoints = mutableMapOf<String, String>()
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
        connectedEndpoints.keys.forEach { meshEncryption.evictPeer(it) }
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
            if (nodeId != null) {
                meshEncryption.evictPeer(endpointId)
                discoveredEndpoints.remove(nodeId)
                notifyPeerChanged()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Connection lifecycle — ECDH handshake triggered here
    // -----------------------------------------------------------------------

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Timber.d("[Nearby] Connection initiated with $endpointId")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Timber.i("[Nearby] Connected to $endpointId")
                    connectedEndpoints[endpointId] = endpointId
                    notifyPeerChanged()

                    // Step 1 of ECDH handshake: send our public key immediately.
                    // The peer will do the same; when we receive theirs in
                    // payloadCallback, we complete the session key derivation.
                    sendHandshake(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED ->
                    Timber.w("[Nearby] Connection rejected by $endpointId")
                else ->
                    Timber.w("[Nearby] Connection error: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.i("[Nearby] Disconnected from $endpointId")
            connectedEndpoints.remove(endpointId)
            // Evict the session key — any cached messages from this peer are now stale
            meshEncryption.evictPeer(endpointId)
            notifyPeerChanged()
        }
    }

    // -----------------------------------------------------------------------
    // Payload receive — demux handshake vs. data frames
    // -----------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (bytes.size < MAGIC_LEN) return

            val magic = bytes.copyOfRange(0, MAGIC_LEN)

            when {
                magic.contentEquals(MAGIC_HANDSHAKE) -> {
                    // Step 2 of ECDH handshake: peer sent us their public key.
                    val peerPubKeyBytes = bytes.copyOfRange(MAGIC_LEN, bytes.size)
                    try {
                        meshEncryption.registerPeer(endpointId, peerPubKeyBytes)
                        Timber.i("[Nearby] ECDH handshake complete with $endpointId")
                    } catch (e: Exception) {
                        Timber.e(e, "[Nearby] ECDH handshake failed with $endpointId — disconnecting")
                        connectionsClient.disconnectFromEndpoint(endpointId)
                    }
                }

                magic.contentEquals(MAGIC_DATA) -> {
                    if (bytes.size <= DATA_HEADER_LEN) {
                        Timber.w("[Nearby] Data frame too short from $endpointId")
                        return
                    }

                    // Check session key is established before attempting decrypt
                    if (!meshEncryption.hasPeerSessionKey(endpointId)) {
                        Timber.w("[Nearby] Dropping data from $endpointId — no session key yet")
                        return
                    }

                    val counter = ByteBuffer.wrap(bytes, MAGIC_LEN, COUNTER_LEN)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .long
                    val ciphertext = bytes.copyOfRange(DATA_HEADER_LEN, bytes.size)

                    try {
                        val plaintext = meshEncryption.decrypt(
                            encrypted     = ciphertext,
                            peerNodeId    = endpointId,
                            senderNodeId  = endpointId,
                            senderCounter = counter
                        )
                        onMessage?.invoke(plaintext.toByteArray(Charsets.UTF_8))
                    } catch (e: Exception) {
                        Timber.e(e, "[Nearby] Decrypt failed from $endpointId — possible replay or tampering")
                    }
                }

                else -> Timber.w("[Nearby] Unknown frame magic from $endpointId: ${magic.toHexString()}")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Monitor large payload transfers if needed
        }
    }

    // -----------------------------------------------------------------------
    // Send / Broadcast — encrypted
    // -----------------------------------------------------------------------

    /**
     * Send [payload] bytes to [targetNodeId].
     *
     * If a session key is established, the payload is AES-256-GCM encrypted.
     * If the handshake has not yet completed, the send is dropped with a
     * warning — callers should retry after [MeshEncryption.hasPeerSessionKey]
     * returns true.
     */
    fun send(targetNodeId: String, payload: ByteArray) {
        val endpointId = connectedEndpoints.entries
            .firstOrNull { it.value == targetNodeId }?.key ?: return

        val frame = buildDataFrame(endpointId, payload) ?: return
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(frame))
            .addOnFailureListener { e -> Timber.w(e, "[Nearby] Send failed to $targetNodeId") }
    }

    /**
     * Broadcast [payload] to all connected peers.
     * Each peer gets an individually encrypted frame (different session key per peer).
     */
    fun broadcast(payload: ByteArray) {
        connectedEndpoints.keys.forEach { endpointId ->
            val frame = buildDataFrame(endpointId, payload) ?: return@forEach
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(frame))
                .addOnFailureListener { e -> Timber.w(e, "[Nearby] Broadcast failed to $endpointId") }
        }
    }

    fun isConnected(nodeId: String): Boolean = connectedEndpoints.containsValue(nodeId)
    fun peerCount(): Int = connectedEndpoints.size

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Send our EC public key to [endpointId] as the opening move of the
     * ECDH handshake. Format: [ MAGIC_HANDSHAKE(2) | PUBLIC_KEY_DER(91) ]
     */
    private fun sendHandshake(endpointId: String) {
        val pubKeyBytes = meshEncryption.exportPublicKeyBytes()
        val frame = MAGIC_HANDSHAKE + pubKeyBytes
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(frame))
            .addOnSuccessListener {
                Timber.d("[Nearby] Handshake sent to $endpointId (${pubKeyBytes.size} key bytes)")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "[Nearby] Handshake send failed to $endpointId")
            }
    }

    /**
     * Build an encrypted data frame for [endpointId].
     *
     * Format: [ MAGIC_DATA(2) | SEND_COUNTER(8 LE) | ENCRYPTED_PAYLOAD ]
     *
     * The send counter is managed by [MeshEncryption] internally; we read
     * the counter *after* encryption because [MeshEncryption.encrypt] increments
     * it and binds the new value into the GCM AAD. We prepend the same counter
     * value so the receiver can reconstruct the AAD for authentication.
     *
     * Implementation note: [MeshEncryption.encrypt] increments the counter then
     * uses it. We obtain the counter from [MeshEncryption.currentSendCounter]
     * after the encrypt call.
     *
     * Returns null and logs a warning if no session key exists yet.
     */
    private fun buildDataFrame(endpointId: String, plainPayload: ByteArray): ByteArray? {
        if (!meshEncryption.hasPeerSessionKey(endpointId)) {
            Timber.w("[Nearby] No session key for $endpointId — payload dropped (handshake pending)")
            return null
        }
        return try {
            val plaintextStr = plainPayload.toString(Charsets.UTF_8)
            val ciphertext = meshEncryption.encrypt(
                plaintext    = plaintextStr,
                peerNodeId   = endpointId,
                senderNodeId = localNodeId
            )
            val counter = meshEncryption.currentSendCounter(endpointId)
            val counterBytes = ByteBuffer.allocate(COUNTER_LEN)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(counter)
                .array()
            MAGIC_DATA + counterBytes + ciphertext
        } catch (e: Exception) {
            Timber.e(e, "[Nearby] Encrypt failed for $endpointId")
            null
        }
    }

    private fun notifyPeerChanged() {
        onPeerChanged?.invoke(discoveredEndpoints.values.toList())
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}
