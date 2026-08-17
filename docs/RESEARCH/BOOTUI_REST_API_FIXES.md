---
title: "BootUI REST API Findings — Analysis and Fixes"
tags: [my-private-digital-school, bootui, rest-api, dto, validation, exception-handling]
status: active
created: 2026-08-13
---

# BootUI REST API findings — analysis and applied fixes

Reference for the findings BootUI's REST API advisor reported on
`com.kaushalya.digitalschool` controllers, how each was fixed, and the
deliberate deferrals (with reasons).

## Report shape

- Endpoint: `GET /bootui/api/rest-api` (read-cached) or the MCP tool
  `rest_api_scan` (fresh scan). Fresh-scan trigger:
  `POST /bootui/api/rest-api/scan`.
- Scope: 7 controllers / 14 handlers / 53 rules under `com.kaushalya.digitalschool`.
- Report is in-memory per process: after every restart the scan must be
  re-triggered before the dashboard card shows findings.

## Before: 10 findings (2 HIGH, 1 MEDIUM, 3 LOW, 4 INFO)

## Fixed

### RAPI-DTO-001 (HIGH) — JPA entities exposed in responses

- `ClassificationController` returned the `@Entity ClassificationRecord`
  directly from 5 handlers (`getAll`, `getById`, `getByChatId`, `getByType`,
  `update`).
- Fix: new `ClassificationResponse` record (`web/ClassificationResponse.java`)
  mirroring exactly the entity's serialized fields
  (`id, chatId, originalText, communicationType, aiDescription,
  imageContentType, tags, contentType, eventDate, uploadedAt, feedHidden`)
  with a static `from(ClassificationRecord)` factory. `imageData` is excluded
  (it was already `@JsonIgnore`).
- **JSON contract unchanged** — same field names/shape, so the existing
  frontend keeps working. Verified against live API and by the new
  `ClassificationApiContractTest` (asserts `imageData` is absent and every
  field matches).

### RAPI-VALID-001 (HIGH) — @RequestBody not validated

- `ClassificationController#update` bound `@RequestBody` without validation and
  hand-rolled a null check returning an empty 400.
- Fix: `@Valid @RequestBody` + `@NotNull` on `communicationType` and
  `eventDate` in `ClassificationUpdateRequest`. Added
  `org.springframework.boot:spring-boot-starter-validation` (managed by the
  Boot 4.1.0 BOM; verified resolved before use). The manual null check was
  removed — validation now happens at the MVC boundary.

### RAPI-ERR-001 (MEDIUM) — no centralized exception handling

- Fix: `GlobalExceptionHandler` (`web/GlobalExceptionHandler.java`),
  a `@RestControllerAdvice(basePackages = "com.kaushalya.digitalschool.web")`
  extending `ResponseEntityExceptionHandler`:
  - `handleMethodArgumentNotValid` → 400 `ProblemDetail` + `fieldErrors`.
  - `handleHttpMessageNotReadable` → 400 `ProblemDetail` (malformed body).
  - generic `Exception` → 500 `ProblemDetail`.
- `basePackages` is scoped to the app's web package so BootUI's own
  controllers are not affected.
- Chose the RFC 9457 `ProblemDetail` shape (via `ResponseEntityExceptionHandler`)
  over a custom error body — BootUI's RAPI-ERR-003 (INFO) flags custom bodies;
  using the Spring-standard shape keeps the report clean. Error responses are
  now `application/problem+json`.

### RAPI-MAP-004 (LOW) — repeated base path

- `CalendarController` repeated `/calendar` on every method with no class-level
  mapping. Hoisted into `@RequestMapping("/calendar")`; methods are now
  `@GetMapping` and `@GetMapping("/day")`. No route change.

### RAPI-VER-002 (LOW) — mutating endpoint without consumes

- `ClassificationController#update` now declares
  `consumes = MediaType.APPLICATION_JSON_VALUE`. The frontend already sends
  `Content-Type: application/json`, so no client change.

## Deliberately deferred (breaking changes / INFO polish)

- **RAPI-PAGE-001 (LOW)** — `getAll`, `getByChatId`, `getByType` return whole
  lists, and `getImage` is flagged. Pagination would change the JSON shape
  (List → Page) and requires coordinated frontend work
  (`classifications.html` loads the full list); `getImage` is a false positive
  (single-image bytes). Defer until the frontend is ready for a paged contract.
- **RAPI-DOC-001 / RAPI-DOC-002 (INFO)** — `@Operation`/`@Tag` OpenAPI
  annotations across 14 handlers / 7 controllers. Cosmetic; large diff.
- **RAPI-NAME-002 (INFO)** — `/api/classifications/{id}/image` "singular noun".
  Renaming would break embedded image URLs in templates.
- **RAPI-VER-001 (INFO)** — no API versioning signal. Architectural decision
  to make before the API is consumed externally.

## Verification

- `./gradlew test`: **174 tests, 0 failures** (was 167). Net +7: 4 new
  `ClassificationApiContractTest` MockMvc cases (DTO shape, 400 validation,
  malformed body, 200 update, 404) + 1 DTO-mapping unit test, minus 2 obsolete
  manual-guard unit tests (coverage moved to the MVC layer).
- Fresh `rest_api_scan` on the restarted build: **1 violation** (the deferred
  LOW pagination finding) + 4 INFO. All HIGH/MEDIUM/fixable-LOW cleared.
- Live checks: `GET /api/classifications` returns DTOs (344 records, no
  `imageData`); `PUT` missing `communicationType` → 400
  `application/problem+json` with `fieldErrors`; valid `PUT` → 200 DTO;
  `/classifications`, `/calendar`, `/calendar/day?date=…`, `/feed`, `/bootui`
  all 200.
