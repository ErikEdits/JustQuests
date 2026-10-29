# JustQuests

**A lightweight quest book for NeoForge, Fabric & Forge — and a server plugin for Paper, Spigot & Purpur.**
No GUI bloat, no heavy dependencies — datapack-driven quests, an in-game quest book, a rotating board of generated quests, team quests and a handful of commands. A focused, server-friendly alternative to FTB Quests and HQM.

> **43 mod builds** for NeoForge, Fabric and Forge across **Minecraft 1.18.2 – 26.3**, plus a **server plugin** for Spigot, Paper and Purpur **1.19 – 26.3** that works with plain vanilla clients. Singleplayer **and** servers.

---

## ✨ Features

- **📖 Quest book** — press **J** to browse quests, follow progress bars and accept, abandon or claim with a click. Grouped by category or status (fold a group with a click), a pixel icon for every quest, a search field, "Accept all" and a stats page.
- **📍 Quest tracker** — your pinned (or newest) quests and their goals in a corner of the screen. **H** turns it on or off, a button in the quest book moves it to another corner.
- **🎁 Claim your rewards** — a finished quest waits with its rewards until you claim them: in the quest book, with the button in chat or with `/quest claim`. Quests can offer a **choice of rewards** — you pick one when claiming. Server owners who want the old way switch to instant rewards.
- **👥 Team quests** — a quest marked `"team": true` belongs to a whole team: anyone takes it, everyone's actions count, and every member gets the rewards — also members who were offline. Make a team with `/quest team`, or use your scoreboard team (`/team`).
- **🎒 Ready to play** — 25 built-in quests. Server owners can hide them with `/quest mainquests off`.
- **🎲 Generated quests** — a fresh board every 12 hours. Every quest is checked to be doable on your version and mod set, sized to a sensible amount of play time and paid with a fitting reward: gather, mine, craft, hunt, enchant, brew and more. Nether and End quests appear once players have been there. Three difficulty levels.
- **🧩 Mod support** — with **Farmer's Delight, Create, Mekanism, The Twilight Forest, Botania, Applied Energistics 2, Immersive Engineering, Biomes O' Plenty, Alex's Mobs, Tinkers' Construct** or **Ars Nouveau** installed, the board also offers quests for their content.
- **🏁 First come, first served** — on servers a generated quest someone accepted is taken. The quest book shows *Taken by Steve* or *Reserved for you*.
- **🌍 Five languages** — the quest book, every chat message, the built-in quests and the generated quests come in **English, German, French, Spanish and Japanese**. Every player reads their own game language.
- **✍️ Write your own quests** — in a datapack or in a per-world `custom-quests.json` that reloads by itself while you edit it. 15 objective types, 8 reward types, item tags and item filters (enchantments, potions, names).
- **🔐 Permissions** — LuckPerms & co. get a node for every command, and a quest can be limited to players with a permission.
- **🔗 Quest logic** — prerequisites and chains (or "any 12 of 16"), quests that start by themselves once they unlock, repeatable quests with cooldowns, "all" or "any" objectives and alternatives ("craft it **or** find it"), progress that resets on death, "10 different foods" goals, categories.
- **🛠️ For server owners** — completion broadcast, stats and leaderboard, admin commands, a self-test and a per-world `settings.json`.

---

## 🖥️ Server plugin (Spigot, Paper, Purpur)

The same quests on a plugin server — players join with a **plain vanilla client**, nothing to install.

- **Quest book as a chest menu** — new players get a quest book item; right-click it or type `/quest`. The tracker is a **boss bar**.
- **The same quests** — built-in quests, datapack quests, `custom-quests.json`, all objective and reward types, claiming, reward choices, team quests, permissions and the five languages.
- **A bigger generator for servers** — a board that grows with your player count, **personal daily quests** for every player, harder **weekly quests**, and a **weekly server goal** everyone works on together, with the same reward for every helper. Rewards with a choice of three, a lucky bonus, streak bonuses and money through your economy plugin.
- **Extras** — announcements with harmless fireworks, **quest givers** (`/quest npc set`: a villager or armor stand opens the quest book) and **PlaceholderAPI** placeholders for scoreboards and tab lists.
- **Moving from the mod** — a world played with the mod keeps its progress, custom quests and teams on the plugin server.

Pick the jar for your server version:

- `JustQuests-plugin-1.19` — Minecraft 1.19 – 1.20.6 (Java 17+)
- `JustQuests-plugin-1.21` — Minecraft 1.21 – 1.21.11 (Java 21+)
- `JustQuests-plugin-26.1` — Minecraft 26.1 – 26.3 (Java 25)

---

