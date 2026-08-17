---
title: "BootUI MCP Server + OpenCode Integration Notes"
tags: [bootui, mcp, opencode, ai-agents, digital-school]
status: active
created: 2026-08-12
---

# BootUI MCP Server + OpenCode — research notes

Question from channel (2026-08-12): BootUI shows a report and "supports adding an
MCP server for local agents" — can opencode agents be wired into that config?

## TL;DR

- **BootUI is the MCP *server***, not a hub you register agents into. It exposes a
  local, opt-in, JSON-RPC 2.0 MCP endpoint at `POST /bootui/api/mcp` so AI coding
  agents can call its advisors and read runtime diagnostics. The panel shows a
  copyable **client** config pointing an agent at BootUI.
- **OpenCode is a first-class MCP *client*** — it consumes remote `http` MCP servers
  via `opencode.json`. So the supported move is: point opencode at BootUI.
- **The reverse is NOT native.** OpenCode does not expose itself as an MCP server
  (no built-in "agent-as-MCP-server" endpoint), so you cannot add an opencode agent
  *into* BootUI's config. Third-party wrappers (e.g. `nosolosoft/opencode-mcp`)
  shell out to the opencode CLI to fake an MCP server — out of scope here.

## Verified on this app (2026-08-12, BootUI 1.13.1, :8080)

`GET /bootui/api/mcp-server` returns:

```json
{
  "enabled": false,
  "configuredMode": "OFF",
  "overridden": false,
  "serverName": "bootui",
  "transport": "http",
  "endpoint": "/bootui/api/mcp",
  "protocolVersion": "2025-06-18",
  "maxResults": 200,
  "toolCount": 21,
  "tools": [ ... ]
}
```

- Server endpoint exists and advertises **21 tools**; it is **disabled by default**
  (fail-closed). Live value confirmed `enabled:false`.
- Tool groups:
  - Advisor scans (actions): `architecture_scan`, `spring_scan`, `hibernate_scan`,
    `memory_scan`, `security_scan`, `pentest_scan`, `rest_api_scan`,
    `graalvm_scan`, `crac_scan`.
  - Diagnostics reads: `get_live_activity`, `get_exceptions`, `get_exception_detail`,
    `get_security_logs`, `get_sql_traces`, `get_traces`, `get_log_tail`,
    `get_http_exchanges`.
  - Core context: `get_overview`, `get_health`, `get_config` (masked), `get_beans`,
    `get_mappings`.

## How to wire opencode to BootUI

1. **Enable BootUI's MCP server** (one of):
   - `application.yml`: `bootui.mcp.enabled: ON`
   - Runtime toggle in the BootUI **MCP Server** panel (`/bootui/#/mcp-server`) —
     overrides the property for the life of the process.
   - `GET /bootui/api/mcp-server` confirms live state (`enabled:true`).

2. **Add BootUI as a remote MCP server in opencode config.** Project-level
   `opencode.json` at the repo root (this repo has none today; global config at
   `~/.config/opencode/opencode.json` holds only provider settings):

   ```json
   {
     "$schema": "https://opencode.ai/config.json",
     "mcp": {
       "bootui": {
         "type": "remote",
         "url": "http://127.0.0.1:8080/bootui/api/mcp",
         "enabled": true
       }
     }
   }
   ```

   (BootUI's own suggested client block uses `"type": "http"` — the `mcp.json`
   dialect for Copilot/Claude Code; opencode's schema uses `"type": "remote"`.)

3. **Result:** every opencode agent running in this project gains BootUI tools,
   prefixed `bootui_*` (e.g. `bootui_hibernate_scan`, `bootui_get_live_activity`).
   Enabling is per-project; can be scoped per agent via the `agent.<name>.tools`
   block or the `tools` allow/deny globs if a subset is preferred.

## Security notes

- Loopback-only, no credentials needed for a local non-browser client (plain HTTP
  config works). Host allow-list + cross-site write defenses still apply.
- Read tools require the backing panel enabled; `*_scan` actions refuse when the
  panel is read-only or `bootui.read-only=true`.
- Secret masking and `bootui.mcp.max-results` (default 200) bound all output.
- The MCP endpoint is never reachable in production (BootUI only active in dev
  profiles / `bootui.enabled=ON`).

## Applied (2026-08-12)

krushnat confirmed "configure and restart the app" — both changes landed:

1. `src/main/resources/application.yml`:
   ```yaml
   bootui:
     enabled: ON
     mcp:
       enabled: ON
   ```
2. New `opencode.json` at the repo root:
   ```json
   {
     "$schema": "https://opencode.ai/config.json",
     "mcp": {
       "bootui": {
         "type": "remote",
         "url": "http://127.0.0.1:8080/bootui/api/mcp",
         "enabled": true
       }
     }
   }
   ```

**Verification (2026-08-12, after restart):**
- `./gradlew test` — **160 tests, 0 failures**.
- App restarted via `./gradlew bootRun` on :8080; boot log shows
  `BootUI activation: Enabled by bootui.enabled=ON`.
- `GET /bootui/api/mcp-server` → `enabled:true`, `configuredMode:"ON"`, `toolCount:21`.
- Real MCP handshake: `POST /bootui/api/mcp` `initialize` returns
  `serverInfo: bootui`, protocol `2025-06-18`, capabilities tools+prompts; `tools/list`
  advertises all 21 tools (9 `*_scan` actions, 8 diagnostics reads, 4 core context).
- `/feed`, `/classifications`, `/calendar`, `/h2-console/` all still 200.
- No errors in boot log.

Now every opencode agent running in this repo has `bootui_*` tools (e.g.
`bootui_hibernate_scan`, `bootui_get_live_activity`) as long as the app is up on :8080.

## References

- AI agents doc (client config + tool list): <https://www.julien-dubois.com/boot-ui/ai-agents>
- Features / MCP panel: <https://www.julien-dubois.com/boot-ui/features>
- Properties: <https://www.julien-dubois.com/boot-ui/properties>
- OpenCode MCP servers config: <https://opencode.ai/docs/mcp-servers>
