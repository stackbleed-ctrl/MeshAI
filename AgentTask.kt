package com.meshai.agent

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A unit of work the agent must complete.
 *
 * Tasks are created by the user (via Goals) or by other agents delegating
 * work across the mesh.
 */
@Serializable
data class AgentTask(
    val taskId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: TaskType,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val originNodeId: String? = null,     // Which node created this task
    val assignedNodeId: String? = null,   // Which node is executing it
    val createdEpoch: Long = System.currentTimeMillis(),
    val completedEpoch: Long? = null,
    val result: String? = null,           // Natural-language result summary
    val subTasks: List<AgentTask> = emptyList()
)

@Serializable
enum class TaskType {
    SEND_SMS,
    ANSWER_CALL,
    TAKE_PHOTO,
    GET_LOCATION,
    MONITOR,            // Ongoing sensor watch
    RESPOND_TO_MESSAGE,
    LLM_REASONING,      // Pure reasoning/planning task
    DELEGATE,           // Route to another mesh node
    CUSTOM              // User-defined goal
}

@Serializable
enum class TaskPriority {
    LOW, NORMAL, HIGH, CRITICAL
}

@Serializable
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    DELEGATED    // Handed off to another mesh node
}
