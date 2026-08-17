---
title: "H2 DB Access — Empty Username Gotcha"
tags: [h2, database, dbeaver, digital-school]
status: active
created: 2026-08-07
---

# H2 Database Access Notes (my-private-digital-school)

## Facts (verified 2026-08-07)

- App datasource: `jdbc:h2:file:./data/digitalschool;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` in `src/main/resources/application.yml`.
- DB file: `REPOS/my-private-digital-school/data/digitalschool.mv.db`. Tables: `classification_record`, `scheduled_event`.
- **Credentials: empty username + empty password.** The app connects as `user=` (see HikariPool log). Explicit `sa`/`SA` FAIL with `JdbcSQLInvalidAuthorizationSpecException: Wrong user name or password [28000-240]`. Do not advise `sa`.
- H2 version in use: **2.4.240** (gradle: `com.h2database:h2` runtimeOnly).

## Verification commands

```bash
cd REPOS/my-private-digital-school
H2JAR=<gradle cache h2-2.4.240.jar>
java -cp "$H2JAR" org.h2.tools.Shell -url "jdbc:h2:file:./data/digitalschool;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE" -user '' -password '' -sql "SHOW TABLES"
```

## DBeaver caveats

- DBeaver's bundled H2 driver is 1.4.199 — cannot read 2.4.240 format files ("write format 3 is larger than supported format 1"). Must update driver to 2.x via Driver Manager.
- Embedded H2 locks the file to one process; no `AUTO_SERVER` in URL. Stop the app before external connections, or add `AUTO_SERVER=TRUE`.

## Live-DB backfill run (2026-08-07, content_type)

- 2026-08-07: added `content_type VARCHAR(20)` to live DB via `ALTER TABLE classification_record ADD COLUMN IF NOT EXISTS content_type VARCHAR(20);`, then backfilled 395 rows with `CASE WHEN image_data IS NOT NULL THEN 'IMAGE' ELSE 'TEXT' END WHERE content_type IS NULL` → 175 IMAGE / 220 TEXT. Verified pre/post counts match.
- During the attempt, H2 Shell failed with `JdbcSQLNonTransientConnectionException: Database may be already in use ... [90020-240]` — root cause was **DBeaver holding an open embedded connection** (confirmed via `lsof <db>.mv.db`), not the app. Ask the operator to disconnect DBeaver before live-DB writes.
- Run H2 Shell from a cwd-free absolute URL (`jdbc:h2:file:<abs path>/digitalschool`) to avoid relative-path surprises.

## Trace file

`data/digitalschool.trace.db` logs all connection attempts (old lock errors + DBeaver 1.4.199 format errors + wrong-password attempts). Useful for diagnosing external-tool connection issues.
