# Todo — Phase 7: Off-Grid LoRa / Mesh Architecture Preparation (Days 37–40)

> Source: `../roadmaps/modernization-roadmap.md` "📻 Phase 7: LoRa / Mesh Architecture Preparation".
> Phases 0–6 complete (HEAD: `f97a67f` on `refactor/modernization-v2`).

## Objective
Prepare an abstraction layer that lets the app seamlessly switch to off-grid hardware (Meshtastic BLE device or custom LoRa transceiver) when internet connectivity fails — without touching ViewModels, Room, or the Map UI.

## Tasks

### Task 7.1: Build the Mesh Hardware Interface
- [ ] `app/src/main/java/com/arielfaridja/ezrahi/domain/mesh/MeshTransceiver.kt` — pure interface:
  - `fun isDeviceConnected(): Flow<Boolean>`
  - `suspend fun connectToNode(bluetoothAddress: String): Result<Unit>`
  - `suspend fun broadcastLocationPacket(location: GeoPoint): Result<Unit>`
  - `suspend fun broadcastEmergencyPacket(message: FieldMessage): Result<Unit>`
  - `fun observeIncomingPackets(): Flow<FieldMessage>`
- [ ] Depends on existing domain models only (`FieldMessage`, `GeoPoint` — both exist in `domain/model/FieldModels.kt`).
- [ ] Build green (no consumers yet; interface-only commit).

### Task 7.2: (Future) Bluetooth LoRa implementation
- [ ] Deferred until hardware is available (Meshtastic BLE device or custom LoRa transceiver). Add a class implementing `MeshTransceiver` when hardware arrives — no other changes needed.

### Task 7.3: (Future) Service-layer degradation hook
- [ ] Deferred: when internet fails (ConnectivityManager listener), swap `EzrahiRepository` writes to the mesh transceiver path. Not in Phase 7 scope — Phase 7 is interface-only.

## Constraints / Notes
- Pure abstraction phase: **no production code changes** beyond the new interface file.
- Do NOT bind `MeshTransceiver` in Hilt yet (no implementation exists); a `@Binds` would fail DI at runtime.
- Keep the interface in `domain/mesh/` so implementations (BLE/cloud relays) live in `data/mesh/` later.

## Definition of Done (Phase 7)
- [ ] `MeshTransceiver.kt` interface matches the roadmap signatures exactly.
- [ ] Build green; no behavioral changes.
