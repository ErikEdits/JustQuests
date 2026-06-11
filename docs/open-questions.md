# Open Design Questions

Numbered so answers can simply reference the number ("3: yes").
Answered questions get moved into the matching brief as a design decision.
Questions marked **[architecture]** should be answered before the related
code gets built — their answer changes the storage or API design.

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
5. Are AI quest **rewards** also chosen by the AI (within the built-in
   limits), or hand-defined reward pools the AI only picks from?
6. If no local AI runtime is installed: is the feature **silently off**,
   or should a simple built-in random generator (templates, no AI) act as
   fallback so everyone gets rotating quests?
7. **[architecture]** Mod on a dedicated server: are AI quests the
   **same set for all players** on the server, or generated per player?

## Difficulty & Expiry System

8. Is the difficulty setting **per world or per player**? And on a
   server: who may change it (OP only)?
9. What difficulty levels do you imagine (e.g. Easy / Normal / Hard) and
   roughly what should each change (expiry yes/no, grace time, quest
   amount, count sizes)?

## GUI (v0.2)

10. How is the GUI opened: **command** (`/quest gui`), **keybind**,
    **physical quest book item**, or several of these?
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
