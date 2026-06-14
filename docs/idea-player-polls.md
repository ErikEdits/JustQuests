# Idea: In-game Player Polls / Feedback

> **Status: idea only — NOT scheduled. Has serious privacy/security
> constraints (read the warnings). Do not implement the unsafe parts.**

## The goal (good and worth doing)

Gather player preferences directly from people who use the mod, to steer
development with real data — the same feedback/data-driven approach used
for the whole project. Show an occasional in-game poll; the player answers
(click an option, or type 1–5 in chat); results are collected and can be
tallied.

## What is fine to build (client side)

- A small in-game poll prompt: a question + 2–5 options, answer by clicking
  a chat option or typing the number.
- Random scheduling: a poll appears at a random time, spaced **1–4 hours**
  apart, never too often.
- **Each poll shown only once per user** — store "seen poll ids" locally.
- Random selection of which poll to show next, from a poll list.

All of the above is local to the client and harmless.

## ⚠️ What must NOT be built as described

**1. The mod cannot "write results to a file on GitHub."**
Writing to GitHub needs an access token. A token embedded in a distributed
mod jar is extractable by anyone who downloads the mod, letting them
write to or delete the repository. **Never embed write credentials in
client-distributed software.** This part is a critical security hole, not
a feature.

**2. "Send a poll to all online users from GitHub" is not possible.**
GitHub cannot push messages to game clients and has no idea who is online.
That needs a real backend server that tracks online users and pushes to
them — commercial-scale infrastructure, which this project deliberately
avoids ([[user-minecraft-modder]]).

**3. Privacy / GDPR.**
The author is in Germany and users are in the EU. Collecting data from
players (poll answers, implicitly who is online/when) is personal-data
processing under GDPR: it needs **explicit opt-in consent** and a privacy
policy. Mods that "phone home" without consent get a very bad reputation.

## ✅ Safe design that achieves the same goal

**In-game prompt that links out to an external poll.**
- The mod occasionally shows: "📊 Quick JustQuests survey — answer here:
  <link>" (Discord poll / a web form), spaced 1–4h, once per user.
- The player chooses whether to click the link → consent is explicit, no
  data leaves the game automatically, no embedded tokens, no backend.
- Results are collected by Discord/the form, which already tally for you.
- Fits the planned Discord community poll workflow ([[open-questions]]).

**If true in-game answer collection is ever wanted** (player answers
inside the game and data is recorded automatically), it requires:
- an explicit **opt-in consent screen** on first launch (off by default),
- a **dev-controlled endpoint** (a tiny web service you own) — NOT GitHub,
  and NEVER a token shipped in the jar,
- a privacy policy.
That is a separate, larger project with legal obligations — only if you
decide it's worth it.

## Note on the GitHub-build concern

The user's instinct "writing the results file shouldn't trigger a build"
is already handled for docs: CI skips builds for `docs/**` and `**.md`
changes (paths-ignore). So any results/notes kept as a markdown/JSON file
under `docs/` won't start a build.

## Decision needed (later)

- Go with the safe "link-out" design (recommended, zero infra), or
- commit to a real opt-in backend (bigger, legal work) — probably no.
