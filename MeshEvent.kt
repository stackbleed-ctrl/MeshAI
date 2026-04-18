package com.meshai.core.protocol

import kotlinx.serialization.Serializable

/**
 * Telemetry event emitted after each task execution.
 * Used by TelemetryCollector, PolicyEngine, and the dashboard.
 */
@Serializable
data class MeshEvent(
    val taskId: String,
    val nodeId: String,
    val latencyMs: Long,
    val costUsd: Double,
    val success: Boolean,
    val toolUsed: String? = null,
    val envelopeId: String? = null,
    val transportLayer: String? = null,   // "BLE" | "NEARBY" | "MESHRABIYA" | "LOCAL"
    val hopCount: Int = 0,
    val timestampEpoch: Long = System.currentTimeMillis()
)
