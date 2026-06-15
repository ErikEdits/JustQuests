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

38. ~~**[architecture]** Item type only, or also tags?~~
    ✅ Answered 2026-06-12: **support tags.** The item field accepts
    either a plain id (`minecraft:oak_log`) **or** a tag
    (`#minecraft:logs` → any item in that tag, incl. modded). Matching
    resolves the tag to its item set; storage/codec must accept both
    forms. Makes modpack quests far more flexible.
39. ~~**[architecture]** NBT/data components, or item id enough?~~
    ✅ Answered 2026-06-12: **both.** Item id/tag is the simple default
    ([[Q38]]); an **optional** data-component/NBT match can be added per
    objective for advanced cases (named/enchanted items). Codec: optional
    `components` field, absent = match any. Simple part ships first; the
    optional component matching can come in a later version.
40. ~~Multi-objective: AND, OR, or both?~~
    ✅ Answered 2026-06-12: **both via a per-quest flag, default AND.**
    Optional field `mode: all` (default) / `mode: any`. Cheap to build,
    full flexibility for pack makers. (Dev decision, not a poll — players
    only see the result, not the mechanism.)
41. ~~New objective ideas beyond the Q13 list?~~
    ✅ Answered 2026-06-12: **add all five** —
    `gain_advancement`, `reach_level`, `tame_animal`, `enchant_item`,
    `visit_dimension`.
    - **Modded content support (dimensions, enchantments, animals):**
      `visit_dimension`, `enchant_item` and `tame_animal` should all work
      with modded content. Everything is identified by registry id, so
      matching ANY dimension/enchantment/mob by id works generically
      (vanilla + modded) without per-mod code. Planned enhancement: a
      curated recognition list for **~100 popular mods** so their content
      gets auto-detected with friendly names/icons.
    - **AI must recognize it all:** the generator reads the loaded
      registries at runtime, so it knows which items/mobs/enchantments/
      dimensions actually exist in the current modpack and only generates
      quests that are completable (ties to the achievability rules in
      [[idea-ai-quest-generator]]).
    - Order these objective types after the Q13 set.
42. ~~Lifetime pickup count or current inventory?~~
    ✅ Answered 2026-06-12: **lifetime pickup counter** (keep current
    behavior). Once collected it counts forever, even if dropped — can't
    be gamed by drop+repickup.

### Quest presentation & tracking

43. ~~Pinned/tracked quest on the HUD?~~
    ✅ Answered 2026-06-12: **yes in principle** (toggleable HUD tracker
    showing active quest + progress) — the exact design/placement is
    decided by a **Discord poll**. ⏳ poll item (added to
    discord-poll-questions.md as Poll 6).
44. ~~Per-quest icon or just text?~~
    ✅ Answered 2026-06-12: **custom icon.** Pack makers set an item as
    the quest icon (`icon: minecraft:diamond_sword`) for a livelier GUI.
    Fallback when omitted: use the first objective's (or reward's) item
    as the icon automatically.
45. ~~GUI sorting/grouping?~~
    ✅ Answered 2026-06-12: support **by category** (AI/custom/datapack/
    team), **by status** (active → available → done), and **custom order**
    (pack-maker defined). Alphabetical not needed. Switchable by the
    player; sensible default = grouped by category, then status,
    respecting the pack's custom order within each group.
46. ~~GUI search/filter box?~~
    ✅ Answered 2026-06-12: **auto-appears only when there are many
    quests** (above a threshold, e.g. 100+). Hidden for small packs where
    scrolling is enough. Search by name/item.
47. ~~Max active quests per player?~~
    ✅ Answered 2026-06-12: **one active quest at a time per player.**
    While a quest is unfinished you can't accept another; finishing it
    frees you to take the next. Applies per player individually. (Total
    quests over time are unlimited — only the *active* count is capped at
    1.) NOTE: this overrides the earlier assumption of multiple active
    quests; commands like `/quest progress` will usually show just one.
    - **Sync:** the available quest pool is shared across players but
      **loosely synced** (not real-time/perfectly synchronized) to avoid
      straining server hardware. Refines [[Q7]] (shared synced set).

### Rewards & completion

48. ~~Reward delivery: instant or claim button?~~
    ✅ Answered 2026-06-12: **claim button in the GUI** — when a quest is
    done the player actively claims the reward (handles full inventory,
    feels more satisfying). Storage needs a **"completed, reward pending"**
    state. Since the GUI arrives in v0.2, v0.1 stays instant; the claim
    flow comes with the GUI.
49. ~~Choice rewards (pick 1 of N)?~~
    ✅ Answered 2026-06-12: **yes** — a quest can offer several rewards
    where the player picks **one**, chosen in the GUI via the claim
    button ([[Q48]]). Comes with the v0.2 GUI. (Clear fit with the claim
    flow — kept as a design decision, no poll needed.)
