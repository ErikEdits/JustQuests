# Idea: AI Quest Generator

> **Status: idea only — NOT scheduled, NOT in any release plan.**
> Do not implement this. It lives here so the idea is not lost.

## The idea in one sentence

The mod generates new quests automatically using an AI that runs locally on
the player's PC, and the quest set refreshes every 12 real-world hours.

## How it should work

**Local AI, no cloud.**
Quest generation runs entirely on the player's machine. No internet, no
API keys, no accounts. If no local AI is available, the feature is simply
off (or falls back to template-based random quests — to be decided).

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

## Open questions (decide later)

- Which local AI runtime? (e.g. an optional companion app the player
  installs; the mod must work fine without it)
- Do expired quests vanish, or can an accepted quest still be finished
  after the refresh?

## Why this fits JustQuests

The quest format is already plain JSON loaded at runtime — a generator
only has to produce the same JSON the datapack loader already understands.
The validation path (Codec parsing with error logging) exists today.
