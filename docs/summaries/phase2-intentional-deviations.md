# Phase 2 — Recorded Intentional Deviations (DO NOT FIX)

These deviations from docs/roadmaps/modernization-execution-plan.md section 2.2 (Phase 2) are deliberate design decisions, not bugs or incomplete work. Do not correct them without revisiting the rationale below — several look like mistakes but are load-bearing.

## D1. Multi-bearer routing is inlined in TacticalDispatchEngine (no standalone MultiBearerRouter)
- Plan diagram (lines 93-113) shows TacticalDispatchEngine -> MultiBearerRouter -> transports, but the Phase 2 task list (lines 343-352) only requires implementing the engine.
- The router role is fulfilled by `orderedAdapters = adapters.sortedBy { it.bearer.ordinal }` (core/network/.../transport/TacticalDispatchEngine.kt:42) plus, in each consumer, `orderedAdapters.any { adapter -> runCatching { adapter.sendX(...) }.onFailure{...}.getOrDefault(false) }` (telemetry :92-96, message :102-106, report :113-117).
- `.any{}` walks adapters in `bearer.ordinal` order and stops at the first successful delivery = multi-bearer failover.
- Intentional: fewer classes, still extensible (add a transport = add an @IntoSet provider). Extract a dedicated MultiBearerRouter only if routing logic grows beyond simple failover.

## D2. Engine uses 3 dispatch channels, not the plan's 2
- Plan spec (lines 348-349): telemetry `Channel(cap=1, DROP_OLDEST)` + one `Channel<FieldMessage>(UNLIMITED)`.
- Code (TacticalDispatchEngine.kt:44-46): `telemetryChannel` (DROP_OLDEST, cap 1), `messageChannel` (UNLIMITED — messages + SOS), `reportChannel` (UNLIMITED — FieldReport).
- Intentional: `TacticalTransportAdapter.sendReport(report)` (line 29) and the separate `FieldReport` type require a dedicated channel/consumer to call `adapter.sendReport(...)`. SOS still flows through `messageChannel` as `FieldMessage(isEmergency=true)`, enqueued priority 1. Meets the verification deliverable (DROP_OLDEST telemetry + reliable SOS/messages/reports).

## D3. MeshTransportAdapter is a deliberate skeleton (bearer BLUETOOTH_LE, stubs sendReport/sendMessage)
- Plan task 6 (line 351) explicitly requires a mock/skeleton to validate multi-bearer extensibility.
- core/network/.../transport/MeshTransportAdapter.kt:18 uses `TransportBearer.BLUETOOTH_LE` (the wrapped MeshTransceiver in domain/mesh is BLE-style); `sendReport`/`sendMessage` return false (lines 40-42); their observers return `emptyFlow()` (lines 49, 51-52). `sendTelemetry`/`sendEmergency` ARE wired to the transceiver.
- Intentional: the skeleton only needs to prove the @IntoSet multibinding injects a 2nd adapter and the engine tolerates a non-delivering one (falls through to Firebase -> outbox). Real mesh transport is a future-phase task; fill the stubs (and possibly flip bearer to LORA_MESH) when hardware lands.

## D4. @JvmSuppressWildcards on the engine's Set<TacticalTransportAdapter> injection is load-bearing
- TacticalDispatchEngine.kt:32 injects `@JvmSuppressWildcards Set<TacticalTransportAdapter>`.
- Kotlin's `Set<out E>` variance compiles the constructor param to `Set<? extends ...>` in bytecode, which is a different Dagger binding key than the provider's `Set<...>` from TransportModule.kt:31-37 (@IntoSet Firebase + Mesh). Without the annotation, `:app:hiltJavaCompileDebug` fails with `error: [Dagger/MissingBinding] java.util.Set<? extends ...> cannot be provided without an @Provides-annotated method.`
- Intentional / DO NOT REMOVE: if adapter injection ever breaks the Hilt build, this annotation is the cause. Reverting to a plain `Set<...>` silently reintroduces the failure.

## Verification — Interactive Smoke Test (Phase 2)
Conducted manually on-device after `assembleDebug` green + `:core:network:testDebugUnitTest` 3/3 pass.

- Step 1 — Launch: app opens without crash; auto-login + event auto-select succeed; lands on Map with markers/reports; other screens navigate; no toast. PASS
- Step 2 — Online send: sent `smoke-test-1` on all broadcast; no crash; message shows in chat UI; confirmed present in Firestore `events/{eventId}/messages` via Firebase CLI. PASS
- Step 3 — Offline enqueue: airplane mode on; sent `smoke-test-offline-q`; no crash; chat UI showed it (optimistic); Firestore did NOT contain it while offline (enqueued to Room outbox). After restoring network, message appeared. PASS
- Step 4 — Exactly-once flush: with device online, Firebase CLI query for `smoke-test-offline-q`:
  - (a) present? YES
  - (b) exactly 1 document (not duplicated)? YES — exactly 1
  - (c) `smoke-test-1` also exactly 1? YES
  - => Outbox flush delivered exactly-once; no double-send. PASS

Verified: offline -> Room outbox -> OutboxSyncWorker (NetworkType.CONNECTED) flush loop works; no crash; no swallowed errors; no duplicate delivery.

Relied upon (not manually re-tested): telemetry DROP_OLDEST bloat (unit test `telemetry_channel_drops_oldest_when_consumer_is_slow`); mesh skeleton failover safety (MeshTransportAdapter returns false -> falls through to Firebase/outbox, unit-covered + code review); TransportErrorLogger -> ExceptionLogger / Firestore app_errors path (code review; fix-6 intact).
