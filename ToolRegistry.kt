package com.meshai.tools

import android.content.Context
import com.meshai.tools.camera.CameraTool
import com.meshai.tools.call.CallTool
import com.meshai.tools.location.LocationTool
import com.meshai.tools.notification.NotificationTool
import com.meshai.tools.sms.SmsTool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central registry for all agent tools.
 *
 * Each tool implements [AgentTool] and is registered here.
 * The [ReActLoop] calls [executeTool] with a tool name and JSON input string.
 *
 * Tools gracefully degrade when permissions are missing — they return a
 * [ToolResult.failure] with a clear message rather than throwing.
 */
@Singleton
class ToolRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsTool: SmsTool,
    private val callTool: CallTool,
    private val notificationTool: NotificationTool,
    private val cameraTool: CameraTool,
    private val locationTool: LocationTool
) {

    private val tools: Map<String, AgentTool> by lazy {
        listOf(
            smsTool,
            callTool,
            notificationTool,
            cameraTool,
            locationTool,
            WaitTool(),
            ReasonTool()
        ).associateBy { it.name }
    }

    /** Execute a tool by name with a JSON input string */
    suspend fun executeTool(toolName: String, jsonInput: String): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult.failure("Unknown tool: '$toolName'. Available: ${tools.keys.joinToString()}")

        return try {
            Timber.d("[Tools] Executing: $toolName | input: $jsonInput")
            val result = tool.execute(jsonInput)
            Timber.d("[Tools] Result: ${result.summary}")
            result
        } catch (e: Exception) {
            Timber.e(e, "[Tools] Tool '$toolName' threw an exception")
            ToolResult.failure("Tool execution error: ${e.message}")
        }
    }

    fun availableTools(): List<AgentTool> = tools.values.toList()
}

// -----------------------------------------------------------------------
// Tool interface
// -----------------------------------------------------------------------

/**
 * Contract for all agent-callable tools.
 */
interface AgentTool {
    val name: String
    val description: String
    val inputSchema: String  // JSON Schema string for documentation/validation
    suspend fun execute(jsonInput: String): ToolResult
}

// -----------------------------------------------------------------------
// ToolResult
// -----------------------------------------------------------------------

data class ToolResult(
    val success: Boolean,
    val summary: String,
    val data: Map<String, String> = emptyMap()
) {
    companion object {
        fun success(summary: String, data: Map<String, String> = emptyMap()) =
            ToolResult(true, summary, data)

        fun failure(reason: String) =
            ToolResult(false, "FAILED: $reason")
    }
}

// -----------------------------------------------------------------------
// Built-in utility tools
// -----------------------------------------------------------------------

/** Tool that pauses execution for a specified duration */
class WaitTool : AgentTool {
    override val name = "wait"
    override val description = "Wait for a specified number of seconds before continuing"
    override val inputSchema = """{"seconds": "number (1-60)"}"""

    override suspend fun execute(jsonInput: String): ToolResult {
        return try {
            val seconds = Json.parseToJsonElement(jsonInput)
                .let { it as? kotlinx.serialization.json.JsonObject }
                ?.get("seconds")
                ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                ?.content?.toLongOrNull() ?: 5L
            val clamped = seconds.coerceIn(1L, 60L)
            kotlinx.coroutines.delay(clamped * 1000L)
            ToolResult.success("Waited ${clamped}s")
        } catch (e: Exception) {
            ToolResult.failure("Wait failed: ${e.message}")
        }
    }
}

/** Tool that lets the agent record a reasoning step without calling an external service */
class ReasonTool : AgentTool {
    override val name = "reason"
    override val description = "Record a reasoning or planning step without calling any external service"
    override val inputSchema = """{"thought": "string"}"""

    override suspend fun execute(jsonInput: String): ToolResult {
        return ToolResult.success("Reasoning recorded.")
    }
}
