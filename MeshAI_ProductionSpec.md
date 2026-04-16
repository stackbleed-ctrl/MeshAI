# MeshAI — Production Architecture Specification
## Principal-Level Systems Design · v1.0

---

## SECTION 1 — ARCHITECTURE DIAGRAM

```
╔══════════════════════════════════════════════════════════════════════════════════╗
║                        MeshAI Android Process (Foreground Service)              ║
║                                                                                  ║
║  ┌─────────────────────────────────────────────────────────────────────────┐    ║
║  │ PRESENTATION LAYER                                                       │    ║
║  │  AgentControlActivity · ObservabilityDashboard · DebugConsole           │    ║
║  └────────────────────────────────┬────────────────────────────────────────┘    ║
║                                   │ StateFlow<AgentState> / ForgeEvent          ║
║  ┌────────────────────────────────▼────────────────────────────────────────┐    ║
║  │ SAFETY + CONTROL LAYER                                    [KILL SWITCH] │    ║
║  │  PolicyEngine ── SafetyGate ── RateLimiter ── AuditLogger               │    ║
║  │  Every action passes here. No exceptions.                               │    ║
║  └────────────────────────────────┬────────────────────────────────────────┘    ║
║                              AllowedAction                                       ║
║  ┌────────────────────────────────▼────────────────────────────────────────┐    ║
║  │ COGNITION ENGINE                                                         │    ║
║  │  GoalEngine ── GoalConstraintSystem ── ConflictDetector                 │    ║
║  │       │                                                                  │    ║
║  │  ReActLoopV2: Plan→Reason→Critique→Refine→Decide→Act                    │    ║
║  │       ├── ConfidenceScorer                                               │    ║
║  │       ├── LoopDetector (hash-based cycle detection)                     │    ║
║  │       └── FailureClassifier                                              │    ║
║  └────────────────────────────────┬────────────────────────────────────────┘    ║
║                              ToolRequest (validated)                             ║
║  ┌────────────────────────────────▼────────────────────────────────────────┐    ║
║  │ TOOL EXECUTION SANDBOX                                                   │    ║
║  │  ToolExecutionGuard ── ArgValidator ── UserConsentGate ── RateLimiter   │    ║
║  │                                                                          │    ║
║  │  [SMS_TOOL] [CALL_TOOL] [CAMERA_TOOL] [LOCATION_TOOL]                   │    ║
║  │  [NOTIFY_TOOL] [MESH_SEND_TOOL] [DB_QUERY_TOOL] [WEB_TOOL]              │    ║
║  └──────────┬──────────────────────────────────────────────────────────────┘    ║
║             │                                                                    ║
║  ┌──────────┴──────────┐  ┌─────────────────────┐  ┌───────────────────────┐  ║
║  │ LLM ENGINE          │  │ MEMORY SYSTEM        │  │ MESH COMM LAYER       │  ║
║  │                     │  │                      │  │                       │  ║
║  │ Gemini Nano / Gemma │  │ WorkingMemory        │  │ NodeDiscovery         │  ║
║  │ via MediaPipe Tasks │  │ EpisodicMemory       │  │ TrustRegistry         │  ║
║  │                     │  │ SemanticMemory       │  │ GossipProtocol        │  ║
║  │ InferenceSession    │  │ ImportanceScorer     │  │ TaskDelegator         │  ║
║  │ PromptBuilder       │  │ MemoryConsolidator   │  │ RetryManager          │  ║
║  │ OutputParser        │  │                      │  │ PartialConnectivity   │  ║
║  └─────────────────────┘  └──────────┬───────────┘  └──────────┬────────────┘  ║
║                                      │                          │               ║
║  ┌───────────────────────────────────▼──────────────────────────▼────────────┐  ║
║  │ PERSISTENCE LAYER                                                          │  ║
║  │  Room DB: TaskEntity · MemoryEntity · NodeEntity · AuditEntity            │  ║
║  │  Android Keystore: DeviceIdentity keys (hardware-backed)                  │  ║
║  │  EncryptedSharedPreferences: config/state                                 │  ║
║  └────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                  ║
║  ┌────────────────────────────────────────────────────────────────────────────┐  ║
║  │ OBSERVABILITY (ForgeEvent System)                                          │  ║
║  │  ForgeEventBus → ForgeLogger → LocalReplayBuffer → DebugInspector         │  ║
║  └────────────────────────────────────────────────────────────────────────────┘  ║
╚══════════════════════════════════════════════════════════════════════════════════╝

  MESH NODE A ←──[Ed25519-signed AES-256-GCM packet]──→ MESH NODE B
       │                                                      │
  TrustRegistry                                         TrustRegistry
  (local scores)                                       (local scores)
```

---

## SECTION 2 — PACKAGE STRUCTURE

```
com.meshai/
├── agent/
│   ├── cognition/
│   │   ├── ReActLoopV2.kt              # Multi-stage reasoning loop
│   │   ├── CognitionStage.kt           # Sealed class: Plan/Reason/Critique/Refine/Decide/Act
│   │   ├── ConfidenceScorer.kt
│   │   ├── LoopDetector.kt             # Hash-based cycle detection
│   │   └── FailureClassifier.kt
│   ├── goal/
│   │   ├── GoalEngine.kt
│   │   ├── GoalConstraintSystem.kt
│   │   ├── GoalPolicy.kt               # Allowed actions, limits, priority
│   │   ├── ConflictDetector.kt
│   │   └── HumanInTheLoopGate.kt
│   └── AgentOrchestrator.kt            # Top-level agent coordinator
│
├── safety/
│   ├── PolicyEngine.kt                 # Rule-based constraint evaluation
│   ├── SafetyGate.kt                   # MANDATORY pass-through for ALL actions
│   ├── RateLimiter.kt                  # Per-tool, per-node, per-time-window
│   ├── AuditLogger.kt                  # Immutable append-only audit trail
│   ├── KillSwitch.kt                   # Hard stop, zero recovery
│   └── policy/
│       ├── PolicyRule.kt               # Sealed class rule definitions
│       └── DefaultPolicies.kt          # Sane production defaults
│
├── tools/
│   ├── sandbox/
│   │   ├── ToolExecutionGuard.kt       # Entry point — nothing bypasses this
│   │   ├── ArgValidator.kt             # Schema-based argument validation
│   │   ├── UserConsentGate.kt          # Coroutine-suspended user approval
│   │   └── ToolRateLimiter.kt
│   ├── definitions/
│   │   ├── Tool.kt                     # Base sealed interface
│   │   ├── SmsTool.kt
│   │   ├── CallTool.kt
│   │   ├── CameraTool.kt
│   │   ├── LocationTool.kt
│   │   ├── NotifyTool.kt
│   │   ├── MeshSendTool.kt
│   │   └── DbQueryTool.kt
│   └── registry/
│       └── ToolRegistry.kt             # Allowlist + tool lookup
│
├── mesh/
│   ├── protocol/
│   │   ├── MeshMessage.kt              # Canonical message type
│   │   ├── MessageType.kt              # Enum: TASK/RESULT/STATE/HEARTBEAT/TRUST_UPDATE
│   │   ├── MessageSigner.kt            # Ed25519 sign/verify via Android Keystore
│   │   ├── NonceManager.kt             # Replay attack prevention
│   │   └── ProtocolVersion.kt
│   ├── transport/
│   │   ├── MeshTransport.kt            # Interface over Nearby/BLE/Meshrabiya
│   │   ├── NearbyTransport.kt
│   │   ├── BleTransport.kt
│   │   └── MeshrabiyaTransport.kt
│   ├── routing/
│   │   ├── NodeDiscovery.kt
│   │   ├── GossipProtocol.kt
│   │   ├── TaskDelegator.kt
│   │   └── PartialConnectivityHandler.kt
│   └── retry/
│       └── RetryManager.kt
│
├── trust/
│   ├── TrustRegistry.kt                # Local peer score store
│   ├── DeviceIdentity.kt               # Hardware-backed key management
│   ├── PeerVerificationHandshake.kt    # Challenge-response on connect
│   ├── TrustScorer.kt                  # Scoring algorithm
│   └── RevocationManager.kt
│
├── memory/
│   ├── WorkingMemory.kt                # In-process, fast, ephemeral
│   ├── EpisodicMemory.kt               # Event log (Room-backed)
│   ├── SemanticMemory.kt               # Vector embeddings (on-device)
│   ├── ImportanceScorer.kt
│   ├── MemoryDecay.kt
│   └── MemoryConsolidator.kt
│
├── llm/
│   ├── LlmEngine.kt                    # Interface
│   ├── GeminiNanoEngine.kt             # MediaPipe Tasks impl
│   ├── GemmaEngine.kt                  # Fallback impl
│   ├── PromptBuilder.kt
│   └── OutputParser.kt
│
├── observability/
│   ├── ForgeEvent.kt                   # Canonical event type
│   ├── ForgeEventBus.kt                # SharedFlow bus
│   ├── ForgeLogger.kt                  # Structured log writer
│   ├── LocalReplayBuffer.kt            # Circular buffer for replay
│   └── DebugInspector.kt              # Snapshot + query tool
│
├── persistence/
│   ├── MeshDatabase.kt                 # Room database
│   ├── entities/
│   │   ├── TaskEntity.kt
│   │   ├── MemoryEntity.kt
│   │   ├── NodeEntity.kt
│   │   └── AuditEntity.kt
│   └── dao/
│       ├── TaskDao.kt
│       ├── MemoryDao.kt
│       ├── NodeDao.kt
│       └── AuditDao.kt
│
└── service/
    ├── AgentForegroundService.kt       # Lifecycle manager
    └── BootReceiver.kt                 # Boot persistence
```

