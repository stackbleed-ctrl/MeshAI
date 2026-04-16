# Contributing to MeshAI

Thank you for your interest in contributing to MeshAI! This project is experimental and welcomes contributions across all areas — networking, AI, Android, security, and documentation.

---

## 🛠️ Getting Started

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/YOUR_USERNAME/MeshAI.git`
3. Open in **Android Studio** (Ladybug 2024.2+ or Meerkat 2025.1+)
4. Sync Gradle dependencies
5. Connect a physical Android 12+ device (emulator won't support Wi-Fi Direct or BLE)
6. Run and explore

---

## 🌿 Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable, buildable code |
| `develop` | Integration branch |
| `feature/xyz` | Individual features |
| `fix/xyz` | Bug fixes |

Please target `develop` for all PRs.

---

## 📋 Areas Most Needing Help

- **Meshrabiya integration** — Wire up `VirtualMeshNode` fully in `MeshrabiyaLayer.kt`
- **Gemini Nano / AICore** — Implement the actual AICore binding in `LlmEngine.kt` (requires Pixel 8+ device)
- **Gemma model download** — Add a UI flow for downloading the Gemma 2B model file to device storage
- **Noise protocol** — Replace the AES-GCM stub with a full Noise_XX handshake in `MeshEncryption.kt`
- **Mesh map visualization** — Implement a proper Canvas/graph visualization in `MeshMapScreen.kt`
- **Call answering** — Complete the `MeshConnectionService` / `MANAGE_OWN_CALLS` path in `CallTool.kt`
- **Camera2 integration** — Complete the background still capture in `CameraTool.kt`
- **Tests** — Unit tests for `ReActLoop`, `GoalEngine`, `MeshEncryption`

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

---

## ✅ PR Checklist

- [ ] Code compiles without errors
- [ ] Lint passes (`./gradlew lint`)
- [ ] New functionality is documented with KDoc comments
- [ ] Permissions are justified in AndroidManifest.xml comments
- [ ] No hardcoded secrets, API keys, or personally identifying info
- [ ] Sensitive operations (camera, SMS, call answering) log to audit trail
- [ ] Graceful degradation when permissions are denied

---

## 🔒 Security

If you discover a security vulnerability, please **do not open a public issue**. Instead, email `security@meshai.example.com` (replace with your actual contact).

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.
