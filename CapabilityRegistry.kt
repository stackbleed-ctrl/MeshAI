package com.meshai.agent

import com.meshai.mesh.NodeAdvertisement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CapabilityRegistry — reliability-aware routing brain.
 *
 * ## What changed (Option A — Resilience)
 *
 * The previous version scored nodes on battery level and agent mode only.
 * Neither tells you whether a node actually *succeeds* at tasks.
 *
 * Now [bestNodeForCapability] combines:
 * - Battery level (40% weight) — don't delegate to a dying device
 * - Agent Mode bonus (30% weight) — owner-away nodes are more available
 * - Reliability score from [ReliabilityScorer] (30% weight) — telemetry-driven
 *
 * A node with 95% success rate and 200ms average latency beats one with
 * 60% success rate at 80ms. Routing becomes empirically grounded.
 *
 * SPEC_REF: CAP-001 / OPTION-A
 */
@Singleton
class CapabilityRegistry @Inject constructor(
    private val reliabilityScorer: ReliabilityScorer
) {
    companion object {
        const val STALE_THRESHOLD_MS = 30_000L
        private const val WEIGHT_BATTERY     = 0.40
        private const val WEIGHT_AGENT_MODE  = 30.0
        private const val WEIGHT_RELIABILITY = 30.0
    }

    private val _registry = MutableStateFlow<Map<String, NodeAdvertisement>>(emptyMap())
    val registry: StateFlow<Map<String, NodeAdvertisement>> = _registry.asStateFlow()

    fun advertise(ad: NodeAdvertisement) {
        _registry.update { it + (ad.nodeId to ad) }
        Timber.d("[CapReg] Updated ${ad.nodeId}: caps=${ad.capabilities} bat=${ad.batteryLevel}%")
    }

    fun evict(nodeId: String) {
        _registry.update { it - nodeId }
        reliabilityScorer.reset(nodeId)
        Timber.d("[CapReg] Evicted $nodeId")
    }

    /**
     * Find the best remote node for [capabilityName].
     *
     * Composite score = battery (40%) + agentMode (30%) + reliability (30%).
     * Returns null if no suitable remote node → execute locally.
     */
    fun bestNodeForCapability(
        capabilityName: String,
        excludeNodeId: String,
        minBatteryPct: Int = 20
    ): NodeAdvertisement? {
        pruneStale()
        return _registry.value.values
            .filter { ad ->
                ad.nodeId != excludeNodeId &&
                capabilityName in ad.capabilities &&
                ad.batteryLevel >= minBatteryPct &&
                !isStale(ad)
            }
            .maxByOrNull { ad -> compositeScore(ad) }
            .also {
                if (it != null) {
                    val rel = reliabilityScorer.scoreOf(it.nodeId)
                    Timber.d("[CapReg] Best for '$capabilityName': ${it.nodeId} " +
                        "(bat=${it.batteryLevel}% rel=${"%.2f".format(rel)})")
                } else {
                    Timber.d("[CapReg] No remote node for '$capabilityName'")
                }
            }
    }

    /**
     * Record a task outcome for a node — feeds [ReliabilityScorer].
     * Call this from [TelemetryCollector.record] or [MeshKernel] after task completion.
     */
    fun recordOutcome(nodeId: String, success: Boolean, latencyMs: Long) {
        reliabilityScorer.record(nodeId, success, latencyMs)
    }

    fun liveNodes(): List<NodeAdvertisement> {
        pruneStale(); return _registry.value.values.toList()
    }
    fun liveNodeCount(): Int = liveNodes().size

    private fun compositeScore(ad: NodeAdvertisement): Double {
        val batteryScore      = ad.batteryLevel * WEIGHT_BATTERY
        val agentModeScore    = if (!ad.isOwnerPresent) WEIGHT_AGENT_MODE else 0.0
        val reliabilityScore  = reliabilityScorer.scoreOf(ad.nodeId) * WEIGHT_RELIABILITY
        return batteryScore + agentModeScore + reliabilityScore
    }

    private fun isStale(ad: NodeAdvertisement): Boolean =
        System.currentTimeMillis() - ad.advertisedAtMs > STALE_THRESHOLD_MS

    private fun pruneStale() {
        _registry.update { current -> current.filterValues { !isStale(it) } }
    }
}