---

## SECTION 3 — KOTLIN INTERFACE DEFINITIONS

### 3.1 Safety Layer

```kotlin
// safety/SafetyGate.kt
interface SafetyGate {
    /**
     * MANDATORY. Every action must pass through here before execution.
     * Returns ActionVerdict. DENY terminates execution chain immediately.
     */
    suspend fun evaluate(request: ActionRequest): ActionVerdict
}

sealed class ActionVerdict {
    object Allow : ActionVerdict()
    data class AllowWithConstraints(val modifiedRequest: ActionRequest) : ActionVerdict()
    data class RequiresConsent(val prompt: ConsentPrompt) : ActionVerdict()
    data class Deny(val reason: DenyReason, val policyRule: PolicyRule) : ActionVerdict()
    object KillSwitchEngaged : ActionVerdict()
}

data class ActionRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val originNodeId: String,           // "LOCAL" or peer node ID
    val toolId: ToolId,
    val args: Map<String, Any>,
    val goalId: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val confidenceScore: Float          // from ConfidenceScorer, 0..1
)

// safety/PolicyEngine.kt
interface PolicyEngine {
    fun evaluate(request: ActionRequest): PolicyDecision
    fun addRule(rule: PolicyRule)
    fun removeRule(ruleId: String)
    fun listRules(): List<PolicyRule>
    fun snapshot(): PolicySnapshot
}

sealed class PolicyRule {
    abstract val ruleId: String
    abstract val priority: Int

    data class DenyTool(
        override val ruleId: String,
        override val priority: Int,
        val toolId: ToolId,
        val reason: String
    ) : PolicyRule()

    data class RequireConsent(
        override val ruleId: String,
        override val priority: Int,
        val toolId: ToolId,
        val consentPrompt: String
    ) : PolicyRule()

    data class RateLimit(
        override val ruleId: String,
        override val priority: Int,
        val toolId: ToolId,
        val maxCallsPerWindow: Int,
        val windowMs: Long
    ) : PolicyRule()

    data class DenyRemoteOrigin(
        override val ruleId: String,
        override val priority: Int,
        val toolId: ToolId
    ) : PolicyRule()

    data class RequireMinConfidence(
        override val ruleId: String,
        override val priority: Int,
        val minScore: Float
    ) : PolicyRule()

    data class DenyDuringHours(
        override val ruleId: String,
        override val priority: Int,
        val startHour: Int,
        val endHour: Int
    ) : PolicyRule()
}

// safety/KillSwitch.kt
interface KillSwitch {
    val isEngaged: StateFlow<Boolean>
    /** Immediately halts ALL agent activity. Cannot be undone in-process. */
    fun engage(reason: String)
    /** Only callable from UI with biometric confirmation. */
    suspend fun disengage(authToken: BiometricAuthToken): Result<Unit>
}

// safety/AuditLogger.kt
interface AuditLogger {
    /** Append-only. Writes to encrypted Room table. Cannot be deleted at runtime. */
    suspend fun log(entry: AuditEntry)
    suspend fun query(filter: AuditFilter): List<AuditEntry>
    suspend fun exportSigned(): ByteArray  // Ed25519-signed JSON export
}

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: AuditEventType,
    val actorNodeId: String,
    val toolId: ToolId?,
    val args: String?,           // JSON-serialized, truncated to 512 chars
    val verdict: String,
    val policyRuleId: String?,
    val goalId: String?,
    val sessionId: String
)

enum class AuditEventType {
    TOOL_ALLOWED, TOOL_DENIED, CONSENT_REQUESTED, CONSENT_GRANTED, CONSENT_DENIED,
    GOAL_STARTED, GOAL_COMPLETED, GOAL_ABORTED, GOAL_CONFLICT,
    PEER_CONNECTED, PEER_REJECTED, TRUST_UPDATED, TRUST_REVOKED,
    KILL_SWITCH_ENGAGED, KILL_SWITCH_DISENGAGED,
    LOOP_DETECTED, CONFIDENCE_LOW, RATE_LIMITED,
    REPLAY_ATTACK_BLOCKED, SIGNATURE_INVALID
}
```

### 3.2 Cognition Engine

```kotlin
// agent/cognition/ReActLoopV2.kt
interface ReActLoopV2 {
    /**
     * Execute multi-stage reasoning over a goal.
     * Stages: Plan → Reason → Critique → Refine → Decide → Act
     * Each stage is observable via ForgeEventBus.
     */
    suspend fun execute(goal: BoundGoal): AgentResult
    fun cancel(reason: String)
}

sealed class CognitionStage {
    data class Plan(val goalDescription: String) : CognitionStage()
    data class Reason(val plan: AgentPlan, val context: MemoryContext) : CognitionStage()
    data class Critique(val reasoning: AgentReasoning) : CognitionStage()
    data class Refine(val critique: CritiqueResult) : CognitionStage()
    data class Decide(val refinedPlan: AgentPlan, val confidence: Float) : CognitionStage()
    data class Act(val decision: AgentDecision) : CognitionStage()
}

sealed class AgentResult {
    data class Success(val output: String, val stepsExecuted: Int) : AgentResult()
    data class PartialSuccess(val output: String, val failedSteps: List<StepFailure>) : AgentResult()
    data class Failure(val reason: FailureReason, val lastStage: CognitionStage) : AgentResult()
    object LoopDetected : AgentResult()
    object KillSwitchEngaged : AgentResult()
    data class ConsentDenied(val toolId: ToolId) : AgentResult()
}

sealed class FailureReason {
    data class LlmError(val message: String) : FailureReason()
    data class ToolError(val toolId: ToolId, val error: String) : FailureReason()
    data class PolicyViolation(val rule: PolicyRule) : FailureReason()
    data class ConfidenceTooLow(val score: Float, val threshold: Float) : FailureReason()
    data class MaxIterationsExceeded(val max: Int) : FailureReason()
    data class GoalConstraintViolation(val constraint: String) : FailureReason()
}

// agent/cognition/LoopDetector.kt
interface LoopDetector {
    /**
     * Hash-based state fingerprint. If the same (stage, memory_hash, tool_sequence)
     * appears twice within a session, we have a loop.
     */
    fun record(fingerprint: StateFingerprint): LoopCheckResult
    fun reset()
}

data class StateFingerprint(
    val stage: String,
    val memoryHash: Int,
    val lastToolSequence: List<ToolId>,
    val iterationIndex: Int
)

sealed class LoopCheckResult {
    object Safe : LoopCheckResult()
    data class LoopDetected(val firstSeenAt: Int, val currentIteration: Int) : LoopCheckResult()
}

// agent/goal/GoalConstraintSystem.kt
interface GoalConstraintSystem {
    fun bind(goal: Goal, policy: GoalPolicy): BoundGoal
    fun validate(action: ActionRequest, boundGoal: BoundGoal): ConstraintResult
    fun checkConflict(newGoal: BoundGoal, activeGoals: List<BoundGoal>): ConflictResult
}

data class GoalPolicy(
    val allowedToolIds: Set<ToolId>,
    val maxIterations: Int,
    val maxDurationMs: Long,
    val allowRemoteDelegation: Boolean,
    val requiresHumanApproval: Boolean,
    val minConfidenceThreshold: Float,
    val budgetTokens: Int,
    val forbiddenKeywords: Set<String>  // For LLM output scanning
)

data class BoundGoal(
    val goal: Goal,
    val policy: GoalPolicy,
    val startedAt: Long = System.currentTimeMillis(),
    val sessionId: String = UUID.randomUUID().toString()
)

sealed class ConflictResult {
    object NoConflict : ConflictResult()
    data class ResourceConflict(val conflictingGoalId: String, val resource: String) : ConflictResult()
    data class PolicyConflict(val reason: String) : ConflictResult()
    data class PriorityConflict(val higherPriorityGoalId: String) : ConflictResult()
}
```

### 3.3 Tool Execution Sandbox

