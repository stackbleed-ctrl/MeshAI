# MeshAI v2 Upgrade Notes

Seven files. Six bugs fixed. One security model rebuilt from scratch.  
Everything below is ready to commit. Read this before opening the first PR.

---

## What changed and why

### 1. `ReActLoop.kt` — 4 bugs fixed

| Bug | Symptom | Fix |
|-----|---------|-----|
| Concurrent tasks overwrote `_loopState` | Silent state corruption on mesh-delegated tasks | `executionMutex` — `Mutex().withLock {}` wraps the entire `execute()` body |
| History grew to 12k+ tokens, silently truncated | LLM returned garbage on step 8+ | `pruneHistory()` — sliding window keeps task prompt + last 4 exchange pairs, summarises middle |
| `parseToolCall` returned only `{` for multiline JSON | Tool calls silently failed, loop burned remaining steps doing nothing | Multi-line collector — gathers all lines after `Action Input:` until next structural keyword |
| Max-steps returned raw LLM reasoning as task result | Owner notification showed `Thought: I should...` | Clean failure string built from `agentMemory.recall()` of the last recorded step |

**Integration:** Drop-in replacement for `ReActLoop.kt`. No dependency changes.

---

### 2. `MeshEncryption.kt` — Security model rebuilt

**The original problem:** Every node generated its own AES key in its own Keystore. That key is hardware-bound and never leaves the device. Result: Node A encrypted with Key-A; Node B tried to decrypt with Key-B. Failure. Every cross-node message hit the `0x00` fallback and was transmitted **unencrypted** in production. The `Timber.w("[Encryption] Received unencrypted message")` was firing on every peer message.

**The fix:** ECDH per-pair session keys.

1. Each node generates an EC secp256r1 identity keypair in AndroidKeyStore on first run (`MeshAI_NodeIdentity_v2` alias).
2. On peer connection, the transport layer (`NearbyLayer`, `BleGattLayer`) calls `exportPublicKeyBytes()` and sends it to the peer. Peer sends theirs back.
3. `registerPeer(peerNodeId, peerPubKeyBytes)` runs ECDH + HKDF-SHA256 to derive a 32-byte AES-256 session key, cached in memory for that pair.
4. All messages between that pair use the shared key — both sides derive the same secret without ever transmitting it.

**Wire format change:** Messages now start with version byte `0x02`. The old `0x00` plaintext fallback marker is now **rejected** with a `SecurityException` rather than silently accepted. Old nodes must upgrade before joining the mesh.

**AAD replay prevention:** Every `encrypt()` call binds `"$senderNodeId:$counter"` as GCM Additional Authenticated Data. A replayed message from Node A cannot be re-presented by Node B — the AAD mismatch causes GCM authentication failure.

**Atomic key rotation:** `rotateIdentityKey()` generates the new key under a temp alias before deleting the old one. The original two-step delete/generate had a crash window that would leave the node with no key.

**Integration steps:**
1. `NearbyLayer.onEndpointConnected()` — add call to `meshEncryption.registerPeer(endpointId, exchangedPubKeyBytes)`
2. `BleGattLayer` — add public key exchange to the GATT handshake characteristic
3. `MeshNetwork.send()` — update to call `meshEncryption.encrypt(payload, peerNodeId, localNode.nodeId)`
4. `MeshNetwork.onMessageReceived()` — update to call `meshEncryption.decrypt(bytes, peerNodeId, senderNodeId, senderCounter)`
5. Add `senderNodeId` and `senderCounter` fields to `MeshMessage` wire format

---

### 3. `AgentForegroundService.kt` — 3 bugs fixed

| Bug | Symptom | Fix |
|-----|---------|-----|
| `initializeAgent()` not idempotent | After 3 OS restarts (START_STICKY), 3 concurrent loops pulled the same tasks from Room and executed them in parallel | `agentLoopJob?.cancel()` at top of `initializeAgent()` before re-launching |
| `acquireWakeLock()` called concurrently | Each concurrent task orphaned a 5-minute wakelock; 3 queued tasks = 15 min of battery drain with no release path | `withWakeLock { }` — `Mutex + try/finally` — only one lock held at a time, always released |
| `registerReceiver()` called on `Dispatchers.Default` thread | On several OEMs silently failed; screen-off/on not detected; owner presence broken | Moved to `onStartCommand()` body (main thread), before any coroutine launch |

**Integration:** Drop-in replacement. No new dependencies.

---

