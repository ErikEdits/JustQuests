# Idea: AI Quest Generator

> **Status: idea only — NOT scheduled, NOT in any release plan.**
> Do not implement this. It lives here so the idea is not lost.

## The idea in one sentence

The mod generates new quests automatically using an AI that runs locally on
the player's PC, and the quest set refreshes every 12 real-world hours.

## How it should work

**Built-in generator, zero dependencies (revised 2026-06-11).**
Quest generation is the mod's **own procedural generator**: weighted
random selection + templates + the built-in rules. No AI runtime, no
installation, nothing to download — it always works, for every player,
on every PC. Quest **descriptions are template-based too** and fully
independent of any AI model. An optional AI-model layer may come later
(see design decisions below).

**12-hour refresh based on the real clock.**
- Quests rotate every 12 hours, following the system clock in the player's
  own timezone
- When Minecraft is closed, the mod saves the current timestamp
- On the next launch it compares the saved time with the current time and
  works out how many 12-hour cycles have passed, then refreshes accordingly
- Example: quit at 18:00, start again next day at 10:00 → 16 hours passed
  → one refresh is due

**Per-world quests.**
- Every world/save has its **own** quest set and its **own** refresh timer
- Quests are never shared between worlds — World A and World B always have
  different AI-generated quests
- Implementation hint: store generation state and timestamps per save
  (world data), not globally

## Design decisions (answered 2026-06-10)

**Balancing and achievability are baked into the mod during development.**
The rules the AI must follow — which items/mobs it may use, sensible count
limits, nothing oversized ("galactic") — are written into the mod itself
as built-in instructions and constraints. The AI calculates quests within
those bounds; it never invents freely outside them. Tuning happens during
development, not by the AI at runtime.

This also covers validation: the built-in constraints plus the existing
Codec parsing (same path as datapack quests) act as the safety net —
anything outside the rules is rejected before it goes live.

**Balancing gets verified through a test phase.**
Before the feature ships, there will be a dedicated testing period that
collects data on the generated quests (what the AI produces, how hard the
quests actually are, how long they take). The built-in rules get tuned
against that data until everything fits — the constraints are not
guesswork, they get adjusted based on real results.

**The AI runs on demand only, never permanently.**
The AI is invoked solely at quest refresh time (the 12-hour rotation) and
is idle otherwise. Everything else — commands, the future GUI, progress
tracking, rewards — works completely independently of the AI. If the AI
is unavailable, the rest of the mod is unaffected.
(The concrete local runtime to integrate is still to be picked during
development.)

**Difficulty is one shared setting per world/server (answered
2026-06-11, Q8).**
- The server owner or world owner (in short: OP) sets the difficulty;
  regular players cannot touch it
- The chosen difficulty applies **equally to all players** in that
  world/server — no per-player difficulty
- Caveat by design: if testing shows per-player would work better, this
  can still change — the test phase decides everything

**Three difficulty levels: Easy / Normal / Hard (answered 2026-06-11,
Q9).** What each level actually changes (expiry, grace time, amounts,
count sizes) is not designed up front — the test phase data decides it.

**Quest expiry depends on a difficulty setting.**
There will likely be a configurable difficulty level that decides what
happens to accepted quests at the 12-hour refresh: whether they expire,
survive, or get a grace period to finish. This gets its own test phase —
player feedback and data from that feedback drive the final tuning.

**AI quests are per-world switchable; custom quests live in their own
file and never rotate (answered 2026-06-11, Q1).**
- AI quests can be turned off per world — one world can be pure
  hand-made quests, another world runs AI quests on top
- The 12-hour reset only ever touches **AI** quests
- Every world gets an **auto-created custom quest file** for self-made
  quests: editable at any time, add quests whenever you want, and its
  contents are never reset by the rotation
- The file and its documentation are in English, and a how-to guide
  ships with it

**Quest amount per 12h cycle depends on the edition (answered
2026-06-11, Q2).**
- **Server (plugin):** the amount is configurable — server owners choose
  how many AI quests each cycle generates
- **Singleplayer (mod):** there is a fixed maximum cap on AI quests per
  cycle (exact number to be tuned during the test phase)