```kotlin
// tools/sandbox/ToolExecutionGuard.kt
interface ToolExecutionGuard {
    /**
     * Single entry point for ALL tool invocations.
     * Chain: ArgValidation → PolicyCheck → RateLimit → ConsentGate → Execute → Audit
     */
    suspend fun execute(request: ToolRequest): ToolResult
}

data class ToolRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val toolId: ToolId,
    val args: Map<String, Any>,
    val origin: RequestOrigin,
    val boundGoal: BoundGoal?
)

sealed class RequestOrigin {
    object Local : RequestOrigin()
    data class Remote(val nodeId: String, val trustScore: Float) : RequestOrigin()
}

sealed class ToolResult {
    data class Success(val data: Map<String, Any>) : ToolResult()
    data class Failure(val error: String, val recoverable: Boolean) : ToolResult()
    object ConsentDenied : ToolResult()
    object RateLimited : ToolResult()
    object PolicyDenied : ToolResult()
    data class ValidationFailed(val fieldErrors: Map<String, String>) : ToolResult()
}

// tools/definitions/Tool.kt
interface Tool {
    val toolId: ToolId
    val schema: ToolSchema
    val requiresPermissions: List<String>
    val requiresUserConsent: Boolean
    val allowedFromRemote: Boolean
    suspend fun execute(validatedArgs: Map<String, Any>, context: ToolContext): ToolResult
}

data class ToolSchema(
    val name: String,
    val description: String,
    val parameters: Map<String, ParameterSchema>
)

data class ParameterSchema(
    val type: ParameterType,
    val required: Boolean,
    val maxLength: Int? = null,
    val allowedValues: List<Any>? = null,
    val regex: String? = null,
    val min: Number? = null,
    val max: Number? = null
)

enum class ParameterType { STRING, INT, FLOAT, BOOLEAN, PHONE_NUMBER, COORDINATE }

// Strongly-typed tool IDs — no string magic
@JvmInline value class ToolId(val value: String) {
    companion object {
        val SMS = ToolId("tool.sms.send")
        val CALL = ToolId("tool.call.initiate")
        val CAMERA_CAPTURE = ToolId("tool.camera.capture")
        val LOCATION_GET = ToolId("tool.location.get")
        val NOTIFY = ToolId("tool.notification.send")
        val MESH_SEND = ToolId("tool.mesh.send")
        val DB_QUERY = ToolId("tool.db.query")
    }
}
```

### 3.4 Mesh Protocol

```kotlin
// mesh/protocol/MeshMessage.kt
data class MeshMessage(
    val version: Int = ProtocolVersion.CURRENT,
    val messageId: String,              // UUID v4
    val type: MessageType,
    val senderId: String,               // hardware-backed device ID (SHA-256 of public key)
    val recipientId: String?,           // null = broadcast
    val timestamp: Long,                // Unix ms — validated against local clock ± 30s
    val nonce: ByteArray,               // 16 bytes, single-use
    val ttl: Int,                       // hop count limit
    val payload: ByteArray,             // AES-256-GCM encrypted
    val signature: ByteArray,           // Ed25519 over (version|messageId|type|senderId|timestamp|nonce|payload)
    val hopPath: List<String> = emptyList()  // node IDs traversed, appended per hop
)

enum class MessageType(val code: Int) {
    TASK(1),
    RESULT(2),
    STATE(3),
    HEARTBEAT(4),
    TRUST_UPDATE(5),
    HANDSHAKE_INIT(6),
    HANDSHAKE_RESPONSE(7),
    REVOCATION(8)
}

// mesh/protocol/MessageSigner.kt
interface MessageSigner {
    /** Sign using hardware-backed Ed25519 key from Android Keystore */
    fun sign(payload: ByteArray): ByteArray
    fun verify(payload: ByteArray, signature: ByteArray, senderPublicKey: ByteArray): Boolean
    fun getPublicKey(): ByteArray
}

// mesh/protocol/NonceManager.kt
interface NonceManager {
    /** Generate cryptographically random 16-byte nonce */
    fun generate(): ByteArray
    /**
     * Returns true if nonce is valid (unseen within window).
     * Rejects: seen nonces, nonces from messages with timestamps outside ±30s window.
     */
    fun validate(nonce: ByteArray, timestamp: Long): Boolean
    /** Called after successful validation to record nonce */
    fun consume(nonce: ByteArray)
}

// trust/TrustRegistry.kt
interface TrustRegistry {
    suspend fun getScore(nodeId: String): TrustScore?
    suspend fun updateScore(nodeId: String, delta: TrustDelta)
    suspend fun revoke(nodeId: String, reason: RevocationReason)
    suspend fun isRevoked(nodeId: String): Boolean
    suspend fun listPeers(): List<PeerRecord>
    suspend fun getPublicKey(nodeId: String): ByteArray?
    suspend fun registerPeer(nodeId: String, publicKey: ByteArray, initialScore: Float)
}

data class TrustScore(
    val nodeId: String,
    val score: Float,           // 0.0 (untrusted) .. 1.0 (fully trusted)
    val tier: TrustTier,
    val lastSeen: Long,
    val messageCount: Int,
    val failureCount: Int,
    val isRevoked: Boolean
)

enum class TrustTier {
    UNKNOWN,          // < 0.2 — no execution, routing only
    PROVISIONAL,      // 0.2..0.5 — read-only tools only
    TRUSTED,          // 0.5..0.8 — standard tool access, no high-privilege
    VERIFIED          // 0.8..1.0 — full access per policy
}

// trust/PeerVerificationHandshake.kt
interface PeerVerificationHandshake {
    /**
     * Initiator sends HANDSHAKE_INIT with:
     *   - own public key
     *   - challenge nonce (32 bytes)
     *   - timestamp
     *   - app version
     *
     * Responder replies with HANDSHAKE_RESPONSE signed by its hardware key.
     * Both sides derive session key via ECDH (X25519).
     */
    suspend fun initiate(transport: MeshTransport, targetNodeId: String): HandshakeResult
    suspend fun respond(initMessage: MeshMessage): HandshakeResult
}

sealed class HandshakeResult {
    data class Success(
        val verifiedNodeId: String,
        val sessionKey: ByteArray,
        val peerPublicKey: ByteArray,
        val initialTrustScore: Float
    ) : HandshakeResult()
    data class Failure(val reason: String) : HandshakeResult()
}
```

### 3.5 Memory System

```kotlin
// memory/WorkingMemory.kt
interface WorkingMemory {
    fun put(key: String, value: Any, importance: Float = 0.5f)
    fun get(key: String): Any?
    fun getAll(): Map<String, MemoryEntry>
    fun remove(key: String)
    fun clear()
    fun snapshot(): WorkingMemorySnapshot
    val capacityUsed: Float  // 0..1
}

data class MemoryEntry(
    val key: String,
    val value: Any,
    val importance: Float,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int
)

// memory/EpisodicMemory.kt
interface EpisodicMemory {
    suspend fun record(event: EpisodicEvent)
    suspend fun recall(filter: EpisodicFilter, limit: Int = 20): List<EpisodicEvent>
    suspend fun recallRecent(n: Int): List<EpisodicEvent>
    suspend fun decay()   // Apply ImportanceDecay, prune low-importance old events
}

data class EpisodicEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: EpisodicEventType,
    val description: String,
    val importance: Float,
    val linkedGoalId: String?,
    val linkedNodeId: String?,
    val metadata: Map<String, String> = emptyMap()
)

// memory/SemanticMemory.kt
interface SemanticMemory {
    /**
     * Store a text fragment with its embedding (computed on-device via Gemma).
     * Used for semantic similarity recall.
     */
    suspend fun store(fragment: SemanticFragment)
    suspend fun search(query: String, topK: Int = 5): List<ScoredFragment>
    suspend fun delete(fragmentId: String)
    suspend fun compact()  // Re-rank and prune by importance score
}

data class SemanticFragment(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val embedding: FloatArray,       // 128-dim or 256-dim on-device embedding
    val sourceGoalId: String?,
    val importance: Float,
    val createdAt: Long = System.currentTimeMillis()
)

data class ScoredFragment(
    val fragment: SemanticFragment,
    val similarity: Float            // cosine similarity 0..1
)
```

### 3.6 Observability

```kotlin
// observability/ForgeEvent.kt
data class ForgeEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val category: ForgeEventCategory,
    val level: ForgeEventLevel,
    val component: String,           // e.g. "ReActLoopV2", "SafetyGate", "TrustRegistry"
    val message: String,
    val data: Map<String, Any> = emptyMap(),
    val linkedGoalId: String? = null,
    val linkedNodeId: String? = null,
    val durationMs: Long? = null
)

enum class ForgeEventCategory {
    COGNITION, TOOL_EXECUTION, SAFETY, MESH, TRUST, MEMORY, LLM, LIFECYCLE, ERROR
}

enum class ForgeEventLevel { TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL }

// observability/ForgeEventBus.kt
interface ForgeEventBus {
    val events: SharedFlow<ForgeEvent>
    suspend fun emit(event: ForgeEvent)
    fun emitBlocking(event: ForgeEvent)  // For non-suspend contexts (use sparingly)
}

// observability/LocalReplayBuffer.kt
interface LocalReplayBuffer {
    /** Circular buffer — retains last N events in memory, newest wins */
    fun append(event: ForgeEvent)
    fun replay(since: Long): List<ForgeEvent>
    fun replayAll(): List<ForgeEvent>
    fun clear()
    val size: Int
}
```

