package com.meshai.tools

import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Validates tool output before it is inserted into the LLM history as an
 * Observation message.
 *
 * ## Why this exists
 *
 * [ToolRegistry.executeTool] returns a [ToolResult] whose [ToolResult.summary]
 * is a raw string — often JSON. If a tool misbehaves and returns malformed
 * output, the current loop inserts it verbatim into the LLM context.
 * The LLM then reasons over garbage, producing confident-sounding nonsense
 * that looks like a valid result.
 *
 * This validator intercepts tool output and:
 * 1. Checks that JSON-shaped output is actually parseable.
 * 2. Checks that required fields declared in [ToolSpec.outputFields] are present.
 * 3. Truncates overlong output to prevent context overflow from a single tool result.
 * 4. Returns a [ValidationResult] the loop can act on — either use the output,
 *    substitute a structured error observation, or mark the step as a parse error.
 *
 * ## Integration
 *
 * Call [validate] in ReActLoop after [ToolRegistry.executeTool] returns,
 * before building the Observation message:
 *
 * ```kotlin
 * val toolResult = toolRegistry.executeTool(toolName, toolInput)
 * val validation = toolOutputValidator.validate(toolName, toolResult.summary)
 * val observationText = when (validation) {
 *     is ValidationResult.Ok      -> "Observation: ${validation.sanitized}"
 *     is ValidationResult.Invalid -> "Observation: Tool returned invalid output. ${validation.reason}"
 * }
 * ```
 */
class ToolOutputValidator {

    companion object {
        /**
         * Maximum character length of a single tool result before truncation.
         * ~750 tokens at 4 chars/token — enough detail without overwhelming context.
         */
        private const val MAX_OUTPUT_CHARS = 3_000

        /**
         * Marker prefix used to signal to the LLM that an observation was
         * sanitised. Keeps the LLM aware something went wrong without
         * exposing raw error internals.
         */
        private const val INVALID_PREFIX = "[Tool output error]"
    }

    /**
     * Validate and sanitise [rawOutput] from tool [toolName].
     *
     * @param toolName  The name of the tool that produced this output,
     *                  used for log attribution.
     * @param rawOutput The raw string result from the tool.
     * @param spec      Optional [ToolOutputSpec] declaring required JSON
     *                  fields. If null, only JSON parseability and length
     *                  are checked.
     */
    fun validate(
        toolName: String,
        rawOutput: String,
        spec: ToolOutputSpec? = null
    ): ValidationResult {

        if (rawOutput.isBlank()) {
            Timber.w("[Validator] $toolName returned blank output")
            return ValidationResult.Invalid(
                reason = "Tool returned empty output",
                sanitized = "$INVALID_PREFIX Tool '$toolName' returned no output."
            )
        }

        // Truncate overlong outputs before any other check
        val truncated = if (rawOutput.length > MAX_OUTPUT_CHARS) {
            Timber.w("[Validator] $toolName output truncated (${rawOutput.length} → $MAX_OUTPUT_CHARS chars)")
            rawOutput.take(MAX_OUTPUT_CHARS) + "\n[... output truncated at $MAX_OUTPUT_CHARS chars]"
        } else {
            rawOutput
        }

        // If the output looks like JSON, validate it
        val trimmed = truncated.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val jsonError = tryParseJson(trimmed)
            if (jsonError != null) {
                Timber.w("[Validator] $toolName returned malformed JSON: $jsonError")
                return ValidationResult.Invalid(
                    reason = "Malformed JSON: $jsonError",
                    sanitized = "$INVALID_PREFIX Tool '$toolName' returned malformed JSON. Raw snippet: ${trimmed.take(100)}"
                )
            }

            // Check required fields if a spec was provided
            if (spec != null && trimmed.startsWith("{")) {
                val missingFields = checkRequiredFields(trimmed, spec.requiredFields)
                if (missingFields.isNotEmpty()) {
                    Timber.w("[Validator] $toolName missing required fields: $missingFields")
                    return ValidationResult.Invalid(
                        reason = "Missing required fields: $missingFields",
                        sanitized = "$INVALID_PREFIX Tool '$toolName' output is missing: ${missingFields.joinToString()}."
                    )
                }
            }
        }

        return ValidationResult.Ok(sanitized = truncated)
    }

    private fun tryParseJson(json: String): String? {
        return try {
            if (json.startsWith("{")) JSONObject(json)
            else org.json.JSONArray(json)
            null // No error
        } catch (e: JSONException) {
            e.message?.take(120)
        }
    }

    private fun checkRequiredFields(json: String, required: List<String>): List<String> {
        return try {
            val obj = JSONObject(json)
            required.filter { !obj.has(it) }
        } catch (e: JSONException) {
            required // If we can't parse, all required fields are "missing"
        }
    }
}

/**
 * Result of a [ToolOutputValidator.validate] call.
 */
sealed class ValidationResult {
    /** Output passed validation. Use [sanitized] as the Observation text. */
    data class Ok(val sanitized: String) : ValidationResult()

    /**
     * Output failed validation. [sanitized] is a safe error message to
     * insert as the Observation so the LLM knows the tool failed.
     * [reason] is for internal logging / [StepTrace.validationError].
     */
    data class Invalid(val reason: String, val sanitized: String) : ValidationResult()
}

/**
 * Optional schema declaration for a tool's expected output shape.
 * Register these in [ToolRegistry] alongside each tool definition.
 */
data class ToolOutputSpec(
    val toolName: String,
    val requiredFields: List<String> = emptyList()
)
