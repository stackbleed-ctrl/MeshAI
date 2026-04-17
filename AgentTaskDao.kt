package com.meshai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meshai.agent.AgentTask
import com.meshai.agent.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [AgentTask].
 *
 * Includes lease-management queries added in v2 to support distributed
 * task deduplication via [TaskLeaseManager].
 *
 * If you already have an AgentTaskDao, merge the new queries into your
 * existing file — do not duplicate the @Dao annotation or class declaration.
 */
@Dao
interface AgentTaskDao {

    // ------------------------------------------------------------------
    // Existing queries (keep as-is)
    // ------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: AgentTask)

    @Update
    suspend fun update(task: AgentTask)

    @Query("SELECT * FROM agent_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getById(taskId: String): AgentTask?

    @Query("SELECT * FROM agent_tasks WHERE status = 'PENDING' ORDER BY priority DESC, taskId ASC")
    suspend fun getPendingTasks(): List<AgentTask>

    @Query("SELECT * FROM agent_tasks ORDER BY priority DESC")
    fun observeAllTasks(): Flow<List<AgentTask>>

    @Query("UPDATE agent_tasks SET status = :status WHERE taskId = :taskId")
    suspend fun updateStatus(taskId: String, status: TaskStatus)

    @Query("DELETE FROM agent_tasks WHERE taskId = :taskId")
    suspend fun delete(taskId: String)

    // ------------------------------------------------------------------
    // v2 lease queries
    // ------------------------------------------------------------------

    /**
     * Returns tasks whose executor lease has expired (executorLeaseExpiry
     * is not null and is in the past) — i.e., tasks that were claimed by
     * a node that may have gone offline.
     *
     * The caller ([TaskLeaseManager.findExpiredLeases]) passes the current
     * wall clock ms as [nowMs].
     */
    @Query(
        """
        SELECT * FROM agent_tasks
        WHERE executorNodeId IS NOT NULL
          AND executorLeaseExpiry IS NOT NULL
          AND executorLeaseExpiry < :nowMs
        """
    )
    suspend fun getTasksWithExpiredLeases(nowMs: Long): List<AgentTask>

    /**
     * Atomically claim a task: sets executor fields only if the task is
     * currently unclaimed OR its lease has expired.
     *
     * Note: Room does not support conditional updates in a single @Query
     * for complex conditions, so [TaskLeaseManager] handles the read-check-
     * write cycle using the Kotlin-side Mutex + [update]. This query is
     * provided as a convenience for direct SQL callers / migration scripts.
     */
    @Query(
        """
        UPDATE agent_tasks
        SET executorNodeId      = :nodeId,
            executorLeasedAt    = :leasedAt,
            executorLeaseExpiry = :leaseExpiry
        WHERE taskId = :taskId
          AND (
            executorNodeId IS NULL
            OR executorLeaseExpiry IS NULL
            OR executorLeaseExpiry < :leasedAt
          )
        """
    )
    suspend fun claimTaskIfAvailable(
        taskId: String,
        nodeId: String,
        leasedAt: Long,
        leaseExpiry: Long
    )

    /**
     * Clear lease fields for a task (on completion, failure, or explicit release).
     */
    @Query(
        """
        UPDATE agent_tasks
        SET executorNodeId      = NULL,
            executorLeasedAt    = NULL,
            executorLeaseExpiry = NULL
        WHERE taskId = :taskId
        """
    )
    suspend fun clearLease(taskId: String)
}