---

## SECTION 4 — KEY IMPLEMENTATIONS

### 4.1 SafetyGate (Complete)

```kotlin
class SafetyGateImpl @Inject constructor(
    private val policyEngine: PolicyEngine,
    private val rateLimiter: RateLimiter,
    private val killSwitch: KillSwitch,
    private val auditLogger: AuditLogger,
    private val forge: ForgeEventBus
) : SafetyGate {

    override suspend fun evaluate(request: ActionRequest): ActionVerdict {
        // 0. Kill switch is always first check — hard stop
        if (killSwitch.isEngaged.value) {
            auditLogger.log(request.toAuditEntry("KILL_SWITCH_ENGAGED", "DENY"))
            forge.emit(ForgeEvent(
                sessionId = request.goalId ?: "none",
                category = ForgeEventCategory.SAFETY,
                level = ForgeEventLevel.CRITICAL,
                component = "SafetyGate",
                message = "Kill switch engaged — request denied",
                data = mapOf("toolId" to request.toolId.value, "origin" to request.originNodeId)
            ))
            return ActionVerdict.KillSwitchEngaged
        }

        // 1. Policy evaluation
        val policyDecision = policyEngine.evaluate(request)
        if (policyDecision is PolicyDecision.Deny) {
            auditLogger.log(request.toAuditEntry("POLICY_DENY", policyDecision.rule.ruleId))
            forge.emit(request.toForgeEvent("Policy denied: ${policyDecision.rule.ruleId}", ForgeEventLevel.WARN))
            return ActionVerdict.Deny(DenyReason.PolicyViolation, policyDecision.rule)
        }

        // 2. Rate limiting
        if (!rateLimiter.checkAndRecord(request.toolId, request.originNodeId)) {
            auditLogger.log(request.toAuditEntry("RATE_LIMITED", "DENY"))
            forge.emit(request.toForgeEvent("Rate limited: ${request.toolId.value}", ForgeEventLevel.WARN))
            return ActionVerdict.Deny(DenyReason.RateLimited, RateLimitPseudoRule)
        }

        // 3. Confidence threshold check
        if (request.confidenceScore < MINIMUM_CONFIDENCE_TO_ACT) {
            auditLogger.log(request.toAuditEntry("CONFIDENCE_TOO_LOW", "DENY"))
            return ActionVerdict.Deny(DenyReason.ConfidenceTooLow, ConfidencePseudoRule)
        }

        // 4. RequiresConsent policy
        if (policyDecision is PolicyDecision.RequireConsent) {
            auditLogger.log(request.toAuditEntry("CONSENT_REQUIRED", "PENDING"))
            return ActionVerdict.RequiresConsent(
                ConsentPrompt(
                    toolId = request.toolId,
                    description = policyDecision.prompt,
                    args = request.args
                )
            )
        }

        // 5. All clear
        auditLogger.log(request.toAuditEntry("ALLOWED", "ALLOW"))
        forge.emit(request.toForgeEvent("Action allowed: ${request.toolId.value}", ForgeEventLevel.DEBUG))
        return ActionVerdict.Allow
    }

    companion object {
        const val MINIMUM_CONFIDENCE_TO_ACT = 0.65f
    }
}
```

### 4.2 ToolExecutionGuard (Complete)

```kotlin
class ToolExecutionGuardImpl @Inject constructor(
    private val safetyGate: SafetyGate,
    private val argValidator: ArgValidator,
    private val consentGate: UserConsentGate,
    private val toolRegistry: ToolRegistry,
    private val auditLogger: AuditLogger,
    private val forge: ForgeEventBus
) : ToolExecutionGuard {

    override suspend fun execute(request: ToolRequest): ToolResult {
        val startMs = System.currentTimeMillis()

        forge.emit(ForgeEvent(
            sessionId = request.boundGoal?.sessionId ?: "none",
            category = ForgeEventCategory.TOOL_EXECUTION,
            level = ForgeEventLevel.DEBUG,
            component = "ToolExecutionGuard",
            message = "Tool request received: ${request.toolId.value}",
            data = mapOf("origin" to request.origin.toString())
        ))

        // Step 1: Tool must exist in allowlist
        val tool = toolRegistry.get(request.toolId)
            ?: return ToolResult.PolicyDenied.also {
                auditLogger.log(request.toAuditEntry("UNKNOWN_TOOL", "DENY"))
                forge.emit(request.toForgeEvent("Rejected: unregistered tool ${request.toolId.value}", ForgeEventLevel.ERROR))
            }

        // Step 2: Remote origin check — some tools can never be called remotely
        if (request.origin is RequestOrigin.Remote && !tool.allowedFromRemote) {
            auditLogger.log(request.toAuditEntry("REMOTE_DENIED", "DENY"))
            return ToolResult.PolicyDenied
        }

        // Step 3: Argument validation against schema
        val validationResult = argValidator.validate(request.args, tool.schema)
        if (validationResult is ValidationResult.Invalid) {
            forge.emit(request.toForgeEvent("Arg validation failed: ${validationResult.errors}", ForgeEventLevel.WARN))
            return ToolResult.ValidationFailed(validationResult.errors)
        }

        // Step 4: Safety gate evaluation
        val actionRequest = ActionRequest(
            originNodeId = when (val o = request.origin) {
                is RequestOrigin.Local -> "LOCAL"
                is RequestOrigin.Remote -> o.nodeId
            },
            toolId = request.toolId,
            args = request.args,
            goalId = request.boundGoal?.goal?.id,
            confidenceScore = request.boundGoal?.let { 1.0f } ?: 0.8f
        )

        return when (val verdict = safetyGate.evaluate(actionRequest)) {
            is ActionVerdict.Deny -> ToolResult.PolicyDenied
            is ActionVerdict.KillSwitchEngaged -> ToolResult.PolicyDenied
            is ActionVerdict.RateLimited -> ToolResult.RateLimited
            is ActionVerdict.RequiresConsent -> {
                // Suspend and show consent dialog — this blocks the coroutine
                val consentResult = consentGate.requestConsent(verdict.prompt)
                if (consentResult == ConsentResult.DENIED) {
                    auditLogger.log(request.toAuditEntry("CONSENT_DENIED", "DENY"))
                    return ToolResult.ConsentDenied
                }
                auditLogger.log(request.toAuditEntry("CONSENT_GRANTED", "ALLOW"))
                tool.execute(request.args, buildContext(request))
            }
            is ActionVerdict.Allow, is ActionVerdict.AllowWithConstraints -> {
                val modifiedArgs = if (verdict is ActionVerdict.AllowWithConstraints)
                    verdict.modifiedRequest.args else request.args
                tool.execute(modifiedArgs, buildContext(request))
            }
        }.also { result ->
            val durationMs = System.currentTimeMillis() - startMs
            forge.emit(ForgeEvent(
                sessionId = request.boundGoal?.sessionId ?: "none",
                category = ForgeEventCategory.TOOL_EXECUTION,
                level = if (result is ToolResult.Success) ForgeEventLevel.INFO else ForgeEventLevel.WARN,
                component = "ToolExecutionGuard",
                message = "Tool ${request.toolId.value} → ${result::class.simpleName}",
                durationMs = durationMs
            ))
        }
    }

    private fun buildContext(request: ToolRequest) = ToolContext(
        sessionId = request.boundGoal?.sessionId ?: "none",
        originNodeId = when (val o = request.origin) {
            is RequestOrigin.Local -> "LOCAL"
            is RequestOrigin.Remote -> o.nodeId
        }
    )
}
```

### 4.3 NonceManager (Complete — Replay Protection)

```kotlin
class NonceManagerImpl : NonceManager {
    // Thread-safe set of seen nonces within the time window
    // Using LinkedHashMap as LRU cache bounded to MAX_NONCES
    private val seenNonces = Collections.synchronizedMap(
        object : LinkedHashMap<String, Long>(MAX_NONCES, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Long>) =
                size > MAX_NONCES
        }
    )
    private val rng = SecureRandom()

    override fun generate(): ByteArray =
        ByteArray(16).also { rng.nextBytes(it) }

    override fun validate(nonce: ByteArray, timestamp: Long): Boolean {
        // 1. Timestamp must be within ±30 seconds of local clock
        val now = System.currentTimeMillis()
        if (abs(now - timestamp) > TIMESTAMP_TOLERANCE_MS) return false

        // 2. Nonce must not have been seen before
        val nonceKey = nonce.toHexString()
        return !seenNonces.containsKey(nonceKey)
    }

    override fun consume(nonce: ByteArray) {
        seenNonces[nonce.toHexString()] = System.currentTimeMillis()
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_NONCES = 10_000
        const val TIMESTAMP_TOLERANCE_MS = 30_000L
    }
}
```

