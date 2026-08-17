---
title: "BootUI Developer Console Setup Notes"
tags: [bootui, developer-console, spring-boot-4, observability, digital-school]
status: active
created: 2026-08-12
---

# BootUI Setup Notes (my-private-digital-school)

What was added, why, and how to run/verify BootUI in this project.

## What is BootUI

BootUI (<https://www.julien-dubois.com/boot-ui/>, github: `jdubois/boot-ui`) is a
local-only developer console for Spring Boot 4 (servlet and WebFlux) and Quarkus.
It is an embedded Vue UI + REST API (`/bootui`, `/bootui/api/**` by default) served
from the app itself — no separate frontend deploy. Panels cover runtime
observability (health, metrics, memory, threads, heap dumps, startup timing),
configuration (masked properties, overrides, loggers, beans, conditions),
data/services (DB pools, Spring Data, Hibernate, scheduled tasks, caches), and
diagnostics/advisors (REST API, Spring, Hibernate, security, pentesting).

## Why it fits here

- Project is Spring Boot **4.1.0** on Java 25 (servlet, `spring-boot-starter-web`).
  BootUI requires Spring Boot 4.x + Java 17+, so this is the supported path.
- Siva's post referenced in the channel points at BootUI as a recommended dev
  console for Spring Boot developers.
- BootUI is loopback-only by default, dev-gated, masks secrets, and fail-closes in
  `prod`/`production` — appropriate for a self-hosted app.

## Changes made

1. `build.gradle` — added the starter as `runtimeOnly` (kept out of compile/test
   classpath, like `h2`/`postgresql`):

   ```groovy
   runtimeOnly 'com.julien-dubois.bootui:bootui-spring-boot-starter:1.13.1'
   ```

   Version **1.13.1** is the latest release (verified on Maven Central,
   2026-08-07 lastUpdated). Its POM depends on Spring Boot 4 artifacts
   (`spring-boot-starter-web`, `spring-boot-starter-actuator`, OTel tracing),
   all version-managed by our Boot 4.1.0 BOM. No conflict with existing deps.

2. `src/main/resources/application.yml` — force-enable so it works with the
   project's normal run (default profile, no `dev` profile exists):

   ```yaml
   bootui:
     enabled: ON
   ```

   Activation rule: BootUI is dormant by default and only wakes for the
   `dev`/`local` profiles, when `spring-boot-devtools` is present, or when
   `bootui.enabled=ON`. `prod`/`production` profiles disable it unless
   `bootui.enabled=ON` is explicitly set (which we now do).

## How to access

- Console: `http://localhost:8080/bootui`
- Health API: `http://localhost:8080/bootui/api/health`
- Started with the same command as before (no extra flags):
  `./gradlew bootRun` (with `TELEGRAM_BOT_TOKEN` sourced from `~/.zshrc`).

## Safety notes / decisions

- `bootui.enabled: ON` keeps the console active on the **default** profile and in
  the **e2e** profile (inherits `application.yml`). It is loopback-only, so remote
  LAN clients (e.g. the phone at `http://192.168.1.40:8080/feed`) are rejected at
  the BootUI surface.
- Trade-off: because we set `ON` explicitly, BootUI stays on even under a
  `prod`/`production` profile if one is ever introduced. To disable globally later,
  set `bootui.enabled: OFF` (or drop the starter — it is `runtimeOnly`).
- BootUI pulls in `spring-boot-starter-actuator` (health, metrics, heapdump, ...)
  at runtime, which powers several panels. This also exposes local actuator
  endpoints; acceptable for a self-hosted dev app.

## Verification (2026-08-12)

- `./gradlew test` — **160 tests, 0 failures** with BootUI active in the default
  profile (MockMvc render tests pass through BootUI's loopback filter).
- Live boot on :8080: `/bootui` → 200 `text/html` (title `BootUI`),
  `/bootui/` → 200, `/bootui/api/health` → UP (db: H2, diskSpace).
  Boot log: `BootUI activation: Enabled by bootui.enabled=ON` and
  `BootUI is available at http://localhost:8080/bootui`.
- Existing pages unaffected: `/feed`, `/classifications`, `/calendar` all 200.
- Telegram bot: `deleteWebhook: true` on startup.

## Found during verification — H2 console (fixed 2026-08-12)

`/h2-console` originally returned 404. Root cause: in Spring Boot 4 the H2 console
auto-configuration moved to a dedicated module (`org.springframework.boot:spring-boot-h2console`)
that was NOT on this project's classpath, so the `spring.h2.console.enabled: true`
setting in `application.yml` was inert — this predated BootUI and was unrelated to it.

**Fix applied:** added to `build.gradle` as `runtimeOnly`
`org.springframework.boot:spring-boot-h2console` (version managed by the Boot 4.1.0 BOM).

**Verified (2026-08-12):** boot log `H2 console available at '/h2-console'`; `/h2-console`
→ 302 to `/h2-console/` → 200 (`<title>H2 Console</title>` login page). Full suite green
(160 tests, 0 failures). Connect with the app datasource URL
`jdbc:h2:file:./data/digitalschool;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` and empty
username/password (see `H2_DB_ACCESS_NOTES.md`).

## BootUI MCP server + OpenCode (2026-08-12)

BootUI also ships an opt-in MCP **server** (`POST /bootui/api/mcp`, JSON-RPC 2.0) so
local AI coding agents can call the advisors and read runtime diagnostics. Enabled
with `bootui.mcp.enabled: ON` (added to `application.yml`).

The project now has an `opencode.json` at the repo root wiring opencode (an MCP
client) to BootUI as a remote server:
`http://127.0.0.1:8080/bootui/api/mcp`. Every opencode agent in the repo gets the
`bootui_*` tools while the app is running. Full detail + verification in
`BOOTUI_MCP_OPENCODE_NOTES.md`.

## References

- Setup docs: <https://www.julien-dubois.com/boot-ui/setup>
- Properties: <https://www.julien-dubois.com/boot-ui/properties>
- Repo: <https://github.com/jdubois/boot-ui>
- Triggering post: <https://x.com/sivalabs/status/2087150368933413069>
