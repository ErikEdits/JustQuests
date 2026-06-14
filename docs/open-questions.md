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
    ⏳ Tendency (2026-06-11): either command-only or a **book that
    appears automatically in the inventory** (not crafted). The book can
    be moved around the inventory freely but **cannot be dropped**.
    Maybe additionally a **clickable button inside the player's
    inventory screen** that opens the quest GUI.
    Final choice goes into the Discord poll.
12. Notifications: stay **chat-only** (like v0.1), or also **toast
    popups / sounds** on progress and completion?
    ⏳ 2026-06-11: goes into the Discord poll.
13. ~~Which v0.2 objective types matter most?~~
    ✅ Answered 2026-06-11 — priority order:
    1. `kill_mob`  2. `place_block`  3. `reach_location`  4. `craft_item`
    (reflected in the README roadmap)

## Progress & Data

14. **[architecture]** Should quest progress ever be **shareable in a
    team/party** (FTB-Quests-style), or strictly per player forever?
    ✅ Answered 2026-06-11 → moved to
    [idea-paper-plugin.md](idea-paper-plugin.md)
    (no own team system, integrates with existing ones; likely a
    dedicated **team quest category** instead of shareable normal
    quests; storage reserves a team/group id field)
15. ~~**[architecture]** World-folder progress file: JSON or NBT?~~
    ✅ Answered 2026-06-11 → moved to
    [idea-paper-plugin.md](idea-paper-plugin.md)
    (both, bridged by codecs: JSON world file + NBT internals; must be
    matured for reliability before shipping)

## Distribution & Releases

16. ~~Publish on **Modrinth only**, or **Modrinth + CurseForge**?~~
    ✅ Answered 2026-06-11: **both platforms.** Plan: set up CI
    auto-publish (release tag → upload to Modrinth + CurseForge in one
    go) so there is no manual double maintenance.
17. ~~After 1.21.1: which MC versions matter most first?~~
    ✅ Answered 2026-06-11: **first 1.20.1**, long-term goal is
    coverage from **1.12.x up to the newest version** (added piece by
    piece). Note: 1.12.x is pre-flattening with very different APIs
    and Forge-only — by far the most expensive port, sensibly last.
18. ~~Same name **"JustQuests"** everywhere?~~
    ✅ Answered 2026-06-11: yes — one name across all loaders and the
    plugin edition; loaders are marked as file tags, not in the name.
19. ~~Is the plugin edition also **MIT-licensed**, same as the mod?~~
    ✅ Answered 2026-06-11: the **whole project switched to LGPL-3.0**
    (mod now, plugin later the same). Reason: forks must stay open
    source; modpack use unaffected. See [licenses.md](licenses.md).

## Localization

20. ~~Should the mod eventually ship **translations**?~~
    ✅ Answered 2026-06-11: **yes, per-user language detection.** The
    mod reads each player's Minecraft language setting and shows quests
    in that player's selected language (English, German, Japanese, … —
    everything detected). Two players on the same server see the same
    quest in their own language.

## Localization follow-ups (queued for 2026-06-12)

21. ~~Where do quest text translations come from?~~
    ✅ Answered 2026-06-12 → moved to
    [idea-localization.md](idea-localization.md)
    (Minecraft's built-in translation keys cover all item/block/mob
    names for free; authors write multi-language title/description
    fields; English is the fallback)

22. ~~Which languages does the mod itself launch with?~~
    ✅ Answered 2026-06-12 → moved to
    [idea-localization.md](idea-localization.md)
    (English only at launch — international default; community
    translations decided when the first PR arrives)

---

## Round 2 (added 2026-06-12)

### v0.1 Release

23. ~~Mod icon~~ ✅ Done 2026-06-12: pixel-art open quest book
    (parchment pages, oak cover, green checkmark, dark rounded
    backdrop). In-jar logo at `src/main/resources/icon.png`, 512px
    project-page version at `docs/assets/icon-512.png`.
24. ~~Release timing~~ ✅ Answered 2026-06-12: **publish v0.1 today.**
    Early release for early feedback (fits the data/feedback approach).
25. ~~Beta test: closed beta or straight to public?~~
    ✅ Answered 2026-06-12: **public beta** — release openly and fast,
    no closed-beta gate. Feedback/test-phase data gets gathered from
    the public release. (v0.1 is already public on GitHub.)

### Quest mechanics

26. **[architecture]** **Repeatable quests:** should quests be
    repeatable (e.g. a per-quest flag, daily/weekly), or is every quest
    one-time forever? Changes the storage format (completed-set vs.
    completion timestamps) — cheap to decide now.
27. **Abandon rules:** may a player re-accept an abandoned quest
    immediately, or with a cooldown? Does abandoning a claimed
    generated quest free it for other players?
28. **Locked quests (v0.3 chains):** are locked quests visible as
    teaser ("???" with lock icon), fully visible but not acceptable,
    or completely invisible until unlocked?
29. **More reward types:** priority for v0.2+ — XP, command execution,
    loot table, choice rewards (player picks 1 of 3)?
30. **Bundled starter quests:** should pack makers be able to disable
    the 10 built-in quests via config, so only their own show up?

### Custom file & data

31. **[architecture]** **Custom quest file location:** in the world
    folder (each world has its own custom quests) or in the config
    folder (same custom quests in every world)? Or both layers?
32. **Live reload:** should the custom file apply changes via
    `/quest reload` while the game runs (no restart needed)?

### Admin & server

33. **Admin commands:** which matter most — give/complete/reset quest
    for a player, view another player's progress, list claimed quests?
34. **Statistics:** should the mod count stats (completed quests per
    player, server leaderboard)? Server owners tend to love this.

### Technical

35. **Config format:** TOML (NeoForge standard) or JSON (consistent
    with the quest files) for the mod's settings?
36. **Update notice:** notify OPs on login when a newer version exists
    (opt-out), or no version checking at all?
37. **GUI style direction:** the book-style two-page layout from the
    GUI brief, a modern single-panel list, or put this into the
    Discord poll too?