### 4.4 ReActLoopV2 (Complete)

```kotlin
class ReActLoopV2Impl @Inject constructor(
    private val llmEngine: LlmEngine,
    private val toolGuard: ToolExecutionGuard,
    private val workingMemory: WorkingMemory,
    private val episodicMemory: EpisodicMemory,
    private val semanticMemory: SemanticMemory,
    private val loopDetector: LoopDetector,
    private val confidenceScorer: ConfidenceScorer,
    private val constraintSystem: GoalConstraintSystem,
    private val forge: ForgeEventBus,
    private val killSwitch: KillSwitch
) : ReActLoopV2 {

    private var cancellationReason: String? = null

    override suspend fun execute(goal: BoundGoal): AgentResult {
        loopDetector.reset()
        var iteration = 0
        val maxIterations = goal.policy.maxIterations

        forge.emit(forgeEvent(goal, CognitionStage.Plan(goal.goal.description), "ReAct loop starting"))

        while (iteration < maxIterations) {
            if (killSwitch.isEngaged.value) return AgentResult.KillSwitchEngaged
            cancellationReason?.let { return AgentResult.Failure(FailureReason.GoalConstraintViolation(it), CognitionStage.Plan(goal.goal.description)) }

            // ─── PLAN ──────────────────────────────────────────────────────────
            val memContext = buildMemoryContext(goal)
            val plan = plan(goal, memContext, iteration) ?: return AgentResult.Failure(
                FailureReason.LlmError("Planning returned null"),
                CognitionStage.Plan(goal.goal.description)
            )

            // ─── REASON ────────────────────────────────────────────────────────
            val reasoning = reason(plan, memContext)

            // ─── CRITIQUE ──────────────────────────────────────────────────────
            val critique = critique(reasoning, goal)

            // ─── REFINE ────────────────────────────────────────────────────────
            val refined = if (critique.hasCriticalIssues) {
                forge.emit(forgeEvent(goal, CognitionStage.Critique(critique), "Critique flagged issues — refining"))
                refine(plan, critique) ?: return AgentResult.Failure(
                    FailureReason.LlmError("Refinement failed"),
                    CognitionStage.Critique(critique)
                )
            } else plan

            // ─── DECIDE ────────────────────────────────────────────────────────
            val confidence = confidenceScorer.score(refined, memContext, iteration)
            if (confidence < goal.policy.minConfidenceThreshold) {
                forge.emit(forgeEvent(goal, CognitionStage.Decide(refined, confidence), "Confidence too low: $confidence"))
                return AgentResult.Failure(FailureReason.ConfidenceTooLow(confidence, goal.policy.minConfidenceThreshold), CognitionStage.Decide(refined, confidence))
            }

            val decision = decide(refined, confidence)

            // ─── LOOP DETECTION ────────────────────────────────────────────────
            val fingerprint = StateFingerprint(
                stage = "Decide",
                memoryHash = workingMemory.snapshot().hashCode(),
                lastToolSequence = decision.proposedTools,
                iterationIndex = iteration
            )
            if (loopDetector.record(fingerprint) is LoopCheckResult.LoopDetected) {
                forge.emit(forgeEvent(goal, CognitionStage.Decide(refined, confidence), "Loop detected at iteration $iteration", ForgeEventLevel.ERROR))
                return AgentResult.LoopDetected
            }

            // ─── ACT ───────────────────────────────────────────────────────────
            if (decision.isTerminal) {
                return AgentResult.Success(decision.output ?: "", iteration + 1)
            }

            val toolResults = mutableListOf<ToolResult>()
            for (toolCall in decision.toolCalls) {
                // Each tool call goes through the full sandbox chain
                val toolRequest = ToolRequest(
                    toolId = toolCall.toolId,
                    args = toolCall.args,
                    origin = RequestOrigin.Local,
                    boundGoal = goal
                )
                val result = toolGuard.execute(toolRequest)
                toolResults.add(result)

                // Consent denied is a hard stop
                if (result == ToolResult.ConsentDenied) {
                    return AgentResult.ConsentDenied(toolCall.toolId)
                }

                // Feed result into working memory for next iteration
                workingMemory.put("tool_result_${toolCall.toolId.value}_$iteration", result)
            }

            // Record episodic event
            episodicMemory.record(EpisodicEvent(
                type = EpisodicEventType.STEP_COMPLETED,
                description = "Iteration $iteration: ${decision.toolCalls.size} tool(s) called",
                importance = confidence,
                linkedGoalId = goal.goal.id
            ))

            iteration++
        }

        return AgentResult.Failure(FailureReason.MaxIterationsExceeded(maxIterations), CognitionStage.Act(AgentDecision.empty()))
    }

    override fun cancel(reason: String) { cancellationReason = reason }

    private suspend fun plan(goal: BoundGoal, ctx: MemoryContext, iteration: Int): AgentPlan? {
        val prompt = PromptBuilder.buildPlanPrompt(goal, ctx, iteration)
        val response = llmEngine.generate(prompt) ?: return null
        return OutputParser.parsePlan(response)
    }

    private suspend fun reason(plan: AgentPlan, ctx: MemoryContext): AgentReasoning {
        val prompt = PromptBuilder.buildReasonPrompt(plan, ctx)
        val response = llmEngine.generate(prompt) ?: return AgentReasoning.empty()
        return OutputParser.parseReasoning(response)
    }

    private suspend fun critique(reasoning: AgentReasoning, goal: BoundGoal): CritiqueResult {
        val prompt = PromptBuilder.buildCritiquePrompt(reasoning, goal.policy)
        val response = llmEngine.generate(prompt) ?: return CritiqueResult.empty()
        return OutputParser.parseCritique(response)
    }

    private suspend fun refine(plan: AgentPlan, critique: CritiqueResult): AgentPlan? {
        val prompt = PromptBuilder.buildRefinePrompt(plan, critique)
        val response = llmEngine.generate(prompt) ?: return null
        return OutputParser.parsePlan(response)
    }

    private suspend fun decide(plan: AgentPlan, confidence: Float): AgentDecision {
        val prompt = PromptBuilder.buildDecidePrompt(plan, confidence)
        val response = llmEngine.generate(prompt) ?: return AgentDecision.empty()
        return OutputParser.parseDecision(response)
    }

    private suspend fun buildMemoryContext(goal: BoundGoal): MemoryContext {
        val recent = episodicMemory.recallRecent(10)
        val semantic = semanticMemory.search(goal.goal.description, topK = 5)
        val working = workingMemory.getAll()
        return MemoryContext(recent, semantic, working)
    }

    private fun forgeEvent(
        goal: BoundGoal, stage: CognitionStage, message: String,
        level: ForgeEventLevel = ForgeEventLevel.DEBUG
    ) = ForgeEvent(
        sessionId = goal.sessionId,
        category = ForgeEventCategory.COGNITION,
        level = level,
        component = "ReActLoopV2",
        message = message,
        linkedGoalId = goal.goal.id
    )
}
```

### 4.5 TrustScorer (Complete)

```kotlin
class TrustScorerImpl @Inject constructor(
    private val trustRegistry: TrustRegistry
) : TrustScorer {

    override suspend fun computeDelta(
        nodeId: String,
        event: TrustEvent
    ): TrustDelta {
        val current = trustRegistry.getScore(nodeId)?.score ?: INITIAL_SCORE

        val delta = when (event) {
            is TrustEvent.MessageDeliveredSuccessfully -> +0.01f
            is TrustEvent.MessageSignatureValid -> +0.02f
            is TrustEvent.TaskCompletedCorrectly -> +0.05f
            is TrustEvent.HeartbeatReceived -> +0.005f
            is TrustEvent.SignatureInvalid -> -0.30f    // Hard penalty
            is TrustEvent.ReplayAttackDetected -> -0.50f // Hard penalty
            is TrustEvent.MalformedMessage -> -0.10f
            is TrustEvent.PolicyViolationAttempt -> -0.25f
            is TrustEvent.TaskFailed -> -0.05f
            is TrustEvent.UnexpectedBehavior -> -0.15f
        }

        // Clamp to [0.0, 1.0]
        val newScore = (current + delta).coerceIn(0.0f, 1.0f)

        // Automatic revocation if score drops below threshold
        if (newScore < REVOCATION_THRESHOLD) {
            trustRegistry.revoke(nodeId, RevocationReason.ScoreTooLow(newScore))
        }

        return TrustDelta(delta, newScore)
    }

    companion object {
        const val INITIAL_SCORE = 0.1f        // New peers start untrusted
        const val REVOCATION_THRESHOLD = 0.05f
    }
}

sealed class TrustEvent {
    object MessageDeliveredSuccessfully : TrustEvent()
    object MessageSignatureValid : TrustEvent()
    object TaskCompletedCorrectly : TrustEvent()
    object HeartbeatReceived : TrustEvent()
    object SignatureInvalid : TrustEvent()
    object ReplayAttackDetected : TrustEvent()
    object MalformedMessage : TrustEvent()
    data class PolicyViolationAttempt(val toolId: ToolId) : TrustEvent()
    object TaskFailed : TrustEvent()
    object UnexpectedBehavior : TrustEvent()
}
```

