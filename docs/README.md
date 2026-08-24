# Ezrahi — Documentation

All project documentation (except this repo's `README.md`) lives under `docs/`.

## Roadmaps (`roadmaps/`)

- [`roadmaps/modernization-roadmap.md`](./roadmaps/modernization-roadmap.md) — overall migration roadmap (Phases 1–7)
- [`roadmaps/תוכנית פיתוח מפורטת למערכת Ezrahi.html`](./roadmaps/תוכנית%20פיתוח%20מפורטת%20למערכת%20Ezrahi.html) — detailed development plan (Hebrew, HTML)

## Summaries (`summaries/`)

- [`summaries/refactor-fixes.md`](./summaries/refactor-fixes.md) — audit findings + progress tracker of the current fix series (F1–F6, B1)

## Planning (`planning/`)

Per-phase work plans derived from the roadmap:

- [`planning/todo.md`](./planning/todo.md)
- [`planning/todo-1.md`](./planning/todo-1.md) — Phase 1: Architecture & DI
- [`planning/todo-2.md`](./planning/todo-2.md) — Phase 2: Auth & Persistence
- [`planning/todo-3.md`](./planning/todo-3.md) — Phase 3: Event Management
- [`planning/todo-4.md`](./planning/todo-4.md) — Phase 4: Map
- [`planning/todo-5.md`](./planning/todo-5.md) — Phase 5: Reports
- [`planning/todo-6.md`](./planning/todo-6.md) — Phase 6: Security & Rules
- [`planning/todo-7.md`](./planning/todo-7.md) — Phase 7: LoRa / Mesh preparation

## Specifications (`specs/`)

Analysis & specification documents for the audit fix series:

- [`specs/todo-fix-1.md`](./specs/todo-fix-1.md) — SnapshotListener leaks (B1)
- [`specs/todo-fix-2.md`](./specs/todo-fix-2.md) — Quick-Dial screen (F1)
- [`specs/todo-fix-3.md`](./specs/todo-fix-3.md) — Event Management screen (F2)
- [`specs/todo-fix-4.md`](./specs/todo-fix-4.md) — Role-based messaging (F6)
- [`specs/todo-fix-5.md`](./specs/todo-fix-5.md) — GPX routes (F3)
- [`specs/todo-fix-6-exception-logging.md`](./specs/todo-fix-6-exception-logging.md) — global frontend exception logging (spec only, not yet developed)
- [`specs/event-map-layers-hybrid-spec.md`](./specs/event-map-layers-hybrid-spec.md) — hybrid map strategy: OpenFreeMap basemap + offline regions + event trails (spec only, not yet developed)
- [`specs/MAPLIBRE_CRASH_DEEP_DIVE.md`](./specs/MAPLIBRE_CRASH_DEEP_DIVE.md) — MapLibre crash investigation
- [`specs/maplibre-migration-fix-summary.md`](./specs/maplibre-migration-fix-summary.md) — osmdroid → MapLibre migration & crash resolution

## Resolved issues

- [`DRAWER_MAP_TOUCH_CONFLICT.md`](./DRAWER_MAP_TOUCH_CONFLICT.md) — resolved drawer/map gesture conflict

## Notes

- Open questions awaiting the PO / a professional review are kept at the bottom of each spec document (e.g. `todo-fix-6-exception-logging.md` §9). A dedicated `to-ask-professional/` folder will be created if there's content for it.