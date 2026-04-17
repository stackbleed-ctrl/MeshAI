package com.meshai.agent

import com.meshai.data.dao.AgentTaskDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages distributed task execution ownership via optimistic leases.
 *
 * ## Problem
 *
 * In a mesh of N nodes, the same [AgentTask] may be replicated to multiple
 * nodes via gossip. When Node A goes offline temporarily and Node B claims
 * the task, there is a window where both execute it if Node A comes back
 * before the lease expires. [TaskLeaseManager] prevents this with a
 * Room-persisted lease that any node can inspect.
 *
 * ## Lease protocol
 *
 * 1. Before executing a task, a node calls [claimTask].
 * 2. [claimTask] reads the current task state. If another node holds a
 *    valid (non-expired) lease, it returns false and the caller backs off.
 * 3. If unclaimed or lease expired, the task is atomically updated with
 *    the claimer's nodeId and a [AgentTask.LEASE_DURATION_MS] expiry.
 * 4. On task completion or failure, the executor calls [releaseTask] to
 *    clear the lease, allowing garbage collection.
 * 5. WorkManager's periodic [LeaseRenewalWorker] should call [renewLease]
 *    every ~2 minutes for tasks still in progress to prevent expiry mid-task.
 *
 * ## Limitations
 *
 * This is a **single-node-atomic** solution: the claim is a single Room
 * update on the local DB. On a true distributed system you would need
 * consensus (Raft, Paxos). For a mesh of phone-scale nodes where the
 * alternative is occasional duplicate execution of a benign task, optimistic
 * leases are the appropriate trade-off.
 *
 * Wall-clock divergence between nodes (typically ±2 s in the same room)
 * means the effective deduplication window is [LEASE_DURATION_MS] - 4 s.
 * At 5 minutes this is well within acceptable bounds.
 */
@Singleton
class TaskLeaseManager @Inject constructor(
    private val taskDao: AgentTaskDao
) {

    /**
     * Local mutex to serialise concurrent [claimTask] calls within this
     * process. Prevents two coroutines on the same node racing to claim
     * different tasks from the same queue snapshot.
     */
    private val claimMutex = Mutex()

    /**
     * Attempt to claim [taskId] for execution by [claimingNodeId].
     *
     * @param taskId        The task to claim.
     * @param claimingNodeId The nodeId of the node requesting execution.
     * @return true if the claim was granted; false if another node holds
     *         a valid lease and the caller should skip this task.
     */
    suspend fun claimTask(taskId: String, claimingNodeId: String): Boolean {
        claimMutex.withLock {
            val task = taskDao.getById(taskId)
            if (task == null) {
                Timber.w("[Lease] Task $taskId not found — cannot claim")
                return false
            }

            if (task.isClaimedBy(claimingNodeId)) {
                Timber.d(
                    "[Lease] Task $taskId already claimed by ${task.executorNodeId} " +
                    "(expires ${task.executorLeaseExpiry})"
                )
                return false
            }

            val claimed = task.withLease(claimingNodeId)
            taskDao.update(claimed)
            Timber.i("[Lease] Task $taskId claimed by $claimingNodeId")
            return true
        }
    }

    /**
     * Renew the lease on [taskId] for [claimingNodeId].
     * Call this every ~2 minutes for tasks that are still executing.
     *
     * @return true if the lease was successfully renewed; false if the
     *         task no longer exists or is now owned by a different node.
     */
    suspend fun renewLease(taskId: String, claimingNodeId: String): Boolean {
        val task = taskDao.getById(taskId) ?: return false
        if (task.executorNodeId != claimingNodeId) {
            Timber.w("[Lease] Lease renewal rejected for $taskId — owned by ${task.executorNodeId}")
            return false
        }
        taskDao.update(task.withLease(claimingNodeId))
        Timber.d("[Lease] Lease renewed for task $taskId by $claimingNodeId")
        return true
    }

    /**
     * Release the lease on [taskId] after completion or failure.
     * Clears executor fields so the task can be claimed again if needed
     * (e.g., for retry after failure).
     */
    suspend fun releaseTask(taskId: String, claimingNodeId: String) {
        val task = taskDao.getById(taskId) ?: return
        if (task.executorNodeId != claimingNodeId) {
            Timber.w("[Lease] Release ignored for $taskId — not owned by $claimingNodeId")
            return
        }
        taskDao.update(task.withLeaseClear())
        Timber.d("[Lease] Lease released for task $taskId")
    }

    /**
     * Returns all tasks whose leases have expired and whose status is still
     * IN_PROGRESS. These are candidates for re-claiming after a node crash.
     */
    suspend fun findExpiredLeases(): List<AgentTask> {
        val now = System.currentTimeMillis()
        return taskDao.getTasksWithExpiredLeases(now)
    }
}