### 4.6 KillSwitch (Complete)

```kotlin
class KillSwitchImpl @Inject constructor(
    private val auditLogger: AuditLogger,
    private val forge: ForgeEventBus,
    private val prefs: EncryptedPrefs
) : KillSwitch {

    private val _isEngaged = MutableStateFlow(
        prefs.getBoolean(PREF_KILL_SWITCH_ENGAGED, false)
    )
    override val isEngaged: StateFlow<Boolean> = _isEngaged.asStateFlow()

    override fun engage(reason: String) {
        _isEngaged.value = true
        prefs.putBoolean(PREF_KILL_SWITCH_ENGAGED, true)
        runBlocking {
            auditLogger.log(AuditEntry(
                eventType = AuditEventType.KILL_SWITCH_ENGAGED,
                actorNodeId = "LOCAL",
                toolId = null,
                args = reason,
                verdict = "ENGAGED",
                policyRuleId = null,
                goalId = null,
                sessionId = "SYSTEM"
            ))
            forge.emit(ForgeEvent(
                sessionId = "SYSTEM",
                category = ForgeEventCategory.SAFETY,
                level = ForgeEventLevel.CRITICAL,
                component = "KillSwitch",
                message = "KILL SWITCH ENGAGED: $reason"
            ))
        }
    }

    override suspend fun disengage(authToken: BiometricAuthToken): Result<Unit> {
        if (!authToken.isValid()) return Result.failure(SecurityException("Invalid biometric token"))
        _isEngaged.value = false
        prefs.putBoolean(PREF_KILL_SWITCH_ENGAGED, false)
        auditLogger.log(AuditEntry(
            eventType = AuditEventType.KILL_SWITCH_DISENGAGED,
            actorNodeId = "LOCAL",
            toolId = null, args = null,
            verdict = "DISENGAGED", policyRuleId = null,
            goalId = null, sessionId = "SYSTEM"
        ))
        return Result.success(Unit)
    }

    companion object {
        const val PREF_KILL_SWITCH_ENGAGED = "kill_switch_engaged"
    }
}
```

### 4.7 DefaultPolicies (Production defaults)

```kotlin
object DefaultPolicies {
    fun productionRules(): List<PolicyRule> = listOf(

        // SMS and Calls always require consent — no exceptions
        PolicyRule.RequireConsent("p001", priority = 100, ToolId.SMS,
            "An agent wants to send an SMS. Review the recipient and message before approving."),
        PolicyRule.RequireConsent("p002", priority = 100, ToolId.CALL,
            "An agent wants to initiate a phone call. Approve only if you requested this."),

        // Camera always requires consent
        PolicyRule.RequireConsent("p003", priority = 100, ToolId.CAMERA_CAPTURE,
            "An agent wants to capture a photo. Approve only if expected."),

        // Location consent required (can be downgraded to approximate in policy)
        PolicyRule.RequireConsent("p004", priority = 90, ToolId.LOCATION_GET,
            "An agent wants to access your precise location."),

        // Remote nodes CANNOT trigger SMS, calls, or camera — ever
        PolicyRule.DenyRemoteOrigin("p005", priority = 200, ToolId.SMS),
        PolicyRule.DenyRemoteOrigin("p006", priority = 200, ToolId.CALL),
        PolicyRule.DenyRemoteOrigin("p007", priority = 200, ToolId.CAMERA_CAPTURE),
        PolicyRule.DenyRemoteOrigin("p008", priority = 200, ToolId.LOCATION_GET),

        // Rate limits
        PolicyRule.RateLimit("p009", priority = 50, ToolId.SMS,
            maxCallsPerWindow = 3, windowMs = 60_000),
        PolicyRule.RateLimit("p010", priority = 50, ToolId.NOTIFY,
            maxCallsPerWindow = 10, windowMs = 60_000),
        PolicyRule.RateLimit("p011", priority = 50, ToolId.MESH_SEND,
            maxCallsPerWindow = 50, windowMs = 60_000),

        // Minimum confidence — low-confidence LLM decisions cannot trigger tools
        PolicyRule.RequireMinConfidence("p012", priority = 80, minScore = 0.65f),

        // No autonomous activity at night without explicit override
        PolicyRule.DenyDuringHours("p013", priority = 60, startHour = 22, endHour = 6)
    )

    fun remoteNodeGoalPolicy(): GoalPolicy = GoalPolicy(
        allowedToolIds = setOf(ToolId.MESH_SEND, ToolId.DB_QUERY, ToolId.NOTIFY),
        maxIterations = 5,
        maxDurationMs = 30_000,
        allowRemoteDelegation = false,    // No chaining remote delegations
        requiresHumanApproval = true,
        minConfidenceThreshold = 0.75f,
        budgetTokens = 2048,
        forbiddenKeywords = setOf()
    )
}
```

---

## SECTION 5 — PROTOCOL JSON SCHEMAS

### 5.1 MeshMessage Envelope

```json
{
  "$schema": "https://json-schema.org/draft/2020-12",
  "$id": "meshai.protocol.MeshMessage/v1",
  "type": "object",
  "required": ["version","messageId","type","senderId","timestamp","nonce","ttl","payload","signature"],
  "properties": {
    "version":     { "type": "integer", "minimum": 1, "maximum": 999 },
    "messageId":   { "type": "string", "format": "uuid" },
    "type":        { "type": "string", "enum": ["TASK","RESULT","STATE","HEARTBEAT","TRUST_UPDATE","HANDSHAKE_INIT","HANDSHAKE_RESPONSE","REVOCATION"] },
    "senderId":    { "type": "string", "pattern": "^[a-f0-9]{64}$" },
    "recipientId": { "type": ["string","null"], "pattern": "^[a-f0-9]{64}$" },
    "timestamp":   { "type": "integer", "description": "Unix millis" },
    "nonce":       { "type": "string", "description": "Base64url-encoded 16 bytes" },
    "ttl":         { "type": "integer", "minimum": 1, "maximum": 10 },
    "payload":     { "type": "string", "description": "Base64url AES-256-GCM encrypted payload" },
    "signature":   { "type": "string", "description": "Base64url Ed25519 signature" },
    "hopPath":     { "type": "array", "items": { "type": "string" }, "maxItems": 10 }
  },
  "additionalProperties": false
}
```

### 5.2 TASK Payload (decrypted)

```json
{
  "$id": "meshai.protocol.TaskPayload/v1",
  "type": "object",
  "required": ["goalId","description","policy","requestedAt"],
  "properties": {
    "goalId":       { "type": "string", "format": "uuid" },
    "description":  { "type": "string", "maxLength": 1024 },
    "policy": {
      "type": "object",
      "required": ["allowedToolIds","maxIterations","maxDurationMs","allowRemoteDelegation","requiresHumanApproval","minConfidenceThreshold","budgetTokens"],
      "properties": {
        "allowedToolIds":         { "type": "array", "items": { "type": "string" } },
        "maxIterations":          { "type": "integer", "maximum": 20 },
        "maxDurationMs":          { "type": "integer", "maximum": 300000 },
        "allowRemoteDelegation":  { "type": "boolean" },
        "requiresHumanApproval":  { "type": "boolean" },
        "minConfidenceThreshold": { "type": "number", "minimum": 0, "maximum": 1 },
        "budgetTokens":           { "type": "integer", "maximum": 8192 }
      }
    },
    "requestedAt":  { "type": "integer" }
  }
}
```

### 5.3 RESULT Payload

```json
{
  "$id": "meshai.protocol.ResultPayload/v1",
  "type": "object",
  "required": ["goalId","status","completedAt"],
  "properties": {
    "goalId":       { "type": "string", "format": "uuid" },
    "status":       { "type": "string", "enum": ["SUCCESS","PARTIAL","FAILURE","LOOP_DETECTED","CONSENT_DENIED","KILLED"] },
    "output":       { "type": ["string","null"], "maxLength": 4096 },
    "stepsExecuted":{ "type": "integer" },
    "failureReason":{ "type": ["string","null"] },
    "completedAt":  { "type": "integer" }
  }
}
```

### 5.4 HEARTBEAT Payload

```json
{
  "$id": "meshai.protocol.HeartbeatPayload/v1",
  "type": "object",
  "required": ["nodeId","appVersion","protocolVersion","activeGoalCount","trustScoreMap","timestamp"],
  "properties": {
    "nodeId":           { "type": "string" },
    "appVersion":       { "type": "string" },
    "protocolVersion":  { "type": "integer" },
    "activeGoalCount":  { "type": "integer" },
    "trustScoreMap":    { "type": "object", "description": "partial view — own scores of known peers" },
    "timestamp":        { "type": "integer" },
    "killSwitchEngaged":{ "type": "boolean" }
  }
}
```

