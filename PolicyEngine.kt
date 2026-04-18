package com.meshai.control

import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskOrigin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PolicyEngine — safety and cost gating for all task executions.
 *
 * Rules (SPEC_REF: SAFETY-002):
 *  - Remote tasks from untrusted origins are rejected unless ownerApproved.
 *  - Tasks exceeding the global cost ceiling are rejected.
 *  - Future: adaptive rules driven by TelemetryCollector stats.
 */
@Singleton
class PolicyEngine @Inject constructor() {
    private val globalMaxCostUsd = 0.10

    fun allow(task: AgentTask): Boolean {
        if (task.constraints.maxCostUsd > globalMaxCostUsd) return false
        if (task.origin == TaskOrigin.REMOTE && !task.ownerApproved) return false
        return true
    }
}
