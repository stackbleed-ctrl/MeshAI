package com.meshai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshai.agent.AgentNode
import com.meshai.agent.AgentTask
import com.meshai.agent.GoalEngine
import com.meshai.agent.OwnerPresenceDetector
import com.meshai.data.repository.AgentRepository
import com.meshai.mesh.MeshNetwork
import com.meshai.mesh.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val meshNetwork: MeshNetwork,
    private val ownerPresenceDetector: OwnerPresenceDetector,
    private val goalEngine: GoalEngine
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        agentRepository.observeNodes(),
        agentRepository.observeTasks(),
        meshNetwork.connectedNodes,
        meshNetwork.networkStatus,
        ownerPresenceDetector.isOwnerPresent
    ) { nodes, tasks, meshNodes, netStatus, isOwnerPresent ->
        DashboardUiState(
            localNodes = nodes,
            meshNodes = meshNodes,
            recentTasks = tasks.take(5),
            pendingCount = tasks.count { it.status == com.meshai.agent.TaskStatus.PENDING },
            networkStatus = netStatus,
            isOwnerPresent = isOwnerPresent,
            isAgentModeActive = !isOwnerPresent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun submitGoal(goalText: String) {
        viewModelScope.launch {
            val tasks = goalEngine.decompose(goalText)
            tasks.forEach { agentRepository.enqueueTask(it) }
        }
    }

    fun toggleAgentMode() {
        viewModelScope.launch {
            val current = ownerPresenceDetector.isOwnerPresent.value
            ownerPresenceDetector.setAgentModeOverride(current)
        }
    }
}

data class DashboardUiState(
    val localNodes: List<AgentNode> = emptyList(),
    val meshNodes: List<AgentNode> = emptyList(),
    val recentTasks: List<AgentTask> = emptyList(),
    val pendingCount: Int = 0,
    val networkStatus: NetworkStatus = NetworkStatus.OFFLINE,
    val isOwnerPresent: Boolean = true,
    val isAgentModeActive: Boolean = false
)
