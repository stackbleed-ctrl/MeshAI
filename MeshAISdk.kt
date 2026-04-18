package com.meshai.sdk

import com.meshai.agent.AgentTask
import com.meshai.agent.MeshKernel
import com.meshai.agent.TaskPriority
import com.meshai.agent.TaskOrigin
import com.meshai.agent.TelemetryCollector
import com.meshai.agent.CapabilityRegistry
import com.meshai.mesh.NodeAdvertisement
import com.meshai.core.model.TaskConstraints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// AgentPlugin — interface for third-party agent extensions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * AgentPlugin — implement this to add a custom agent to the MeshAI runtime.
 *
 * ## Option C — Developer SDK
 *
 * Third-party developers implement [AgentPlugin] to extend MeshAI without
 * touching internal kernel code. A plugin declares what capabilities it
 * provides and handles tasks whose [AgentTask.type] matches.
 *
 * ```kotlin
 * class WeatherPlugin : AgentPlugin {
 *     override val id = "weather.fetch"
 *     override val capabilities = listOf("weather.fetch", "weather.forecast")
 *     override val description = "Fetches current weather from OpenWeatherMap"
 *
 *     override suspend fun execute(task: AgentTask): PluginResult {
 *         val city = JSONObject(task.description).getString("city")
 *         val weather = weatherApi.current(city)
 *         return PluginResult.success("Weather in $city: ${weather.summary}")
 *     }
 * }
 *
 * // Register at SDK init:
 * MeshAISdk.builder(context)
 *     .plugin(WeatherPlugin())
 *     .build()
 * ```
 */
interface AgentPlugin {
    /** Unique plugin ID — used as tool name in the ToolRegistry. */
    val id: String

    /** Capabilities this plugin advertises to the mesh via NODE_ADVERTISE. */
    val capabilities: List<String>

    /** Human-readable description shown to the LLM reasoning loop. */
    val description: String

    /** Execute a task. Never throws — return PluginResult.failure on error. */
    suspend fun execute(task: AgentTask): PluginResult
}

