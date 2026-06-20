# JustQuests

**A lightweight quest book for NeoForge.**
No GUI bloat, no heavy dependencies — just quests and a handful of commands.
Built as a focused, server-friendly alternative to FTB Quests and HQM for
packs that want questing without the weight.

> **Still command-only for now.** A full in-game GUI is on the way (see the
> roadmap below). Everything here works right now on **NeoForge 1.21.1**,
> in singleplayer and on servers.

---

## 💬 Join the community — help shape the GUI

The big **in-game GUI (v0.2)** is being designed **by the community**: its
style, layout and features will be decided by polls on the Discord. The more
people vote, the better the GUI everyone gets.

**👉 [Join the JustQuests Discord](https://discord.gg/cMTGE9QCja)** to:

- 🗳️ **Vote on the upcoming GUI** — style, layout and which features land first
- 🛟 Get **support**, report bugs and follow update news
- 👀 See **sneak peeks** and try **early builds** before release

In-game you can run `/quest discord` anytime to get the invite.

---

## ✨ What JustQuests does

- **Ready to play** — ships with a built-in quest progression you can start
  right away, no setup required.
- **Lightweight** — tiny and fast, with no heavy dependencies.
- **Make your own quests** — drop them in a simple per-world JSON file or a
  datapack. No restart needed: the file reloads automatically as you edit it.
- **Speaks the player's language** — quest text can be written in multiple
  languages, and item/mob/block names show up translated for each player
  automatically.
- **Persistent progress** — saved per world, kept across sessions and
  through death.
- **Server-friendly** — runs server-side with per-world storage; stays out
  of the way of other mods and UI overhauls.

---

## 🧩 Objective & reward types

Quests are built from these objectives — mix and match, and choose whether a
quest needs **all** objectives or just **any one** of them:

- **collect_item** – gather items (mining, harvesting, mob drops)
- **mine_block** – break blocks of a type
- **craft_item** – craft a given item
- **smelt_item** – smelt items in a furnace
- **consume_item** – eat or drink an item
- **place_block** – place blocks
- **kill_mob** – defeat entities of a type
- **tame_animal** – tame animals
- **breed_animal** – breed animals of a type
- **reach_level** – reach an XP level
- **reach_location** – arrive at a position (optionally in a set dimension)
- **visit_dimension** – enter a dimension (vanilla **or** modded, by id)
- **gain_advancement** – earn an advancement

Item objectives accept a single id (`minecraft:oak_log`), a list, or a
**tag** (`#minecraft:logs`).

Rewards:

- **give_item** – hand over items
- **loot_table** – roll a loot table for a random reward
- **command** – run any command as the player (`{player}` is substituted) —
  covers economy payouts, effects and more with **no** hard dependency

---

## 🎮 Commands

| Command | What it does |
|---------|--------------|
| `/quest list` | List all quests with their description, goal and reward |
| `/quest accept <id>` | Start a quest (with tab completion) |
| `/quest progress` | Show your active quests and how far along you are |
| `/quest abandon <id>` | Drop an active quest (suggests your active ones) |
| `/quest discord` | Get the community Discord invite (vote on the GUI!) |
| `/quest reload` | *(OP)* reload custom quests |
| `/quest test` | *(OP)* run a self-test and write a diagnostics report |

Quest ids support **tab completion** — `accept` suggests every available
quest, `abandon` suggests only the ones you've taken.

---

## ✍️ Custom quests

On first launch, JustQuests writes a `custom-quests.json` file into your
world folder (`<world>/justquests/`) with an explained example and blank
slots to fill in. Save the file and your quests appear in-game within a few
seconds — no restart. A custom quest overrides a datapack quest with the
same id.

Quest titles and descriptions can be a plain string:

```json
"title": "Mining Trip",
"description": "Mine 20 iron ore."
```

…**or** a per-language map keyed by language code (`en_us`, `de_de`,
`fr_fr`, …), so a single quest reads correctly for everyone:

```json
"title": { "en_us": "Mining Trip", "de_de": "<German title here>" },
"description": { "en_us": "Mine 20 iron ore.", "de_de": "<German text here>" }
```

Each player sees their own client language, falling back to English.

---

## 📦 Built-in quests

JustQuests ships with a ready-to-play starter progression (English + German
out of the box) so you can jump in right away.

| Quest | Goal | Reward |
|-------|------|--------|
| First Steps | 16 oak logs | 4 bread |
| Stone Age | 32 cobblestone | 16 torches, stone pickaxe |
| Fuel for the Fire | 8 coal | 32 torches |
| Iron Harvest | 5 raw iron | iron pickaxe, 8 bread |
| Green Thumb | 32 wheat | 4 golden carrots, 16 bone meal |
| Monster Hunter | 8 rotten flesh + 8 bones + 8 string | bow, 32 arrows |
| Shine Bright | 3 diamonds | 8 XP bottles, golden apple |
| Deep Down | 32 cobbled deepslate | 8 lanterns, golden apple |
| Hot Stuff | 4 blaze rods | 8 ender pearls, 4 magma cream |
| Ender Seeker | 8 ender pearls | 12 obsidian, 2 diamonds |
| Slayer | kill 10 zombies | 5 iron ingots |
| Lumberjack | collect 32 logs (any kind, via tag) | iron axe |

> **Good to know:** `collect_item` counts items **picked up** (mining,
> harvesting, mob drops). To track items the player makes, use `craft_item`.

---

## 📥 Installation

1. Install **NeoForge** for your Minecraft version (1.21, 1.21.1 or 1.21.2)
2. Download the JustQuests jar that matches your version (e.g.
   `JustQuests-neoforge-1.21.1-0.1.6.jar`) into your `mods` folder
3. Launch the game and run `/quest list`

---

## ✅ Compatibility

- **Minecraft:** 1.21, 1.21.1, 1.21.2 (more versions added over time)
- **Loader:** NeoForge (Fabric/Forge planned)
- **Environment:** singleplayer **and** servers (runs server-side)

---

## 🗺️ Roadmap

JustQuests is built step by step. Already shipped: a deep set of objective
and reward types, item tags, quest modes & categories, per-world custom
quests with live reload, and multi-language quest text.

Planned next:

- **v0.2 — in-game GUI**: a real quest book screen, claim button, choice
  rewards and an optional HUD tracker — **designed from
  [Discord](https://discord.gg/cMTGE9QCja) community polls**
- **Server & QoL**: admin commands, statistics/leaderboard, difficulty
  settings, permission gating, completion broadcasts
- **Automatic quest generator**: rotating, registry-aware generated quests
- **Wider reach**: more Minecraft versions, additional loaders
  (Fabric/Forge), and a Paper/Bukkit plugin edition

Feedback shapes the priorities — bug reports and suggestions are very
welcome on the [issue tracker](https://github.com/ErikEdits/JustQuests/issues).

---

## 📄 License

Released under the **GNU LGPL v3**. Free to use in any modpack, no
permission needed. Modified versions and forks must stay open source
under the same license.

**💬 Community & GUI polls:** https://discord.gg/cMTGE9QCja
**Source & issues:** https://github.com/ErikEdits/JustQuests
