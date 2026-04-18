package com.meshai.runtime

import com.meshai.control.PolicyEngine
import com.meshai.core.model.*
import com.meshai.core.protocol.*
import com.meshai.runtime.cognition.CognitionEngine
import com.meshai.runtime.execution.TaskExecutor
import com.meshai.transport.TransportManager
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshRouter v2 — capability-aware, split-runtime task router.
 *
 * Routing decision tree:
 *  1. PolicyEngine gate — reject if cost/safety violated.
 *  2. CapabilityRegistry query — is there a better remote node?
 *     a. Remote node available + within constraints → delegate via TransportManager.
 *     b. No remote node → run locally.
 *  3. Local execution path:
 *     a. Deterministic task (non-LLM) → TaskExecutor.
 *     b. Reasoning task (LLM_REASONING) → CognitionEngine.
 *
 * SPEC_REF: ROUTE-001
 */
@Singleton
class MeshRouter @Inject constructor(
    private val taskExecutor: TaskExecutor,
    private val cognitionEngine: CognitionEngine,
    private val transportManager: TransportManager,
    private val capabilityRegistry: CapabilityRegistry,
    private val policyEngine: PolicyEngine,
    private val telemetryCollector: TelemetryCollector
) {
    suspend fun route(task: AgentTask, localNode: AgentNode): MeshMessage {
        // ── 1. Policy gate ────────────────────────────────────────────────
        if (!policyEngine.allow(task)) {
            Timber.w("[MeshRouter] Task '${task.title}' blocked by policy")
            telemetryCollector.record(
                MeshEvent(task.taskId, localNode.nodeId, 0L, 0.0, false, transportLayer = "POLICY_DENIED")
            )
            return rejectedMessage(task, localNode, "Policy denied: constraint violated")
        }

        val constraints = TaskConstraints(
            maxCostUsd   = task.constraints.maxCostUsd,
            maxLatencyMs = task.constraints.maxLatencyMs.toInt()
        )

        // ── 2. Capability routing ─────────────────────────────────────────
        val requiredCap = capabilityNameFor(task)
        val remoteCap   = capabilityRegistry.bestNode(
            capabilityName = requiredCap,
            excludeNodeId  = localNode.nodeId,
            constraints    = constraints
        )

        return if (remoteCap != null && shouldDelegate(task, localNode)) {
            // ── 2a. Delegate to remote node ───────────────────────────────
            Timber.d("[MeshRouter] Delegating '${task.title}' to ${remoteCap.deviceId} via ${remoteCap.name}")
            val envelope = buildDelegateEnvelope(task, localNode, remoteCap)
            val result   = transportManager.sendEnvelope(envelope)
            telemetryCollector.record(
                MeshEvent(
                    taskId        = task.taskId,
                    nodeId        = localNode.nodeId,
                    latencyMs     = result.latencyMs,
                    costUsd       = remoteCap.costUsd,
                    success       = result.success,
                    transportLayer = transportManager.activeTransportName(),
                    hopCount      = 1
                )
            )
            meshMessageFrom(task, localNode, result)
        } else {
            // ── 3. Execute locally ────────────────────────────────────────
            Timber.d("[MeshRouter] Running '${task.title}' locally on ${localNode.displayName}")
            val result = if (taskExecutor.canHandle(task)) {
                taskExecutor.execute(task)
            } else {
                cognitionEngine.reason(task, localNode)
            }
            telemetryCollector.record(
                MeshEvent(
                    taskId        = task.taskId,
                    nodeId        = localNode.nodeId,
                    latencyMs     = result.latencyMs,
                    costUsd       = 0.0,
                    success       = result.success,
                    transportLayer = "LOCAL"
                )
            )
            meshMessageFrom(task, localNode, result)
        }
    }

    /**
     * Delegate only when local battery is critical OR node lacks capability.
     * AGENT_MODE nodes always try to execute locally first.
     */
    private fun shouldDelegate(task: AgentTask, node: AgentNode): Boolean {
        if (node.status == NodeStatus.AGENT_MODE) return false
        if (node.batteryLevel < 10) return true
        return false // default: execute locally if possible
    }

    private fun capabilityNameFor(task: AgentTask): String =
        task.type.name   // NodeCapability enum names match TaskType names

    private fun buildDelegateEnvelope(
        task: AgentTask,
        localNode: AgentNode,
        remoteCap: Capability
    ): MeshEnvelope = MeshEnvelope.Companion.wrap(
        type              = EnvelopeType.TASK_DELEGATE,
        payload           = task,
        originNodeId      = localNode.nodeId,
        destinationNodeId = remoteCap.deviceId
    )

    private fun meshMessageFrom(
        task: AgentTask,
        node: AgentNode,
        result: TaskResult
    ) = MeshMessage(
        messageId       = UUID.randomUUID().toString(),
        taskId          = task.taskId,
        senderNodeId    = node.nodeId,
        recipientNodeId = null,
        payload         = result.resultText,
        status          = if (result.success) MessageStatus.DELIVERED else MessageStatus.FAILED,
        costUsd         = result.costUsd,
        latencyMs       = result.latencyMs
    )

    private fun rejectedMessage(task: AgentTask, node: AgentNode, reason: String) =
        MeshMessage(
            messageId       = UUID.randomUUID().toString(),
            taskId          = task.taskId,
            senderNodeId    = node.nodeId,
            recipientNodeId = null,
            payload         = reason,
            status          = MessageStatus.FAILED
        )
}
