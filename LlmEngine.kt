package com.meshai.llm

import android.content.Context
import android.os.Build
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Unified LLM engine with tiered inference:
 *
 * Tier 1: Gemini Nano via Android AICore (PromptAPI / AICore on Pixel 8+ / Android 16)
 * Tier 2: Gemma 2B via MediaPipe LLM Inference (all Android 12+ devices)
 *
 * The engine auto-selects the best available tier at initialization.
 *
 * NOTE: Gemini Nano / AICore API is accessed via the Android ML Kit GenAI APIs.
 * The actual AICore binding is device-specific. This implementation uses a
 * documented approach; follow the latest AICore developer preview docs for
 * exact API surface as it may change.
 */
@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var activeTier: InferenceTier = InferenceTier.NONE
    private var mediaPipeEngine: LlmInference? = null

    /** Available inference tiers in priority order */
    enum class InferenceTier { GEMINI_NANO, GEMMA_MEDIAPIPE, NONE }

    init {
        initializeBestAvailable()
    }

    private fun initializeBestAvailable() {
        // Try Gemini Nano first (Android 16 / AICore devices)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (tryInitGeminiNano()) {
                activeTier = InferenceTier.GEMINI_NANO
                Timber.i("[LLM] Using Gemini Nano via AICore")
                return
            }
        }

        // Fall back to MediaPipe Gemma
        if (tryInitMediaPipe()) {
            activeTier = InferenceTier.GEMMA_MEDIAPIPE
            Timber.i("[LLM] Using Gemma 2B via MediaPipe")
            return
        }

        Timber.w("[LLM] No LLM backend available — agent reasoning degraded")
        activeTier = InferenceTier.NONE
    }

    private fun tryInitGeminiNano(): Boolean {
        return try {
            // Gemini Nano via AICore is accessed through the on-device inference APIs.
            // Actual binding uses android.ai.app (AICore) when available on the device.
            // As of Android 16, this is via the ML Kit GenAI Inference SDK:
            //   GenerativeModel("gemini-nano") with on-device = true
            // This stub returns false until the production API is stable.
            // Replace with actual AICore binding when deploying on Pixel 8+.
            false
        } catch (e: Exception) {
            Timber.w("[LLM] Gemini Nano init failed: ${e.message}")
            false
        }
    }

    private fun tryInitMediaPipe(): Boolean {
        return try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(getGemmaModelPath())
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.7f)
                .setRandomSeed(42)
                .build()
            mediaPipeEngine = LlmInference.createFromOptions(context, options)
            true
        } catch (e: Exception) {
            Timber.w("[LLM] MediaPipe init failed: ${e.message}")
            false
        }
    }

    /**
     * Path to the on-device Gemma model file.
     * Model must be downloaded to device storage.
     * See: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
     */
    private fun getGemmaModelPath(): String {
        val externalModelPath = context.getExternalFilesDir(null)?.absolutePath
        return "$externalModelPath/gemma2b-it-cpu-int4.bin"
    }

    // -----------------------------------------------------------------------
    // Public interface
    // -----------------------------------------------------------------------

    /**
     * Complete a prompt using the best available LLM tier.
     *
     * @param systemPrompt High-level instruction context
     * @param messages Conversation history (user/assistant turns)
     * @return Generated response text
     */
    suspend fun complete(
        systemPrompt: String,
        messages: List<LlmMessage>
    ): String = withContext(Dispatchers.IO) {
        when (activeTier) {
            InferenceTier.GEMINI_NANO -> completeGeminiNano(systemPrompt, messages)
            InferenceTier.GEMMA_MEDIAPIPE -> completeMediaPipe(systemPrompt, messages)
            InferenceTier.NONE -> simulatedResponse(messages.lastOrNull()?.content ?: "")
        }
    }

    private suspend fun completeGeminiNano(
        systemPrompt: String,
        messages: List<LlmMessage>
    ): String {
        // Placeholder — replace with actual AICore API when device supports it
        // Example binding when stable:
        //   val model = GenerativeModel(modelName = "gemini-nano", generationConfig = ...)
        //   model.generateContent(prompt).text
        return simulatedResponse(messages.lastOrNull()?.content ?: "")
    }

    private suspend fun completeMediaPipe(
        systemPrompt: String,
        messages: List<LlmMessage>
    ): String = suspendCoroutine { continuation ->
        val engine = mediaPipeEngine
        if (engine == null) {
            continuation.resumeWithException(IllegalStateException("MediaPipe engine not initialized"))
            return@suspendCoroutine
        }

        val prompt = buildGemmaPrompt(systemPrompt, messages)
        Timber.d("[LLM] Sending to Gemma (${prompt.length} chars)")

        try {
            val result = engine.generateResponse(prompt)
            continuation.resume(result)
        } catch (e: Exception) {
            Timber.e(e, "[LLM] MediaPipe inference failed")
            continuation.resumeWithException(e)
        }
    }

    /**
     * Build a prompt string from system + messages for Gemma instruction format.
     * Gemma uses: <start_of_turn>user\n...<end_of_turn>\n<start_of_turn>model\n
     */
    private fun buildGemmaPrompt(systemPrompt: String, messages: List<LlmMessage>): String {
        val sb = StringBuilder()
        sb.appendLine("<start_of_turn>system")
        sb.appendLine(systemPrompt)
        sb.appendLine("<end_of_turn>")
        for (msg in messages) {
            val role = if (msg.role == LlmRole.USER) "user" else "model"
            sb.appendLine("<start_of_turn>$role")
            sb.appendLine(msg.content)
            sb.appendLine("<end_of_turn>")
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    /**
     * Fallback response when no LLM is available.
     * Returns a structured "no LLM" message so the ReAct loop can gracefully degrade.
     */
    private fun simulatedResponse(userInput: String): String {
        return "FINAL ANSWER: I was unable to process this request because no on-device LLM is available. " +
                "Please ensure a Gemma model is downloaded or that this device supports Gemini Nano."
    }

    fun getActiveTier(): InferenceTier = activeTier

    fun isAvailable(): Boolean = activeTier != InferenceTier.NONE

    fun release() {
        mediaPipeEngine?.close()
        mediaPipeEngine = null
    }
}

// -----------------------------------------------------------------------
// Data classes
// -----------------------------------------------------------------------

data class LlmMessage(
    val role: LlmRole,
    val content: String
)

enum class LlmRole { USER, ASSISTANT, SYSTEM }
