# PR descriptions - write for a non-programmer reviewer

The human approves PRs and is new to server development. Every PR body uses exactly this shape,
plain language, no jargon (or explain it in brackets). Max ~15 lines.

## What this does
One or two sentences: what changes for the server or players.

## Why
One sentence: the problem it fixes or the goal it serves.

## How to check it
First line: the exact folder to run from, the PR branch's worktree (the main folder lacks unmerged code):
`cd C:\Users\joshs\Documents\GitHub\Cosmic\.worktrees\<card-id>`
Then numbered, human steps (e.g. "start the server, log in as admin, open Henesys, NPC X now sells Y").
If it cannot be checked by hand, say "no visible change, tests only".
Always include copy-paste commands, even for "tests only" PRs, in this order:
  1. `cd` line above (create it first if missing: `git worktree add .worktrees/<card-id> origin/<branch>`)
  2. Tests: `.\mvnw.cmd -B test` (or the one class: `-Dtest=<Name>`) and what "pass" looks like
  3. Smoke test (gameplay PRs): `powershell -ExecutionPolicy Bypass -File tools/agent/smoke_test.ps1`
  4. In-game steps, with GM commands spelled out (e.g. `!warp 910000000`). If they need 2 characters
     or the real client, say so, and mark which steps are optional.
Never write "run the smoke test" without the command. Close any other running server first (port 8484).

## Risk
Low / Medium / High, plus one line why. Say what could break.

## Tests
Build: pass/fail (N tests). Smoke test: pass/fail/not run (why).

## Needs from you
"Safe to merge" OR the exact question/decision you need. Never leave this blank.

## Technical notes (optional, last)
File paths, packet/opcode notes, sources. Keep code detail here only.

Keep "How to check it" short: max 5 numbered steps, one command per step. If a config key is needed, say exactly which
file and that it belongs in the PR folder's own config.local.yaml (never the main one), and say how to remove it afterwards.
Put optional/extra checks in a separate "Optional" line so the human can skip them and still approve.
