---
title: "WhatsApp Webhook Integration Plan"
tags: [whatsapp, webhook, integration, digital-school]
status: draft
created: 2026-08-01
---

# WhatsApp Webhook Integration Plan

Follow-up to krushnat's question: integrate a WhatsApp hook so the app receives
WhatsApp messages the same way it currently receives Telegram messages (classify →
persist → feed).

## Feasibility

Yes. The existing ingestion pipeline is channel-agnostic and fully reusable:

```
inbound message
  -> AiChatService.classify()            (shared)
  -> publish ClassificationEvent          (shared)
  -> ClassificationEventListener.save()   (shared, already @TransactionalEventListener)
  -> feed / calendar / day pages          (already render whatever is in the DB)
```

A WhatsApp webhook only needs to add the *receiving* boundary (HTTP endpoint +
Graph API media download + optional reply), then hand off to the same classes.
See `DigitalSchoolBot.consume()` → `ClassificationEvent` for the Telegram analog.

## What "WhatsApp integration" means

- **WhatsApp Cloud API (Meta Graph API)** — the only official webhook-based path.
  Requires a WhatsApp Business Account + Business phone number. Receiving inbound
  messages and replying in service conversations is free.
- Personal (non-business) WhatsApp numbers have NO official webhook. Unofficial
  bridges (whatsapp-web.js, Baileys) are Node.js, not HTTP webhooks, and violate
  WhatsApp ToS — not recommended for this app.

## Server-side design (Spring Boot, no new runtime dependency)

All HTTP via Spring MVC; Graph API calls via `RestClient` (already on classpath in
Spring Boot 4). No new library → no version-compat risk.

### New: `WhatsAppWebhookController` (`@RestController`, path `/api/whatsapp/webhook`)

1. **`GET`** — Meta subscription verification handshake:
   - Read `hub.mode`, `hub.verify_token`, `hub.challenge`.
   - If `mode == "subscribe"` and `verify_token` matches configured
     `whatsapp.webhook-verify-token` → respond 200 with `hub.challenge` body.
   - Otherwise 403. (Meta requires this before enabling the subscription.)
2. **`POST`** — receive message callbacks:
   - Verify `X-Hub-Signature-256` = HMAC-SHA256(appSecret, rawBody) against the
     header. Reject mismatches (403/401) before parsing.
   - Parse `entry[].changes[].value.messages[]`; return `200` promptly (Meta
     retries on non-200).

### New: `WhatsAppMessageService` (ingestion; mirrors `DigitalSchoolBot`)

- **Text message** → `classify(text)` → publish
  `ClassificationEvent(chatId, text, type, null, eventDate, now)` → listener
  persists → appears in feed automatically. Extract scheduled events into
  `ScheduledEvent` exactly as the Telegram path does.
- **Image message** → download bytes via Graph API
  `GET /{graphUrl}/media/{mediaId}` (header `Authorization: Bearer <token>`) →
  `describeImage(bytes, contentType)` → `classify(caption + description)` →
  publish event with `imageData`/`imageContentType` (Telegram photo flow).
- **Document (PDF)** → download, extract text with pdfbox (already a dependency),
  same as `handleDocumentMessage`.
- **Reply** (parity with Telegram) → `POST /{phoneNumberId}/messages` with
  `messaging_product=whatsapp`, `to=<from>`, text = classification result. Use
  Graph API `RestClient`; failure logged, never blocks ingest.

### Mapping

- `chatId` = sender `from` (E.164 phone number parsed to `Long`). Distinct from
  Telegram chat IDs by nature (phone digits vs telegram ids); feed shows the value.
- Message types supported: **text, image, PDF** (mirrors Telegram parity).

### Config (env-backed, presence-gated)

```yaml
whatsapp:
  webhook-verify-token: ${WHATSAPP_WEBHOOK_VERIFY_TOKEN:}
  app-secret:            ${WHATSAPP_APP_SECRET:}
  access-token:          ${WHATSAPP_ACCESS_TOKEN:}
  phone-number-id:       ${WHATSAPP_PHONE_NUMBER_ID:}
```

Controller returns 403 "not configured" when the token is absent, so the app runs
unchanged until Meta credentials exist.

### Tests

- Unit: handshake (valid/invalid token → challenge/403); signature verify
  (valid/missing/tampered → pass/403); payload→service mapping (mock `AiChatService`).
- `@SpringBootTest` (existing mock-AiChatService pattern): POST a real Meta sample
  payload → assert a `ClassificationRecord` is persisted; assert 200 before any AI call.
- Full suite stays green alongside existing 49 tests.

## Prerequisites (external, needed before Meta will call us)

1. Meta Developer account → WhatsApp app → WhatsApp Business Account → Business
   phone number (Cloud API).
2. **Permanent access token** (System User token) + **Phone Number ID** + **App Secret**.
3. **Public HTTPS webhook URL** — Meta will not call localhost. Locally: ngrok /
   cloudflared tunnel; otherwise deploy. The verify handshake must complete before
   Meta enables the subscription.

## Open questions for operator

1. Do you have / can you create a WhatsApp Business Account + Business number for
   the Cloud API? (Personal numbers can't receive webhooks.)
2. Public HTTPS endpoint: tunnel (ngrok/cloudflared) for local dev, or deploy?
3. Confirm scope: text + image + PDF, and reply-to-sender with the classification
   result (Telegram parity)?
