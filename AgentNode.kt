package com.meshai.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single AI agent node in the mesh.
 * Serializable so it can be broadcast during mesh discovery.
 */
@Serializable
data class AgentNode(
    val nodeId: String = UUID.randomUUID().toString(),
    val displayName: String = "MeshAI-${nodeId.take(6)}",
    val capabilities: Set<NodeCapability> = NodeCapability.entries.toSet(),
    val status: NodeStatus = NodeStatus.IDLE,
    val batteryLevel: Int = 100,
    val isOwnerPresent: Boolean = true,
    val meshIp: String? = null,
    val lastSeenEpoch: Long = System.currentTimeMillis()
)

@Serializable
enum class NodeCapability {
    SEND_SMS, ANSWER_CALLS, READ_NOTIFICATIONS,
    CAMERA, GPS_LOCATION,
    LLM_INFERENCE, GEMINI_NANO,
    CELLULAR_RELAY, WIFI_MESH, BLUETOOTH_MESH
}

@Serializable
enum class NodeStatus {
    IDLE, ACTIVE, AGENT_MODE, SLEEPING, OFFLINE
}
