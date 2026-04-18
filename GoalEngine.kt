package com.meshai.runtime

import com.meshai.ai.LlmMessage
import com.meshai.ai.LlmRole
import com.meshai.ai.ModelProvider
import com.meshai.core.model.AgentTask
import com.meshai.core.model.TaskPriority
import com.meshai.core.model.TaskType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decomposes free-text goals into typed AgentTask lists via LLM.
 *
 * Input:  "Monitor my house, alert me if motion, order groceries weekly"
 * Output: [MONITOR task, SEND_SMS task, CUSTOM task]
 */
@Singleton
class GoalEngine @Inject constructor(private val modelProvider: ModelProvider) {

    private val systemPrompt = """
You are a task decomposition engine for an autonomous AI agent.
Given a high-level goal, return ONLY a JSON array of task objects.
Each task must have: title (string), description (string),
type (SEND_SMS|ANSWER_CALL|TAKE_PHOTO|GET_LOCATION|MONITOR|RESPOND_TO_MESSAGE|LLM_REASONING|DELEGATE|CUSTOM),
priority (LOW|NORMAL|HIGH|CRITICAL).
No preamble, no markdown, ONLY the JSON array.
""".trimIndent()

    suspend fun decompose(goalText: String): List<AgentTask> {
        Timber.d("[GoalEngine] Decomposing: $goalText")
        val response = try {
            modelProvider.complete(systemPrompt, listOf(LlmMessage(LlmRole.USER, goalText)))
        } catch (e: Exception) {
            Timber.e(e, "[GoalEngine] LLM failed")
            return listOf(fallbackTask(goalText))
        }
        return try {
            Json.decodeFromString<List<RawDto>>(response.trim()).mapNotNull { dto ->
                runCatching {
                    AgentTask(
                        title       = dto.title,
                        description = dto.description,
                        type        = TaskType.valueOf(dto.type),
                        priority    = TaskPriority.valueOf(dto.priority)
                    )
                }.getOrNull()
            }.ifEmpty { listOf(fallbackTask(goalText)) }
        } catch (e: Exception) {
            Timber.w(e, "[GoalEngine] Parse failed")
            listOf(fallbackTask(goalText))
        }
    }

    private fun fallbackTask(goal: String) = AgentTask(
        title = "User Goal", description = goal, type = TaskType.CUSTOM
    )

    @Serializable
    private data class RawDto(
        val title: String,
        val description: String,
        val type: String,
        val priority: String = "NORMAL"
    )
}
