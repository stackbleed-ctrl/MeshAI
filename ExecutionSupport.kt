package com.meshai.agent

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

// ═══════════════════════════════════════════════════════════════════════════
// ExecutionBudget
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Per-run token ceiling with priority scaling.
 * Token counts estimated at ~4 chars/token. Not exact — correct enough to
 * catch runaway tasks before context overflow, which is all that matters.
 */
class ExecutionBudget(val maxTokens: Int = DEFAULT_MAX_TOKENS) {

    companion object {
        const val DEFAULT_MAX_TOKENS = 6_000
        private const val CHARS_PER_TOKEN = 4
    }

    private var tokensSpent: Int = 0

    val remaining: Int       get() = (maxTokens - tokensSpent).coerceAtLeast(0)
    val isExhausted: Boolean get() = tokensSpent >= maxTokens
    val fractionUsed: Float  get() = tokensSpent.toFloat() / maxTokens

    fun charge(promptText: String, responseText: String): Int {
        val cost = estimate(promptText) + estimate(responseText)
        tokensSpent += cost
        Timber.d("[Budget] +$cost tokens ($tokensSpent/$maxTokens)")
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

// ═══════════════════════════════════════════════════════════════════════════
// ExecutionTrace + StepTrace
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Structured record of a complete ReActLoop run.
 * Stored alongside the AgentTask in Room and surfaced in the dashboard.
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
    enum class Outcome { SUCCESS, BUDGET_EXCEEDED, MAX_STEPS, ERROR }

    val stepCount: Int get() = steps.size

    fun summary(): String = "'$taskTitle': ${outcome.name} | ${stepCount} steps | ${budget.tokensSpent} tokens | ${totalElapsedMs}ms"
}

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
    enum class StepType { TOOL_CALL, THOUGHT_ONLY, FINAL_ANSWER, BUDGET_STOP, PARSE_ERROR }
}

/** Return value of ReActLoop.execute. */
data class ExecutionResult(val answer: String, val trace: ExecutionTrace)

// ═══════════════════════════════════════════════════════════════════════════
// ToolOutputValidator
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Validates tool output before it enters the LLM context.
 * Malformed JSON → structured error observation the LLM can reason about.
 */
class ToolOutputValidator {

    companion object {
        private const val MAX_OUTPUT_CHARS = 3_000
        private const val INVALID_PREFIX   = "[Tool output error]"
    }

    fun validate(
        toolName: String,
        rawOutput: String,
        requiredFields: List<String> = emptyList()
    ): ValidationResult {
        if (rawOutput.isBlank()) {
            return ValidationResult.Invalid("Empty output",
                "$INVALID_PREFIX Tool '$toolName' returned no output.")
        }

        val truncated = if (rawOutput.length > MAX_OUTPUT_CHARS) {
            Timber.w("[Validator] $toolName output truncated")
            rawOutput.take(MAX_OUTPUT_CHARS) + "\n[...truncated]"
        } else rawOutput

        val trimmed = truncated.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonError = tryParseJson(trimmed)
            if (jsonError != null) {
                return ValidationResult.Invalid("Malformed JSON: $jsonError",
                    "$INVALID_PREFIX Tool '$toolName' returned malformed JSON. Snippet: ${trimmed.take(80)}")
            }
            if (requiredFields.isNotEmpty() && trimmed.startsWith("{")) {
                val missing = checkRequired(trimmed, requiredFields)
                if (missing.isNotEmpty()) {
                    return ValidationResult.Invalid("Missing fields: $missing",
                        "$INVALID_PREFIX Tool '$toolName' missing: ${missing.joinToString()}.")
                }
            }
        }
        return ValidationResult.Ok(sanitized = truncated)
    }

    private fun tryParseJson(json: String): String? = runCatching {
        if (json.startsWith("{")) JSONObject(json) else JSONArray(json); null
    }.getOrElse { it.message?.take(100) }

    private fun checkRequired(json: String, required: List<String>): List<String> = runCatching {
        val obj = JSONObject(json); required.filter { !obj.has(it) }
    }.getOrDefault(required)
}

sealed class ValidationResult {
    data class Ok(val sanitized: String) : ValidationResult()
    data class Invalid(val reason: String, val sanitized: String) : ValidationResult()
}
