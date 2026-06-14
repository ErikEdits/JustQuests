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

26. ~~**[architecture]** Repeatable quests?~~
    ✅ Answered 2026-06-12 → moved to
    [idea-ai-quest-generator.md](idea-ai-quest-generator.md)
    (no quest repeats within a rolling 6-day window; repeatable after
    that → storage uses timestamped history, not a plain done-set)
27. ~~Abandon rules: re-accept immediately or cooldown?~~
    ✅ Answered 2026-06-12: an abandoned quest is **locked from
    re-acceptance** afterwards (not immediately re-takeable) — likely
    until the next rotation. Exact lockout duration to be tuned in the
    test phase. (Whether it frees a claimed generated quest for other
    players: assume no while locked; revisit during test phase.)
28. ~~Locked quests (v0.3 chains): how shown?~~
    ✅ Answered 2026-06-12: shown as a **teaser** ("???" with a lock
    icon) — but only when this has been **enabled via a command**.
    Default otherwise: hidden until unlocked. So OPs/packs opt into the
    teaser display.
29. ~~More reward types: priority?~~
    ✅ Answered 2026-06-12: **loot-table rewards** (random reward from a
    table) are the priority — used especially for the refreshed/
    generated quests, so the AI rotation hands out varied random loot.
    XP / command / choice rewards stay lower priority for later.
30. ~~Bundled starter quests: disable-able?~~
    ✅ Answered 2026-06-12: **not for now** — the 10 built-in quests
    stay always present, no disable toggle yet. May add a config option
    later ("mal gucken").

### Custom file & data

31. ~~**[architecture]** Custom quest file location?~~
    ✅ Answered 2026-06-12: **in the world folder** — each world has its
    own custom quests (World A differs from World B). Matches the
    per-world progress file; custom quests travel with the world.
32. ~~Live reload via command?~~
    ✅ Answered 2026-06-12: **automatic live reload** — no action
    needed. The mod watches the custom quest file and applies changes
    on its own while the game runs (no command, no restart). A manual
    `/quest reload` can still exist as a fallback.

### Admin & server

33. ~~Admin commands: which matter most?~~
    ✅ Answered 2026-06-12 — priority admin commands:
    - reset a player's quest progress (`/quest reset <player>`)
    - view another player's progress (`/quest progress <player>`)
    - list claimed quests (who has what on the server)
    (give/complete-for-player are lower priority / later.)
34. ~~Statistics?~~
    ✅ Answered 2026-06-12: **yes** — track completed quests per player
    and a server leaderboard (who completed the most). Fits server
    play; data also feeds the test-phase tuning.

### Technical

35. ~~Config format: TOML or JSON?~~
    ✅ Answered 2026-06-12: **self-managed JSON** for cross-loader
    uniformity. TOML is native only to NeoForge/Forge (not Fabric);
    using a loader's built-in config API would differ per loader. A
    JSON config read by the mod's own code is identical on NeoForge,
    Fabric and Forge, and consistent with the quest + progress files.
36. ~~Update notice?~~
    ✅ Partial 2026-06-12: **yes** to an update notice in principle —
    but the exact behavior (opt-out, frequency, wording) goes into the
    Discord poll to decide what works best. ⏳ poll item.
37. ~~GUI style direction?~~
    ✅ Answered 2026-06-12: **the Discord poll decides** — book-style
    two-page layout vs. modern single-panel list goes to the community.
    ⏳ poll item.

---

## Round 3 (added 2026-06-12)

### Objectives & matching

38. **[architecture]** Should `collect_item` (and other item objectives)
    match by **plain item type only**, or also support **item tags**
    (e.g. `#minecraft:logs` accepts any log)? Tags make modpack quests
    far easier but change how matching is stored.
39. **[architecture]** Should objectives be able to require a **specific
    item with NBT/data components** (e.g. a named or enchanted item), or
    is item id enough for now?
40. Multi-objective quests: must **all** objectives be completed (AND), or
    should a quest be able to need **any one** of several (OR)? Or both
    via a flag?
41. New objective ideas beyond the Q13 list — worth having any of:
    **gain advancement**, **reach level**, **tame animal**, **enchant
    item**, **visit dimension**? Pick any that matter.
42. Should objective progress **persist if the player drops/loses items**
    (lifetime pickup count, like now), or track **current inventory**
    amount? (Current = can go down; lifetime = can't be gamed.)

### Quest presentation & tracking

43. **Pinned/tracked quest on the HUD** — a small overlay showing your
    active quest + progress on screen (toggle)? Or keep it command/GUI
    only?
44. **Per-quest icon** — should a quest show a custom icon (an item as its
    icon) in the GUI, or just text?
45. GUI sorting/grouping — by **category**, by **status**
    (active/available/done), **alphabetical**, or a pack-defined **custom
    order**?
46. GUI **search/filter** box — needed, or overkill for the expected
    quest counts?
47. Should there be a configurable **max number of active quests** at once
    per player, or unlimited?

### Rewards & completion

48. **Reward delivery** — handed out **instantly** on completion (current
    behavior), or a **claim button** in the GUI the player must click?
49. **Choice rewards** (pick 1 of N) — wanted eventually, and if so does
    the player pick in the GUI or via a command?
50. What should happen if a reward references an **item from a mod that's
    no longer installed** (invalid id)? Skip silently, log, or substitute?

### Integrations & multiplayer

51. **Advancement integration** — completing a quest could **grant a
    vanilla advancement**, and/or an advancement could **complete/unlock**
    a quest. Worth it, which direction?
52. **Economy integration** — if a server has an economy mod/plugin,
    should a reward be able to **pay currency** (via command reward), or
    stay out of economy entirely?
53. **Server announcement** when a player completes a rare/marked quest
    (broadcast in chat), or keep completions private?
54. **[architecture]** Quest source precedence — if a datapack quest, a
    custom-file quest and a generated quest share the **same id**, which
    wins? (Define load order now.)
55. **Per-quest permission** — should some quests be restricted to certain
    ranks/permission nodes (server use), or all quests open to everyone?

### API & data

56. **[architecture]** Should the mod expose a small **API / event hooks**
    so other mods or datapacks can react to quest accept/complete? (Design
    affects internal structure.)
57. **Export/import of player progress** for admins (backup a player's
    quests, move between servers) — needed, what format?
58. Should **statistics** (Q34) be viewable **in-game** (a stats GUI/
    command) or only sent to the owner, and should the leaderboard be
    **public to players**?

### Generator & balance (AI feature, later)

59. Should generated quest **difficulty scale with player progress**
    (e.g. later quests get harder as the player advances), or stay flat
    within a difficulty level?
60. Should the 6-day no-repeat window (Q26) be **configurable** (server
    owners change the days), or fixed at 6?
61. Should the generator avoid quests the player **can't currently do**
    (e.g. Nether items before the player has been to the Nether), or not
    care?

### Polish

62. **Sounds** — which sound on quest **complete** (vanilla level-up,
    XP, custom?), and should there be a subtle sound on **progress
    tick**? (Likely a Discord poll item too.)
