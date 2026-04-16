package com.meshai.mesh

import kotlinx.serialization.Serializable

/**
 * Normalized message structure exchanged between mesh nodes.
 *
 * All messages are JSON-serialized and then encrypted with the Noise/Age
 * session key before transmission.
 */
@Serializable
data class MeshMessage(
    val messageId: String,
    val originNodeId: String,
    val targetNodeId: String?,          // null = broadcast
    val type: MessageType,
    val payload: String,                // JSON-encoded payload, type-specific
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5                    // Max hops in mesh routing
)

@Serializable
enum class MessageType {
    /** Node announces itself with capabilities */
    NODE_ANNOUNCEMENT,

    /** Delegate a task to another node */
    TASK_DELEGATE,

    /** Result of a completed task */
    TASK_RESULT,

    /** Shared memory/knowledge base gossip */
    MEMORY_SYNC,

    /** Heartbeat / keep-alive */
    HEARTBEAT,

    /** Natural language message (owner-to-owner relay) */
    MESSAGE_RELAY,

    /** Emergency broadcast */
    ALERT
}
