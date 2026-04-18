package com.meshai.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshai.control.OwnerPresenceDetector
import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.model.CapabilityRegistry
import com.meshai.runtime.GoalEngine
import com.meshai.runtime.MeshRouter
import com.meshai.runtime.TelemetryCollector
import com.meshai.runtime.TelemetryStats
import com.meshai.storage.AgentRepository
import com.meshai.transport.TransportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val nodes: List<AgentNode>         = emptyList(),
    val tasks: List<AgentTask>         = emptyList(),
    val isOwnerPresent: Boolean        = true,
    val agentModeOverride: Boolean     = false,
    val connectedPeers: Int            = 0,
    val knownCapabilityNodes: Int      = 0,
    val telemetry: TelemetryStats      = TelemetryStats(),
    val activeTransport: String        = "NONE",
    val isLoading: Boolean             = false,
    val error: String?                 = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val ownerPresenceDetector: OwnerPresenceDetector,
    private val transportManager: TransportManager,
    private val goalEngine: GoalEngine,
    private val meshRouter: MeshRouter,
    private val telemetryCollector: TelemetryCollector,
    private val capabilityRegistry: CapabilityRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Core data flows
        viewModelScope.launch {
            combine(
                agentRepository.observeNodes(),
                agentRepository.observeTasks(),
                ownerPresenceDetector.isOwnerPresent,
                ownerPresenceDetector.agentModeOverride
            ) { nodes, tasks, ownerPresent, agentMode ->
                _uiState.update {
                    it.copy(
                        nodes             = nodes,
                        tasks             = tasks,
                        isOwnerPresent    = ownerPresent,
                        agentModeOverride = agentMode,
                        connectedPeers    = transportManager.totalPeers()
                    )
                }
            }.collect()
        }

        // Live telemetry updates
        viewModelScope.launch {
            telemetryCollector.events.collect { _ ->
                _uiState.update {
                    it.copy(
                        telemetry       = telemetryCollector.stats(),
                        activeTransport = transportManager.activeTransportName(),
                        knownCapabilityNodes = capabilityRegistry.liveNodeCount()
                    )
                }
            }
        }

        // Capability registry changes
        viewModelScope.launch {
            capabilityRegistry.registry.collect { reg ->
                _uiState.update { it.copy(knownCapabilityNodes = reg.size) }
            }
        }
    }

    fun submitGoal(goalText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tasks = goalEngine.decompose(goalText)
                tasks.forEach { task ->
                    agentRepository.upsertTask(task)
                    val localNode = agentRepository.getLocalNode() ?: return@forEach
                    meshRouter.route(task, localNode)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleAgentMode(enabled: Boolean) {
        viewModelScope.launch { ownerPresenceDetector.setAgentModeOverride(enabled) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