### 5.5 TRUST_UPDATE Payload

```json
{
  "$id": "meshai.protocol.TrustUpdatePayload/v1",
  "type": "object",
  "required": ["subjectNodeId","newScore","reason","updatedAt","revoking"],
  "properties": {
    "subjectNodeId": { "type": "string" },
    "newScore":      { "type": "number", "minimum": 0, "maximum": 1 },
    "reason":        { "type": "string", "enum": ["REPLAY_ATTACK","SIGNATURE_INVALID","POLICY_VIOLATION","MALFORMED_MESSAGE","SCORE_TOO_LOW","MANUAL","TASK_SUCCESS"] },
    "revoking":      { "type": "boolean" },
    "updatedAt":     { "type": "integer" }
  }
}
```

### 5.6 HANDSHAKE_INIT Payload

```json
{
  "$id": "meshai.protocol.HandshakeInitPayload/v1",
  "type": "object",
  "required": ["initiatorPublicKey","challengeNonce","appVersion","protocolVersion","timestamp"],
  "properties": {
    "initiatorPublicKey": { "type": "string", "description": "Base64url Ed25519 public key" },
    "challengeNonce":     { "type": "string", "description": "Base64url 32 bytes" },
    "appVersion":         { "type": "string" },
    "protocolVersion":    { "type": "integer" },
    "timestamp":          { "type": "integer" }
  }
}
```

---

## SECTION 6 — SECURITY MODEL

### 6.1 Device Identity

Every MeshAI device generates an **Ed25519 key pair stored in Android Keystore** with hardware-backed storage (`StrongBox` where available, TEE fallback). The `nodeId` is the SHA-256 hash of the public key, encoded as 64 hex characters. The private key never leaves the secure element — all signing is done via `KeyStore.getInstance("AndroidKeyStore")` with `Signature.getInstance("Ed25519")`.

Keys are generated with:
- `KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY`
- `setIsStrongBoxBacked(true)` where supported
- `setUserAuthenticationRequired(false)` — key is device-scope, not user-scope (biometric required only for KillSwitch disengage)
- `setAttestationChallenge(...)` — enables remote attestation if ever needed

### 6.2 Message Security

Every outbound `MeshMessage`:
1. Payload serialized to JSON
2. Payload encrypted: AES-256-GCM, random 12-byte IV, session key derived via ECDH (X25519) during handshake
3. Envelope fields (version, messageId, type, senderId, timestamp, nonce) are signed with sender's Ed25519 key
4. Nonce is 16 cryptographically random bytes, single-use, validated against `NonceManager`

Every inbound message:
1. Signature verified against sender's registered public key in `TrustRegistry`
2. Nonce checked for uniqueness + timestamp checked within ±30s
3. TTL decremented; message dropped if TTL = 0
4. Payload decrypted with session key
5. Trust score updated via `TrustScorer`

### 6.3 Zero-Trust Mesh Model

- **No implicit trust**: New peers start at score 0.1 (UNKNOWN tier). They can route messages but cannot trigger any tool execution.
- **Trust is local**: Each node maintains its own `TrustRegistry`. Trust scores are not accepted verbatim from peers — TRUST_UPDATE messages are treated as advisory inputs, not facts.
- **Capability tiers by trust score**:
  - UNKNOWN (0.0–0.2): route-only
  - PROVISIONAL (0.2–0.5): DB_QUERY, NOTIFY only
  - TRUSTED (0.5–0.8): standard tools, never high-privilege
  - VERIFIED (0.8–1.0): policy-defined full access — still requires consent for SMS/CALL/CAMERA/LOCATION
- **Remote high-privilege tools are permanently denied** via `DenyRemoteOrigin` rules regardless of trust tier. SMS, CALL, CAMERA, LOCATION cannot be triggered by any remote node.

### 6.4 Session Key Derivation

```
ECDH(initiator_X25519_ephemeral, responder_X25519_ephemeral)
  → HKDF-SHA256(shared_secret, salt=challengeNonce, info="meshai-session-v1")
  → 32-byte AES-256-GCM session key
```

Session keys are not persisted. A new handshake is required on reconnection.

---

## SECTION 7 — DATA FLOW WALKTHROUGH (End-to-End)

### Scenario: Remote node delegates a TASK to local device

```
Remote Node (TRUSTED tier, score 0.65)
  │
  ├─ 1. Constructs TaskPayload JSON with GoalPolicy
  ├─ 2. Encrypts with session key (AES-256-GCM)
  ├─ 3. Signs envelope (Ed25519)
  ├─ 4. Sets TTL=3, nonce=random16bytes
  └─ 5. Transmits via Nearby/BLE/Meshrabiya

Local Device — MeshTransport receives raw bytes
  │
  ├─ 6. Deserialize MeshMessage envelope
  ├─ 7. NonceManager.validate(nonce, timestamp) → reject if replay/stale
  ├─ 8. TrustRegistry.getScore(senderId) → score=0.65, tier=TRUSTED
  ├─ 9. MessageSigner.verify(payload, signature, senderPublicKey) → valid
  ├─ 10. TrustScorer.computeDelta(SIGNATURE_VALID) → +0.02
  ├─ 11. NonceManager.consume(nonce)
  └─ 12. Decrypt payload → TaskPayload

AgentOrchestrator
  │
  ├─ 13. Parse goal + policy from TaskPayload
  ├─ 14. GoalConstraintSystem.bind(goal, policy)
  │       → Validate policy fields (maxIterations ≤ 20, no SMS in allowedTools, etc.)
  ├─ 15. ConflictDetector.checkConflict(newGoal, activeGoals) → NoConflict
  ├─ 16. HumanInTheLoopGate: policy.requiresHumanApproval=true
  │       → Show notification: "Node abc123 wants to run goal: XYZ. Approve?"
  │       → User approves → BoundGoal created
  └─ 17. ReActLoopV2.execute(boundGoal)

ReActLoopV2 — Iteration 0
  │
  ├─ 18. Plan: LLM generates plan given goal + memory context
  ├─ 19. Reason: LLM reasons over plan
  ├─ 20. Critique: LLM critiques reasoning
  ├─ 21. Refine: clean pass (no critical issues)
  ├─ 22. ConfidenceScorer → 0.78 (above 0.75 threshold)
  ├─ 23. Decide: AgentDecision { toolCalls: [DB_QUERY(args)] }
  ├─ 24. LoopDetector.record(fingerprint) → Safe
  └─ 25. Act: ToolExecutionGuard.execute(ToolRequest(DB_QUERY, LOCAL, boundGoal))

ToolExecutionGuard
  │
  ├─ 26. ToolRegistry.get(DB_QUERY) → found, allowedFromRemote=true (origin is LOCAL)
  ├─ 27. ArgValidator.validate(args, schema) → Valid
  ├─ 28. SafetyGate.evaluate(ActionRequest) → ...
  │         KillSwitch: NOT engaged
  │         PolicyEngine: no rule denies DB_QUERY from LOCAL at confidence 0.78
  │         RateLimiter: within limits
  │         → ActionVerdict.Allow
  ├─ 29. DbQueryTool.execute(args, context)
  └─ 30. ToolResult.Success(data)

ReActLoopV2 — Iteration continues...
  │
  ├─ 31. WorkingMemory.put("tool_result_db_query_0", result)
  ├─ 32. EpisodicMemory.record(STEP_COMPLETED)
  ├─ 33. ForgeEventBus.emit(TOOL_EXECUTION/INFO: "DB_QUERY → Success")
  └─ 34. Next iteration...

Goal completion
  │
  ├─ 35. AgentResult.Success(output, stepsExecuted=2)
  ├─ 36. Construct ResultPayload
  ├─ 37. Encrypt + sign RESULT message
  ├─ 38. MeshTransport.send(resultMessage, targetNodeId=senderId)
  └─ 39. AuditLogger.log(GOAL_COMPLETED, ...)
```

---

## SECTION 8 — THREAT MODEL (STRIDE)

### S — Spoofing

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Peer impersonation | Attacker claims to be a trusted node | Hardware-backed Ed25519 identity, `nodeId = SHA256(pubkey)` |
| Replay of valid messages | Retransmit a captured valid message | NonceManager (single-use nonces + ±30s timestamp window) |
| Key cloning | Attacker extracts private key | Android Keystore StrongBox — keys are non-exportable |

### T — Tampering

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Message payload modification | Alter task parameters in transit | AES-256-GCM authenticated encryption (GCM tag detects tampering) |
| Arg injection | Malformed args bypass validation | ArgValidator schema enforcement — reject unknown fields, validate types/ranges |
| Memory poisoning | Remote node injects false memories | WorkingMemory only writeable by local ReActLoopV2; episodic memory source-tagged |
| Policy injection | Remote node tries to override GoalPolicy | GoalConstraintSystem rejects policies that exceed local maximums |

