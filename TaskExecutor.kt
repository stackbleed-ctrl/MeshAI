package com.meshai.runtime.execution

import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskType
import com.meshai.core.protocol.TaskResult
import com.meshai.runtime.ToolRegistry
import com.meshai.runtime.ToolResult
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskExecutor — deterministic execution layer.
 *
 * Handles ALL tasks that do NOT require LLM reasoning.
 * This layer is fast, predictable, and testable without mocking an LLM.
 *
 * Only tasks of type LLM_REASONING are passed upward to the cognition layer.
 *
 * SPEC_REF: EXEC-001
 */
@Singleton
class TaskExecutor @Inject constructor(
    private val toolRegistry: ToolRegistry
) {
    /**
     * Returns true if this executor can handle the task without LLM involvement.
     */
    fun canHandle(task: AgentTask): Boolean =
        task.type != TaskType.LLM_REASONING && task.type != TaskType.DELEGATE

    /**
     * Execute a task directly via the ToolRegistry.
     * Input JSON is synthesised from the task's description field.
     */
    suspend fun execute(task: AgentTask): TaskResult {
        Timber.d("[TaskExecutor] Executing ${task.type} task '${task.title}'")
        val startMs = System.currentTimeMillis()

        val toolName = toolNameFor(task.type)
        val result: ToolResult = if (toolName != null) {
            toolRegistry.executeTool(toolName, """{"description":"${task.description}"}""")
        } else {
            ToolResult.failure("No direct tool mapping for task type ${task.type}")
        }

        val latencyMs = System.currentTimeMillis() - startMs
        Timber.i("[TaskExecutor] ${task.type} '${task.title}' → success=${result.success} (${latencyMs}ms)")

        return TaskResult(
            taskId    = task.taskId,
            success   = result.success,
            resultText = result.summary,
            latencyMs  = latencyMs
        )
    }

    private fun toolNameFor(type: TaskType): String? = when (type) {
        TaskType.SEND_SMS            -> "send_sms"
        TaskType.TAKE_PHOTO          -> "take_photo"
        TaskType.GET_LOCATION        -> "get_location"
        TaskType.MONITOR             -> "monitor"
        TaskType.RESPOND_TO_MESSAGE  -> "respond_to_message"
        TaskType.ANSWER_CALL         -> "answer_call"
        TaskType.CUSTOM              -> "custom_action"
        else                         -> null
    }
}
