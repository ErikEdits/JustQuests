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

## Open questions (decide later)

- Which local AI runtime? (e.g. an optional companion app the player
  installs; the mod must work fine without it)
- How does the AI know which items/mobs exist in the loaded modpack, so
  generated quests are actually completable?
- Validation: every generated quest must be checked against the same rules
  as datapack quests (valid item IDs, sane counts) before it goes live
- Do expired quests vanish, or can an accepted quest still be finished
  after the refresh?

## Why this fits JustQuests

The quest format is already plain JSON loaded at runtime — a generator
only has to produce the same JSON the datapack loader already understands.
The validation path (Codec parsing with error logging) exists today.
