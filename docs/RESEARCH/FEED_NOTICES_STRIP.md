---
title: "Feed Notices Strip"
tags: [ui, feed, notices, digital-school]
status: active
created: 2026-08-01
---

# Feed Notices Strip

User request (2026-08-01): "feed page can have notice about events or actionable
notices on top."

## What was built

A "Notices" strip pinned to the top of `/feed` (full-width, spans the 3-column
grid via `grid-column: 1 / -1`), showing upcoming scheduled events parsed from
planner uploads:

- Source: `scheduled_event` rows with `event_date >= today` up to the end of the
  current academic year (`academicStart.plusYears(1).atDay(1).minusDays(1)`),
  ordered by date ascending (nearest first).
- Respects the feed's chat filter when set
  (`findByChatIdAndEventDateBetweenOrderByEventDateAsc`).
- Each row: event-type badge (reuses `badge-*` styles), formatted date
  ("EEE, MMM d"), title, and a red "Action needed" flag for actionable types.
- "View calendar" link to `/calendar`.

## Decision (operator can override)

Actionable types (flagged "Action needed", soft-red highlight): `EXAM`,
`SUBMISSION_DEADLINE`, `PARENT_TEACHER_MEETING`. Informational: `HOLIDAY`,
`FIELD_TRIP`, `OTHER`. `FIELD_TRIP` might warrant action (permission slips) —
flagged as informational for now.

No cap on number of notices: all upcoming events in the academic year are shown.
If a full year of planner data is uploaded this could grow long; a cap or
horizontal-scroll strip can be added if needed.

## Files changed

- `storage/ScheduledEventRepository.java` — added
  `findByChatIdAndEventDateBetweenOrderByEventDateAsc`.
- `web/FeedController.java` — injects `ScheduledEventRepository`; computes
  `notices` (`List<Notice>` record: `event`, `actionable`); static
  `ACTIONABLE_TYPES` set.
- `resources/templates/feed.html` — notices `<section>` first child of the feed
  layout.
- `resources/static/css/style.css` — `.feed-notices*`, `.feed-notice` styles.
- `src/test/java/.../web/FeedControllerTest.java` — constructor updated; 3 new
  tests (upcoming-notices ordering + actionable flags, actionable-type set,
  chat-filter pass-through).

## Verification

- `./gradlew test`: 52 tests, 0 failures (was 49).
- Booted `e2e` profile on port 8081, seeded 3 scheduled events (PTM +1d,
  Diwali +2d, Exams +14d): `/feed` rendered 200 with 3 notice rows, correct
  badges, 2 "Action needed" flags (PTM + Exams; Holiday not). Chat filter
  verified: `?chatId=12345` → 2 notices, `?chatId=67890` → 1 notice.
- Note: the app instance on port 8080 (IntelliJ) predates these changes and
  must be restarted to serve the notices.

## Follow-up: filters as checkboxes (2026-08-01)

User request: "filters on the left can be check boxes instead of drop-down."

- The type filter is now **multi-select checkboxes** (one per
  `CommunicationType`, rendered with its badge), submitted as repeated
  `types=<T>` query params. Chat ID stays a number input (it's an arbitrary
  ID, not a category).
- Backend: `ClassificationRecordRepository.findFeed` now takes
  `List<CommunicationType> types` (`:types IS NULL OR r.communicationType IN :types`).
  `FeedController` binds `List<CommunicationType> types`; **empty selection is
  normalized to null = all types** (decision). Model attr renamed
  `selectedType` → `selectedTypes`.
- Template: `.filter-checks` / `.filter-check` styles; checked state round-trips
  via `th:checked="${selectedTypes != null and #lists.contains(selectedTypes, t)}"`.
- Tests: `FeedControllerTest` updated (pass-through now multi-type, empty→null
  test); `ClassificationRecordFeedQueryTest` updated + new multi-type query test.
- Verification: `./gradlew test` → 54 tests, 0 failures. e2e boot on 8081:
  12 checkboxes render; `types=REMINDER` → 1 card; `types=REMINDER&types=GENERAL_INFORMATION`
  → 2 cards with both boxes checked on return; combined `types`+`chatId` filters correctly.

## Follow-up: top 4 filters visible + More toggle (2026-08-01)

User request: "filters on the left, by default top 4 filters visible and more
option to expand if required."

- The 12 type checkboxes now show the **first 4 by default**; the remaining 8 are
  hidden behind a **"More types" / "Less"** toggle button (pure client-side
  class toggle on `#typeChecks`).
- Decision: "top 4" = first 4 in `CommunicationType` enum declaration order
  (DAILY_LESSON_UPDATE, HOMEWORK_ASSIGNMENT, CIRCULAR_NOTICE, ACTION_REQUIRED).
  Overridable if the operator wants a curated/ordered top 4.
- **Auto-expand:** if any of the hidden 8 types is currently selected, the list
  renders expanded so no checked box is invisible; the toggle still allows
  collapsing.
- Server-side: `FeedController` exposes `typesExpanded`
  (`hasSelectedHiddenType(selectedTypes)`, hidden = enum index >= 4).
  `VISIBLE_TYPE_FILTERS = 4` constant.
- Template: `.filter-check.is-extra` labels, container gets `collapsed` class;
  small IIFE toggles the class, swaps label text, sets `aria-expanded`.
- Tests: 2 new (`typeFilterCollapsedByDefaultUnlessHiddenTypeSelected`,
  `modelExposesTypesExpandedFlag`).
- Verification: `./gradlew test` → 56 tests, 0 failures. e2e boot on 8081 +
  headless Chromium: 14/14 checks passed — default collapsed with 4 visible and
  hidden extras not visible, click expands/collapses with correct label +
  `aria-expanded`, auto-expand when `types=REMINDER` selected, checked state
  preserved.
