package com.meshai.runtime

import timber.log.Timber

// ============================================================================
// ExecutionBudget
// ============================================================================

/**
 * Per-run token ceiling with priority scaling.
 *
 * Tracks estimated token cost across a single [ReActLoop] run and enforces
 * a hard ceiling so runaway tasks don't burn 12 full LLM calls.
 *
 * Token counts are estimated at ~4 chars/token (standard English approximation).
 * Not exact, but consistent — correctness requires hitting the ceiling *before*
 * context overflow, not perfect accounting.
 *
 * SPEC_REF: EXEC-002 / OPTION-B
 */
class ExecutionBudget(val maxTokens: Int = DEFAULT_MAX_TOKENS) {

    companion object {
        const val DEFAULT_MAX_TOKENS = 6_000
        private const val CHARS_PER_TOKEN = 4
    }

    private var tokensSpent: Int = 0

    val remaining: Int          get() = (maxTokens - tokensSpent).coerceAtLeast(0)
    val isExhausted: Boolean    get() = tokensSpent >= maxTokens
    val fractionUsed: Float     get() = tokensSpent.toFloat() / maxTokens

    fun charge(promptText: String, responseText: String): Int {
        val cost = estimate(promptText) + estimate(responseText)
        tokensSpent += cost
        Timber.d("[Budget] +$cost tokens (total $tokensSpent/$maxTokens)")
        return cost
    }

    fun chargeTool(toolInput: String, toolOutput: String): Int {
        val cost = estimate(toolInput) + estimate(toolOutput)
        tokensSpent += cost
        return cost
    }

    fun snapshot() = BudgetSnapshot(tokensSpent, maxTokens, remaining, fractionUsed)

    private fun estimate(text: String): Int = (text.length / CHARS_PER_TOKEN).coerceAtLeast(1)
}

data class BudgetSnapshot(
    val tokensSpent: Int,
    val maxTokens: Int,
    val remaining: Int,
    val fractionUsed: Float
)

// ============================================================================
// ExecutionTrace
// ============================================================================

/**
 * Structured record of a complete [ReActLoop] run.
 *
 * Stored alongside the [AgentTask] in Room and exposed to the dashboard
 * for per-task debugging: which tools were called, where budget went,
 * which step produced a validation error, total wall time.
 *
 * SPEC_REF: EXEC-003 / OPTION-B
 */
data class ExecutionTrace(
    val taskId: String,
    val taskTitle: String,
    val steps: List<StepTrace>,
    val finalResult: String,
    val outcome: Outcome,
    val budget: BudgetSnapshot,
    val totalElapsedMs: Long
) {
    enum class Outcome {
        SUCCESS,
        BUDGET_EXCEEDED,
        MAX_STEPS,
        ERROR
    }

    val stepCount: Int get() = steps.size

    fun summary(): String = buildString {
        append("'$taskTitle': ${outcome.name} ")
        append("| ${stepCount} steps | ${budget.tokensSpent} tokens | ${totalElapsedMs}ms")
    }
}

/**
 * Record of a single ReAct iteration.
 */
data class StepTrace(
    val stepNumber: Int,
    val type: StepType,
    val llmResponseSnippet: String,
    val toolName: String?          = null,
    val toolInputSnippet: String?  = null,
    val toolResultSnippet: String? = null,
    val tokensCost: Int,
    val elapsedMs: Long,
    val validationError: String?   = null
) {
    enum class StepType {
        TOOL_CALL,
        THOUGHT_ONLY,
        FINAL_ANSWER,
        BUDGET_STOP,
        PARSE_ERROR
    }
}

/**
 * Return value of [ReActLoop.execute].
 * [answer] goes to [AgentRepository.completeTask].
 * [trace] goes to [AgentRepository.storeTrace] and the dashboard.
 */
data class ExecutionResult(
    val answer: String,
    val trace: ExecutionTrace
)

// ============================================================================
// ToolOutputValidator
// ============================================================================

/**
 * Validates and sanitises tool output before it enters the LLM context.
 *
 * If a tool returns malformed JSON, the LLM reasons over garbage and produces
 * confident-looking nonsense. This validator intercepts that and replaces bad
 * output with a structured `[Tool output error]` observation that the LLM can
 * reason about correctly.
 *
 * SPEC_REF: EXEC-004 / OPTION-B
 */
class ToolOutputValidator {

    companion object {
        private const val MAX_OUTPUT_CHARS = 3_000
        private const val INVALID_PREFIX = "[Tool output error]"
    }

    fun validate(
        toolName: String,
        rawOutput: String,
        requiredFields: List<String> = emptyList()
    ): ValidationResult {
        if (rawOutput.isBlank()) {
            return ValidationResult.Invalid(
                reason    = "Empty output",
                sanitized = "$INVALID_PREFIX Tool '$toolName' returned no output."
            )
        }

        val truncated = if (rawOutput.length > MAX_OUTPUT_CHARS) {
            Timber.w("[Validator] $toolName output truncated (${rawOutput.length} chars)")
            rawOutput.take(MAX_OUTPUT_CHARS) + "\n[...truncated]"
        } else rawOutput

        val trimmed = truncated.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonError = tryParseJson(trimmed)
            if (jsonError != null) {
                return ValidationResult.Invalid(
                    reason    = "Malformed JSON: $jsonError",
                    sanitized = "$INVALID_PREFIX Tool '$toolName' returned malformed JSON. Snippet: ${trimmed.take(80)}"
                )
            }
            if (requiredFields.isNotEmpty() && trimmed.startsWith("{")) {
                val missing = checkRequired(trimmed, requiredFields)
                if (missing.isNotEmpty()) {
                    return ValidationResult.Invalid(
                        reason    = "Missing fields: $missing",
                        sanitized = "$INVALID_PREFIX Tool '$toolName' output missing: ${missing.joinToString()}."
                    )
                }
            }
        }
        return ValidationResult.Ok(sanitized = truncated)
    }

    private fun tryParseJson(json: String): String? = runCatching {
        if (json.startsWith("{")) org.json.JSONObject(json)
        else org.json.JSONArray(json)
        null
    }.getOrElse { it.message?.take(100) }

    private fun checkRequired(json: String, required: List<String>): List<String> = runCatching {
        val obj = org.json.JSONObject(json)
        required.filter { !obj.has(it) }
    }.getOrDefault(required)
}

sealed class ValidationResult {
    data class Ok(val sanitized: String) : ValidationResult()
    data class Invalid(val reason: String, val sanitized: String) : ValidationResult()
}