**Quests are organized into categories with own symbols (answered
2026-06-11, Q3).**
- Everything gets categorized — AI quests, custom quests, datapack
  quests are separate categories
- Each category will likely get its own symbol/icon, so players always
  see at a glance what kind of quest they are looking at
- Ties into the v0.2 GUI design: the status icons in
  [gui-design-brief.md](gui-design-brief.md) will need category icons
  on top of the state icons (available/active/completed/locked)

**No reroll for players; accepted quests survive the rotation (answered
2026-06-11, Q4).**
- Regular players can **not** reroll AI quests — short answer: no
- On servers, only the OP / server admin can trigger a reroll
- Quests a player has already **accepted** are never reset by the
  rotation — they stay until finished
- Note to reconcile during the test phase: the difficulty setting was
  earlier planned to decide expiry of accepted quests (expire / survive /
  grace period). "Accepted quests survive until finished" is the default;
  whether higher difficulties may still expire them gets decided with
  test-phase data

**The AI picks rewards itself, inside data-driven limits (answered
2026-06-11, Q5).**
- Rewards are chosen by the AI, not from hand-defined pools
- The limits the AI must respect are **determined from collected data**
  (test phase) — same data-driven approach as the quest balancing
- Confirmed: where decisions overlap (e.g. expiry vs. difficulty), the
  test phase decides everything

**Generator is self-programmed; an AI model is only an optional extra
(answered 2026-06-11, Q6).**
- Decision: **Option B** — the generator is our own code (procedural:
  weighted random + templates + rules), built into the mod and plugin,
  zero external dependencies
- Quest **descriptions** are also generated from templates, independent
  of any AI model
- **Server (plugin):** expected to run entirely without an AI model
- **Singleplayer (mod):** an AI model may optionally be supported later —
  probably unlocked via a key the player provides, integrated through our
  own code; purely a bonus layer for fancier text
- Because the core generator is built in, the old fallback question is
  moot: rotating quests always work, with or without any AI model
- This supersedes the earlier "local AI runtime" framing above — there is
  no required runtime anymore

**One shared, synced quest set — with exclusive claiming (answered
2026-06-11, Q7).**
- **Singleplayer/LAN:** the generated set is the same for everyone in
  the world
- **Server:** one shared set for all players — per-player generation
  would be overkill; the configurable server limits (see Q2) exist
  precisely to keep the amount regulated
- The set is **synced to every player**
- **Exclusive claiming:** once a player accepts a generated quest, no
  other player can accept that same quest — first come, first served
- Design implication: on busy servers the configurable quest amount is
  what server owners scale with their player count, since claimed
  quests are locked for everyone else

**No quest repeats within a 6-day window (answered 2026-06-12, Q26).**
- The generator must never produce the same quest twice inside a rolling
  **6-day** period — keeps the rotation feeling fresh
- After 6 days a quest may appear again (so quests are repeatable, just
  rate-limited, not one-time-forever)
- **Storage implication:** the system keeps a timestamped history of
  recently generated/used quests (not just a done/not-done flag), so the
  generator can filter out anything used in the last 6 days. This is the
  timestamp-based storage format flagged as architecture-critical.

**The custom file is a fill-in-the-blanks template (added 2026-06-11).**
- The auto-created file already contains prepared **empty quest slots**:
  the fields for type, item id, count and reward are all there but
  empty — players only fill in values, they never write JSON structure
  themselves
- The template is generated to a length of roughly **400 lines** of
  ready-to-fill slots (about 20 empty quests)
- Top priority: it must be easy to understand for non-technical players
- Loader rule that makes this work: **slots that are still empty are
  skipped silently** — only filled-in entries become quests, blank slots
  never produce errors or log spam

## Timeline expectations (rough, 2026-06-10)

- Gets built later, step by step like everything else — after the core
  mod is where it needs to be
- Expect **2+ months** to mature the feature; support for several (not
  all) MC versions lands within that window
- After that, roughly **3 more weeks** to port it to other loaders
  (Fabric, Forge)
- Remaining MC versions follow gradually, piece by piece — no big-bang
  release across everything at once

## Why this fits JustQuests

The quest format is already plain JSON loaded at runtime — a generator
only has to produce the same JSON the datapack loader already understands.
The validation path (Codec parsing with error logging) exists today.
