package com.meshai.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.meshai.agent.AgentNode
import com.meshai.security.MeshEncryption
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Bluetooth Low Energy (BLE) GATT layer — discovery, beaconing, and ECDH key exchange.
 *
 * ## ECDH Key Exchange via GATT
 *
 * BLE is the lowest-power transport and is used primarily for peer discovery
 * and small-payload delivery. We use a dedicated GATT characteristic
 * ([KEY_EXCHANGE_CHAR_UUID]) to perform the public key exchange that bootstraps
 * the [MeshEncryption] session key for this transport.
 *
 * ### Server role (we are always advertising)
 *
 * - [KEY_EXCHANGE_CHAR_UUID] is READ + WRITE.
 * - Its value is pre-populated with our own EC public key bytes.
 * - When a remote GATT client reads this characteristic, they receive our key.
 * - When a remote GATT client writes to this characteristic, we receive their
 *   public key and call [MeshEncryption.registerPeer].
 * - [onCharacteristicReadRequest] also responds with our public key bytes.
 *
 * ### Client role (we initiate after a scan hit)
 *
 * When [scanCallback.onScanResult] fires for a new MeshAI peer:
 * 1. Connect as a GATT client ([BluetoothDevice.connectGatt]).
 * 2. Discover services.
 * 3. Read [KEY_EXCHANGE_CHAR_UUID] → call [MeshEncryption.registerPeer] with bytes received.
 * 4. Write our own public key bytes to [KEY_EXCHANGE_CHAR_UUID] on the remote server.
 * 5. Disconnect the one-shot GATT client connection (key exchange is complete;
 *    ongoing data uses the server path or Nearby/Meshrabiya for larger payloads).
 *
 * ### Why separate client connections?
 *
 * GATT server callbacks only fire when a *remote* device initiates reads/writes
 * to us. If both sides only act as servers, neither ever exchanges keys. The
 * client-side connection on scan discovery is the initiating move; both sides
 * end up triggering a write to the other's server characteristic, completing
 * a full symmetric exchange within ~500 ms of mutual discovery.
 */
class BleGattLayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshEncryption: MeshEncryption
) {

    companion object {
        /** Custom 128-bit UUID for MeshAI BLE service */
        val MESH_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

        /** Characteristic for mesh message exchange (beacon payloads) */
        val MESH_MESSAGE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")

        /** Characteristic for node announcement / metadata */
        val NODE_ANNOUNCE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")

        /**
         * Characteristic for ECDH public key exchange.
         * READ: returns this node's EC public key (DER, 91 bytes).
         * WRITE: accepts the remote peer's EC public key.
         */
        val KEY_EXCHANGE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
    }

    var onBeacon: ((ByteArray) -> Unit)? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var isAdvertising = false

    /**
     * Tracks in-progress GATT client connections initiated by this node
     * for key exchange. Key = device.address. Entries are removed after
     * the exchange completes or the connection fails.
     */
    private val pendingKeyExchanges = ConcurrentHashMap<String, BluetoothGatt>()

    /**
     * Tracks devices we have already exchanged keys with (by address) to
     * avoid redundant re-connections on repeated scan hits.
     */
    private val completedKeyExchanges = ConcurrentHashMap.newKeySet<String>()

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun start(localNode: AgentNode) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.w("[BLE] Bluetooth not available or disabled — skipping BLE layer")
            return
        }
        startGattServer()
        startAdvertising(localNode)
        startScanning()
    }

    fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        scanner?.stopScan(scanCallback)
        pendingKeyExchanges.values.forEach { it.disconnect(); it.close() }
        pendingKeyExchanges.clear()
        completedKeyExchanges.clear()
        gattServer?.close()
        isAdvertising = false
        Timber.i("[BLE] BLE layer stopped")
    }

    // -----------------------------------------------------------------------
    // GATT Server
    // -----------------------------------------------------------------------

    private fun startGattServer() {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)

        val service = BluetoothGattService(
            MESH_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Message exchange characteristic (read/write/notify)
        val messageChar = BluetoothGattCharacteristic(
            MESH_MESSAGE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Node announcement characteristic (read only)
        val announceChar = BluetoothGattCharacteristic(
            NODE_ANNOUNCE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        // Key exchange characteristic (read + write)
        // Pre-populated with our own public key so clients can read it.
        val keyExchangeChar = BluetoothGattCharacteristic(
            KEY_EXCHANGE_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ).also {
            it.value = meshEncryption.exportPublicKeyBytes()
        }

        service.addCharacteristic(messageChar)
        service.addCharacteristic(announceChar)
        service.addCharacteristic(keyExchangeChar)
        gattServer?.addService(service)

        Timber.d("[BLE] GATT server started — key exchange char pre-loaded with ${
            meshEncryption.exportPublicKeyBytes().size} byte public key")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(
            device: BluetoothDevice, status: Int, newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED ->
                    Timber.d("[BLE] GATT client connected: ${device.address}")
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("[BLE] GATT client disconnected: ${device.address}")
                    // Evict session key if the device address maps to a known peer.
                    // Using device.address as peerNodeId for BLE — consistent with
                    // how we register in gattClientCallback below.
                    meshEncryption.evictPeer(device.address)
                    completedKeyExchanges.remove(device.address)
                }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                KEY_EXCHANGE_CHAR_UUID -> {
                    // Respond with our public key bytes regardless of offset.
                    // EC public key is 91 bytes — well within a single ATT response.
                    val pubKey = meshEncryption.exportPublicKeyBytes()
                    gattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, 0, pubKey
                    )
                    Timber.d("[BLE] Served public key to ${device.address} on read request")
                }
                else -> {
                    gattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.value
                    )
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                KEY_EXCHANGE_CHAR_UUID -> {
                    // Peer is writing their public key to us — complete our side
                    // of the ECDH handshake.
                    try {
                        meshEncryption.registerPeer(device.address, value)
                        completedKeyExchanges.add(device.address)
                        Timber.i("[BLE] ECDH handshake complete (server-side) with ${device.address}")
                    } catch (e: Exception) {
                        Timber.e(e, "[BLE] Key exchange write failed from ${device.address}")
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                        )
                    }
                }

                MESH_MESSAGE_CHAR_UUID -> {
                    Timber.d("[BLE] Received ${value.size} bytes via GATT write from ${device.address}")
                    onBeacon?.invoke(value)
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                        )
                    }
                }

                else -> {
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // GATT Client — one-shot key exchange on peer discovery
    // -----------------------------------------------------------------------

    /**
     * Initiates a GATT client connection to [device] for the sole purpose of
     * exchanging EC public keys.
     *
     * Sequence:
     * 1. Connect
     * 2. Discover services
     * 3. Read KEY_EXCHANGE_CHAR → register peer
     * 4. Write our public key to KEY_EXCHANGE_CHAR on remote server
     * 5. Disconnect
     */
    private fun initiateKeyExchange(device: BluetoothDevice) {
        if (completedKeyExchanges.contains(device.address)) return
        if (pendingKeyExchanges.containsKey(device.address)) return

        Timber.d("[BLE] Initiating GATT key exchange with ${device.address}")
        val gatt = device.connectGatt(context, false, gattClientCallback)
        pendingKeyExchanges[device.address] = gatt
    }

    private val gattClientCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("[BLE] GATT client connected to ${gatt.device.address} — discovering services")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("[BLE] GATT client disconnected from ${gatt.device.address}")
                    pendingKeyExchanges.remove(gatt.device.address)
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.w("[BLE] Service discovery failed on ${gatt.device.address} (status $status)")
                gatt.disconnect()
                return
            }

            val keyChar = gatt.getService(MESH_SERVICE_UUID)
                ?.getCharacteristic(KEY_EXCHANGE_CHAR_UUID)

            if (keyChar == null) {
                Timber.w("[BLE] ${gatt.device.address} has no key exchange characteristic — not a MeshAI v2 node")
                gatt.disconnect()
                return
            }

            // Step 3: read their public key
            gatt.readCharacteristic(keyChar)
        }

        @Suppress("DEPRECATION")  // readCharacteristic(char) + value field — API <33 path
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid != KEY_EXCHANGE_CHAR_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.w("[BLE] Key read failed on ${gatt.device.address} (status $status)")
                gatt.disconnect()
                return
            }

            val peerPubKeyBytes = characteristic.value
            if (peerPubKeyBytes == null || peerPubKeyBytes.isEmpty()) {
                Timber.w("[BLE] Empty key read from ${gatt.device.address}")
                gatt.disconnect()
                return
            }

            // Step 3 complete: register the peer
            try {
                meshEncryption.registerPeer(gatt.device.address, peerPubKeyBytes)
                Timber.i("[BLE] Registered peer key from ${gatt.device.address}")
            } catch (e: Exception) {
                Timber.e(e, "[BLE] Failed to register peer ${gatt.device.address}")
                gatt.disconnect()
                return
            }

            // Step 4: write our public key back to their KEY_EXCHANGE_CHAR
            val ourPubKey = meshEncryption.exportPublicKeyBytes()
            characteristic.value = ourPubKey
            @Suppress("DEPRECATION")
            val writeSuccess = gatt.writeCharacteristic(characteristic)
            if (!writeSuccess) {
                Timber.w("[BLE] Key write initiation failed on ${gatt.device.address}")
                gatt.disconnect()
            }
        }

        // API 33+ callback override — called on Android 13+ instead of the deprecated one
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (characteristic.uuid != KEY_EXCHANGE_CHAR_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Timber.w("[BLE] Key read failed on ${gatt.device.address} (status $status)")
                gatt.disconnect()
                return
            }

            try {
                meshEncryption.registerPeer(gatt.device.address, value)
                Timber.i("[BLE] Registered peer key from ${gatt.device.address} (API33+)")
            } catch (e: Exception) {
                Timber.e(e, "[BLE] Failed to register peer ${gatt.device.address}")
                gatt.disconnect()
                return
            }

            val ourPubKey = meshEncryption.exportPublicKeyBytes()
            characteristic.value = ourPubKey
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid != KEY_EXCHANGE_CHAR_UUID) return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                completedKeyExchanges.add(gatt.device.address)
                Timber.i("[BLE] ECDH handshake complete (client-side) with ${gatt.device.address}")
            } else {
                Timber.w("[BLE] Key write failed on ${gatt.device.address} (status $status)")
            }

            // Step 5: one-shot connection served its purpose — disconnect
            gatt.disconnect()
        }
    }

    // -----------------------------------------------------------------------
    // Advertising
    // -----------------------------------------------------------------------

    private fun startAdvertising(localNode: AgentNode) {
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Timber.w("[BLE] BLE advertising not supported on this device")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val nodeBytes = localNode.nodeId.take(16).chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .take(8).toByteArray()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .addManufacturerData(0x05AC, nodeBytes)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Timber.i("[BLE] BLE advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Timber.w("[BLE] BLE advertising failed: error $errorCode")
        }
    }

    // -----------------------------------------------------------------------
    // Scanning — triggers client-side key exchange on new peer discovery
    // -----------------------------------------------------------------------

    private fun startScanning() {
        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            Timber.w("[BLE] BLE scanning not available")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        scanner?.startScan(listOf(filter), settings, scanCallback)
        Timber.d("[BLE] BLE scanning started for MeshAI peers")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Timber.d("[BLE] Discovered MeshAI peer: ${device.address} RSSI=${result.rssi}")

            // Pass manufacturer beacon bytes to the mesh layer
            val manufacturerData = result.scanRecord?.getManufacturerSpecificData(0x05AC)
            if (manufacturerData != null) {
                onBeacon?.invoke(manufacturerData)
            }

            // If we haven't exchanged keys yet, initiate the GATT client handshake
            if (!completedKeyExchanges.contains(device.address)) {
                initiateKeyExchange(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.w("[BLE] BLE scan failed: error $errorCode")
        }
    }

    // -----------------------------------------------------------------------
    // Broadcast
    // -----------------------------------------------------------------------

    /**
     * Broadcast small payload (<512 bytes) via BLE GATT notifications.
     * Note: BLE payload is not individually encrypted per peer here because
     * GATT notify goes to all subscribed clients simultaneously. Use Nearby or
     * Meshrabiya for encrypted unicast delivery; BLE broadcast is for low-power
     * discovery and routing table gossip only.
     */
    fun broadcast(payload: ByteArray) {
        val char = gattServer?.getService(MESH_SERVICE_UUID)
            ?.getCharacteristic(MESH_MESSAGE_CHAR_UUID) ?: return
        char.value = payload
        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach { device ->
            gattServer?.notifyCharacteristicChanged(device, char, false)
        }
        Timber.d("[BLE] GATT broadcast ${payload.size} bytes to " +
                "${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size} clients")
    }

    fun isAdvertising(): Boolean = isAdvertising
}
