---
title: "True User Research — Teachers vs Parents"
tags: [product, research, personas, adoption, parent-teacher]
status: active
created: 2026-08-07
---

# True User Research — Teachers vs Parents

Prompt: krushnat (thread `9fbd4d54c41783586fc7b87c6e40094b0cb582df3e873d8e8df683b7708dbf50`)
"who can be true user of this app? teachers or parents? pros, cons.
observation/recommendations. thorough research."

## TL;DR

**Parents are the true (primary) user; teachers are the supply-side accelerator.**
The product works today with zero institutional adoption because a single parent can
photograph their child's notebook and get an organized record. ClassDojo's lesson is that
the moment teachers adopt, parents follow — but the product has to be designed
parent-first and make any teacher participation near-zero-effort.

## Where the product sits today (code + data evidence)

- Ingest is a Telegram bot: text / photo / PDF in -> AI classify -> structured feed +
  calendar + classifications list. Any chat ID can submit; there are no roles or
  permissions today (`DigitalSchoolBot.consume`, `TestSeedController` seed chats 12345/67890).
- Live data (~395 records) is dominated by daily-lesson-update notebook photos — the
  parent-capture pattern (student pencil date top, teacher red-ink date bottom).
- Capable of official content (seed includes notices, holidays, exams, PTM) but nothing
  in the product is "official/published" vs "personal" — no roster, no class scope, no
  ack tracking.
- Planned WhatsApp webhook (`RESEARCH/WHATSAPP_WEBHOOK_INTEGRATION_PLAN.md`) reuses the
  same channel-agnostic pipeline. This is the right architecture: capture on the channel
  parents already live in.

## Parents as true user — pros

- Intrinsic motivation (their own child) -> retention without institutional support.
- Zero school adoption needed: value is immediate (current notebook-photo usage proves it).
- Every feature (feed, calendar, exam/PTM reminders, homework) benefits them directly.
- Two-sided demographic: 2 parents + grandparents per child.
- Monetization precedent: ClassDojo monetized parents (Plus), not teachers.

## Parents as true user — cons

- Each parent's content only covers their own child -> fragmented, no network effect.
- Cannot produce official/school-wide content (notices, schedules) — depends on teachers
  or the school for that tier of value.
- Single-parent stream is low volume; the AI pipeline is under-utilized.
- Children's images/photos on a personal server — privacy expectations and DPDPA-grade
  care needed if going multi-family.

## Teachers as true user — pros

- Content originator: official info flows once and reaches all parents.
- Network effect: one teacher -> 20+ parents (the ClassDojo playbook).
- Renewal: content renews each school day -> sustained engagement.
- Institutional memory + analytics (acks/read receipts) that WhatsApp groups cannot give.

## Teachers as true user — cons

- Classic adoption bottleneck: time-poor, wary of new tools, already "covered" by
  WhatsApp groups; no personal benefit to the teacher; needs training and trust.
- If teachers are the only users and parents don't engage, value collapses.
- Requires school buy-in and survives teacher turnover only with process.
- Current product has zero teacher-facing features (no roster, no send-to-class,
  no acknowledgment tracking).

## Observations

- India channel reality (2026): ~95% of parents are already on WhatsApp; dedicated school
  apps get ~30% downloads and ~10-15% daily activity and collapse to single digits in ~3
  months; ~94% of parents prefer school updates via WhatsApp. Parents will NOT adopt a
  new app they must install. The channel-first (Telegram now, WhatsApp planned) design is
  therefore correct.
- Parent cadence preference: ~74% of parents prefer weekly or monthly updates (schools
  send daily) — the product should digest, not firehose. Daily capture stays silent in
  the background; the weekly summary is the "aha".
- Teachers' actual edtech: WhatsApp is their #1 communication tool; only ~45% use any
  specialized education app. Building teacher flows on top of their existing chat is
  easier than a new dashboard.
- WhatsApp's weakness is precisely this product's wedge: zero structure, no
  institutional memory, no analytics, teacher-exit = data loss. The app is the AI layer
  that structures the channel parents and teachers already use — it should not try to be
  "another school app".

## Recommendations

1. **Primary persona: parent.** Position as "your child's school life, organized
   automatically." The zero-effort photo -> structured record is the killer feature.
2. **Teachers are phase 2, not the foundation.** When added: same near-zero-effort
   capture (photo/text/PDF to the bot), then flows to the class. Minimum teacher mode:
   roster/class concept, publish-to-class, read receipts, weekly summary. Pilot with
   1-2 trusted teachers.
3. **Stay channel-first, never app-first.** Web feed/calendar is the structured layer;
   chat is the inbox. The planned WhatsApp webhook is the correct next channel.
4. **Match cadence to preference:** weekly digest for parents, quiet daily capture.
5. **Before 20 families:** add per-parent scoping (a parent sees only their child's
   records) and a privacy stance for children's images.

## Sources

- ClassDojo network effects / teacher-first bottom-up growth: Sam Chaudhary, "In Depth"
  podcast (First Round Capital) + ReadySetLaunch case study + FuelK12 Go-To-Market piece.
- India WhatsApp vs school-app adoption, 94% WhatsApp preference, school-app 3-month
  collapse: EdPayU "Why Indian Schools Need WhatsApp-First Communication" (2026-01).
- WhatsApp universal but structureless; recommended pattern = WhatsApp primary + AI
  intelligence layer: Chatmadi "WhatsApp vs School App vs ERP India" (2026-03).
- Parent cadence preference (74% weekly/monthly) and channel preference (50% WhatsApp):
  The Hindu/PTI parent-survey report (2024-10).
- Teacher edtech habits (WhatsApp top tool, ~45% use specialized apps): Central Square
  Foundation "Bharat Survey for EdTech 2025" (BaSE 2025).
