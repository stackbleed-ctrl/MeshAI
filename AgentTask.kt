package com.meshai.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A unit of work for the agent.
 * Augmented with Constraints for mesh economy routing.
 *
 * SPEC_REF: SAFETY-002 — origin gates remote tasks from sensitive tools.
 * SPEC_REF: SAFETY-004 — ownerApproved required for irreversible actions when owner absent.
 */
@Serializable
data class AgentTask(
    val taskId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: TaskType = TaskType.CUSTOM,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val origin: TaskOrigin = TaskOrigin.LOCAL,
    val ownerApproved: Boolean = false,
    /** Mesh economy: cost/latency constraints for routing decisions. */
    val constraints: Constraints = Constraints()
)

@Serializable
data class Constraints(
    val maxCostUsd: Double = 0.05,   // Max acceptable cost to execute this task
    val maxLatencyMs: Long = 5_000   // Max acceptable latency
)

enum class TaskType {
    SEND_SMS, ANSWER_CALL, TAKE_PHOTO, GET_LOCATION,
    MONITOR, RESPOND_TO_MESSAGE, LLM_REASONING,
    DELEGATE, CUSTOM
}

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }

enum class TaskOrigin { LOCAL, REMOTE }
