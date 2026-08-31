# Ezrahi — Agent Instructions

## Living Project Roadmap
The file `docs/roadmaps/תוכנית פיתוח מפורטת למערכת Ezrahi.html` is the **living
project roadmap** and must be kept up to date whenever work progresses or the
tech stack changes.

When you complete work or the architecture changes, update this HTML file to
match reality:
- **Check** any task/sub-task that is now implemented but unchecked.
- **Uncheck** any task/sub-task that is checked but is not actually implemented.
- **Update the tech-stack wording** wherever it references osmdroid, Firebase
  Realtime Database, or SQLite (the current stack uses **MapLibre**, **Firestore**,
  and **Room** respectively).

> Note: the page persists checkbox state in `localStorage` under
> `ezrahi_checklist_vN_`. If you change the `checked` attributes in the HTML
> source, also bump the `storageKeyPrefix` version in the `<script>` block so a
> stale browser cache does not override the corrected states.