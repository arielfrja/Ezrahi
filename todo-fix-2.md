# Todo — Audit Fix 2: Role-Based Quick Dial Screen

> Source: `refactor-fixes.md` §1 Functional Regression #1 — Quick-Dial Screen.
> Original requirement: a role-based emergency/contact dialer where guides and participants instantly call role-holders (Lead Guide, Medic, Sweep Guide, Basecamp) without knowing their numbers.
> Current status: only a `speed_dial` placeholder route exists.

## Tasks
- [ ] `app/ui/dial/QuickDialViewModel.kt` — `hiltViewModel`, mirrors `MapViewModel` pattern (`loadEvent(eventId)` → `repository.getParticipants(eventId)`), restarts its collect job when the event changes.
- [ ] `app/ui/dial/QuickDialScreen.kt`:
  - TopAppBar with drawer hamburger; `eventId == null` → "Select an activity first" empty state.
  - Lists **staff** participants only (`role != MEMBER` and phone not blank), grouped by role with Hebrew/English role labels.
  - Each card: name, role, phone number; tap or call button → `Intent(ACTION_DIAL, tel:...)`.
  - Empty staff → "No staff contacts yet".
- [ ] `MainActivity.kt`: hoist `currentEventId` state (set on event select), replace `speed_dial` placeholder with `QuickDialScreen(eventId = currentEventId, ...)`.
- [ ] Build green, stage APK.
- [ ] PO test: pick an event → drawer → Speed Dial → staff roles listed → tap dials.
- [ ] Commit; update `refactor-fixes.md` (F1 → done).

## Constraints / Notes
- Uses `ACTION_DIAL` (not `ACTION_CALL`) — no `CALL_PHONE` permission needed; user confirms before calling.
- Staff roles: MANAGER, LEAD_GUIDE, SWEEP_GUIDE, MEDIC, LOGISTICS (anything but MEMBER).
- Icons: stick to the material-icons-core set already used (`Phone`, `Person`, `Menu`) — no extended-icons dependency.
- No data-model changes; reads use the leak-fixed `getParticipants`.