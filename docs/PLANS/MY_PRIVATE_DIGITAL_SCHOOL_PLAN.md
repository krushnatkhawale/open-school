---
title: "My Private Digital School — Current Architecture"
tags: [spring-boot, telegram-bot, java, architecture]
status: active
created: 2026-07-28
updated: 2026-10-31
---

# My Private Digital School — Current Architecture

> Originally the initial build plan; that plan is implemented. This file now
> records the current shape of the app. Runtime/database details live in the
> README and `docs/RESEARCH/POSTGRES_MIGRATION.md`.

## Tech stack

- **Java 25** (Gradle toolchain), **Spring Boot 4.1.0**, **Gradle 9.5.x**
- **Spring Modulith** for module boundaries
- **Spring AI + Ollama** (`llama3` chat, `qwen2.5vl` vision) for classification
- **TelegramBots 10.1.0** long-polling for message ingestion
- **Thymeleaf** server-rendered UI (feed, classifications, extraction rules, calendar)
- **Postgres 16** in Docker (`openschool-db`) at runtime; **H2 in-memory** for tests
- **Playwright** for end-to-end tests

## Structure

```
src/main/java/com/kaushalya/digitalschool/
├── classification/   # AI classification, tags, content types, extraction rules
├── onboarding/       # school/user onboarding
├── security/         # profile-based endpoint lockdown
├── storage/          # JPA entities and repositories (classification records, images)
├── telegram/         # Telegram bot ingestion
├── web/              # MVC controllers and page rendering
└── shared/           # shared configuration/properties
src/main/resources/
├── application.yml       # Postgres default datasource + app config
├── application-e2e.yml   # in-memory H2 for Playwright
└── templates/, static/   # Thymeleaf views and assets
src/test/                  # JUnit tests, all on in-memory H2
e2e/                       # Playwright specs
```

## Current capabilities

- Telegram messages (text and photos) are classified into records with tags,
  content types, dates, and optional images, then shown in `/feed`.
- Feed supports search, tag/school/chat filters, and an academic-year month meter.
- Classifications, extraction rules, and calendar pages are editable in the UI.
- Extraction rules are stored in the database and editable at runtime.
- Onboarding creates schools and users.

## Tests and CI

- `./gradlew test` runs the JUnit suite on in-memory H2; no Postgres or Docker needed.
- `npx playwright test` runs the e2e suite with the `e2e` profile (also H2).
- GitHub Actions runs the JUnit suite on pushes/PRs to `main`; the README badge
  shows its status.
