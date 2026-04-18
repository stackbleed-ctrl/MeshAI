package com.meshai.runtime.cognition

import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.TaskResult
import com.meshai.runtime.ExecutionBudget
import com.meshai.runtime.ExecutionResult
import com.meshai.runtime.ReActLoop
import com.meshai.storage.AgentRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CognitionEngine v2 — optional AI reasoning layer.
 *
 * Wraps [ReActLoop] and surfaces the full [ExecutionResult] (answer + trace)
 * upward. The trace is persisted to storage so the dashboard can show per-task
 * step breakdowns, budget consumption, and validation errors.
 *
 * ## Engine/Cognition separation (Option B)
 *
 * [TaskExecutor] handles all deterministic tasks (no LLM). [CognitionEngine]
 * handles only tasks that require multi-step reasoning. This means:
 * - Unit tests for tool execution never need a live LLM.
 * - Devices without on-device AI can still execute deterministic tasks.
 * - The reasoning layer can be upgraded (e.g., swap Gemma → Gemini Nano)
 *   without touching execution infrastructure.
 *
 * [MeshRouter] calls [canReason] before routing here — it never calls
 * [reason] for task types that [TaskExecutor] handles.
 *
 * SPEC_REF: COG-001 / OPTION-B
 */
@Singleton
class CognitionEngine @Inject constructor(
    private val reActLoop: ReActLoop,
    private val agentRepository: AgentRepository
) {
    /**
     * Run a full ReAct reasoning cycle for [task].
     *
     * Budget is automatically scaled to [task.priority] by [ReActLoop].
     * Pass an explicit [budget] override for special cases (e.g., retry
     * with reduced budget after a first failure).
     *
     * The [ExecutionTrace] is persisted immediately on completion so the
     * dashboard reflects results even if the caller crashes before handling
     * the return value.
     */
    suspend fun reason(
        task: AgentTask,
        localNode: AgentNode,
        budget: ExecutionBudget? = null
    ): TaskResult {
        Timber.d("[CognitionEngine] Starting reasoning for '${task.title}'")
        val startMs = System.currentTimeMillis()

        return try {
            val execResult: ExecutionResult = if (budget != null) {
                reActLoop.execute(task, localNode, budget)
            } else {
                reActLoop.execute(task, localNode)
            }

            // Persist trace immediately — don't wait for caller
            runCatching { agentRepository.storeTrace(task.taskId, execResult.trace) }
                .onFailure { Timber.e(it, "[CognitionEngine] Trace persist failed for ${task.taskId}") }

            Timber.i("[CognitionEngine] '${task.title}' → ${execResult.trace.summary()}")

            TaskResult(
                taskId     = task.taskId,
                success    = execResult.trace.outcome == com.meshai.runtime.ExecutionTrace.Outcome.SUCCESS,
                resultText = execResult.answer,
                latencyMs  = System.currentTimeMillis() - startMs,
                costUsd    = 0.0  // local LLM — no monetary cost
            )
        } catch (e: Exception) {
            Timber.e(e, "[CognitionEngine] Reasoning threw for '${task.title}'")
            TaskResult(
                taskId     = task.taskId,
                success    = false,
                resultText = "Reasoning error: ${e.message}",
                latencyMs  = System.currentTimeMillis() - startMs
            )
        }
    }
}
