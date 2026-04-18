package com.meshai.transport

import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshMessage

/**
 * Transport abstraction for cross-node task delegation.
 * Implementations: NearbyTransport, MeshrabiyaTransport, BleTransport.
 */
interface MeshTransport {
    suspend fun send(task: AgentTask): MeshMessage
    fun isConnected(): Boolean
    fun peerCount(): Int
}
