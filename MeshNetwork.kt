package com.meshai.transport

import android.content.Context
import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshMessage
import com.meshai.core.protocol.MessageStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified mesh abstraction. Tries transport layers in priority order:
 * Meshrabiya → Nearby Connections → BLE GATT
 */
@Singleton
class MeshNetwork @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meshrabiyaLayer: MeshrabiyaLayer,
    private val nearbyLayer: NearbyLayer,
    private val bleGattLayer: BleGattLayer
) : MeshTransport {

    private val _connectedPeers = MutableStateFlow<Set<String>>(emptySet())
    val connectedPeers: StateFlow<Set<String>> = _connectedPeers

    override suspend fun send(task: AgentTask): MeshMessage {
        val transport = when {
            meshrabiyaLayer.isConnected() -> meshrabiyaLayer
            nearbyLayer.isConnected()     -> nearbyLayer
            else                          -> bleGattLayer
        }
        Timber.d("[MeshNetwork] Routing via ${transport::class.simpleName}")
        return transport.send(task)
    }

    override fun isConnected() =
        meshrabiyaLayer.isConnected() || nearbyLayer.isConnected() || bleGattLayer.isConnected()

    override fun peerCount() =
        meshrabiyaLayer.peerCount() + nearbyLayer.peerCount() + bleGattLayer.peerCount()
}
