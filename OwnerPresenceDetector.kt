package com.meshai.control

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OwnerPresenceDetector — signals whether the device owner is present.
 * Used by PolicyEngine and the dashboard's agent-mode toggle.
 */
@Singleton
class OwnerPresenceDetector @Inject constructor() {
    private val _isOwnerPresent   = MutableStateFlow(true)
    private val _agentModeOverride = MutableStateFlow(false)

    val isOwnerPresent:    StateFlow<Boolean> = _isOwnerPresent
    val agentModeOverride: StateFlow<Boolean> = _agentModeOverride

    fun setOwnerPresent(present: Boolean) { _isOwnerPresent.value = present }
    fun setAgentModeOverride(override: Boolean) { _agentModeOverride.value = override }
}
