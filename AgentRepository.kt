package com.meshai.storage

import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.MeshEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AgentRepository — in-process storage (backed by Room in production).
 * Extended to persist MeshEvents for TelemetryCollector.
 */
@Singleton
class AgentRepository @Inject constructor() {
    private val _nodes  = MutableStateFlow<List<AgentNode>>(emptyList())
    private val _tasks  = MutableStateFlow<List<AgentTask>>(emptyList())
    private val _events = MutableStateFlow<List<MeshEvent>>(emptyList())

    fun observeNodes(): Flow<List<AgentNode>> = _nodes.asStateFlow()
    fun observeTasks(): Flow<List<AgentTask>> = _tasks.asStateFlow()
    fun observeEvents(): Flow<List<MeshEvent>> = _events.asStateFlow()

    fun upsertTask(task: AgentTask) {
        _tasks.update { list -> list.filter { it.taskId != task.taskId } + task }
    }
    fun upsertNode(node: AgentNode) {
        _nodes.update { list -> list.filter { it.nodeId != node.nodeId } + node }
    }
    fun upsertEvent(event: MeshEvent) {
        _events.update { list -> (list + event).takeLast(500) }
    }
    suspend fun getTask(taskId: String): AgentTask? =
        _tasks.value.firstOrNull { it.taskId == taskId }
    suspend fun getLocalNode(): AgentNode? = _nodes.value.firstOrNull()
}
