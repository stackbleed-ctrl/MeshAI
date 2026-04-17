package com.meshai.agent

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a unit of work for the agent to complete.
 *
 * ## Distributed task ownership (v2 additions)
 *
 * The mesh can have multiple nodes that each hold a copy of delegated tasks.
 * Without ownership tracking, both the origin node and a delegated node can
 * execute the same task simultaneously after a reconnect (e.g., Node A goes
 * offline for 30 s; Node B claims the delegated copy; Node A comes back and
 * also starts executing it).
 *
 * To prevent this, [executorNodeId] and [executorLeaseExpiry] implement an
 * **optimistic lease** pattern:
 *
 * - A node atomically writes its own nodeId and a lease expiry into these
 *   fields via [TaskLeaseManager.claimTask] before executing.
 * - Any node checking this task sees the claim and backs off.
 * - If the executor crashes or goes offline, the lease expires after
 *   [LEASE_DURATION_MS] and another node may claim it.
 *
 * The lease is persisted in Room so it survives process restarts.
 *
 * SPEC_REF: SAFETY-002 / SAFETY-004
 * [origin] and [ownerApproved] are evaluated by SafetyGate before any tool execution.
 */
@Entity(tableName = "agent_tasks")
data class AgentTask(
    @PrimaryKey
    val taskId: String,
    val title: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.NORMAL,

    /**
     * SPEC_REF: SAFETY-002
     * Where this task originated. Remote tasks are subject to stricter tool
     * restrictions (e.g. send_sms is denied for REMOTE origin — INV-002).
     */
    val origin: TaskOrigin = TaskOrigin.LOCAL,

    /**
     * SPEC_REF: SAFETY-004
     * Whether the owner explicitly approved this task before becoming
     * unavailable. Required to allow irreversible actions when owner is
     * absent (INV-006).
     */
    val ownerApproved: Boolean = false,

    // ------------------------------------------------------------------
    // Distributed ownership fields (v2)
    // ------------------------------------------------------------------

    /**
     * The nodeId of the node that originally created this task.
     * Immutable after creation. Used for result routing.
     */
    val ownerNodeId: String = "",

    /**
     * The nodeId of the node that has claimed execution of this task.
     * Null if unclaimed. Written atomically by [TaskLeaseManager.claimTask].
     */
    val executorNodeId: String? = null,

    /**
     * Wall-clock ms when the executor node claimed this task.
     * Used for lease age diagnostics.
     */
    val executorLeasedAt: Long? = null,

    /**
     * Wall-clock ms when the execution lease expires.
     * If [System.currentTimeMillis] > this value, the task may be
     * re-claimed by any node regardless of [executorNodeId].
     */
    val executorLeaseExpiry: Long? = null
) {
    companion object {
        /** Lease duration: 5 minutes. Matches max wake-lock hold time. */
        const val LEASE_DURATION_MS = 5 * 60 * 1000L
    }

    /**
     * Returns true if this task is currently claimed by another node
     * with a valid (non-expired) lease.
     *
     * @param claimingNodeId  The nodeId of the node checking whether it
     *                        can claim this task.
     */
    fun isClaimedBy(claimingNodeId: String): Boolean {
        if (executorNodeId == null) return false
        if (executorNodeId == claimingNodeId) return false  // we already own it
        val now = System.currentTimeMillis()
        return executorLeaseExpiry != null && now < executorLeaseExpiry
    }

    /**
     * Returns a copy of this task with an active lease assigned to
     * [claimingNodeId], expiring [LEASE_DURATION_MS] from now.
     */
    fun withLease(claimingNodeId: String): AgentTask {
        val now = System.currentTimeMillis()
        return copy(
            executorNodeId      = claimingNodeId,
            executorLeasedAt    = now,
            executorLeaseExpiry = now + LEASE_DURATION_MS
        )
    }

    /** Returns a copy of this task with the lease cleared. */
    fun withLeaseClear(): AgentTask = copy(
        executorNodeId      = null,
        executorLeasedAt    = null,
        executorLeaseExpiry = null
    )
}

enum class TaskPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class TaskOrigin   { LOCAL, REMOTE }
enum class TaskStatus   { PENDING, IN_PROGRESS, COMPLETED, FAILED }
