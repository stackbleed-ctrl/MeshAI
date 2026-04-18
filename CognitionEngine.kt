package com.meshai.runtime.cognition

import com.meshai.core.model.AgentNode
import com.meshai.core.model.AgentTask
import com.meshai.core.protocol.TaskResult
import com.meshai.runtime.ReActLoop
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CognitionEngine — optional AI reasoning layer.
 *
 * Wraps ReActLoop and GoalEngine.  Only invoked when a task requires
 * multi-step reasoning.  The rest of the runtime operates without it.
 *
 * Decoupling this from TaskExecutor means:
 *  - Unit tests never need a live LLM.
 *  - Devices without on-device AI can still execute deterministic tasks.
 *  - Reasoning can be upgraded independently.
 *
 * SPEC_REF: COG-001
 */
@Singleton
class CognitionEngine @Inject constructor(
    private val reActLoop: ReActLoop
) {
    /**
     * Run a ReAct reasoning cycle for the given task.
     * Returns a TaskResult with the final answer.
     */
    suspend fun reason(task: AgentTask, localNode: AgentNode): TaskResult {
        Timber.d("[CognitionEngine] Starting reasoning for '${task.title}'")
        val startMs = System.currentTimeMillis()
        return try {
            val answer = reActLoop.execute(task, localNode)
            TaskResult(
                taskId     = task.taskId,
                success    = true,
                resultText = answer,
                latencyMs  = System.currentTimeMillis() - startMs
            )
        } catch (e: Exception) {
            Timber.e(e, "[CognitionEngine] Reasoning failed for '${task.title}'")
            TaskResult(
                taskId     = task.taskId,
                success    = false,
                resultText = "Reasoning error: ${e.message}",
                latencyMs  = System.currentTimeMillis() - startMs
            )
        }
    }
}
