# Todo — Audit Fix 3: Event Management Screen

> Source: `refactor-fixes.md` §1 Functional Regression #2 — Missing Activity/Event Management Screen.
> Requirement: event managers create/edit events, view + assign role permissions, upload/switch GPX routes, inspect field report checkpoints.
> Current status: only an `activity_overview` placeholder route exists.

## Scope Decision
- **In scope now:** edit (rename) event, view participants, assign/change participant roles (manager only), inspect field reports summary. Replaces the `activity_overview` placeholder.
- **Deferred:** event *creation* (no create UI exists in the picker — matches legacy; keep), GPX upload/switch (fix-5, `todo-fix-5.md` — screen shows a placeholder line).
- **Rules change required:** current `participants` write rule is `request.auth.uid == userId` only → manager role assignment would be DENIED. Change to `request.auth.uid == userId || isEventManager(eventId)` and redeploy.

## Tasks
- [ ] `EzrahiRepository` + impl: `updateParticipantRole(eventId, userId, role)` and `updateEventName(eventId, name)` (Firestore update, `Result<Unit>`).
- [ ] `firestore.rules`: participants write = own doc OR event manager; deploy (`firebase deploy --only firestore:rules`).
- [ ] `app/ui/management/EventManagementViewModel.kt`: loads event + participants + reports (`getEventUpdates`/`getParticipants`/`getReports`); `isManager` (auth.uid == event.managerId); `assignRole(...)`, `renameEvent(...)` with status messages.
- [ ] `app/ui/management/EventManagementScreen.kt`:
  - eventId null → "Select an activity first".
  - Event card: name (+ rename field/button when manager), manager uid, GPX route status line (placeholder → real in fix-5).
  - Participants: name, phone, role label; role dropdown to change role (manager only).
  - Reports: count by type + recent reports (title, type, time).
- [ ] `MainActivity`: replace `activity_overview` placeholder with `EventManagementScreen(eventId = currentEventId, ...)`.
- [ ] Build green, stage APK.
- [ ] PO test: open Activity Overview from drawer → rename event, change a role, verify reports listed; verify map still shows updated roles.
- [ ] Commit; update `refactor-fixes.md` (F2 → done).

## Constraints / Notes
- Only the event manager (managerId) can rename or change roles — UI hides controls otherwise; rules enforce server-side.
- Role change writes only the `role` field of `events/{eventId}/participants/{userId}`.
- Icons: core material-icons set only.