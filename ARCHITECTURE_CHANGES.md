# MeshAI v2 — Architecture Changes
_Codex Options A + B + C applied simultaneously_

---

## Summary

| Layer | Before | After |
|-------|--------|-------|
| Protocol | Loose `MeshMessage` / `MeshEvent` | **`MeshEnvelope` v1** — versioned, typed, loop-safe |
| Runtime | Monolithic `runtime/` | **Split**: `execution/` (deterministic) + `cognition/` (LLM) |
| Routing | `MeshRouter` → `MeshTransport` directly | `MeshRouter` → `CapabilityRegistry` → `TransportManager` |
| Transport | 3 independent layers + `MeshNetwork` | `TransportManager` + `EnvelopeDispatcher` + full `NearbyLayer` |
| Telemetry | Unused `MeshEvent` | `TelemetryCollector` → `AgentRepository` → `DashboardViewModel` |
| Lifecycle | Services own logic | `RuntimeController` owns state; services call it |

---

## Option A: Production Architecture Refactor

### New files
- `runtime/execution/TaskExecutor.kt` — handles all non-LLM tasks deterministically
- `runtime/cognition/CognitionEngine.kt` — wraps ReActLoop, optional
- `runtime/TelemetryCollector.kt` — records every event, persists to storage
- `runtime/RuntimeController.kt` — lifecycle FSM (STOPPED → RUNNING → PAUSED)
- `core/model/Capability.kt` — advertised skill with routing score
- `core/model/CapabilityRegistry.kt` — in-memory registry with stale pruning

### Modified files
- `runtime/MeshRouter.kt` — now queries CapabilityRegistry before routing
- `feature/dashboard/DashboardViewModel.kt` — subscribes to TelemetryCollector + CapabilityRegistry
- `storage/AgentRepository.kt` — adds `upsertEvent()` + `observeEvents()`

### Key design decisions
- `TaskExecutor.canHandle()` returns false for `LLM_REASONING` and `DELEGATE` — those go to CognitionEngine
- `CognitionEngine` is injected optionally; devices without AI still run all deterministic tasks
- `TelemetryCollector` uses a rolling 200-event in-memory window + async Room persist
- `RuntimeController` is a pure Kotlin object — no Android dependency, fully unit-testable

---

## Option B: Mesh Protocol v1 Spec

### New files
- `core/protocol/MeshEnvelope.kt` — single versioned contract for ALL inter-node traffic
- `MESH_PROTOCOL_v1_SPEC.md` — gRPC/OpenAPI-level specification

### Modified files
- `core/protocol/MeshMessage.kt` — demoted to result storage record; adds `envelopeId` field
- `core/protocol/MeshEvent.kt` — adds `envelopeId`, `transportLayer`, `hopCount`

### Protocol guarantees (previously missing)
1. **Version negotiation** — nodes reject incompatible peers at the envelope level
2. **Type safety** — every payload is tagged with `EnvelopeType`; receivers dispatch by type, never by content sniffing
3. **Loop prevention** — `hopTrace` + TTL; loops detected and dropped
4. **Economy per-hop** — `accumulatedCostUsd` accumulates across hops; `TransportManager` enforces ceiling
5. **Security stubs** — `noiseSessionId` + `payloadHmac` fields reserved; SEC-001 can implement without breaking v1

---

## Option C: Nearby + BLE Multi-Device Demo

### New files
- `transport/TransportManager.kt` — single entry point; priority: Meshrabiya → Nearby → BLE
- `transport/EnvelopeDispatcher.kt` — inbound deserialise + validate + dispatch to SharedFlows
- `transport/NearbyLayer.kt` (rewritten) — full `ConnectionLifecycleCallback` + `EndpointDiscoveryCallback` + `PayloadCallback`

### Transport selection logic
```
TransportManager.sendEnvelope(envelope):
  for transport in [Meshrabiya, Nearby, BLE]:
    if transport.isConnected():
      result = transport.send(task)
      if result.status != FAILED: return result
  return FAILED("All transports unavailable")
```

### NearbyLayer v2 additions
- `startAdvertising(localNodeId)` — makes device discoverable on `SERVICE_ID`
- `startDiscovery()` — finds peers, auto-requests connection
- `ConnectionLifecycleCallback` — auto-accepts connections (SEC-001 will add HMAC verification)
- `EndpointDiscoveryCallback` — maintains `_connectedEndpoints` map
- `PayloadCallback` — receives bytes, hands off to `EnvelopeDispatcher`
- `StateFlow<Int>` for peer count — dashboard subscribes directly

### What's still TODO (marked in code)
- `BleGattLayer`: inject Context, init BluetoothManager, implement GATT server/client
- `MeshrabiyaLayer`: init `MeshrabiyaNode`, wire `meshrabiyaNode.send(virtualIp, payload)`
- `EnvelopeDispatcher.payloadCallback`: wire inbound bytes to `onBytesReceived()`
- SEC-001: Noise handshake + HMAC verification in `connectionLifecycleCallback`

---

## What Did NOT Change

- `ReActLoop.kt` — production quality, unchanged
- `GoalEngine.kt` — production quality, unchanged
- `ToolRegistry.kt` — production quality, unchanged
- `AgentWorker.kt` — unchanged
- `Agent.kt` — unchanged
- `AgentTask.kt`, `AgentNode.kt` — unchanged
- `DashboardScreen.kt` — UI unchanged (ViewModel feeds it the same shape)
- `MeshAITheme.kt`, `MeshAINavGraph.kt`, `OtherScreens.kt` — unchanged
