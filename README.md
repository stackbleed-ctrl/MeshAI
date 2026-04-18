# MeshAI v2

Distributed autonomous agent OS for Android.
All three codex options applied: **A** (Architecture) + **B** (Protocol) + **C** (Transport).

## Module Map

```
MeshAI/
├── core/
│   ├── protocol/          ← MeshEnvelope v1, MeshMessage, MeshEvent, TaskResult
│   ├── model/             ← AgentTask, AgentNode, Capability, CapabilityRegistry
│   └── util/              ← Extensions, nowMs()
│
├── runtime/
│   ├── execution/         ← TaskExecutor  (deterministic, no LLM)  [NEW - Option A]
│   ├── cognition/         ← CognitionEngine wrapping ReActLoop      [NEW - Option A]
│   ├── ReActLoop.kt       ← unchanged
│   ├── GoalEngine.kt      ← unchanged
│   ├── MeshRouter.kt      ← upgraded: CapabilityRegistry + split exec [Option A]
│   ├── TelemetryCollector.kt ← records + persists every event        [NEW - Option A]
│   ├── RuntimeController.kt  ← lifecycle FSM, services call this     [NEW - Option A]
│   ├── AgentWorker.kt     ← unchanged
│   └── ToolRegistry.kt    ← unchanged
│
├── transport/
│   ├── TransportManager.kt   ← single entry, priority fallback       [NEW - Option C]
│   ├── EnvelopeDispatcher.kt ← inbound deserialise + dispatch        [NEW - Option C]
│   ├── NearbyLayer.kt        ← full lifecycle (advertise/discover)   [REWRITTEN - Option C]
│   ├── MeshrabiyaLayer.kt    ← stub (TODO: init MeshrabiyaNode)
│   └── BleGattLayer.kt       ← stub (TODO: inject Context, GattServer)
│
├── control/
│   ├── PolicyEngine.kt    ← unchanged
│   └── OwnerPresenceDetector.kt ← unchanged
│
├── storage/
│   ├── AgentRepository.kt ← extended: upsertEvent(), observeEvents()  [Option A]
│   └── AgentMemory.kt     ← unchanged
│
├── feature/dashboard/
│   ├── DashboardViewModel.kt ← wired to TelemetryCollector + CapabilityRegistry [Option A]
│   └── DashboardScreen.kt    ← unchanged
│
├── MESH_PROTOCOL_v1_SPEC.md  ← full protocol spec                    [Option B]
└── ARCHITECTURE_CHANGES.md   ← detailed change log
```

## Key Invariants (v2)

1. **Every cross-node byte is a MeshEnvelope.** `MeshMessage` is for local storage only.
2. **TaskExecutor handles all non-LLM tasks.** CognitionEngine is opt-in.
3. **CapabilityRegistry drives routing.** MeshRouter never guesses which node to use.
4. **TransportManager is the only exit point.** No layer calls transport directly.
5. **TelemetryCollector sees every event.** Nothing is routed without a record.
6. **RuntimeController owns lifecycle.** Services call start/stop/pause; nothing else.

## TODO (marked in code)

- `BleGattLayer`: inject Context, init `BluetoothManager`, implement GATT server
- `MeshrabiyaLayer`: init `MeshrabiyaNode`, wire `meshrabiyaNode.send(virtualIp, bytes)`
- `EnvelopeDispatcher` ← `NearbyLayer.payloadCallback`: wire inbound bytes
- `SEC-001`: Noise_XX handshake + HMAC in NearbyLayer.connectionLifecycleCallback
- Room database: replace in-memory `AgentRepository` maps with DAOs
- `NODE_ADVERTISE` scheduler: broadcast on startup + every 15s