### 4. `AgentTask.kt` — Distributed lease fields added

Four new nullable columns:

```
ownerNodeId          String  — who created the task (immutable)
executorNodeId       String? — who claimed execution
executorLeasedAt     Long?   — when the claim was made (ms)
executorLeaseExpiry  Long?   — when the claim expires (ms)
```

Helper methods on the data class:
- `isClaimedBy(nodeId)` — returns true if another node holds a valid lease
- `withLease(nodeId)` — returns a copy with lease set (5-min expiry)
- `withLeaseClear()` — returns a copy with lease fields nulled

**Integration:** Requires `MIGRATION_1_2` (see below).

---

### 5. `TaskLeaseManager.kt` — New file

Distributed task deduplication. Inject alongside `AgentRepository`.  

Call before executing any task:

```kotlin
// In AgentForegroundService startAgentLoop():
val task = pendingTasks.first()
if (!taskLeaseManager.claimTask(task.taskId, localNode.nodeId)) {
    continue  // Another node claimed it — skip
}

// During long-running tasks, renew every 2 min:
taskLeaseManager.renewLease(task.taskId, localNode.nodeId)

// On completion or failure:
taskLeaseManager.releaseTask(task.taskId, localNode.nodeId)
```

Add a periodic WorkManager task to call `findExpiredLeases()` and requeue stale tasks.

---

### 6. `AgentTaskDao.kt` — New lease queries

Add these to your existing `AgentTaskDao` interface (or replace the whole file if you don't have one yet).

Key additions:
- `getTasksWithExpiredLeases(nowMs)` — finds orphaned in-progress tasks
- `claimTaskIfAvailable(...)` — SQL-level atomic claim for migration scripts
- `clearLease(taskId)` — nulls executor fields

---

### 7. `DatabaseMigrations.kt` — Room migration 1→2

Register in `MeshAIDatabase`:

```kotlin
@Database(entities = [AgentTask::class, ...], version = 2)
abstract class MeshAIDatabase : RoomDatabase() { ... }

// In AppModule or wherever you build the Room instance:
Room.databaseBuilder(context, MeshAIDatabase::class.java, "meshai.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

---

### 8. `NodeRouter.kt` — New file

Capability-scored peer selection for task delegation.  
Replaces the `isConnected()` check in `MeshNetwork.send()`.

Usage:
```kotlin
// Inject NodeRouter into MeshNetwork or AgentRepository
val target = nodeRouter.bestNodeForTask(task, meshNetwork.connectedPeers())
if (target != null) {
    meshNetwork.delegateTask(task, target)
}
```

Scoring breakdown (max 100 pts):
- Battery level: 40 pts max (0.4 × battery%)
- Agent Mode active / owner away: 30 pts flat
- Required capability match: 4 pts per matched cap
- Extra capabilities: 0.5 pts per cap beyond required

---

## Recommended commit order

```
git add AgentTask.kt DatabaseMigrations.kt AgentTaskDao.kt
git commit -m "feat: add distributed task lease fields (Room migration 1→2)"

git add TaskLeaseManager.kt
git commit -m "feat: TaskLeaseManager — optimistic task ownership leases"

git add MeshEncryption.kt
git commit -m "fix(security): ECDH per-pair session keys — fix cross-node plaintext transmission"

git add ReActLoop.kt
git commit -m "fix(agent): mutex, history pruning, multiline parser, clean max-steps error"

git add AgentForegroundService.kt
git commit -m "fix(service): idempotent init, mutex'd wakelock, receiver on main thread"

git add NodeRouter.kt
git commit -m "feat: NodeRouter — capability-scored mesh task delegation"
```

---

## Quick test checklist

- [ ] Two physical devices, both running the app, mesh connected via Nearby
- [ ] `hasPeerSessionKey()` returns true on both sides after connect
- [ ] Send a message — `Timber.w("[Encryption] Received unencrypted message")` does NOT appear
- [ ] Kill app mid-task on both devices; restart; confirm task executes once (not twice)
- [ ] Force-kill service 3× rapidly; confirm only one agent loop running (single `[Service] Processing task:` log stream)
- [ ] Queue 3 tasks; confirm `[MeshAI:AgentTaskWakeLock]` appears once in `dumpsys power`, not 3×
- [ ] Rotate identity key; confirm both devices re-handshake and encryption continues working
- [ ] Step 9+ of a long ReAct task — confirm history pruning log appears and LLM still responds coherently
