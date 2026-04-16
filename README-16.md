# 🕸️ MeshAI — Decentralized Autonomous AI Agents with Multi-Radio Mesh Networking

> **Every Android phone is a node. Every node is an agent. The mesh never sleeps.**

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://github.com/stackbleed-ctrl/MeshAI/blob/main/LICENSE)
[![Android](https://img.shields.io/badge/Android-16%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple)](https://kotlinlang.org)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue)](https://developer.android.com/about/versions/16)
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
- **BLE GATT** — ultra-low-power discovery and small payload beaconing
- Automatic hybrid routing: Mesh → Nearby → Cellular/Wi-Fi fallback
- Offline-first: full functionality with zero internet

### 🤖 Autonomous AI Agent Core

- **Gemini Nano** via Android AICore (on-device, no API key)
- **Gemma 2B/7B** via MediaPipe LLM Inference (fallback)
- **ReAct-style reasoning loop**: Think → Act → Observe → Repeat
- Short-term memory + shared encrypted mesh knowledge base
- Persistent `ForegroundService` + `WorkManager` for 24/7 operation

### 🛠️ Device Tool Use

Agents can autonomously:

- 📱 Send SMS via `SmsManager`
- 📞 Answer/screen calls via `CallScreeningService` + `ConnectionService`
- 🔔 Read & respond to notifications via `NotificationListenerService`
- 📷 Access camera, GPS, accelerometer, microphone
- 🗂️ Delegate tasks across the mesh based on battery/capability

### 🧠 Human Hand-off & Autonomy

- **Owner Unavailable** detection: screen-off >30 min, DND mode, low battery, or explicit toggle
- Agents proactively complete queued tasks, respond to messages and calls on your behalf
- Define high-level goals: *"Monitor front door. Alert me if motion. Order groceries weekly."*
- Agents decompose goals into subtasks and execute across the mesh

### 🔒 Security

- Noise protocol for mesh message encryption
- Jetpack Security (AES-256-GCM) for local storage
- Age encryption for cross-node key exchange
- Permission-gated with graceful degradation

---

## 📸 Screenshots

> Screenshots coming soon. The dashboard, mesh map, and task queue views are under active development.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│   Jetpack Compose · Material 3 · Dashboard · Mesh Map    │
├─────────────────────────────────────────────────────────┤
│                    Agent Core Layer                      │
│   ReAct Loop · Goal Decomposition · Memory · Planning    │
├──────────────┬──────────────────────┬───────────────────┤
│   LLM Layer  │     Tool Layer       │   Mesh Layer       │
│  Gemini Nano │  SMS · Calls · Notif │  Meshrabiya        │
│  Gemma 2B/7B │  Camera · Location   │  Nearby Conn.      │
│  MediaPipe   │  Sensors             │  BLE GATT          │
├──────────────┴──────────────────────┴───────────────────┤
│                   Infrastructure Layer                   │
│   Room · DataStore · Hilt · Coroutines · Flow            │
│   Jetpack Security · Noise Protocol · WorkManager        │
└─────────────────────────────────────────────────────────┘
```

---

## 🗂️ Key Source Files

The repository is currently structured as a flat collection of Kotlin source files at the root alongside the build config. A modular multi-module layout is planned.

| File | Purpose |
|------|---------|
| `AgentNode.kt` | Core agent node identity and state |
| `ReActLoop.kt` | Think → Act → Observe reasoning loop |
| `GoalEngine.kt` | High-level goal decomposition into subtasks |
| `AgentMemory.kt` | Short-term + mesh-shared memory store |
| `AgentWorker.kt` | WorkManager task execution |
| `AgentForegroundService.kt` | 24/7 persistent background service |
| `LlmEngine.kt` | Gemini Nano / Gemma abstraction layer |
| `MeshNetwork.kt` | Unified mesh routing coordinator |
| `MeshrabiyaLayer.kt` | Wi-Fi Direct multi-hop mesh |
| `NearbyLayer.kt` | Google Nearby Connections transport |
| `BleGattLayer.kt` | BLE GATT beacon and discovery |
| `MeshEncryption.kt` | Noise protocol + Age key exchange |
| `ToolRegistry.kt` | Dynamic tool registration for agents |
| `SmsTool.kt` / `CallTool.kt` | SMS and call tool implementations |
| `CameraTool.kt` / `LocationTool.kt` | Camera and GPS tools |
| `NotificationTool.kt` | Notification listener tool |
| `OwnerPresenceDetector.kt` | Owner availability heuristics |
| `AgentRepository.kt` | Data access layer |
| `MeshAIDatabase.kt` | Room database definition |
| `DashboardScreen.kt` / `DashboardViewModel.kt` | Main UI |
| `AppModule.kt` | Hilt dependency injection module |

---

## 🚀 Setup

### Prerequisites

- Android Studio **Meerkat 2025.1+** (or Ladybug 2024.2+)
- Android device running **Android 12+** (SDK 31+); **Android 16 (SDK 36) recommended**
- Device must support **Wi-Fi Direct** for Meshrabiya mesh features
- Gemini Nano requires a **Pixel 8+** or compatible AICore device — other devices automatically fall back to Gemma via MediaPipe

### Build & Run

```bash
git clone https://github.com/stackbleed-ctrl/MeshAI.git
cd MeshAI
# Open in Android Studio, sync Gradle, then run on a physical device
./gradlew assembleDebug
```

> **Note:** Mesh networking features (Wi-Fi Direct, BLE) require a **physical device**. Most cannot be tested on an emulator.

### Dependency Versions

Dependencies are managed via the version catalog in `libs.versions.toml`. Sync Gradle after cloning to resolve all libraries automatically.

### Required Permissions

Grant all permissions on first launch. Some permissions require manual steps in Settings:

| Permission | Why | How to Grant |
|------------|-----|--------------|
| `MANAGE_OWN_CALLS` | Call screening | Declared in manifest |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Read & respond to notifications | **Settings → Apps → Special app access → Notification access** |
| `BIND_CALL_SCREENING_SERVICE` | Screen incoming calls | **Settings → Apps → Special app access → Default calling app** |
| `ACCESS_FINE_LOCATION` | Wi-Fi Direct / BLE scanning | Runtime prompt on launch |
| `NEARBY_WIFI_DEVICES` (Android 13+) | Meshrabiya Wi-Fi mesh | Runtime prompt on launch |

---

## 📦 Releases

The latest release is **[v0.1.0](https://github.com/stackbleed-ctrl/MeshAI/releases/tag/v0.1.0)** — an initial Android APK build.

---

## 🤝 Contributing

See [CONTRIBUTING.md](https://github.com/stackbleed-ctrl/MeshAI/blob/main/CONTRIBUTING.md) for guidelines on opening issues, submitting pull requests, and the code style expectations.

---

## 📄 License

MIT License — see [LICENSE](https://github.com/stackbleed-ctrl/MeshAI/blob/main/LICENSE).

---

## ⚠️ Disclaimer

MeshAI is a **research/experimental project**. Autonomous device control (call answering, SMS sending, notification access) requires explicit user consent and careful configuration. Always comply with local laws regarding automated communications. The authors are not responsible for misuse.
