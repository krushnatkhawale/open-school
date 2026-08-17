---
title: "My Private Digital School Plan"
tags: [spring-boot, telegram-bot, java]
status: active
created: 2026-07-28
---

# My Private Digital School

## Tech Stack
- **Java 25** — OpenJDK 25 (2025-09-16)
- **Spring Boot 4.1.0** — latest 4.x
- **Gradle 9.5.x** — Kotlin DSL
- **TelegramBots 10.1.0** — long-polling starter

## Project Structure

```
my-private-digital-school/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/kaushalya/digitalschool/
    │   │   ├── MyPrivateDigitalSchoolApplication.java
    │   │   └── bot/
    │   │       └── DigitalSchoolBot.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/kaushalya/digitalschool/
            └── MyPrivateDigitalSchoolApplicationTests.java
```

## Current State
- Bot listens to Telegram messages via long-polling
- Responds with echo of the user's message
- Placeholder token in `application.yml` — replace before running

## Next Steps
1. Replace `PLACEHOLDER_TOKEN_REPLACE_ME` with a real Telegram bot token
2. Add more message handlers (commands, media types)
3. Consider feature scope for the "digital school" domain
