# ADR 0001 — How simulated players work

Status: **Accepted** (human, 2026-11-03; E3 re-activated, social features first)

## Context
The live-server feel needs other "players" in towns, training maps and the Free Market. Options:

| Option | How | Pros | Cons |
|---|---|---|---|
| A. Server-side residents | Real `Character` objects with a socketless `Client`; server broadcasts their movement/attacks/chat to real players | Cheap (hundreds per JVM), no networking, full access to server state | Needs care so code paths that assume a socket don't NPE; not a load test |
| B. Headless protocol bots | Separate process speaks v83 protocol like a real client | Exercises real code paths; doubles as load test | Must implement login, crypto, movement packets; heavier per bot |
| C. Scripted NPC look-alikes | NPCs styled as players | Trivial | Not interactive; doesn't feel live |

## Decision (proposed)
Start with **A** for population/feel, gated by `SIM_PLAYERS_ENABLED`. Build **B** in E0 as the test client anyway,
and later let it drive a small number of sims for load testing.

## Priority (human, 2026-11-03): social features first
Economy guardrails are NOT a priority for sims. Phase 1 goal is players interacting with bots:
1. Bots visible in towns, walking, chatting (phrase bank, replies to simple greetings/whispers).
2. Bot accepts a real player's party invite; bot can send party invites; party chat works.
3. Bots on buddy list / guild / messenger where a socketless character can be supported.
4. Later: bots follow and fight in a party; Party Quests (per-quest logic, separate cards).
Each social system (party, buddy, guild, trade) needs a small cosmic-core card so it tolerates a socketless Client.
Merchant/Free Market bots and budget tracking move to a later phase.

## Bot placement (human, 2026-11-03)
- Fixed home map per bot, fixed names (config list). No random wandering between maps.
- In-map routine: idle / walk / sit / occasional chat line. Population by hour of day, capped per map and channel.
- Real-player interaction (party invite, whisper) switches a bot to "responding": follows the party leader across
  maps, returns home when the party disbands.
- Config flag `SIM_PLAYERS_ENABLED: false` by default; GM command to list/spawn/remove; bots flagged in DB for one-query wipe.
- Later: scripted town-to-training-map moves, random names.

## Consequences
- New package `src/main/java/sim/` owned by `cosmic-sim`; touches to `Client`/`Character` go via small, reviewed
  `cosmic-core` cards (e.g. "make Client tolerate null channel").
- Sim characters are stored in DB with a flag so they can be wiped in one query.
- Economy guardrails tracked in E2 telemetry.
