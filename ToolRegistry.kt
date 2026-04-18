package com.meshai.runtime

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class ToolDefinition(val name: String, val description: String)
data class ToolResult(val summary: String, val success: Boolean) {
    companion object {
        fun failure(msg: String) = ToolResult(msg, false)
        fun success(msg: String) = ToolResult(msg, true)
    }
}

typealias ToolHandler = suspend (input: String) -> ToolResult

/**
 * Central registry for all agent tools.
 * Tools are registered at startup via Hilt multi-bindings.
 */
@Singleton
class ToolRegistry @Inject constructor() {

    private val tools = mutableMapOf<String, Pair<ToolDefinition, ToolHandler>>()

    fun register(definition: ToolDefinition, handler: ToolHandler) {
        tools[definition.name] = definition to handler
        Timber.d("[ToolRegistry] Registered: ${definition.name}")
    }

    fun availableTools(): List<ToolDefinition> = tools.values.map { it.first }

    suspend fun executeTool(name: String, input: String): ToolResult {
        val (_, handler) = tools[name]
            ?: return ToolResult.failure("Unknown tool: $name")
        return handler(input)
    }
}
