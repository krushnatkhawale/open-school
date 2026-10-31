---
title: "Postgres Migration — H2 to openschool-db"
tags: [database, postgres, h2, migration, operations]
status: active
created: 2026-10-31
updated: 2026-10-31
---

# Postgres Migration (H2 → `openschool-db`)

## Decision

Postgres is the only runtime database from 2026-10-31. H2 is kept solely for
automated tests (in-memory H2 via test overrides and the Playwright `e2e`
profile). The old file-based H2 store (`./data/digitalschool`) is frozen and no
longer opened by the app.

## Live database

| Item | Value |
|------|-------|
| Container | `openschool-db` (Postgres 16) |
| Host port | 5434 (5432 is used by other containers on this machine) |
| Database / user / password | `openschool` / `openschool` / `openschool` |
| Volume | `openschool-db-data` |
| JDBC URL | `jdbc:postgresql://localhost:5434/openschool` |
| Restart policy | `unless-stopped` |

`src/main/resources/application.yml` points its default datasource at this
database. The former `postgres` profile was removed because it is now the
default; `local`/`dev` profiles only add developer tooling and inherit the
Postgres datasource.

## Data state after migration (verified 2026-10-31)

| Table | Rows |
|-------|------|
| `school` | 4 |
| `app_user` | 4 |
| `classification_record` | 469 (236 with images, 28,597,806 bytes) |
| `extraction_rule` | 5 |
| `scheduled_event` | 0 |

Migration was run in two passes: the initial Sep 2026 copy (405 records) and a
delta copy of the 64 records added to H2 afterwards. The final pass upserted
every H2 row by primary key and compared H2 vs Postgres with a SHA-256 digest
over all columns (timestamps normalized to instants, binary columns compared as
bytes). All five tables reported `MATCH`.

Tooling used for the delta copy (one-off, not committed): a JDBC program built
against `h2-2.4.240` and `postgresql-42.7.11` extracted from the application jar.
A pre-migration H2 backup was saved to
`/tmp/openschool-migration/digitalschool.pre-postgres-migration.mv.db`.

## Tests and CI

- The JUnit suite runs entirely on in-memory H2 and needs no Postgres or Docker.
  `src/test/resources/application.properties` supplies the default H2 datasource
  (`jdbc:h2:mem:digitalschool-test;MODE=PostgreSQL;...`) and excludes the Telegram
  long-polling starter. Individual tests may point at their own in-memory DB.
- Run everything with `./gradlew test`. JaCoCo 0.8.16 attaches to the same run and
  writes an HTML/XML/CSV report to `build/reports/jacoco/test/`.
- `./gradlew jacocoTestCoverageVerification` (also part of `./gradlew check`)
  fails below **65% line** / **55% branch** coverage. Current: 68.0% lines,
  63.6% branches, 65.9% instructions (221 tests).
- The Playwright suite uses `application-e2e.yml` (also in-memory H2) and is not
  counted in JaCoCo coverage.
- `.github/workflows/ci.yml` runs `./gradlew test jacocoTestCoverageVerification`
  on pushes and pull requests to `main` with Temurin Java 25, uploads JUnit
  results/report and the JaCoCo HTML report, and backs the README CI badge. No
  database service is configured in CI.
- CI regenerates the coverage badges from the JaCoCo CSV and publishes
  `jacoco.svg`/`branches.svg` to the `badges` branch on pushes to `main`; the
  README coverage badges read from that branch.

## Operational setup on this machine

- Docker Desktop now starts at login through the LaunchAgent
  `~/Library/LaunchAgents/com.docker.desktop.autostart.plist`. Previously nothing
  started Docker at login, so `openschool-db` stayed down after reboots and the
  app failed with `Connection to localhost:5434 refused`.
- `openschool-db` uses `--restart unless-stopped`, so it starts with the Docker
  daemon and only stays down if explicitly stopped.
- The app runs as the `com.openschool.app` LaunchAgent (launchd `KeepAlive`) on
  `0.0.0.0:8080`, using the default (Postgres) datasource.
- The old `com.kaushalya.digitalschool.local` agent, which launched the app
  against H2 and held port 8080, is disabled:
  `launchctl print-disabled gui/$(id -u) | grep digitalschool`.

## Known pitfalls

- **Hibernate + Postgres parameter typing.** When a nullable `String` parameter is
  `null`, Hibernate 7 binds it as `JAVA_OBJECT`, and Postgres cannot resolve
  `lower(?)` / `lower('%'||?||'%')` — it raises
  `function lower(bytea) does not exist` (SQLState 42883). H2 tolerates this, so
  H2 tests never catch it. The feed search hit exactly this on 2026-10-31.
  Fix pattern: build the `LIKE` pattern in Java and `CAST` the parameter in HQL,
  e.g. `LOWER(r.originalText) LIKE CAST(:qPattern AS string)`; never apply
  `LOWER()` directly to a nullable parameter. Validate new queries against real
  Postgres, not only H2. The `/feed` failure was fixed this way in
  `ClassificationRecordRepository` (`findFeed` builds the `LIKE` patterns in Java
  and casts them in HQL).
- The app and Postgres must not share port 8080: only one app instance can bind
  it. Do not re-enable the H2 LaunchAgent.
- `docker` is not on `PATH` in non-login shells; use
  `/Applications/Docker.app/Contents/Resources/bin/docker` or fix PATH.
- The H2 file needs empty username/password, not `sa`; see
  `H2_DB_ACCESS_NOTES.md` if the frozen store ever has to be read again.
