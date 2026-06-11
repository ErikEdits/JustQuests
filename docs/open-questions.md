# Open Design Questions

Numbered so answers can simply reference the number ("3: yes").
Answered questions get moved into the matching brief as a design decision.
Questions marked **[architecture]** should be answered before the related
code gets built — their answer changes the storage or API design.

> **Community survey plan (2026-06-11):** questions marked ⏳ as
> community-decided will be part of **one big poll on the Discord server**
> once it has enough members — not asked one by one.

## AI Quest Generator

1. ~~Should players be able to turn AI quests **off per world**?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (yes; plus auto-created, never-rotating custom quest file per world)
2. ~~How many AI quests per 12-hour cycle?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (server: configurable amount; singleplayer: fixed maximum cap)
3. ~~Should AI-generated quests be **visually marked**?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (everything categorized, likely distinct symbols per category)
4. ~~May players **reroll** an AI quest they don't like?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (players: no; server OP/admin only; accepted quests survive rotation)
5. ~~Are AI quest **rewards** also chosen by the AI?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (yes, AI-chosen; limits determined from collected test-phase data)
6. ~~If no local AI runtime is installed: silently off or fallback?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (moot: generator is self-programmed and built in, always works;
   AI model only as optional singleplayer extra via key + own code)
7. ~~**[architecture]** Same set for all players, or per player?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (one shared synced set everywhere; accepted quests are exclusively
   claimed — no other player can take them)

## Difficulty & Expiry System

8. ~~Is the difficulty setting **per world or per player**?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (per world/server, same for all players, OP-only; test phase may
   still revise)
9. ~~What difficulty levels and what should each change?~~
   ✅ Answered 2026-06-11 → moved to
   [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
   (Easy / Normal / Hard; effects of each level decided by test data)

## GUI (v0.2)

10. How is the GUI opened: **command** (`/quest gui`), **keybind**,
    **physical quest book item**, or several of these?
    ⏳ 2026-06-11: decision deferred — user will **ask the community**
    which way works best and evaluate the feedback before deciding.
11. Should there be a **craftable quest book item**? Should new players
    get it automatically on first join?
12. Notifications: stay **chat-only** (like v0.1), or also **toast
    popups / sounds** on progress and completion?
13. Which v0.2 objective types matter most — priority order for:
    `kill_mob`, `place_block`, `reach_location`, `craft_item`?
    (`craft_item` would lift the pickup-only limitation but needs a
    different event hook.)

## Progress & Data

14. **[architecture]** Should quest progress ever be **shareable in a
    team/party** (FTB-Quests-style), or strictly per player forever?
    This changes the storage format and is much cheaper to decide now.
15. **[architecture]** The future world-folder progress file: **JSON**
    (human-readable, plugin-friendly, easy to debug) or **NBT**
    (Minecraft-native, compact)? JSON is the natural fit for the
    mod↔plugin bridge.

## Distribution & Releases

16. Publish on **Modrinth only**, or **Modrinth + CurseForge**?
17. After 1.21.1: which MC versions matter most to you first?
    (1.20.1 is by far the most popular pack version right now.)
18. Same name **"JustQuests"** on every loader and for the plugin, or a
    suffix like "JustQuests (Fabric)" per platform page?
19. Is the plugin edition also **MIT-licensed**, same as the mod?

## Localization

20. Should the mod eventually ship **translations** (e.g. German), and
    should quest packs support per-language quest texts — or do quest
    authors simply write in whatever language their pack targets?
