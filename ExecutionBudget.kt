package com.meshai.agent

import timber.log.Timber

/**
 * Tracks token-cost budget consumption across a single ReAct run.
 *
 * ## Why this exists
 *
 * MAX_STEPS caps iterations but not cost. A run that calls a cheap
 * tool 12 times costs far less than one that generates a 1024-token
 * LLM response on every step. Budget tracking gives you a second,
 * orthogonal enforcement axis: stop the loop when the *cumulative cost*
 * of this task exceeds a threshold, regardless of step count.
 *
 * ## Cost model
 *
 * Costs are estimated token counts — not dollars. On Gemma 2B running
 * locally there is no monetary cost, but token throughput is the
 * scarce resource (battery, latency). The budget ceiling is a proxy
 * for "how much compute should one task be allowed to burn."
 *
 * Default ceiling [DEFAULT_MAX_TOKENS] is 6000 — enough for ~6 full
 * LLM response + observation pairs within Gemma 2B's 8k context, with
 * headroom for the system prompt.
 *
 * ## Estimation
 *
 * Token counts are estimated at ~4 chars per token (standard English
 * approximation). Not exact, but consistent — what matters is that the
 * ceiling is hit *before* the context window is blown, not that the
 * count is perfectly accurate.
 */
class ExecutionBudget(val maxTokens: Int = DEFAULT_MAX_TOKENS) {

    companion object {
        const val DEFAULT_MAX_TOKENS = 6_000
        private const val CHARS_PER_TOKEN = 4
    }

    private var tokensSpent: Int = 0

    val remaining: Int get() = (maxTokens - tokensSpent).coerceAtLeast(0)
    val isExhausted: Boolean get() = tokensSpent >= maxTokens
    val fractionUsed: Float get() = tokensSpent.toFloat() / maxTokens

    /**
     * Record the cost of one LLM prompt + response cycle.
     *
     * @param promptText    The full prompt sent to the LLM this step.
     * @param responseText  The LLM response received.
     * @return              Estimated tokens charged for this step.
     */
    fun charge(promptText: String, responseText: String): Int {
        val cost = estimateTokens(promptText) + estimateTokens(responseText)
        tokensSpent += cost
        Timber.d("[Budget] Charged $cost tokens (total $tokensSpent / $maxTokens)")
        return cost
    }

    /**
     * Record the cost of a tool invocation.
     * Tool inputs and outputs are typically JSON — usually cheaper than
     * LLM responses but still worth tracking.
     *
     * @param toolInput   Raw JSON input string passed to the tool.
     * @param toolOutput  Tool result summary string.
     * @return            Estimated tokens charged.
     */
    fun chargeTool(toolInput: String, toolOutput: String): Int {
        val cost = estimateTokens(toolInput) + estimateTokens(toolOutput)
        tokensSpent += cost
        Timber.d("[Budget] Tool charge $cost tokens (total $tokensSpent / $maxTokens)")
        return cost
    }

    /**
     * Returns a snapshot of current budget state for inclusion in
     * [StepTrace] and the final [ExecutionTrace].
     */
    fun snapshot(): BudgetSnapshot = BudgetSnapshot(
        tokensSpent = tokensSpent,
        maxTokens = maxTokens,
        remaining = remaining,
        fractionUsed = fractionUsed
    )

    fun reset() {
        tokensSpent = 0
    }

    private fun estimateTokens(text: String): Int =
        (text.length / CHARS_PER_TOKEN).coerceAtLeast(1)
}

data class BudgetSnapshot(
    val tokensSpent: Int,
    val maxTokens: Int,
    val remaining: Int,
    val fractionUsed: Float
)