50. ~~Invalid reward item handling?~~
    ✅ Answered 2026-06-12: **log + substitute.** Write a warning to the
    log (so the pack maker sees the broken id) AND give the player a
    **placeholder item with a hint** (e.g. paper/barrier named with the
    missing item's id). Nothing crashes, the error is visible, and the
    player still sees what was meant.

### Integrations & multiplayer

51. ~~Advancement integration: which direction?~~
    ✅ Answered 2026-06-12: **both directions.**
    - Quest → Advancement: completing a quest grants a vanilla
      advancement (optional `grants_advancement` field).
    - Advancement → Quest: reaching an advancement completes/unlocks a
      quest (works with the `gain_advancement` objective from [[Q41]] and
      the v0.3 unlock/chain system).
52. ~~Economy integration?~~
    ✅ Answered 2026-06-12: **yes, via the command reward.** Because the
    command reward runs any command, it pays currency through **any**
    economy plugin — including custom/self-written ones — with **no
    direct dependency or per-plugin code**. Works for many economy
    plugins out of the box. General note: more integrations and features
    will keep being added over time.
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

---

## Round 4 (added 2026-06-12) — UX details & edge cases

63. **Accept confirmation** — does accepting a quest pop a confirm
    ("Accept this quest?") or just start instantly?
64. **GUI theming** — support light/dark variants of the GUI, or one fixed
    look?
65. **Controller/gamepad** navigation in the GUI — worth supporting, or
    keyboard/mouse only?
66. **Accessibility** — narrator/screen-reader labels and colorblind-safe
    category icons (not color-only)? In from the start or later?
67. **Pagination size** — how many quests per GUI page before it scrolls/
    paginates?
68. **Hover tooltips** — show the full vanilla item tooltip when hovering
    a reward/objective item in the GUI?
69. **Progress display style** — bar, `5/16` fraction, percent, or all of
    them?
70. **Empty state** — what shows when there are no quests available (a
    friendly message, hidden button, etc.)?
71. **Completion feedback** — particles/animation on completion, or just
    text + sound?
72. **Chat format** — should the chat prefix/colors be configurable, or
    fixed JustQuests styling?
73. **Chat pagination** — should `/quest list` paginate in chat when there
    are many quests (page buttons), instead of dumping all lines?
74. **Player favorites** — let a player bookmark/favorite quests to the
    top of their list?
75. **Cooldown time display** — relative ("available in 3h") or absolute
    ("18:00") for repeatable/locked quests?
76. **First-join hint** — show new players a one-time message pointing
    them to the quest system?
77. **[architecture]** Categories ([[Q3]]) — a **fixed** set (AI / custom
    / datapack / team) or **pack-definable custom categories**? Affects
    the data model.
78. **Quest detail view** — clicking a quest opens a dedicated detail
    panel, or is everything shown inline in the list?

---

## Round 5 (added 2026-06-12) — server & multiplayer operations

79. **Cross-dimension tracking** — do objectives count in any dimension
    (Nether/End included), or can a quest be dimension-restricted?
80. **Offline progress** — confirm nothing advances while a player is
    offline (only the 12h rotation clock runs)?
81. **[architecture]** Multiverse servers — is progress **per world** or
    **per server** when several worlds exist? (Ties to the world-folder
    storage decision.)
82. **Season/world wipe** — an admin command to wipe ALL quest progress
    server-wide (for resets/seasons)?
83. ~~Permission system?~~
    ✅ Answered 2026-06-12 (volunteered): support **both** — vanilla OP
    levels **and LuckPerms / other permission-rank plugins** for certain
    functions (e.g. who may create polls/quests, change difficulty,
    access restricted quests). Permission-gated features use permission
    nodes so any compatible plugin works. Relates to [[Q55]].
84. **Event logging** — log quest accept/complete events to the server
    log or a file for admins?
85. **Creative/spectator** — should objectives count in creative mode, or
    only survival/adventure?
86. **Definition changes mid-progress** — if a quest's required count
    changes (e.g. 16 → 32) while a player is mid-progress, keep progress,
    reset it, or cap it?
87. **Claim race** — if two players accept the same generated quest in the
    same instant, how is the exclusive claim ([[Q7]]) resolved?
88. **Quest transfer** — should an admin be able to move/copy one player's
    progress to another (e.g. account migration)?

---

## Round 6 (added 2026-06-12) — project, community & meta

89. **Documentation home** — GitHub wiki, the Modrinth pages, or keep
    everything in the repo `docs/`?
90. **Versioning scheme** — strict semver (major.minor.patch), and what
    counts as a breaking change for a quest mod?
91. **Contributions** — accept community PRs, and for which areas
    (translations, quests, code)? Any areas off-limits?
92. **Example pack / template** — ship a small example datapack or a
    template repo so pack makers have a starting point?
93. **Showcase media** — a short trailer/GIF for the Modrinth/CurseForge
    page beyond the gallery screenshots?
94. **Translation crowdsourcing** — use a platform (e.g. Crowdin) for
    community translations later, or just accept lang-file PRs?
95. **Support channel** — GitHub issues only, or also a Discord support
    channel (ties into the bot)?
96. **Telemetry** — confirm the mod ships with **zero** automatic
    telemetry/analytics (all feedback is opt-in via the Discord polls)?

---

## Round 7 / FINAL — reserved (NOT yet written)

> Per user request (2026-06-12): the final round will be **50 questions
> about NEW features the user plans for the mod**. **Do not generate these
> until the user explicitly says "letzte Runde".** Placeholder only.

## Special survey section — reserved (NOT yet written)

> Per user request (2026-06-14): AFTER the final round (Round 7) is
> finished, create a **special survey part of 70 ultra-detailed
> questions**, as elaborate as possible. **Do not generate until the
> final round is complete.** Placeholder only.

## Planned: step-by-step build TODO

> Per user request (2026-06-14): produce a sophisticated, step-by-step
> build TODO for the mod — each feature broken into ordered sub-steps,
> more detailed than `implementation-order.md`. To be created when
> development starts (user will signal). See implementation-order.md for
> the high-level phase order in the meantime.
