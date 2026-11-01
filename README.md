# open-school

[![CI](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml/badge.svg)](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml)
[![Coverage](https://raw.githubusercontent.com/krushnatkhawale/open-school/badges/jacoco.svg)](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml)
[![Branch coverage](https://raw.githubusercontent.com/krushnatkhawale/open-school/badges/branches.svg)](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml)
[![E2E](https://github.com/krushnatkhawale/open-school/actions/workflows/e2e.yml/badge.svg)](https://github.com/krushnatkhawale/open-school/actions/workflows/e2e.yml)

Spring Boot 4.1 / Java 25 app that ingests school communications (Telegram),
classifies them, and serves a feed and management UI.

## Database

**Postgres is the only runtime database.** It runs in Docker as `openschool-db`
(Postgres 16, host port **5434**, database/user/password `openschool`) on the
persistent volume `openschool-db-data`. The default datasource in
`src/main/resources/application.yml` is:

```text
jdbc:postgresql://localhost:5434/openschool
```

**H2 is tests-only.** The JUnit suite and the Playwright `e2e` profile both use
in-memory H2; nothing in the test path needs Postgres or Docker. The old
file-based H2 store (`./data/digitalschool`) is frozen as a fallback and is no
longer opened by the app.

Migration history, verification results, and Postgres/Hibernate pitfalls are in
[`docs/RESEARCH/POSTGRES_MIGRATION.md`](docs/RESEARCH/POSTGRES_MIGRATION.md).

### Start the database

```bash
# one-time setup
docker volume create openschool-db-data
docker run -d --name openschool-db \
  -e POSTGRES_DB=openschool -e POSTGRES_USER=openschool -e POSTGRES_PASSWORD=openschool \
  -p 5434:5432 -v openschool-db-data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:16

# afterwards
docker start openschool-db
docker stop openschool-db
```

`--restart unless-stopped` brings the container back whenever the Docker daemon
starts. On this machine Docker Desktop also starts at login through the
`com.docker.desktop.autostart` LaunchAgent.

## Build and run

```bash
./gradlew bootJar
source ~/.zshrc  # provides TELEGRAM_BOT_TOKEN
export JAVA_HOME=/Users/hulk/Library/Java/JavaVirtualMachines/openjdk-25/Contents/Home
"$JAVA_HOME/bin/java" -jar build/libs/my-private-digital-school-0.0.1-SNAPSHOT.jar \
  --server.address=0.0.0.0 --server.port=8080
```

Hibernate creates/updates the schema (`ddl-auto: update`) and
`ExtractionRuleSeeder` seeds the default extraction rules on first start. The
`local`/`dev` profiles only add developer tooling (BootUI, actuator details);
they inherit the Postgres datasource.

## Tests

Run the whole JUnit suite locally with no Postgres or Docker:

```bash
./gradlew test
```

- Tests use in-memory H2 (`MODE=PostgreSQL`). `src/test/resources/application.properties`
  provides the default H2 datasource, and individual tests can point at their own
  in-memory database.
- H2 is not a Postgres substitute for SQL typing: validate new queries with
  nullable parameters against real Postgres too (see the pitfall note in
  `docs/RESEARCH/POSTGRES_MIGRATION.md`).
- Playwright end-to-end tests use the `e2e` profile (also in-memory H2) and
  start the app automatically through `playwright.config.ts`:

  ```bash
  npm ci
  npx playwright install chromium
  npm run test:e2e        # or: npx playwright test e2e/feed-filters.spec.ts
  ```

  The suite covers the feed and its filters/search, classification list and
  editor, calendar scoping and day details, shared navigation and mobile menu,
  extraction-rule editing/reset, JSON API contracts, and the theme switcher.
  Specs call `POST /api/test/seed` (see `e2e/helpers/seed.ts`) so each run uses
  fresh, deterministic records; extraction-rule specs reset the rules they
  change. Use `npm run test:e2e:ui` for the interactive runner.

### Coverage

JaCoCo (`0.8.16`) measures coverage during `./gradlew test`; the report is
written to `build/reports/jacoco/test/html/index.html` (XML and CSV alongside
it). After a run:

```bash
./gradlew test jacocoTestReport      # coverage report
./gradlew jacocoTestCoverageVerification  # fail if below the floor
```

Current coverage (JaCoCo, instruction / branch / line):

| Metric | Covered |
| --- | --- |
| Instructions | **65.9%** |
| Branches | **63.6%** |
| Lines | **68.0%** |

`./gradlew check` and CI enforce a floor of **65% line** and **55% branch**
coverage, so a regression fails the build. The two coverage badges at the top
of this README are regenerated on every push to `main` from the JaCoCo CSV and
published to the `badges` branch by CI.

CI runs on every push and pull request to `main` with Temurin Java 25:

- `.github/workflows/ci.yml` runs the JUnit suite and coverage verification and
  uploads JUnit results, the HTML test report, and the JaCoCo report. The
  coverage badge refresh only runs on pushes to `main`.
- `.github/workflows/e2e.yml` installs Chromium, runs the Playwright suite
  against the `e2e` profile, and uploads the HTML report plus traces/video for
  failures.

## Documentation

- [`docs/RESEARCH/POSTGRES_MIGRATION.md`](docs/RESEARCH/POSTGRES_MIGRATION.md) — runtime database, migration, operational pitfalls
- [`docs/RESEARCH/SPRING_BOOT_4_MOCKMVC_NOTES.md`](docs/RESEARCH/SPRING_BOOT_4_MOCKMVC_NOTES.md) — test setup gotchas
- [`docs/PLANS/`](docs/PLANS/) and [`docs/RESEARCH/`](docs/RESEARCH/) — feature plans and research notes

## On this machine

The app runs as the `com.openschool.app` LaunchAgent (launchd keeps it alive and
starts it at login). The old `com.kaushalya.digitalschool.local` agent, which ran
the app against H2 on port 8080, is disabled.