## 🧩 Objectives & rewards

**Objectives:**
- **collect_item** – pick up items (mining, harvesting, mob drops)
- **craft_item** – craft an item (crafting table or stonecutter)
- **smelt_item** – take items out of a furnace
- **consume_item** – eat or drink an item
- **enchant_item** – enchant an item at an enchanting table
- **use_item** – use an item (throw, place, apply…)
- **mine_block** – break blocks
- **place_block** – place blocks
- **kill_mob** – defeat mobs
- **tame_animal** – tame animals
- **breed_animal** – breed animals
- **reach_level** – reach an XP level
- **reach_location** – arrive at a position
- **visit_dimension** – enter a dimension (vanilla **or** modded)
- **gain_advancement** – earn an advancement

Items, blocks and mobs accept an id (`minecraft:oak_log`) or a **tag** (`#minecraft:logs`); items can also filter by enchantments, potion or name.

**Rewards:**
- **give_item** – hand over items
- **loot_table** – roll a loot table
- **xp** – experience points
- **effect** – a potion effect
- **message** – a chat message (per-language)
- **title** – a big on-screen title
- **command** – run any command for the player (`{player}` is replaced) — economy payouts and more, with **no** hard dependency
- **choice** – the player picks one of several rewards

---

## 🎮 Commands

- `/quest list [category]` – all quests, grouped by category
- `/quest accept <id>` / `/quest abandon <id>` – start or drop a quest
- `/quest accept all` – start every quest you can take now
- `/quest autoaccept on|off` – whether unlocked quests start by themselves (on by default)
- `/quest progress` – your active quests
- `/quest claim [id]` – take the rewards of finished quests
- `/quest team [create|invite|accept|leave|kick]` – your team for team quests
- `/quest stats` / `/quest leaderboard` – your stats / the server's top 10
- `/quest discord` – the community Discord invite

**Operators:**
- `/quest reload` – reload custom quests and settings
- `/quest mainquests on|off` – show or hide the built-in quests
- `/quest difficulty [easy|normal|hard]` – difficulty of generated quests
- `/quest reroll` – a new board of generated quests now
- `/quest generator status|stats|preview|explain|release` – inspect the generator, free a taken quest
- `/quest admin view|reset|complete <player>` – manage a player's progress
- `/quest test` – self-test with a diagnostics report

The plugin adds `/quest track`, `/quest bossbar`, `/quest book`, `/quest goal` and `/quest npc`.

---

## ✍️ Custom quests

On first start JustQuests writes `<world>/justquests/custom-quests.json` with an explained example and blank slots. Save the file and your quests appear in-game within seconds — no restart. A custom quest overrides a datapack quest with the same id.

Titles and descriptions can be a plain string or a per-language map:

```json
"title": { "en_us": "Mining Trip", "de_de": "Bergbau-Ausflug" }
```

A full [example datapack](https://github.com/ErikEdits/JustQuests/tree/main/docs/example-datapack) with one quest for every objective and reward type is on GitHub, and the [README](https://github.com/ErikEdits/JustQuests#readme) explains every field and setting.

---

## 📥 Installation

**Mod:**
1. Install **NeoForge**, **Fabric** (+ **Fabric API**) or **Forge** for your Minecraft version.
2. Download the jar for **your loader and exact version** — the file name says which, e.g. `JustQuests-fabric-1.21.1-0.4.0.jar` — into your `mods` folder.
3. Start the game and press **J**, or run `/quest list`.

Install the mod on the server **and** on the clients.

**Plugin:** put the plugin jar for your server version into `plugins/` and start the server. Players need nothing.

---

## ✅ Compatibility

- **NeoForge:** 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1.2, 26.2
- **Fabric** *(needs Fabric API)*: 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1 – 26.3
- **Forge:** 1.18.2, 1.19.2, 1.19.4, 1.20.1
- **Spigot / Paper / Purpur (plugin):** 1.19 – 1.21.11, 26.1 – 26.3
- **Quest book** on 1.20.1 and newer. 1.18 and 1.19 are command-only — quests, tracking and rewards work the same.
- Minecraft 26.x needs **Java 25**.

---

## 💬 Community

**[Join the JustQuests Discord](https://discord.gg/cMTGE9QCja)** for support, update news and sneak peeks — or run `/quest discord` in-game. Bug reports and ideas are very welcome on the [issue tracker](https://github.com/ErikEdits/JustQuests/issues) or on Discord.

---

## 📄 License

Released under the **GNU LGPL v3**. Free to use in any modpack, no permission needed. Modified versions must stay open source under the same license.

**Source & issues:** https://github.com/ErikEdits/JustQuests
