# Todo — Audit Fix 4: Role-Based Messaging (dynamic roles)

> Source: `refactor-fixes.md` §1 Functional Regression #6 — Superficial Role-Based Messaging.
> Requirement: separate WhatsApp-style channels filtered by role (e.g., "Medics Only", "Staff Only", "All Broadcast").
> Current status: `messages/{eventId}` placeholder only; `getMessages` returns one flat stream.

## Design: dynamic roles (user requirement)
Role options are **not hardcoded in UI code**. The app loads a role list from a
Firestore settings document (`settings/roles`, field `options` = array of
`{name, label, isStaff}`), with the built-in enum list as fallback when the doc
is missing/empty. The chat channels, management filter chips, and the role
combobox all render from this list — swapping roles later means editing one
Firestore doc (or a future settings screen), not code.

## Tasks
- [ ] `RoleOption` model (`domain/model`) + shared `app/util/RoleOptions.kt` (defaults + label helpers); dedupe `roleLabel` from QuickDial/Management screens.
- [ ] `EzrahiRepository.getRoleOptions(): Flow<List<RoleOption>>` — callbackFlow listener on `settings/roles` doc, fallback to defaults; leak-safe.
- [ ] `firestore.rules`: `match /settings/{docId} { allow read: if isSignedIn(); allow write: if false; }` → deploy.
- [ ] Seed `settings/roles` with the 5 roles (admin REST write).
- [ ] `app/ui/chat/ChatViewModel.kt`: collects messages (sorted by timestamp), role options, own participant role; `send(text, targetRole)` with sender name/role.
- [ ] `app/ui/chat/ChatScreen.kt`: channel tabs (All Broadcast + one per dynamic role), message list (bubbles, emergency styling, mine/others), input row + send.
- [ ] `MainActivity`: replace `messages/{eventId}` placeholder with `ChatScreen`.
- [ ] Event Management: filter chips + role combobox now driven by `getRoleOptions()` (not static list).
- [ ] Build green, stage APK.
- [ ] PO test: chat channels (All + per-role), send targeted message → visible only in that channel; change `settings/roles` doc → app reflects without rebuild.
- [ ] Commit; update `refactor-fixes.md` (F6 → done).

## Constraints / Notes
- `targetRole` stays `UserRole?` in the domain/Room model (null = broadcast). Unknown dynamic role names are mapped to `null` (broadcast) until the enum catches up.
- Rules for messages unchanged (participant create, `senderId == uid`).
- Icons: core set only (`Send`, `Menu`).