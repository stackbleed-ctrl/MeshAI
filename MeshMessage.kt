package com.meshai.mesh

import kotlinx.serialization.Serializable

/**
 * Normalized message structure exchanged between mesh nodes.
 *
 * All messages are JSON-serialized and then encrypted with the Noise/Age
 * session key before transmission.
 *
 * Protocol-Version: 1 (MESHAI_PROTO_V1)
 *
 * Invariants enforced at construction:
 *   SPEC_REF: MESH-001 — signature must be present before send (enforced in MeshNetwork)
 *   SPEC_REF: MESH-002 — nonce must be unique per message (checked by SeenNonceCache)
 *   SPEC_REF: MESH-003 / INV-008 — ttl must be in range 1..5
 */
@Serializable
data class MeshMessage(
    val messageId: String,
    val originNodeId: String,
    val targetNodeId: String?,     // null = broadcast
    val type: MessageType,
    val payload: String,           // JSON-encoded, type-specific
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * SPEC_REF: MESH-003 / INV-008
     * TTL must be 1..5 at creation. Nodes decrement on forward; drop at 0.
     */
    val ttl: Int = 5,

    /**
     * SPEC_REF: MESH-002 — unique per-message nonce for replay protection.
     * Must be validated against SeenNonceCache before processing.
     */
    val nonce: String = generateNonce(),

    /**
     * SPEC_REF: MESH-001 — cryptographic signature over the message content.
     * Null until [com.meshai.mesh.MeshEncryption.sign] is called.
     * MeshNetwork MUST reject unsigned messages.
     */
    val signature: String? = null
) {
    init {
        // SPEC_REF: MESH-003 / INV-008 — enforce TTL range at construction time
        require(ttl in 1..5) {
            "SPEC VIOLATION: MESH-003 / INV-008 — TTL must be 1..5, was $ttl"
        }
    }

    companion object {
        private fun generateNonce(): String =
            java.util.UUID.randomUUID().toString()

        /**
         * Create a signed copy of this message.
         * SPEC_REF: MESH-001
         */
        fun MeshMessage.assertSigned() {
            require(signature != null) {
                "SPEC VIOLATION: MESH-001 / INV-004 — message must be signed before transmission"
            }
        }
    }
}

@Serializable
enum class MessageType {
    NODE_ANNOUNCEMENT,
    TASK_DELEGATE,
    TASK_RESULT,
    MEMORY_SYNC,
    HEARTBEAT,
    MESSAGE_RELAY,
    ALERT
}
