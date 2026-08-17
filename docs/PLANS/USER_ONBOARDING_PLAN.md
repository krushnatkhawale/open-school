---
title: "User Onboarding Plan (School → Teacher → Parent)"
tags: [onboarding, telegram, users, schools, multi-tenant, plan]
status: active
created: 2026-08-07
updated: 2026-08-07
---

# User Onboarding Plan

Prompt: krushnat (thread `ccfa4b2ea79dc10c9526d75ebf11a794928b8bc51794a203b6b687d0eda9bc67`):
on a Telegram message: store the message, then check the user database; if the user is
unknown, ask for the school they represent, store it plus a few details. Onboard school
users now; parents (and teachers of a school) later.

## Objective

Add a persisted user concept with a conversational Telegram onboarding flow, without
disturbing the existing "store every message" behavior. Today the app is a personal
capture tool keyed only by `chatId`; this is the first step toward multi-school scoping.

## Current flow (unchanged in behavior)

```
message -> classify -> persist ClassificationRecord -> reply type.name()
```
`DigitalSchoolBot.consume()` dispatches text/photo/document; each handler persists via
`ClassificationEvent` and replies `sendMessage(chatId, type.name())`.

## New entities

### `School`
| col | type | notes |
|---|---|---|
| id | UUID | PK |
| name | varchar | normalized (trim, case-insensitive matching); null when anonymous |
| city | varchar | optional — disambiguates duplicate school names |
| registered_number | varchar | optional — school registration/affiliation number |
| created_at | timestamp | |

### `app_user` (`AppUser`)
| col | type | notes |
|---|---|---|
| id | UUID | PK |
| telegram_chat_id | bigint | unique — the natural key from the bot |
| role | enum | `SCHOOL` now; `TEACHER`, `PARENT` reserved (no UI yet) |
| school_id | uuid FK | nullable (null until school confirmed; null if anonymous) |
| onboarding_status | enum | `PENDING_SCHOOL`, `PENDING_CITY`, `PENDING_REG_NO`, `ACTIVE` |
| created_at / onboarded_at | timestamp | |

`onboarding_status` is **persisted**, so a pending conversation survives app restarts and
relay hiccups (no in-memory dialog state).

## Onboarding flow (school users, per operator decisions 2026-08-07)

1. **Any message from an unknown `telegram_chat_id`**:
   - Store/classify the message exactly as today (requirement: "store the message").
   - Create `AppUser` (role `SCHOOL`) with `PENDING_SCHOOL`.
   - Reply: current classification result **first**, then
     `Welcome! What is the name of your school? (reply "skip" to stay anonymous)`
2. **Reply while `PENDING_SCHOOL`**:
   - `skip` (case-insensitive) → anonymous: no school, status → `ACTIVE` immediately,
     confirmation reply. (City/registered-number are school details, so skipping the school
     name skips them too — decided at implementation time.)
   - otherwise → match existing school by normalized name (case-insensitive; create if
     missing), set `school_id`; status → `PENDING_CITY`; ask city (or skip).
3. **Reply while `PENDING_CITY`** → set school city if not `skip`; status → `PENDING_REG_NO`;
   ask registered number (or skip).
4. **Reply while `PENDING_REG_NO`** → set registered number if not `skip`; status → `ACTIVE`;
   reply with a confirmation summary.
5. **`ACTIVE` user** → normal behavior; onboarding never re-asked.

Every optional step accepts `skip` so a user can complete onboarding with nothing but
their school name — or fully anonymous if they skip the school name too.

### Handling non-answer messages while pending
- Photo/document/video while pending: still store the message (as required), then re-ask
  the current pending question ("please reply with text").
- Any text while pending is treated as the answer to the current step (per the request).

### Reply precedence
- First (unknown) message: classification reply **plus** onboarding prompt.
- Pending steps: onboarding prompt **only** (no classification reply for the answer).
- ACTIVE: classification reply only (today's behavior).

## What gets stored about a school user

- Telegram chat id (identity) + school name (+ optional city, registered number). Anonymous
  skip → school_id null.
- Teacher/parent roles are reserved in the enum so a later role step/flow can be added
  without schema change (e.g., teacher onboarding adds a role question; parents add child
  details). Same persisted state machine supports future branching.

## Integration points

- `DigitalSchoolBot`: after each handler persists its message, call
  `OnboardingService.handle(chatId, textOrNull)` and use its reply (if any) instead of /
  in addition to `type.name()`.
- New package `com.kaushalya.digitalschool.onboarding`:
  - `School`, `AppUser`, `UserRole`, `OnboardingStatus` entities/enums.
  - `SchoolRepository`, `AppUserRepository` (JPA).
  - `OnboardingService` — pure state-machine logic (easy unit tests, no Telegram deps).
- Schema: JPA `ddl-auto: update` (consistent with current setup) + mirror in
  `db/postgres-schema.sql`.

## Existing data

- The operator's own Telegram chat will be unknown → onboarded as the first school on the
  next message. Records already stored keep their `chatId`; a nullable `user_id` FK on
  `classification_record` can be backfilled later for scoping — **not** required for this
  step.

## Tests (incremental)

1. `OnboardingServiceTest` (unit, mock repos or @DataJpaTest-style): unknown chat →
   PENDING_SCHOOL + classification-result-and-prompt; school reply links/creates school →
   PENDING_CITY; city reply → PENDING_REG_NO; reg-no reply → ACTIVE; `skip` anywhere →
   skips that detail (including anonymous, school_id null); school name case-insensitive
   match; duplicate name reuse; pending state survives "restart" (re-load from DB);
   photo-while-pending re-asks.
2. `DigitalSchoolBot` integration with mocked `AiChatService`: unknown-chat message persists
   the record AND produces the onboarding prompt; ACTIVE chat gets the normal reply.
3. Full suite stays green (currently 118 tests) before and after each step.

## Breaking changes / risks

- **Additive only** — new tables/columns; nothing existing is altered.
- Reply text for unknown users changes (prompt appears) — expected, it is the feature.
- `chatId` is unique per AppUser: if the same school adds multiple teachers, each Telegram
  chat becomes its own AppUser row linked to the same School (no collision).
- Multi-tenant scoping (a school seeing only its data) is a **later** step; schema now makes
  it possible, but this step does not change any feed/calendar queries.

## Suggested execution order

1. Entities + repositories + schema (green suite).
2. `OnboardingService` state machine + unit tests.
3. Wire into `DigitalSchoolBot` (reply precedence) + integration tests.
4. Manual end-to-end via a Telegram bot test chat (operator acts as a school user).
5. Optional small admin UI later; not in this step.

## Decisions from operator (2026-08-07, all answered)

1. School-only now; keep the approach easily adoptable for the teacher role later
   (reserved enum values, state machine ready to branch).
2. Ask for: school name, city, and (optionally) a registered number.
3. Show the classification result first, then ask for details; give the user a "skip"
   option so they can remain anonymous.
4. Covered by #3 — "skip" means anonymous; no defaulting to Telegram's name.
