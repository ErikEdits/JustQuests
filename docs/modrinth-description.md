# JustQuests

**A lightweight quest book for NeoForge, Fabric & Forge — and a server plugin for Bukkit, Spigot, Paper, Purpur, Leaf, Leaves & Folia.**
No GUI bloat, no heavy dependencies — datapack-driven quests, an in-game quest book, a rotating board of generated quests, team quests, badges and a handful of commands. A focused, server-friendly alternative to FTB Quests and HQM.

> **43 mod builds** for NeoForge, Fabric and Forge across **Minecraft 1.18.2 – 26.3**, plus a **server plugin** for **1.19 – 26.3** that works with plain vanilla clients. Singleplayer **and** servers.

---

## ✨ Features

- **📖 Quest book** — press **J** to browse quests, follow progress bars and accept, abandon or claim with a click. Grouped by category or status, a pixel icon for every quest, a search field, "Accept all" and a stats page.
- **📍 Quest tracker & popups** — your pinned quests in a corner of the screen (**H** turns it on or off), and a small popup when a quest is done or a new one unlocks. With a minimap mod they move out of its way.
- **🎨 Resource packs can restyle everything** — the book, buttons, progress bars, icons, the tracker, the popups, colours, the font and a sound for every moment.
- **🎁 Claim your rewards** — a finished quest waits with its rewards until you claim them, and a quest can offer a **choice of rewards**.
- **🏅 Badges** — players earn badges for their first quest, 10 / 50 / 100 / 500 quests and a finished chapter. Their sign stands before the player's name in chat and tab list, and quest packs can bring their own badges. Optional: link your Minecraft name to the JustQuests Discord and get your badges there as roles (opt-in).
- **👥 Team quests** — a quest marked `"team": true` belongs to a whole team: everyone's actions count and every member gets the rewards, also members who were offline.
- **🎒 Ready to play** — 25 built-in quests. Server owners can hide them with `/quest mainquests off`.
- **🎲 Generated quests** — a fresh board every 12 hours, sized to a sensible amount of play time and paid with a fitting reward: gather, mine, craft, hunt, fish, trade, brew, deliver, find biomes and structures and more. The board follows the server's progress (iron, diamonds, the Nether, the End). Three difficulty levels.
- **🍲 Farmer's Delight built in** — cutting board, cooking pot, skillet, stove and rich soil goals switch on by themselves when Farmer's Delight is installed.
- **🧩 More mod support** — with **Create, Mekanism, The Twilight Forest, Botania, Applied Energistics 2, Immersive Engineering, Biomes O' Plenty, Alex's Mobs, Tinkers' Construct** or **Ars Nouveau** installed, the board also offers quests for their content. The optional **JustQuests Create Addon** counts what Create's machines really make.
- **🌍 Five languages** — English, German, French, Spanish and Japanese. Every player reads their own game language.
- **✍️ Write your own quests** — in a datapack, in a per-world `custom-quests.json`, or in as many files as you like in the `quests/` folder next to it (any names, sub-folders too). They reload by themselves while you edit them. **27 objective types** (30 with Farmer's Delight), 9 reward types, tags and item filters.
- **🔗 Quest logic** — prerequisites and chains (or "any 12 of 16"), quests that start by themselves once they unlock, repeatable quests with cooldowns, alternatives ("craft it **or** find it"), progress that resets on death, "10 different foods" goals, categories.
- **🔐 Permissions** — LuckPerms & co. get a node for every command, and a quest can be limited to players with a permission.
- **🛠️ For server owners** — completion broadcast, stats and leaderboard, admin commands, a self-test, a per-world `settings.json`, and data files that are never lost: a damaged file is kept as a copy instead of being overwritten.

---

## 🖥️ Server plugin

The same quests on a plugin server — players join with a **plain vanilla client**, nothing to install.

- **Quest book as a chest menu** — new players get a quest book item; right-click it or type `/quest`. The tracker is a **boss bar**.
- **The same quests** — built-in, datapack and custom quests, all objective types of the game itself, rewards, team quests, badges, permissions and the five languages.
- **A bigger generator for servers** — a board that grows with your player count, **personal daily quests** with streaks, **weekly quests** and a **weekly server goal** everyone works on together. Money rewards through your economy plugin.
- **Extras** — announcements with harmless fireworks, **quest givers** (a villager or armor stand opens the quest book) and **PlaceholderAPI** placeholders.

**Pick the jar for your server type and version:** `JustQuests-plugin-<type>-<range>-<version>.jar`

- **Type:** `bukkit`, `spigot`, `paper`, `purpur`, `leaf`, `leaves` or `folia`
- **Range:** `1.19` (1.19 – 1.20.6, Java 17+), `1.21` (1.21 – 1.21.11, Java 21+), `26.1` (26.1 – 26.3, Java 25)

Example: a Purpur 1.21.11 server takes `JustQuests-plugin-purpur-1.21-<version>.jar`. A jar on the wrong kind of server says in the console which one to take.

---

## 🧩 Objectives & rewards

**Objectives:**
- **collect_item** – pick up items · **have_item** – have items in the inventory, wherever they came from
- **craft_item** · **smelt_item** · **consume_item** · **use_item**
- **enchant_item** · **anvil_item** · **grindstone_item**
- **mine_block** · **place_block** · **use_block** – right-click a block
- **kill_mob** · **tame_animal** · **breed_animal** · **interact_entity** – right-click a mob (milk, shear…)
- **submit_item** – hand items in (they are taken from the inventory)
- **fish_item** · **trade_villager** · **brew_potion**
- **stat** – raise any statistic: blocks walked, jumps, play time…
- **find_structure** · **find_biome** · **reach_location** · **visit_dimension** (vanilla **or** modded)
- **reach_level** · **gain_advancement**
- With Farmer's Delight: **cut_item** · **cook_item** · **rich_soil**

Items, blocks, mobs, biomes and structures accept an id or a **tag** (`#minecraft:logs`); items can also filter by enchantments, potion or name.

**Rewards:** give_item · loot_table · xp · effect · message · title · command (`{player}` is replaced) · choice (the player picks one) · badge

---

## 🎮 Commands

- `/quest list [category]` · `/quest progress` · `/quest stats` · `/quest leaderboard`
- `/quest accept <id>` / `/quest accept all` / `/quest abandon <id>`
- `/quest claim [id]` – take the rewards of finished quests
- `/quest submit [id]` – hand in the items of delivery quests
- `/quest team [create|invite|accept|leave|kick]` – your team for team quests
- `/quest badges` – your badges; `/quest discord` – the community Discord

**Operators:** `/quest reload` · `/quest mainquests on|off` · `/quest difficulty` · `/quest reroll` · `/quest generator …` · `/quest admin view|reset|complete|badge` · `/quest test`

---

## ✍️ Custom quests

On first start JustQuests writes `<world>/justquests/custom-quests.json` with an explained example. Save it, or put more files into the `quests/` folder next to it, and your quests appear in-game within seconds — no restart.

```json
"title": { "en_us": "Mining Trip", "de_de": "Bergbau-Ausflug" }
```

A full [example datapack](https://github.com/ErikEdits/JustQuests/tree/main/docs/example-datapack) with examples for the objective and reward types is on GitHub, and the [wiki](https://github.com/ErikEdits/JustQuests/wiki) explains every field and setting.

---

## 📥 Installation

**Mod:**
1. Install **NeoForge**, **Fabric** (+ **Fabric API**) or **Forge** for your Minecraft version.
2. Download the jar for **your loader and exact version** — the file name says which, e.g. `JustQuests-fabric-1.21.1-<version>.jar` — into your `mods` folder.
3. Start the game and press **J**, or run `/quest list`.

Install the mod on the server **and** on the clients.

**Plugin:** put the jar for your server type and version into `plugins/` and start the server. Players need nothing.

---

## ✅ Compatibility

- **NeoForge:** 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1.2, 26.2
- **Fabric** *(needs Fabric API)*: 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21 – 1.21.11, 26.1 – 26.3
- **Forge:** 1.18.2, 1.19.2, 1.19.4, 1.20.1
- **Plugin:** Bukkit, Spigot, Paper, Purpur, Leaf, Leaves and Folia, 1.19 – 26.3
- **Quest book** on 1.20.1 and newer. 1.18 and 1.19 are command-only — quests, tracking and rewards work the same.
- Minecraft 26.x needs **Java 25**.

---

## 🌐 Hosting

The JustQuests Discord bot (badges as roles) runs on **[bot-hosting.net](https://bot-hosting.net/?aff=erikedits_ae)**, where you can host Discord bots for free. Want to run a bot of your own? Signing up through this link supports JustQuests.
*(Affiliate link — I may get a small commission.)*

---

## 💬 Community

**[Join the JustQuests Discord](https://discord.gg/cMTGE9QCja)** for support, update news and sneak peeks — or run `/quest discord` in-game. Bug reports and ideas are very welcome on the [issue tracker](https://github.com/ErikEdits/JustQuests/issues) or on Discord.

---

## 📄 License

Released under the **GNU LGPL v3**. Free to use in any modpack, no permission needed. Modified versions must stay open source under the same license.

**Source & issues:** https://github.com/ErikEdits/JustQuests
