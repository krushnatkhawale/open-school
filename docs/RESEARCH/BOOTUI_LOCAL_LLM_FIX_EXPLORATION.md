---
title: "Using a Local LLM to Fix BootUI REST API Findings"
tags: [bootui, local-llm, ollama, spring-ai, rest-api, opencode, digital-school]
status: active
created: 2026-08-12
---

# Local LLM → fix BootUI "REST api" overview findings

Question from channel (2026-08-12): how can we use a local LLM to fix the issues
under the **REST api** scanner card on BootUI's overview dashboard?

## Verified data path (2026-08-12, app live on :8080)

**Findings source — same JSON via two routes:**
- REST: `GET /bootui/api/rest-api` → 200
- MCP: `bootui_rest_api_scan` tool (now wired into opencode via `opencode.json`)

Current scan: 7 controllers, 14 handlers, 53 rules → **10 findings**
(2 HIGH, 1 MEDIUM, 3 LOW, 4 INFO). Report shape:

```
localOnly, disclaimer, basePackages, controllersAnalyzed, handlersAnalyzed,
rulesEvaluated, violationsFound, severityCounts, scan{status:SCANNED,...}, results[]
```

Each result: `id, name, category, severity, description, status, violationCount,
sampleViolations, recommendation, learnMoreUrl, dismissed`.

Current HIGH findings (the targets worth fixing first):
- `RAPI-DTO-001` — Don't expose JPA entities in responses (5 violations, e.g.
  `ClassificationController#getAll` returns a JPA @Entity).
- `RAPI-VALID-001` — `@RequestBody` not validated (`ClassificationController#update`).

**Local LLM — already available:**
- Ollama live on `http://localhost:11434`; installed models include coding models:
  `qwen3-coder:30b`, `qwen2.5-coder:32b`, `qwen3.6:35b`, `gpt-oss:20b`.
- The project already uses Spring AI `ChatClient` → Ollama
  (`AiChatServiceImpl`, `spring.ai.ollama.chat.model: llama3:latest`), so the
  LLM plumbing exists — only a new consumer is needed.
- Smoke test: `qwen3-coder:30b` returned a correct, concrete Spring Boot fix for
  RAPI-DTO-001 (new `record UserDto(...)`, map in service, return DTO from controller).

## Fix loop options

### Option 1 — Agent loop (recommended, zero app changes)
opencode agent running the **local** Ollama model + BootUI MCP tools:
1. Run `bootui_rest_api_scan` (or read `GET /bootui/api/rest-api`).
2. LLM reads findings + controller source, produces the code change.
3. Apply in a worktree, run `./gradlew test` (full suite, must stay green).
4. Re-run the scan → REST API score improves; repeat.

This is exactly the pattern BootUI's AI-agents doc describes, and the local model
is just opencode's configured Ollama provider (`~/.config/opencode/opencode.json`
already lists `qwen3-coder:30b`). No Java changes.

### Option 2 — In-app Spring AI service (suggest-first)
New `RestApiFixService` using the existing `ChatClient`:
1. Fetch `GET /bootui/api/rest-api`.
2. Build a prompt: findings (id/severity/description/sampleViolations/recommendation)
   + the relevant controller/service source files.
3. Call the local coder model (e.g. `qwen3-coder:30b` via `builder().model(...)`).
4. Return a prioritized fix plan / suggested diffs via a dev-only endpoint.

Safe by construction if it only *suggests*; auto-applying code from a running app is
risky and not recommended (apply in git worktree + tests instead).

### Option 3 — Standalone script
Small Python/shell tool: pull findings from `/bootui/api/rest-api`, POST to Ollama,
print diffs. Lightest, but ad hoc; duplicates what options 1–2 give.

## Recommendation

- **Actually fixing**: Option 1 — it reuses existing wiring (BootUI MCP + Ollama
  provider), keeps the full test suite as the gate, and works for every advisor
  (security, spring, hibernate), not just REST api.
- **If the app itself should generate fix suggestions** (dev-facing feature):
  Option 2, suggest-only, surfaced behind a dev endpoint.

## References

- BootUI AI agents pattern: <https://www.julien-dubois.com/boot-ui/ai-agents>
- REST API advisor rules: <https://www.julien-dubois.com/boot-ui/rest-api-checks>
- Project's existing Spring AI + Ollama usage:
  `src/main/java/com/kaushalya/digitalschool/classification/AiChatServiceImpl.java`
- opencode + BootUI MCP wiring: `opencode.json` (repo root), `BOOTUI_MCP_OPENCODE_NOTES.md`
