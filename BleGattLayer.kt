package com.meshai.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
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
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Bluetooth Low Energy (BLE) GATT layer for:
 *
 * 1. **Advertising**: Broadcasts node presence as a BLE beacon.
 *    Other devices scan and discover this node without pairing.
 *    Payload: node ID + capabilities bitmask (up to ~28 bytes in advertisement).
 *
 * 2. **GATT Server**: Exposes a custom service/characteristic for small
 *    payload exchange (up to ~512 bytes via MTU negotiation).
 *    Used for mesh routing table gossip and low-bandwidth task delegation.
 *
 * 3. **Scanning**: Discovers nearby MeshAI nodes via BLE scan.
 *
 * BLE is the lowest-power transport — used for discovery and small payloads
 * when Meshrabiya/Nearby are unavailable or the device is in battery saver mode.
 *
 * Requires: BLUETOOTH_ADVERTISE, BLUETOOTH_SCAN, BLUETOOTH_CONNECT permissions (API 31+)
 */
class BleGattLayer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** Custom 128-bit UUID for MeshAI BLE service */
        val MESH_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

        /** Characteristic for mesh message exchange */
        val MESH_MESSAGE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")

        /** Characteristic for node announcement */
        val NODE_ANNOUNCE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
    }

    var onBeacon: ((ByteArray) -> Unit)? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var isAdvertising = false

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

        service.addCharacteristic(messageChar)
        service.addCharacteristic(announceChar)
        gattServer?.addService(service)

        Timber.d("[BLE] GATT server started with MeshAI service")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: android.bluetooth.BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED ->
                    Timber.d("[BLE] GATT client connected: ${device.address}")
                BluetoothProfile.STATE_DISCONNECTED ->
                    Timber.d("[BLE] GATT client disconnected: ${device.address}")
            }
        }

        override fun onCharacteristicWriteRequest(
            device: android.bluetooth.BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (characteristic.uuid == MESH_MESSAGE_CHAR_UUID) {
                Timber.d("[BLE] Received ${value.size} bytes via GATT write")
                onBeacon?.invoke(value)
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
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
            .setTimeout(0) // Advertise indefinitely
            .build()

        // Encode node ID (first 8 bytes) as manufacturer data
        val nodeBytes = localNode.nodeId.take(16).chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
            .take(8).toByteArray()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .addManufacturerData(0x05AC, nodeBytes) // 0x05AC = Apple-style; change to your company ID
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
    // Scanning
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
            val rssi = result.rssi
            Timber.d("[BLE] Discovered MeshAI peer: ${device.address} RSSI=$rssi")
            // Manufacturer data contains node ID — extract and announce
            val manufacturerData = result.scanRecord?.getManufacturerSpecificData(0x05AC)
            if (manufacturerData != null) {
                onBeacon?.invoke(manufacturerData)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.w("[BLE] BLE scan failed: error $errorCode")
        }
    }

    // -----------------------------------------------------------------------
    // Public utilities
    // -----------------------------------------------------------------------

    /**
     * Broadcast small payload (<512 bytes) via BLE GATT notifications.
     * Used as last-resort transport for critical mesh messages.
     */
    fun broadcast(payload: ByteArray) {
        val char = gattServer?.getService(MESH_SERVICE_UUID)
            ?.getCharacteristic(MESH_MESSAGE_CHAR_UUID) ?: return
        char.value = payload
        // Notify all connected clients
        bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach { device ->
            gattServer?.notifyCharacteristicChanged(device, char, false)
        }
        Timber.d("[BLE] GATT broadcast ${payload.size} bytes to ${bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size} clients")
    }

    fun isAdvertising(): Boolean = isAdvertising
}
