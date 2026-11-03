# Card workflow rules (avoid failed starts and wasted tokens)

1. One branch = one live worktree. A follow-up on an open PR uses a new branch `agent/<slug>-fix`
   (based on the PR branch) or the lead first removes the finished card's worktree.
2. Concurrency cap: max 4 cards running/ready (ROADMAP.md). Sub-cards are created blocked; the lead unblocks them.
3. After creating a card, the lead checks it started (Diagnostics) within a minute; spawn failures cost no tokens
   but 2 failures = give up.
4. Java and gh come from Windows PATH; restart Hermes after any install so agents inherit it.
5. PR text follows PR_STYLE.md, and its claims must match what the tests actually assert.

- A new top-level key added to config.local.yaml (e.g. event_calendar) makes every branch that lacks that key fail at startup
  ("Unable to find property"). Remove the key (or test from the PR that adds it) before testing other branches.

- Any server a worker starts by hand (java -jar ...) must be stopped before the card is completed:
  `powershell -ExecutionPolicy Bypass -File tools/agent/stop_servers.ps1`. Leftover servers hold port 8484 and hang the next smoke test.
- Before running the smoke test, merge origin/master into the card branch (git fetch origin && git merge origin/master) so the fixed smoke_test.ps1 (PR #15) is used; the old one leaks servers.
