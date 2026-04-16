package com.meshai.agent

/**
 * Represents a unit of work for the agent to complete.
 *
 * SPEC_REF: SAFETY-002 / SAFETY-004
 * [origin] and [ownerApproved] are evaluated by SafetyGate before any tool execution.
 */
data class AgentTask(
    val taskId: String,
    val title: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.NORMAL,

    /**
     * SPEC_REF: SAFETY-002
     * Where this task originated. Remote tasks are subject to stricter tool restrictions
     * (e.g. send_sms is denied for REMOTE origin — INV-002).
     */
    val origin: TaskOrigin = TaskOrigin.LOCAL,

    /**
     * SPEC_REF: SAFETY-004
     * Whether the owner explicitly approved this task before becoming unavailable.
     * Required to allow irreversible actions when owner is absent (INV-006).
     */
    val ownerApproved: Boolean = false
)

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }
