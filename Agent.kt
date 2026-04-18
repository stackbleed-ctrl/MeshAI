package com.meshai.runtime

import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshMessage

/**
 * Contract for any executable agent in the mesh.
 * Implement this interface to create specialised agent types.
 */
interface Agent {
    suspend fun execute(task: AgentTask): MeshMessage
}