data class PluginResult(
    val success: Boolean,
    val output: String,
    val costUsd: Double = 0.0
) {
    companion object {
        fun success(output: String, costUsd: Double = 0.0) = PluginResult(true, output, costUsd)
        fun failure(error: String) = PluginResult(false, error)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MeshAIConfig — configuration DSL
// ═══════════════════════════════════════════════════════════════════════════

/**
 * MeshAIConfig — immutable runtime configuration.
 *
 * Build via [MeshAIConfig.Builder]:
 * ```kotlin
 * val config = MeshAIConfig.Builder()
 *     .maxQueueSize(50)
 *     .maxLlmCallsPerMinute(4)
 *     .taskTimeoutMs(20_000)
 *     .enableMultiHop(true)
 *     .build()
 * ```
 */
data class MeshAIConfig(
    val maxQueueSize: Int           = 100,
    val maxLlmCallsPerMinute: Int   = 6,
    val taskTimeoutMs: Long         = 30_000L,
    val enableMultiHop: Boolean     = true,
    val enableAckRetransmit: Boolean = true,
    val globalCostCeilingUsd: Double = 0.10,
    val debugMode: Boolean          = false
) {
    class Builder {
        private var maxQueueSize          = 100
        private var maxLlmCallsPerMinute  = 6
        private var taskTimeoutMs         = 30_000L
        private var enableMultiHop        = true
        private var enableAckRetransmit   = true
        private var globalCostCeilingUsd  = 0.10
        private var debugMode             = false

        fun maxQueueSize(v: Int)            = apply { maxQueueSize = v }
        fun maxLlmCallsPerMinute(v: Int)    = apply { maxLlmCallsPerMinute = v }
        fun taskTimeoutMs(v: Long)          = apply { taskTimeoutMs = v }
        fun enableMultiHop(v: Boolean)      = apply { enableMultiHop = v }
        fun enableAckRetransmit(v: Boolean) = apply { enableAckRetransmit = v }
        fun globalCostCeilingUsd(v: Double) = apply { globalCostCeilingUsd = v }
        fun debugMode(v: Boolean)           = apply { debugMode = v }

        fun build() = MeshAIConfig(maxQueueSize, maxLlmCallsPerMinute, taskTimeoutMs,
            enableMultiHop, enableAckRetransmit, globalCostCeilingUsd, debugMode)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TaskBuilder — fluent task construction DSL
// ═══════════════════════════════════════════════════════════════════════════

/**
 * TaskBuilder — fluent DSL for constructing [AgentTask] instances.
 *
 * ```kotlin
 * val task = TaskBuilder("Send a location-aware weather summary")
 *     .description("Get the weather for the user's GPS location and summarise it")
 *     .priority(TaskPriority.NORMAL)
 *     .requireCapability("weather.fetch")
 *     .requireCapability("get_location")
 *     .maxCostUsd(0.01)
 *     .maxLatencyMs(10_000)
 *     .ownerApproved(true)
 *     .build()
 *
 * sdk.client.submit(task)
 * ```
 */
class TaskBuilder(private val title: String) {
    private var description       = ""
    private var priority          = TaskPriority.NORMAL
    private var origin            = TaskOrigin.LOCAL
    private var ownerApproved     = false
    private var maxCostUsd        = 0.05
    private var maxLatencyMs      = 30_000L
    private val requiredCaps      = mutableListOf<String>()
    private val metadata          = mutableMapOf<String, String>()

    fun description(v: String)       = apply { description = v }
    fun priority(v: TaskPriority)    = apply { priority = v }
    fun origin(v: TaskOrigin)        = apply { origin = v }
    fun ownerApproved(v: Boolean)    = apply { ownerApproved = v }
    fun maxCostUsd(v: Double)        = apply { maxCostUsd = v }
    fun maxLatencyMs(v: Long)        = apply { maxLatencyMs = v }
    fun requireCapability(cap: String) = apply { requiredCaps.add(cap) }
    fun metadata(key: String, value: String) = apply { metadata[key] = value }

    fun build(): AgentTask = AgentTask(
        taskId               = UUID.randomUUID().toString(),
        title                = title,
        description          = description,
        priority             = priority,
        origin               = origin,
        ownerApproved        = ownerApproved,
        ownerNodeId          = "",  // set by service on submit
        requiredCapabilities = requiredCaps.toList(),
        constraints          = TaskConstraints(maxCostUsd, maxLatencyMs.toInt())
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// MeshAIClient — clean public API surface
// ═══════════════════════════════════════════════════════════════════════════

/**
 * MeshAIClient — the public-facing API for interacting with the MeshAI runtime.
 *
 * This is what third-party developers and app code use. Internal classes
 * ([MeshKernel], [TaskScheduler], [DecisionEngine], etc.) are never exposed
 * directly. The client provides a stable, minimal surface.
 *
 * Obtain via [MeshAISdk.client] after initialisation.
 */
@Singleton
class MeshAIClient @Inject constructor(
    private val kernel: MeshKernel,
    private val telemetryCollector: TelemetryCollector,
    private val capabilityRegistry: CapabilityRegistry
) {
    /**
     * Submit a task to the kernel for execution.
     * The task is queued and executed asynchronously — observe results via
     * [AgentRepository.observeTaskById] or the [TelemetryCollector.events] flow.
     *
     * ```kotlin
     * val task = TaskBuilder("Summarise my unread emails")
     *     .priority(TaskPriority.HIGH)
     *     .maxLatencyMs(20_000)
     *     .build()
     * sdk.client.submit(task)
     * ```
     */
    fun submit(task: AgentTask) {
        Timber.i("[MeshAIClient] submit: '${task.title}' [${task.priority}]")
        kernel.submit(task)
    }

    /**
     * Convenience: build and submit a simple task in one call.
     *
     * ```kotlin
     * sdk.client.submit("Send SMS to Alice", "Message: I'll be late") {
     *     priority(TaskPriority.HIGH)
     *     requireCapability("send_sms")
     * }
     * ```
     */
    fun submit(title: String, description: String = "", block: TaskBuilder.() -> Unit = {}): AgentTask {
        val task = TaskBuilder(title).description(description).apply(block).build()
        submit(task)
        return task
    }

    /** Live telemetry events — collect in a coroutine for real-time dashboard updates. */
    fun telemetryFlow() = telemetryCollector.events

    /** Current snapshot of runtime telemetry. */
    fun telemetrySnapshot() = telemetryCollector.stats()

    /** All currently live remote nodes and their capabilities. */
    fun liveNodes(): List<NodeAdvertisement> = capabilityRegistry.liveNodes()

    /** Number of live remote nodes in the mesh. */
    fun meshNodeCount(): Int = capabilityRegistry.liveNodeCount()
}

// ═══════════════════════════════════════════════════════════════════════════
// MeshAISdk — single entry point for SDK consumers
// ═══════════════════════════════════════════════════════════════════════════

/**
 * MeshAISdk — public entry point for MeshAI.
 *
 * ## Option C — Developer SDK
 *
 * Third-party developers integrate MeshAI as a library by:
 * 1. Adding the dependency.
 * 2. Calling [MeshAISdk.builder] in `Application.onCreate()`.
 * 3. Using [client] to submit tasks and observe results.
 *
 * ```kotlin
 * class MyApp : Application() {
 *     lateinit var meshAI: MeshAISdk
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         meshAI = MeshAISdk.builder()
 *             .config(MeshAIConfig.Builder()
 *                 .maxLlmCallsPerMinute(4)
 *                 .enableMultiHop(true)
 *                 .build())
 *             .plugin(WeatherPlugin())
 *             .plugin(VisionPlugin())
 *             .build()
 *     }
 * }
 *
 * // Anywhere in your app:
 * MyApp.meshAI.client.submit("Get weather for my location") {
 *     requireCapability("weather.fetch")
 *     priority(TaskPriority.NORMAL)
 * }
 * ```
 *
 * When used within the MeshAI app itself, obtain via Hilt injection — the
 * [MeshAIClient] singleton is bound in [AppModule].
 */
class MeshAISdk private constructor(
    val config: MeshAIConfig,
    val plugins: List<AgentPlugin>,
    val client: MeshAIClient
) {
    companion object {
        fun builder() = Builder()
    }

    class Builder {
        private var config  = MeshAIConfig()
        private val plugins = mutableListOf<AgentPlugin>()
        private var client: MeshAIClient? = null

        fun config(c: MeshAIConfig)  = apply { config = c }
        fun plugin(p: AgentPlugin)   = apply { plugins.add(p) }

        /** For internal use: inject the Hilt-managed client. */
        internal fun client(c: MeshAIClient) = apply { client = c }

        fun build(): MeshAISdk {
            val resolvedClient = client
                ?: error("MeshAISdk: client not set. Use Hilt injection or call client().")
            Timber.i("[MeshAISdk] Initialised — ${plugins.size} plugins, multiHop=${config.enableMultiHop}")
            return MeshAISdk(config, plugins.toList(), resolvedClient)
        }
    }
}