### R — Repudiation

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Deny taking action | Node claims it didn't send a message | Ed25519 signatures are non-repudiable; AuditLogger is append-only |
| Log tampering | Erase audit trail | AuditEntity uses Room with no delete permission at runtime; exported logs are signed |

### I — Information Disclosure

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Eavesdropping on mesh | Passive attacker reads mesh traffic | All payloads AES-256-GCM encrypted with session key |
| Memory exfiltration | Remote task queries sensitive memory | DbQueryTool has schema allowlist — cannot return arbitrary tables |
| Location leakage | Heartbeats expose location | HEARTBEAT payloads do not include location; LOCATION_GET always requires consent |

### D — Denial of Service

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Message flooding | Malicious node sends thousands of messages | RateLimiter per-node per-tool; TTL limits hop propagation |
| Goal bombing | Remote node submits infinite goals | GoalEngine queue size bounded; PROVISIONAL/UNKNOWN tier cannot submit goals |
| Loop exhaustion | Agent stuck in infinite reasoning loop | LoopDetector + MaxIterationsExceeded + maxDurationMs in GoalPolicy |
| Battery drain attack | Keep agent awake via constant heartbeats | Foreground service with WorkManager constraints; battery optimization handling |

### E — Elevation of Privilege

| Threat | Attack | Mitigation |
|--------|--------|-----------|
| Remote SMS trigger | Compromised TRUSTED peer triggers SMS | `DenyRemoteOrigin` rule permanently blocks SMS/CALL/CAMERA/LOCATION from any remote origin |
| Trust tier escalation | Peer fakes successful tasks to raise score | Trust delta per event is small (+0.05 max); score changes are slow; VERIFIED requires sustained good behavior |
| LLM jailbreak | Prompt injection causes LLM to bypass safety | LLM output goes through ArgValidator + SafetyGate before any tool executes — LLM cannot directly invoke tools |
| KillSwitch bypass | Attacker attempts to re-enable agent | Disengage requires valid biometric `BiometricAuthToken` + audit log entry |

---

## SECTION 9 — MIGRATION PLAN

### Phase 0: Pre-migration (Day 1–2)
- [ ] Tag current HEAD as `v0-unsafe-baseline`
- [ ] Document current DB schema and Room migrations
- [ ] List all current tool invocation sites — every place tools are called directly
- [ ] Identify all places `GoalEngine` or `ReActLoop` currently call tools

### Phase 1: Safety Foundation (Day 3–7) — BLOCKING
These must land before any new features.

- [ ] Implement `SafetyGate`, `PolicyEngine`, `KillSwitch`, `AuditLogger`
- [ ] Implement `DefaultPolicies.productionRules()`
- [ ] Wire `KillSwitch` to UI with a persistent notification action
- [ ] Add `AuditEntity` + `AuditDao` to Room DB, write migration
- [ ] Add `ForgeEventBus` and `ForgeLogger` — instrument SafetyGate immediately
- [ ] Verify: every existing tool call path now passes through SafetyGate

### Phase 2: Tool Sandbox (Day 8–12)
- [ ] Implement `ToolExecutionGuard`, `ArgValidator`, `UserConsentGate`
- [ ] Define `ToolSchema` for every existing tool (SMS, CALL, CAMERA, LOCATION, NOTIFY)
- [ ] Implement `ToolRegistry` with the allowlist
- [ ] Replace every direct tool invocation with `toolExecutionGuard.execute(...)`
- [ ] Add `DenyRemoteOrigin` rules to `DefaultPolicies`
- [ ] Add consent dialogs to `UserConsentGate` (bottom sheet, not toast)

### Phase 3: Protocol Hardening (Day 13–18)
- [ ] Implement `DeviceIdentity` — generate Ed25519 key in Android Keystore on first run
- [ ] Implement `MessageSigner` (sign + verify)
- [ ] Implement `NonceManager`
- [ ] Migrate `MeshMessage` to new schema with `version`, `nonce`, `signature`, `hopPath`
- [ ] Implement `PeerVerificationHandshake`
- [ ] Implement `TrustRegistry` + `TrustScorer`
- [ ] Add `TrustTier`-based capability gating in `ToolExecutionGuard`
- [ ] Room migration: `NodeEntity` gains `publicKey`, `trustScore`, `trustTier`, `isRevoked`

### Phase 4: Cognition Upgrade (Day 19–25)
- [ ] Implement `LoopDetector`
- [ ] Implement `ConfidenceScorer` (heuristic + LLM self-assessment)
- [ ] Refactor `ReActLoop` → `ReActLoopV2` with 6 stages
- [ ] Implement `GoalConstraintSystem` + `GoalPolicy`
- [ ] Implement `ConflictDetector`
- [ ] Implement `HumanInTheLoopGate`
- [ ] Wire `ReActLoopV2` to `ForgeEventBus` at every stage transition

### Phase 5: Memory System (Day 26–30)
- [ ] Implement `WorkingMemory` (in-memory, LRU-bounded)
- [ ] Implement `EpisodicMemory` (Room-backed, existing `AgentMemory` migration)
- [ ] Implement `ImportanceScorer` + `MemoryDecay`
- [ ] Implement `SemanticMemory` (on-device embedding via Gemma's embedding layer)
- [ ] `MemoryConsolidator` scheduled via WorkManager (nightly)

### Phase 6: Observability + Hardening (Day 31–35)
- [ ] Implement `LocalReplayBuffer` (circular, 1000 events)
- [ ] Implement `DebugInspector` (query interface for debug builds)
- [ ] Add `ObservabilityDashboard` to UI (debug builds only)
- [ ] ProGuard/R8 rules for all new classes
- [ ] Dependency security review (audit `build.gradle` against known CVEs)
- [ ] Battery optimization: add `PowerManager.WakeLock` release paths, `WorkManager` constraints

### Phase 7: Integration Testing (Day 36–40)
- [ ] Two-device mesh integration test
- [ ] Replay attack test (capture and re-send a valid message)
- [ ] Kill switch test (engage mid-goal)
- [ ] Trust tier test (UNKNOWN peer attempts high-privilege tool)
- [ ] Loop detection test (craft a goal that produces identical states)
- [ ] Consent gate test (verify UI blocks before execution)

---

## SECTION 10 — TRADEOFFS AND ASSUMPTIONS

### Tradeoffs

**Ed25519 in Android Keystore vs Bouncy Castle**
Using Android Keystore means keys are hardware-protected but introduces platform dependency and potential latency (~5–15ms per sign on older devices). The security benefit outweighs the latency cost for this threat model. On API < 28 (pre-StrongBox), fall back to TEE-backed keys with a warning log.

**Blocking consent dialogs**
`UserConsentGate` suspends the coroutine until the user responds. This means a long-running goal can stall indefinitely. Mitigation: `boundGoal.policy.maxDurationMs` includes consent wait time; a timeout is applied (default 30s) after which the consent is treated as DENIED.

**Trust scoring is advisory, not cryptographic**
Trust scores are soft signals. A well-behaved attacker could slowly raise their score. The hard mitigation is `DenyRemoteOrigin` for high-privilege tools — trust tiers only gate medium-privilege tools. No amount of trust allows a remote peer to trigger SMS/CALL/CAMERA/LOCATION.

**On-device LLM quality**
Gemini Nano / Gemma 2B on-device models produce lower quality reasoning than cloud LLMs. The Critique and ConfidenceScorer stages compensate somewhat, but users should expect lower goal completion rates than cloud-backed agents. This is an explicit design choice: privacy > capability.

**Gossip protocol and convergence**
The gossip-based node discovery does not guarantee strong consistency. Trust score gossip is advisory only — each node recomputes scores from its own observations. This is correct behavior for a zero-trust model but means the network can have temporarily inconsistent views of a node's trustworthiness.

### Assumptions

1. Android Keystore hardware-backed storage is available on target devices (API 28+, StrongBox preferred).
2. MediaPipe Tasks SDK is pinned to a specific version; LLM engine interface is stable.
3. Meshrabiya/Nearby/BLE transports provide message ordering best-effort, not guaranteed. `RetryManager` handles losses.
4. The user is the device owner. Multi-user Android profiles are out of scope for v1.
5. The threat model assumes an external attacker, not a compromised Android OS. Rooted devices are explicitly out of scope.
6. `ForgeEvent` data in debug builds may contain sensitive goal descriptions — `DebugInspector` is disabled in release builds via a compile-time flag.
7. Vector embeddings for `SemanticMemory` are approximate (cosine similarity) and do not require a vector DB — a simple in-memory `FloatArray` dot product suffices for a single device's memory corpus.

---

## VERSION HISTORY

| Version | Date | Notes |
|---------|------|-------|
| 1.0 | 2026-04-16 | Initial production spec |

---

*This document is a living spec. Each follow-up implementation pass should bump the version and record changes.*
