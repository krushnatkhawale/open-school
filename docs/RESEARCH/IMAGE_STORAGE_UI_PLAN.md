---
title: "Image Storage UI Changes Plan"
tags: [digital-school, ui, thymeleaf, images, database]
status: active
created: 2026-07-31
---

# UI Changes Required for Storing Images in the Database

## Current State

In `DigitalSchoolBot.handlePhotoMessage()` the photo bytes are downloaded,
passed to the vision model for description, then **discarded**. Only
`image_description` (text) is persisted via `ClassificationEvent` →
`ClassificationEventListener`.

## Backend Contract Needed Before UI Can Render Images

1. Add columns to `classification_record` (and JPA entity + H2 file DB):
   - `image_data BYTEA` (JPA: `byte[]`, works as `BLOB` in H2, `BYTEA` in PG)
   - `image_content_type VARCHAR(50)` (e.g. `image/jpeg`)
   - `image_file_name VARCHAR(255)` (optional)
2. New endpoint serving raw bytes:
   - `GET /api/classifications/{id}/image`
   - Returns `ResponseEntity<byte[]>` with `Content-Type`, `Cache-Control: private, max-age=31536000`
   - Returns `404`/`204` when record has no image
3. Exclude `imageData` from the JSON list API (`@JsonIgnore` or DTO projection).
   Jackson serializes `byte[]` to base64 by default → `/api/classifications`
   would balloon in size.
4. Pass `imageData` bytes through `ClassificationEvent` → listener persists.

## UI Changes by Page

### classifications.html
- New **Image** column (thumbnail, ~96px max), loaded from
  `/api/classifications/{id}/image`
- Text-only records show placeholder (—), no broken-image icon
- Click thumbnail → lightbox modal (vanilla JS, no deps) with full image +
  description text
- Column count in empty-state row must bump from 6 to 7

### day.html
- Same Image column + lightbox as classifications
- Keep lesson date header + upload time; thumbnail sits beside the description

### calendar.html
- Almost unchanged (still a grid of day cells)
- Optional enhancement: photo indicator on day cells that have image-backed
  records + legend entry → requires `CalendarController` to compute
  `hasImages` per day

## Recommendations

- Serve via endpoint; **never** base64-embed in HTML
- Thumbnails via CSS downscale first (simple); generate server-side
  thumbnails (ImageIO) only if volume grows
- Hide broken-image icons with `onerror` on the img tag
- `@JsonIgnore` keeps JSON API lean and backward compatible

## Implemented + Verified (2026-07-31)

Implemented by Java Server side programmer; reviewed end-to-end (booted app
on `e2e` profile + temp H2 file DB, seeded, checked rendered HTML).

**Bug found & fixed:** `byte[] imageData` without size mapping makes H2 create
`BINARY VARYING(255)` — any image > 255 bytes fails to save ("Value too long",
seed returned 500). Fix: `@Column(name = "image_data", columnDefinition = "bytea")`.
- `@Lob` and `@Column(length=...)` both make Hibernate emit `BLOB`, which H2 in
  `MODE=PostgreSQL` rejects ("Unknown data type"). `bytea` works on H2 PG-mode
  AND matches the Postgres DDL.
- Existing dev DBs keep the 255-byte column (Hibernate won't resize) — delete
  `data/digitalschool.mv.db` to apply.
  - **CORRECTION (verified by Java Server side programmer on the real dev DB):**
    booting with the `bytea` mapping **does** let Hibernate resize the stale
    column to unbounded `bytea` in place, with the 25 existing records intact —
    no DB deletion needed. My "Hibernate won't resize" claim was wrong for this
    stack (Hibernate 6 + H2 PG-mode); the `ddl-auto: update` migrator handled the
    type change.

**Test gap:** `ClassificationImagePersistenceTest` used 10-byte / 3-byte
fixtures, so it never crossed the 255 limit. **Closed by Java Server side
programmer** — fixture is now > 255 bytes; 30/30 tests green.

**Verified OK:** seed persists 500-byte PNG; `GET /api/classifications/{id}/image`
returns `image/png` + `Cache-Control: private, max-age=31536000`; JSON API omits
`imageData` but keeps `imageContentType`; thumbnails + `—` placeholders render
on `classifications` and `day` pages; lightbox data attributes survive commas
and quotes in descriptions (`th:attr` protects `${...}` from comma-splitting).
30/30 tests green at HEAD.
