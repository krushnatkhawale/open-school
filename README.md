# open-school

[![CI](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml/badge.svg)](https://github.com/krushnatkhawale/open-school/actions/workflows/ci.yml)

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
- Playwright end-to-end tests use the `e2e` profile (also in-memory H2):

  ```bash
  npm install
  npx playwright install
  npx playwright test
  ```

CI (`.github/workflows/ci.yml`) runs `./gradlew test` on every push and pull
request to `main` with Temurin Java 25, and uploads the JUnit results/report as
artifacts. The status badge at the top of this README reflects that workflow.

## Documentation

- [`docs/RESEARCH/POSTGRES_MIGRATION.md`](docs/RESEARCH/POSTGRES_MIGRATION.md) — runtime database, migration, operational pitfalls
- [`docs/RESEARCH/SPRING_BOOT_4_MOCKMVC_NOTES.md`](docs/RESEARCH/SPRING_BOOT_4_MOCKMVC_NOTES.md) — test setup gotchas
- [`docs/PLANS/`](docs/PLANS/) and [`docs/RESEARCH/`](docs/RESEARCH/) — feature plans and research notes

## On this machine

The app runs as the `com.openschool.app` LaunchAgent (launchd keeps it alive and
starts it at login). The old `com.kaushalya.digitalschool.local` agent, which ran
the app against H2 on port 8080, is disabled.
