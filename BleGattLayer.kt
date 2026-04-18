package com.meshai.transport

import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshMessage
import com.meshai.core.protocol.MessageStatus
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE GATT — ultra-low-power presence beaconing and small payload transfer.
 * Used for node discovery and minimal signalling when Wi-Fi is unavailable.
 * Requires BLUETOOTH_ADVERTISE + BLUETOOTH_CONNECT + BLUETOOTH_SCAN (API 31+).
 */
@Singleton
class BleGattLayer @Inject constructor() : MeshTransport {

    private var connected = false
    private var peers = 0

    // TODO: Inject Context; init BluetoothManager / GattServer
    // MESH_SERVICE_UUID = UUID.fromString("12345678-...")

    override suspend fun send(task: AgentTask): MeshMessage {
        Timber.d("[BLE] Sending task ${task.taskId}")
        return MeshMessage(
            messageId       = UUID.randomUUID().toString(),
            taskId          = task.taskId,
            senderNodeId    = "local",
            recipientNodeId = null,
            payload         = "ble payload pending",
            status          = MessageStatus.PENDING,
            costUsd         = 0.0
        )
    }

    override fun isConnected() = connected
    override fun peerCount() = peers
}
