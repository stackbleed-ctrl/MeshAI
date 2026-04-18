package com.meshai.core.protocol

import kotlinx.serialization.Serializable

/**
 * MeshMessage — legacy intra-process message record.
 * Preserved for storage/logging.  Cross-node traffic now uses MeshEnvelope.
 *
 * MIGRATION: MeshRouter now emits MeshEnvelope; MeshMessage is produced AFTER
 * a round-trip completes so the result can be persisted by AgentRepository.
 */
@Serializable
data class MeshMessage(
    val messageId: String,
    val taskId: String,
    val senderNodeId: String,
    val recipientNodeId: String?,
    val payload: String,
    val status: MessageStatus = MessageStatus.PENDING,
    val result: Map<String, String>? = null,
    val costUsd: Double = 0.0,
    val latencyMs: Long = 0L,
    val timestampEpoch: Long = System.currentTimeMillis(),
    /** Non-null when this message resulted from an envelope round-trip. */
    val envelopeId: String? = null
)

enum class MessageStatus { PENDING, DELIVERED, FAILED, TIMEOUT }
