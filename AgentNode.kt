package com.meshai.agent

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single AI agent node in the mesh.
 *
 * A node has a unique ID, a display name, capabilities it supports,
 * and current status information.
 */
@Serializable
data class AgentNode(
    val nodeId: String = UUID.randomUUID().toString(),
    val displayName: String = "MeshAI-${nodeId.take(6)}",
    val capabilities: Set<NodeCapability> = NodeCapability.entries.toSet(),
    val status: NodeStatus = NodeStatus.IDLE,
    val batteryLevel: Int = 100,       // 0–100
    val isOwnerPresent: Boolean = true,
    val meshIp: String? = null,        // Meshrabiya virtual IP (e.g. "10.241.x.x")
    val lastSeenEpoch: Long = System.currentTimeMillis()
)

/**
 * What this node is capable of doing.
 * Capabilities are broadcast during mesh discovery so other agents know
 * what tasks to delegate here.
 */
@Serializable
enum class NodeCapability {
    SEND_SMS,
    ANSWER_CALLS,
    READ_NOTIFICATIONS,
    CAMERA,
    GPS_LOCATION,
    LLM_INFERENCE,       // Can run local LLM
    GEMINI_NANO,         // Has Gemini Nano (AICore)
    CELLULAR_RELAY,      // Has active cellular — can forward to cloud
    WIFI_MESH,           // Supports Meshrabiya
    BLUETOOTH_MESH       // Supports BLE GATT mesh
}

/**
 * Lifecycle state of this agent node.
 */
@Serializable
enum class NodeStatus {
    IDLE,           // Agent running, owner present, no active tasks
    ACTIVE,         // Actively working on a task
    AGENT_MODE,     // Owner unavailable — fully autonomous
    SLEEPING,       // Low battery conservation
    OFFLINE         // Not reachable on mesh
}
