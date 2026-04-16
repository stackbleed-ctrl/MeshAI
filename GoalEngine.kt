package com.meshai.agent

import com.meshai.llm.LlmEngine
import com.meshai.llm.LlmMessage
import com.meshai.llm.LlmRole
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decomposes high-level user goals into a list of [AgentTask]s.
 *
 * Example input:
 *   "Monitor my house, alert me if motion detected, order groceries if low"
 *
 * Example output:
 *   - Task: Monitor camera feed (type: MONITOR)
 *   - Task: Send SMS alert on motion detection (type: SEND_SMS)
 *   - Task: Check grocery inventory daily (type: MONITOR)
 *   - Task: Order groceries if below threshold (type: CUSTOM)
 */
@Singleton
class GoalEngine @Inject constructor(
    private val llmEngine: LlmEngine
) {

    companion object {
        private val SYSTEM_PROMPT = """
            You are a task decomposition engine for an autonomous AI agent system.
            
            Given a high-level user goal, decompose it into specific, actionable subtasks.
            Each subtask must be concrete, executable by one of these task types:
            SEND_SMS, ANSWER_CALL, TAKE_PHOTO, GET_LOCATION, MONITOR, RESPOND_TO_MESSAGE, LLM_REASONING, DELEGATE, CUSTOM
            
            Return ONLY a JSON array of task objects. Each task object must have:
            - title: string (short name)
            - description: string (what to do)
            - type: string (one of the task types above)
            - priority: string (LOW, NORMAL, HIGH, CRITICAL)
            
            Example:
            [
              {"title": "Monitor motion", "description": "Continuously check camera for motion", "type": "MONITOR", "priority": "HIGH"},
              {"title": "Alert on motion", "description": "Send SMS to owner when motion is detected", "type": "SEND_SMS", "priority": "HIGH"}
            ]
            
            Return ONLY the JSON array. No preamble, no explanation.
        """.trimIndent()
    }

    /**
     * Decompose [goalText] into a list of tasks.
     * Falls back to a single CUSTOM task if the LLM fails to parse.
     */
    suspend fun decompose(goalText: String): List<AgentTask> {
        Timber.d("[GoalEngine] Decomposing goal: $goalText")

        val response = try {
            llmEngine.complete(
                systemPrompt = SYSTEM_PROMPT,
                messages = listOf(LlmMessage(LlmRole.USER, goalText))
            )
        } catch (e: Exception) {
            Timber.e(e, "[GoalEngine] LLM failed during decomposition")
            return listOf(fallbackTask(goalText))
        }

        return try {
            val rawList = Json.decodeFromString<List<RawTaskDto>>(response.trim())
            rawList.mapNotNull { dto ->
                runCatching {
                    AgentTask(
                        title = dto.title,
                        description = dto.description,
                        type = TaskType.valueOf(dto.type),
                        priority = TaskPriority.valueOf(dto.priority)
                    )
                }.getOrNull()
            }.ifEmpty { listOf(fallbackTask(goalText)) }
        } catch (e: Exception) {
            Timber.w(e, "[GoalEngine] Failed to parse task list from LLM response")
            listOf(fallbackTask(goalText))
        }
    }

    private fun fallbackTask(goal: String) = AgentTask(
        title = "User Goal",
        description = goal,
        type = TaskType.CUSTOM,
        priority = TaskPriority.NORMAL
    )

    @kotlinx.serialization.Serializable
    private data class RawTaskDto(
        val title: String,
        val description: String,
        val type: String,
        val priority: String = "NORMAL"
    )
}
