---
title: "Externalized Extraction-Rules Config (Runtime-Editable Prompt Pieces)"
tags: [ai, prompts, config, runtime, plan, classification]
status: active
created: 2026-08-13
updated: 2026-08-14
---

# Externalized Extraction-Rules Config

Prompt: krushnat (event `60352e9285cae104ff5479d72b4d0831f4c3569d841d2e292a887c23dce28727`):
externalize the prompt pieces that tell the model *how* to extract each field from message
text or image data, so behavior can change at runtime from the UI. A page lists every
extracted field with editable extraction rules; the assembled rules feed the prompt.

## Objective

Today every extraction heuristic is a hard-coded string inside
`AiChatServiceImpl` (`CLASSIFY_SYSTEM_PROMPT`, the image-description prompt, the date,
scheduled-events, and tags prompts). Changing behavior means an edit + rebuild + restart.
Goal: make the *extraction rules* per field editable at runtime from a web page, without a
restart, while keeping the machine-parseable output contract safe.

## Current extracted fields (the editable set)

| key | method | parses | output contract (fixed) |
|---|---|---|---|
| `classification` | `classify` | `CommunicationType` enum | exactly one category name |
| `imageDescription` | `describeImage` | free text | prose description |
| `date` | `extractDate` | `LocalDate` | `YYYY-MM-DD` or `NONE` |
| `scheduledEvents` | `extractScheduledEvents` | JSON array of `ScheduledEventData` | `[{date,type,title}]` or `[]` |
| `tags` | `extractTags` | `List<String>` | comma-separated lowercase or `NONE` |

## Core design decision

Split every system prompt into two parts:

1. **Output contract — fixed in code.** The format/grammar the Java parser relies on
   (exact enum names, `YYYY-MM-DD`, JSON array shape, `NONE` sentinel, "no explanation").
   Never user-editable; editing it silently breaks parsing.
2. **Extraction rules — editable at runtime.** The *how*: heuristics, context, examples,
   e.g. "prefer the teacher's red bottom date; fall back to the pencil top date". This is
   what the page edits.

Final prompt = `contract + current rules`. Rules are read from the DB on every call, so a
save applies to the next extraction — no restart.

## Storage

JPA entity `ExtractionRule` (table `extraction_rule`), consistent with existing JPA style:

| col | type | notes |
|---|---|---|
| `key` | varchar (PK) | one of the five keys above |
| `label` | varchar | display name on the page |
| `description` | TEXT | what the field is / shown as help |
| `rules` | TEXT | the editable extraction instructions |
| `enabled` | boolean | master switch per field; disabled → fall back to default |
| `updated_at` | timestamp | |
| `version` | int | optimistic locking for concurrent edits |

**Seeding:** on startup, `ApplicationRunner` inserts a row for any missing key using the
current hard-coded prompt text as the default rules. Existing rows are never overwritten —
edits survive restarts. Defaults also live as code constants so tests don't need the DB.

**Fallback:** if a key has no row, is disabled, or rules are blank, the caller uses the
built-in default. Extraction can never break because config is missing.

## Runtime wiring

- New `ExtractionRuleRepository extends JpaRepository<ExtractionRule, String>`.
- New `ExtractionRuleService`:
  - `String rulesFor(key)` → DB rules or default, respecting `enabled`.
  - `save(...)` with validation (key must be a known field, rules must not be blank when
    enabled) + optimistic lock (409 on stale version).
  - `resetToDefault(key)`.
- `AiChatServiceImpl` refactor: `ChatClient` prompts now build
  `contractPrompt(key) + "\n\n" + rulesFor(key)`. Existing tests keep passing with the
  built-in defaults (they assert on current text, which the defaults preserve).

## UI

New page `/extraction-rules` (Thymeleaf, follows `classifications.html` style + navbar):

- One card per field: `label`, `description`, enabled checkbox, big textarea for rules,
  Save + Reset-to-default.
- Save → `POST /extraction-rules` → `ExtractionRuleService.save` → success/failure flash,
  redirect back. No restart; next incoming message uses the new rules.
- A "live test" box (paste sample text/image caption → run that one extraction against the
  current rules and show the result) is a stretch goal — decide with krushnat.

New classes: `ExtractionRule` (storage), `ExtractionRuleRepository`,
`ExtractionRuleService`, `ExtractionRuleViewController`, template
`extraction-rules.html`. Existing `AiChatServiceImpl` gains the service dependency.

## Breaking changes

None at runtime: the JSON/UI surface and the prompt text shipped to the model are
byte-identical with defaults. The only signature change is internal
(`AiChatServiceImpl` constructor + `AiChatServiceImplTest` setup).

## Verification

- Unit: `ExtractionRuleServiceTest` (seed on startup, edit persists, reset, disabled →
  default, blank-rules rejected, stale version → conflict); `AiChatServiceImplTest`
  extended to assert the assembled prompt contains the editable rules when an override
  exists and the contract fragment is always present.
