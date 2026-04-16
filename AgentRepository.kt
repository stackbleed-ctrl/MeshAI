package com.meshai.data.repository

import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.NodeCapability
import com.meshai.agent.NodeStatus
import com.meshai.agent.TaskStatus
import com.meshai.data.db.NodeDao
import com.meshai.data.db.NodeEntity
import com.meshai.data.db.TaskDao
import com.meshai.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for agent data (tasks and nodes).
 * Abstracts the Room database behind a clean domain API.
 */
@Singleton
class AgentRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val nodeDao: NodeDao
) {

    // -----------------------------------------------------------------------
    // Local node identity
    // -----------------------------------------------------------------------

    /** Returns the local node, creating a persistent identity if none exists */
    suspend fun getOrCreateLocalNode(): AgentNode {
        // In production, store the node ID in DataStore so it persists across installs
        // For now, we generate a stable ID from a UUID stored in shared preferences
        val nodeId = getStoredNodeId()
        val existing = nodeDao.getById(nodeId)

        if (existing != null) {
            return existing.toAgentNode()
        }

        val newNode = AgentNode(
            nodeId = nodeId,
            displayName = "MeshAI-${nodeId.take(6).uppercase()}",
            capabilities = NodeCapability.entries.toSet(),
            status = NodeStatus.IDLE
        )
        nodeDao.insert(newNode.toEntity())
        Timber.i("[Repo] Created new local node: ${newNode.displayName}")
        return newNode
    }

    suspend fun updateLocalNodeStatus(isOwnerPresent: Boolean) {
        val nodeId = getStoredNodeId()
        val status = if (isOwnerPresent) NodeStatus.IDLE else NodeStatus.AGENT_MODE
        nodeDao.updateStatus(nodeId, status.name, System.currentTimeMillis())
    }

    fun observeNodes(): Flow<List<AgentNode>> =
        nodeDao.observeAll().map { list -> list.map { it.toAgentNode() } }

    suspend fun upsertNode(node: AgentNode) = nodeDao.insert(node.toEntity())

    // -----------------------------------------------------------------------
    // Tasks
    // -----------------------------------------------------------------------

    suspend fun enqueueTask(task: AgentTask) {
        Timber.d("[Repo] Enqueueing task: ${task.title}")
        taskDao.insert(task.toEntity())
    }

    suspend fun getPendingTasks(): List<AgentTask> =
        taskDao.getPending().map { it.toAgentTask() }

    fun observeTasks(): Flow<List<AgentTask>> =
        taskDao.observeAll().map { list -> list.map { it.toAgentTask() } }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus) =
        taskDao.updateStatus(taskId, status)

    suspend fun completeTask(taskId: String, result: String) =
        taskDao.complete(taskId, System.currentTimeMillis(), result)

    suspend fun pruneOldTasks(maxAgeMs: Long = 7 * 24 * 3600 * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        taskDao.deleteCompleted(cutoff)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun getStoredNodeId(): String {
        // Simplified: use a fixed UUID constant per install
        // In production: read from DataStore (encrypted)
        return "meshai-local-${android.os.Build.FINGERPRINT.hashCode().toUInt()}"
    }
}

// -----------------------------------------------------------------------
// Mapping extensions
// -----------------------------------------------------------------------

fun AgentTask.toEntity() = TaskEntity(
    taskId = taskId,
    title = title,
    description = description,
    type = type,
    priority = priority,
    status = status,
    originNodeId = originNodeId,
    assignedNodeId = assignedNodeId,
    createdEpoch = createdEpoch,
    completedEpoch = completedEpoch,
    result = result
)

fun TaskEntity.toAgentTask() = AgentTask(
    taskId = taskId,
    title = title,
    description = description,
    type = type,
    priority = priority,
    status = status,
    originNodeId = originNodeId,
    assignedNodeId = assignedNodeId,
    createdEpoch = createdEpoch,
    completedEpoch = completedEpoch,
    result = result
)

fun AgentNode.toEntity() = NodeEntity(
    nodeId = nodeId,
    displayName = displayName,
    capabilities = Json.encodeToString(capabilities.map { it.name }),
    status = status.name,
    batteryLevel = batteryLevel,
    isOwnerPresent = isOwnerPresent,
    meshIp = meshIp,
    lastSeenEpoch = lastSeenEpoch
)

fun NodeEntity.toAgentNode() = AgentNode(
    nodeId = nodeId,
    displayName = displayName,
    capabilities = runCatching {
        Json.decodeFromString<List<String>>(capabilities)
            .mapNotNull { runCatching { NodeCapability.valueOf(it) }.getOrNull() }
            .toSet()
    }.getOrDefault(emptySet()),
    status = runCatching { NodeStatus.valueOf(status) }.getOrDefault(NodeStatus.OFFLINE),
    batteryLevel = batteryLevel,
    isOwnerPresent = isOwnerPresent,
    meshIp = meshIp,
    lastSeenEpoch = lastSeenEpoch
)
