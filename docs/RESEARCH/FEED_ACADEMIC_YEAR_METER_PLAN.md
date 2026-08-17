---
title: "Feed Academic-Year Month Meter Plan"
tags: [ui, feed, scrollspy, digital-school]
status: active
created: 2026-08-01
---

# Feed Academic-Year Month Meter Plan

Follow-up to `/feed` (Twitter/X-style feed, right-side month meter). User feedback:
the right meter currently shows only the current month (1–31 day strip). It should
show **all months since the school's academic year started** — newest at top, oldest
at bottom — and "zip" (collapse) months as the user scrolls past them.

## Goal

- Right meter = vertical list of every month of the current academic year up to now.
- Newest month on top, oldest at the bottom.
- As the user scrolls down the stream (into the past), months above the viewport
  collapse to a compact label — "zipped" — so more months stay visible at once.
- Clicking a meter month scrolls the stream to that month.

## Backend changes (`FeedController`, repo already supports it)

1. **Academic-year config** — `school.academic-year-start-month=4` in
   `application.properties` (April default; set to the real school start).
   Compute `YearMonth` range: start of current academic year → current month.
2. **Single range query, group in controller** — existing
   `findFeed(from, to, type, chatId)` already accepts a range and orders
   `eventDate DESC, uploadedAt DESC`. Fetch the whole academic year in one query.
   Group into `List<MonthSection>` (newest→oldest), each holding:
   - `monthLabel` (e.g. "July 2026"), `year`, `month`, `daysInMonth`
   - `records`
   - `daysWithPosts` (distinct day-of-month for meter dots)
3. **Model attrs** — replace per-month `year/month/records` with `sections`.
   Keep `types`/`selectedType`/`selectedChatId`. Prev/next month nav can go
   (or become a "Jump to top/current month" helper).
4. **Tests** — update `FeedControllerTest` / `ClassificationRecordFeedQueryTest`
   for the grouped model + academic-year range.

## Frontend changes (`feed.html`, `style.css`)

1. **Stream** — one continuous page. Each month is a section:
   `<h2 class="feed-month-section" id="month-<year>-<month>">July 2026 · N posts</h2>`
   followed by its cards. Cards unchanged.
2. **Meter** — list of month blocks, newest on top. Expanded block = label +
   record count + mini 1–31 day strip (days with posts bolded, same styling).
   Zipped block = label only.
3. **Zip behavior (CSS + IntersectionObserver, no layout math):**
   - `.meter-month.zipped .meter-month-days { max-height: 0; opacity: 0; }`
     with a transition — collapse is pure CSS.
   - `IntersectionObserver` on month sections → mark the in-view month `.active`
     (expanded), mark sections scrolled past `.zipped`.
   - Click meter month → `scrollIntoView({ behavior: 'smooth' })`.
   - Keep the overall progress fill.
4. **Empty months** — a month with zero records shows a dimmed label only,
   no day strip (avoids 31 empty rows).

## Open questions for the operator

1. What month does the school year actually start? (April default.)
2. Current academic year only, or include previous academic years in the meter?

## Implemented (2026-08-01)

Server side (Java) — done, all tests green (49 total):

- `shared/SchoolProperties` (`@ConfigurationProperties(prefix = "school")`, enabled via
  `@ConfigurationPropertiesScan`) with `academicYearStartMonth`, default 4 (April).
  `application.yml` now has `school.academic-year-start-month: 4`.
- `FeedController` rewritten: `GET /feed` takes only optional `type`/`chatId`.
  Computes academic-year range via static `academicYearStart(current, startMonth)` (clamps
  1–12, rolls back to previous year before the start month), runs the existing
  `findFeed(from, to, type, chatId)` in ONE query over the whole year, and groups into
  `List<MonthSection>` newest→oldest. Model now exposes `sections`, `totalCount`,
  `types`, `selectedType`, `selectedChatId`. Per-month attrs (`year`, `month`,
  `monthName`, `daysInMonth`, `prev/next*`) and `records` removed.
- `MonthSection` record: `monthLabel`, `year`, `month`, `daysInMonth`, `records`,
  `daysWithPosts` (TreeSet of day-of-month). Static factory builds it from a `YearMonth`.
- `feed.html`: stream is one continuous page with per-month `<h2 class="feed-month-section">`
  (id `month-<year>-<month>`, "July 2026 · N posts"); cards unchanged; prev/next nav and
  hidden year/month inputs dropped. Meter is a vertical list of `.meter-month` blocks
  newest→top: label + count + 7-column mini day strip (days with posts `.has`), empty
  months show a dimmed label only. JS marks the in-view month `.active` and months scrolled
  past `.zipped` (pure CSS `max-height: 0` collapse, hover re-expands), click scrolls the
  stream via `scrollIntoView`, thin progress bar kept. Meter is `overflow-y: auto` so the
  whole year fits.
- Tests: `FeedControllerTest` (manual construction with `SchoolProperties`) covers academic
  range default, grouping + empty months, filter pass-through, boundary roll-over, clamping.
  `ClassificationRecordFeedQueryTest` covers cross-month newest-first ordering.

Decisions made (operator can override):
- Academic year start month defaults to April (April 4) until the real month is confirmed.
- Meter covers the current academic year only (per plan); previous years not included.
- Rendered `/feed` verified on e2e profile (port 8090): 5 sections Aug→Apr 2026, August
  day strip marks seeded days 1–3, empty months label-only, filters + empty-state work.

## UI review + polish (2026-08-01, Expert UI developer)

### Bug found and fixed
- `th:classappend="${sec.records.empty ? 'empty' : ''}${secStat.first ? ' current' : ''}"`
  (two adjacent `${}` expressions) is NOT parseable by Thymeleaf — `/feed` returned 500.
  Rewritten as one expression with `+`:
  `${(sec.records.empty ? 'empty' : '') + (secStat.first ? ' current' : '')}`.
  Verified live render after fix.

### Polish applied (`feed.html`, `style.css`)
- **Zip affordance** — new `.meter-chevron` in each month head; rotates up when zipped,
  down when expanded; hidden on empty months (nothing to expand). Empty months now also
  drop the `0` count and the pointer cursor.
- **Day strip scannability** — days-with-posts (`.meter-day.has`) are now solid navy
  filled pills (white text) instead of bold gray text; strip gap bumped to 2px.
- **Meter anatomy** — only `.feed-meter-months` scrolls (was the whole panel); the
  "Academic year" head and bottom progress bar stay pinned.
- **A11y / UX** — arrow-key + Home/End roving navigation in the meter, `aria-current`
  set on the active month, reduced-motion aware smooth scrolling, `prefers-reduced-motion`
  disables transitions, month labels ellipsize.
- **Stream header** — month sections get a 4px accent left bar (timeline cue).

### Verified (Playwright, e2e profile, port 8080)
- 5 sections Aug 2026 (top) → Apr 2026 (bottom), correct class state, day strip open
  on the active/current month, empty months label-only.
- Simulated tall feed (cloned cards): scrolling past a month zips it (Aug→Jul→Jun→May
  zip sequentially), active month tracks the in-view section.
- Real mouse hover re-expands a zipped month (0 → 88px).
- Clicking a meter month scrolls the stream so the target section lands at top=88
  (exactly under the sticky navbar).
- Full suite: 49 tests, 0 failures.