- `./gradlew test` green (currently 182).
- Live: edit the date rules on the page → next photo arrives with the new behavior
  (visible in a `POST /api/classifications` response) without restart; restart → edits
  persist.

## Open questions for krushnat

1. **Editable scope** — only the extraction *rules* (recommended), or the full system
   prompt including output format? Full-prompt editing risks breaking parsing.
2. **Field set** — fixed five fields now, or support adding/removing fields at runtime
   too? Adding fields needs a dynamic output contract (much bigger design).
3. **Version history** — need old-rule snapshots / rollback, or last-write-wins is fine?
4. **Auth** — the app has no auth today; the page is reachable by anyone on the network
   (like `/feed`). Acceptable, or gate it (e.g. local-only like BootUI)?
5. **Live-test box** on the page — include in v1 or skip?

## Decisions (krushnat, 2026-08-13)

1. **Rules only** — output contract stays code-owned. The page shows a read-only **full
   prompt preview** (contract + rules, plus the fixed user instruction for images) so the
   operator sees the whole input the model receives.
2. **All fields editable** — the fixed five-field set, every field editable + enable/disable.
3. **Last-write-wins** for now; schema left extensible for per-school scoping and history
   later (a `school_id` / snapshot table would be additive).
4. **Page access open** — like `/feed`, no auth gate (app has no auth today).
5. **Tests included** — a per-field live-test box (paste text or upload an image, run the
   real extraction, show the result) **and** automated JUnit coverage.

## Implementation notes (2026-08-13)

Built and verified live on :8080.

New classes: `ExtractionField` (enum: key/label/description/defaultRules/contract/
userInstruction per field, plus prompt assembly), `ExtractionRule` (entity),
`ExtractionRuleRepository`, `ExtractionRuleServiceImpl` + `ExtractionRuleService` +
`ExtractionRuleView`, `ExtractionRuleSeeder` (ApplicationRunner), `ExtractionRuleViewController`
(GET page, POST save, POST reset, POST test), template `extraction-rules.html` + CSS.

`AiChatServiceImpl` now builds every system prompt via
`extractionRuleService.systemPromptFor(field)`; the fixed output contracts live in the
enum, editable heuristics live in the DB. `reset` deletes the override row (true revert to
the code default; the seeder re-seeds on next start).

Verified: **214 tests, 0 failures** (+32 new). Live: save/reset round-trip through
`/extraction-rules`; test endpoint runs the real local model; a date override changed the
result from `2026-07-29` (red bottom date, default) to `2026-07-28` (pencil date) in the
same running process — no restart. App restarted detached via `java -jar` (Gradle daemon
died with its shell, same trap as before).

## UI redesign (2026-08-14)

krushnat asked for a "new generation editor" feel (left field list, center scrolls to the
field). Redesigned `/extraction-rules` as a two-pane studio — template + CSS only, zero
Java changes, all five fields' behavior untouched:

- Left rail: sticky field index with icons + live enabled/disabled status dots; scrollspy
  (IntersectionObserver) highlights the active section, click smooth-scrolls the canvas.
- Center canvas: sheet-style sections with icon headers, an enable/disable toggle switch
  (checkbox linked to the save form via the HTML5 `form` attribute), auto-growing rules
  textarea, always-open full-prompt preview, and a collapsible test box that auto-expands
  after a run.
- Inline SVG icon sprite; responsive (rail collapses to a chip row under 900px).

Full suite 214 tests green; live HTTP save/reset round-trip verified on :8080; Playwright
computed-style + geometry checks pass (layout, scrollspy, toggle linkage, collapsibles,
navbar clearance, mobile stack).

## Full-prompt popup + line-ending normalization (2026-08-14)

krushnat: "why does full prompt look like a different prompt? … have a 'View Full prompt'
option on the left pane that previews the full prompt for all fields in a large popup."

- The preview shows the **effective** runtime prompt (contract + current DB rules). A saved
  override with CRLF line endings (the `date` field) is what made it read like a different
  prompt; defaults are byte-identical to the pre-externalization prompts.
- Added a **`View Full prompt` button in the left rail** opening a modal with all five
  fields' full prompts (icon, label, status, scrollable `<pre>`, per-field `Copy` + `Copy
  all`); closes via ✕ / backdrop / Escape; full-screen on mobile. Template + CSS only.
- **Line endings normalized** in `ExtractionRuleServiceImpl` (`\r\n`/`\r` → `\n` on save
  and on read), so previously saved CRLF rules render and ship clean.
- Verification: `./gradlew test` **219 green** (+3 service, +2 render); Playwright
  **17/17** including new `e2e/extraction-rules.spec.ts` (run against an e2e-profile
  instance on :8081 — the default config's webServer teardown SIGTERMs a reused :8080
  process). Live on :8080 confirmed.

