---
title: "BootUI Architecture Findings — Analysis and Fixes"
tags: [my-private-digital-school, bootui, architecture, archunit, telegram]
status: active
created: 2026-08-12
---

# BootUI architecture findings — analysis and applied fixes

Reference for the architecture findings BootUI's ArchUnit-hygiene advisor
reported on `com.kaushalya.digitalschool`, how each was fixed, and why the
chosen API is the correct one.

## Report shape

- Endpoint: `GET /bootui/api/architecture` (read-cached) or the MCP tool
  `architecture_scan` (fresh scan). Fresh-scan command:
  `POST /bootui/api/mcp` `{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"architecture_scan","arguments":{}}}`.
- Scope: 43 app classes under `com.kaushalya.digitalschool`, 41 rules.
- The report is fail-soft: findings are severity-graded (CRITICAL…INFO) and
  each carries `id`, `name`, `description`, `sampleViolations`,
  `recommendation`, and a `learnMoreUrl`.

## Finding ARCH-CODE-002 — generic exceptions

Detects `throw new Exception / RuntimeException / Throwable`.

- Location: `DigitalSchoolBot.handlePhotoMessage` threw
  `new RuntimeException("No photo sizes available")` from `Stream.max().orElseThrow`.
- Fix: throw `java.util.NoSuchElementException` (the idiomatic "no element
  found" exception for an empty `Optional`/`Stream`). Specific and catchable.
- Extracted `static PhotoSize largestPhoto(List<PhotoSize>)` so the behavior is
  unit-testable without Telegram API mocks.

## Finding ARCH-CODE-009 — deprecated API

Detects usage of `@Deprecated` members.

- Location: `DigitalSchoolBot` implemented
  `org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer`.
- Why deprecated (telegrambots-longpolling 10.1.0): it uses a **shared static
  single-thread executor** across all bot instances, causing issues when more
  than one bot is registered.
- Replacement: `org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer`
  (abstract class, per-instance daemon executor, `close()` shuts it down).
- Fix: `public class DigitalSchoolBot extends DefaultLongPollingUpdateConsumer
  implements SpringLongPollingBot`. `getUpdatesConsumer()` still returns `this`;
  `consume(Update)` unchanged. The starter's `TelegramBotInitializer` calls
  `getUpdatesConsumer()` and passes it to `registerBot`, so wiring is untouched.

## Finding ARCH-SPRING-015 — mutable @ConfigurationProperties

Detects non-final fields in `@ConfigurationProperties` classes.

- Location: `SchoolProperties` (prefix `school`) had a mutable `int` field +
  setter; enabled via `@ConfigurationPropertiesScan` on the application class.
- Fix: immutable record with constructor binding and an explicit default:
  ```java
  @ConfigurationProperties(prefix = "school")
  public record SchoolProperties(@DefaultValue("4") int academicYearStartMonth) {
  }
  ```
- `org.springframework.boot.context.properties.bind.DefaultValue` is in
  spring-boot 4.1.0 (confirmed on the classpath) and preserves the previous
  April default when `school.academic-year-start-month` is absent. Verified
  with `ApplicationContextRunner` + `@EnableConfigurationProperties`
  (the same binder path `@ConfigurationPropertiesScan` uses).
- Callers: `FeedController` now uses the record accessor `academicYearStartMonth()`.

## Verification

- `./gradlew test`: 167 tests, 0 failures (was 160; +7 new).
- Fresh `architecture_scan` on the restarted build: **0 violations** /
  41 rules / 43 classes (was 3 violations).
- App endpoints (`/feed`, `/classifications`, `/calendar`, `/h2-console/`,
  `/bootui`) all 200 after restart; Telegram bot registers normally.
