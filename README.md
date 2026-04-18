# 🕸️ MeshAI — Decentralized Autonomous AI Agents with Multi-Radio Mesh Networking

> **Every Android phone is a node. Every node is an agent. The mesh never sleeps.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-16%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-31-blue)](https://developer.android.com/about/versions/12)
[![Build](https://github.com/stackbleed-ctrl/MeshAI/actions/workflows/ci.yml/badge.svg)](https://github.com/stackbleed-ctrl/MeshAI/actions)

---

## 📖 Overview

MeshAI transforms Android devices into **autonomous AI agent nodes** that self-organize into a resilient, multi-radio mesh network. No internet required. No central server. Just phones talking to phones, running local LLMs, completing tasks, and collaborating — 24/7.

**Why Android-only?**
iOS sandboxing prevents background Wi-Fi Direct usage, peer-to-peer socket servers, persistent background services, and low-level BLE GATT server operation. Apple explicitly blocks the APIs needed for true mesh networking. **Android is the only mobile platform where this is possible.**

---

## ✨ Features

### 🌐 Multi-Radio Mesh Network

- **Meshrabiya** — true multi-hop Wi-Fi mesh with virtual IPs, TCP/UDP sockets, and WPA3 encryption
- **Google Nearby Connections** — Bluetooth + Wi-Fi peer discovery and data transfer
- **BLE GATT** — ultra-low-power discovery, small payload beaconing, and key exchange
- Automatic hybrid routing: Mesh → Nearby → BLE fallback
- Offline-first: full functionality with zero internet

### 🔒 End-to-End Mesh Encryption (v2)

- **ECDH per-pair session keys** — each node pair derives a unique AES-256 session key via EC secp256r1 key agreement + HKDF-SHA256. No shared secret is ever transmitted.
- **BLE GATT key exchange** — public keys are exchanged automatically on peer discovery via a dedicated `KEY_EXCHANGE_CHAR_UUID` characteristic
- **Nearby handshake** — EC public keys are exchanged as the first payload on every new Nearby connection before any data flows
- **GCM AAD replay prevention** — every message binds `senderNodeId:counter` into the AES-GCM authentication tag, blocking cross-sender replay attacks
- **Atomic key rotation** — identity keypair rotation is crash-safe; new key is generated under a temp alias before the old key is deleted
- Keys are hardware-backed in Android Keystore where available (requires API 31+)

### 🤖 Autonomous AI Agent Core

- **Gemini Nano** via Android AICore (on-device, no API key)
- **Gemma 2B/7B** via MediaPipe LLM Inference (fallback)
- **ReAct reasoning loop** — Think → Act → Observe → Repeat, with execution mutex, sliding-window history pruning, and multiline JSON parser
- **Execution budget enforcement** — per-task token ceiling, scaled by priority (CRITICAL gets 2×, LOW gets 0.5×)
- **Structured execution traces** — every run produces a `StepTrace` per iteration: tool called, tokens spent, elapsed ms, validation errors
- **Tool output validation** — JSON schema checking before tool results enter LLM context
- Short-term memory + shared encrypted mesh knowledge base
- Persistent `ForegroundService` + `WorkManager` for 24/7 operation

### 🗂️ Distributed Task Execution

- **Optimistic task leases** — nodes claim tasks with a 5-minute renewable lease before executing, preventing duplicate execution across the mesh after reconnects
- **Capability-scored routing** — `NodeRouter` scores peer nodes on battery, agent mode status, capability match, and tool coverage before delegating
- **Lease recovery** — expired leases on crashed nodes are detected and tasks are re-queued automatically

### 🛠️ Device Tool Use

Agents can autonomously:

- 📱 Send SMS via `SmsManager`
- 📞 Answer/screen calls via `CallScreeningService` + `ConnectionService`
- 🔔 Read & respond to notifications via `NotificationListenerService`
- 📷 Access camera, GPS, accelerometer, microphone
- 🗂️ Delegate tasks across the mesh based on battery/capability scoring

### 🧠 Human Hand-off & Autonomy

- **Owner Unavailable** detection: screen-off >30 min, DND mode, low battery, or explicit toggle
- Agents proactively complete queued tasks, respond to messages and calls on your behalf
- Define high-level goals: *"Monitor front door. Alert me if motion. Order groceries weekly."*
- Agents decompose goals into subtasks and execute across the mesh

---

## 📸 Screenshots

| Mesh Map | Agent Dashboard | Task Queue |
|----------|----------------|------------|
| *(coming soon)* | *(coming soon)* | *(coming soon)* |

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                          UI Layer                             │
│   Jetpack Compose · Material 3 · Dashboard · Mesh Map         │
│   ExecutionTrace display · Task detail · Budget indicator     │
├──────────────────────────────────────────────────────────────┤
│                      Agent Core Layer                         │
│   ReActLoop · ExecutionBudget · ExecutionTrace                │
│   GoalEngine · AgentMemory · TaskLeaseManager                 │
├───────────────┬──────────────────────┬────────────────────────┤
│   LLM Layer   │      Tool Layer      │      Mesh Layer        │
│  Gemini Nano  │  SMS · Calls · Notif │  Meshrabiya            │
│  Gemma 2B/7B  │  Camera · Location   │  NearbyLayer (ECDH)    │
│  MediaPipe    │  ToolOutputValidator  │  BleGattLayer (ECDH)   │
│               │  ToolRegistry        │  NodeRouter            │
├───────────────┴──────────────────────┴────────────────────────┤
│                     Security Layer                            │
│   MeshEncryption (ECDH · HKDF · AES-256-GCM · AAD replay)    │
│   Android Keystore · Hardware-backed EC identity keypair      │
├──────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                        │
│   Room (v2 schema + lease columns) · DataStore · Hilt         │
│   Coroutines · Flow · WorkManager · AgentTaskDao              │
└──────────────────────────────────────────────────────────────┘
```

---

## 🚀 Setup

### Prerequisites

- Android Studio Meerkat 2025.1+ recommended
- Android device running **Android 12+** (SDK 31+, minSdk 31); Android 16 recommended
- Device must support **Wi-Fi Direct** for Meshrabiya mesh features
- Gemini Nano requires **Pixel 8+** or compatible AICore device; other devices fall back to Gemma

### Build & Run

```bash
git clone https://github.com/stackbleed-ctrl/MeshAI.git
cd MeshAI
# Open in Android Studio, sync Gradle, run on device
./gradlew assembleDebug
```

### Required Permissions

Grant all permissions on first launch. Agent Mode requires:

- `MANAGE_OWN_CALLS` — call screening
- `BIND_NOTIFICATION_LISTENER_SERVICE` — notification access (grant in Settings)
- `BIND_CALL_SCREENING_SERVICE` — call screening (grant in Settings)
- `BLUETOOTH_ADVERTISE` / `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` — BLE mesh (API 31+)

---

## 🗺️ Project Structure

```
com/meshai/
├── agent/
│   ├── ReActLoop.kt              # ReAct execution engine (mutex, budget, tracing, pruning)
│   ├── ExecutionBudget.kt        # Per-run token ceiling with priority scaling
│   ├── ExecutionTrace.kt         # Structured per-step observability record
│   ├── AgentTask.kt              # Task model with distributed lease fields (v2)
│   ├── AgentMemory.kt            # Short-term key-value memory with mesh gossip
│   ├── AgentNode.kt              # Node identity, capabilities, battery, status
│   ├── GoalEngine.kt             # High-level goal decomposition
│   ├── OwnerPresenceDetector.kt  # Screen-off / DND / manual agent mode detection
│   └── TaskLeaseManager.kt       # Distributed task ownership — prevents duplicate execution
│
├── mesh/
│   ├── MeshNetwork.kt            # Transport orchestration (Meshrabiya + Nearby + BLE)
│   ├── NearbyLayer.kt            # Nearby Connections with ECDH handshake + encrypted send
│   ├── BleGattLayer.kt           # BLE GATT server + client-side key exchange on discovery
│   ├── MeshrabiyaLayer.kt        # Wi-Fi mesh transport
│   ├── NodeRouter.kt             # Capability-scored peer selection for task delegation
│   └── MeshMessage.kt            # Wire message model (includes senderNodeId, counter)
│
├── security/
│   └── MeshEncryption.kt         # ECDH key exchange, HKDF, AES-256-GCM, AAD replay prevention
│
├── tools/
│   ├── ToolRegistry.kt           # Tool registration + output spec lookup
│   ├── ToolOutputValidator.kt    # JSON validation before LLM ingestion
│   ├── SmsTool.kt
│   ├── CallTool.kt
│   ├── NotificationTool.kt
│   ├── CameraTool.kt
│   └── LocationTool.kt
│
├── service/
│   ├── AgentForegroundService.kt # 24/7 service (idempotent init, mutex wakelock, priority budgets)
│   └── AgentWorker.kt            # WorkManager fallback
│
├── data/
│   ├── MeshAIDatabase.kt         # Room DB (version 2 — lease columns)
│   ├── DatabaseMigrations.kt     # Migration 1→2 (ownerNodeId, executorNodeId, lease fields)
│   ├── dao/AgentTaskDao.kt       # Includes lease queries (getTasksWithExpiredLeases, etc.)
│   └── repository/AgentRepository.kt
│
├── ui/
│   ├── DashboardScreen.kt
│   ├── DashboardViewModel.kt
│   └── OtherScreens.kt
│
└── di/
    └── AppModule.kt
```

---

## 🔐 Security Model

MeshAI v2 uses **ECDH-derived per-pair session keys** for all cross-node traffic.

| Property | Implementation |
|----------|---------------|
| Key algorithm | EC secp256r1 (AndroidKeyStore, hardware-backed) |
| Key agreement | ECDH → 32-byte shared secret |
| Key derivation | HKDF-SHA256 (no external library — standard JCE Mac) |
| Bulk encryption | AES-256-GCM |
| Replay prevention | GCM AAD = `"senderNodeId:monotonicCounter"` |
| Key exchange (Nearby) | First payload on `STATUS_OK` — `[0xEC,0x44]` frame prefix |
| Key exchange (BLE) | GATT `KEY_EXCHANGE_CHAR_UUID` read+write on peer discovery |
| Key rotation | Atomic alias swap — crash-safe |
| Session eviction | On disconnect from either transport |

> **Note:** The original v1 implementation used a single device-local AES key that never left the Keystore — meaning cross-node decryption always failed silently and all mesh traffic was transmitted unencrypted via the `0x00` fallback. This is fully replaced in v2.

---

## 📊 Execution Engine

The `ReActLoop` is a **budgeted, observable, non-reentrant** reasoning engine:

| Safeguard | Mechanism |
|-----------|-----------|
| Concurrent task safety | `Mutex` — non-reentrant singleton |
| Context window overflow | Sliding-window pruning before each LLM call |
| Multiline JSON output | Multi-line `Action Input:` collector |
| Garbage failure results | Clean error string from `AgentMemory`, not raw LLM dump |
| Runaway token use | `ExecutionBudget` — hard ceiling per run, priority-scaled |
| Tool output garbage | `ToolOutputValidator` — JSON parse + required field check |
| Silent failures | `ExecutionTrace` — per-step structured record for dashboard |

Budget ceilings by priority (default 6,000 tokens):

| Priority | Token budget |
|----------|-------------|
| LOW | 3,000 |
| NORMAL | 6,000 |
| HIGH | 9,000 |
| CRITICAL | 12,000 |

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📄 License

MIT License — see [LICENSE](LICENSE).

---

## ⚠️ Disclaimer

MeshAI is a research/experimental project. Autonomous device control (call answering, SMS sending) requires careful user consent. Always comply with local laws regarding automated communications. The authors are not responsible for misuse.
