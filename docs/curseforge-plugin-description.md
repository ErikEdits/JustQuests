# JustQuests (server plugin)

**A lightweight quest system for Bukkit, Spigot, Paper, Purpur, Leaf, Leaves and Folia — players join with a plain vanilla client.**
Datapack-driven quests, a chest-menu quest book, a boss-bar tracker, a quest generator built for servers, team quests, badges and a weekly server goal. No client mod, no heavy dependencies.

> One jar per server type and range of versions (**1.19 – 1.20.6**, **1.21 – 1.21.11**, **26.1 – 26.3**). Also available as a mod for NeoForge, Fabric and Forge (JustQuests under Mods).

***

## ✨ Features

- **📖 Quest book as a chest menu** — new players get a quest book item; right-click it or type `/quest` (also `/quests`, `/jq`). Stats, quests by status and by category, and a page per quest with its goals, rewards and the Accept / Abandon / Claim button.
- **📍 Boss-bar tracker** — the pinned (or first active) quest and its progress; progress also shows above the hotbar.
- **🎁 Claim your rewards** — finished quests wait until the player claims them; a quest can offer a **choice of rewards**. Or switch to instant rewards in `config.yml`.
- **👥 Team quests** — a quest marked `"team": true` belongs to a whole team (`/quest team` or a scoreboard team): everyone's actions count and every member gets the rewards, also members who were offline.
- **🎒 Ready to play** — 25 built-in quests in **English, German, French, Spanish and Japanese**; every player reads their own game language, and item, block and mob names are translated by their game.
- **🏅 Badges** — for the first quest, 10 / 50 / 100 / 500 quests, a finished chapter, a week of daily quests and helping a server goal. The sign of a badge stands before the player's name; quest packs can bring their own badges.
- **✍️ Your own quests** — in `custom-quests.json`, in as many files as you like in `plugins/JustQuests/quests/` (any names, sub-folders too), or in a datapack. **27 objective types** — deliveries (`/quest submit`), fishing, trading, brewing, statistics, biomes, structures and more — 10 reward types (including money and badges), tags and item filters.

## 🎲 The server generator

- **A growing board** — generated quests, first come first served; the board grows with the players of the last 7 days.
- **Personal daily quests** — every player gets their own quests each day, sized to their own progress.
- **Weekly quests** — harder, longer quests with valuable loot, new every week.
- **Weekly server goal** — one big goal everyone works on together, for example "mine 1,000 copper ore". 25, 50, 75 and 100 % are announced, and every helper gets the same reward (also when offline).
- **Better rewards** — a choice of three, a lucky bonus, a streak bonus for personal quests, a reward multiplier, and money through your economy plugin's command (`eco give {player} {amount}`, no Vault needed).
- **Always doable** — every generated quest is checked against your server version; Nether and End quests appear once players have been there; three difficulty levels.

## 🧩 Extras

- **Announcements** for a new board, new weekly quests and a new server goal, with harmless fireworks when the goal is reached.
- **Quest givers** — look at a villager, any mob or an armor stand and type `/quest npc set [name]`: a right-click opens the quest book.
- **PlaceholderAPI** — `%justquests_completed%`, `%justquests_tracked%`, `%justquests_goal_percent%`, `%justquests_streak%` and more for scoreboards, tab lists and chat.
- **Permissions** — a node for every command (`justquests.command.<name>`, `justquests.admin.<name>`), and quests limited to players with a permission.
- **Moving from the mod** — a world played with the JustQuests mod keeps its progress, custom quests, teams and generated board when it moves to a plugin server.

***

## 🎮 Commands

- `/quest` – open the quest book
- `/quest list [category]`, `/quest progress`, `/quest accept <id>`, `/quest abandon <id>`
- `/quest accept all` – start every quest you can take now
- `/quest autoaccept on|off` – whether unlocked quests start by themselves (on by default)
- `/quest claim [id]` – take the rewards of finished quests
- `/quest track <id>` / `/quest bossbar` – choose or hide the boss bar
- `/quest book` – a new quest book item
- `/quest goal` – the server goal
- `/quest submit [id]` – hand in the items of delivery quests
- `/quest badges` – your badges
- `/quest team [create|invite|accept|leave|kick]` – your team for team quests
- `/quest stats` / `/quest leaderboard`

**Operators:** `/quest reload`, `/quest reroll`, `/quest difficulty`, `/quest generator …`, `/quest mainquests on|off`, `/quest npc set|remove`, `/quest admin view|reset|complete`, `/quest test`.

***

## 📥 Installation

1. Pick the jar for your server type and version: `JustQuests-plugin-<type>-<range>-<version>.jar`
   - **Type:** `bukkit`, `spigot`, `paper`, `purpur`, `leaf`, `leaves` or `folia`
   - **Range:** `1.19` (1.19 – 1.20.6, Java 17+), `1.21` (1.21 – 1.21.11, Java 21+), `26.1` (26.1 – 26.3, Java 25)
   - Example: Purpur 1.21.11 → `JustQuests-plugin-purpur-1.21-<version>.jar`. A jar on the wrong kind of server says in the console which one to take.
2. Put it into `plugins/` and start the server. Everything is set in `plugins/JustQuests/config.yml`.

Players need nothing — any vanilla client works.

***

## 🌐 Hosting

[![Free hosting on bot-hosting.net](https://cdn.jsdelivr.net/gh/ErikEdits/JustQuests@main/docs/assets/badges/bot-hosting_46h.png)](https://bot-hosting.net/?aff=erikedits_ae)

The JustQuests Discord bot (badges as roles) runs on bot-hosting.net, where you can host Discord bots for free. Want to run a bot of your own? Signing up through this button supports JustQuests.
*(Affiliate link — I may get a small commission.)*

***

## 💬 Community

[![Chat with us on Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/social/discord-plural_46h.png)](https://discord.gg/cMTGE9QCja)

Join the JustQuests Discord for support and update news. Bug reports and ideas: [issue tracker](https://github.com/ErikEdits/JustQuests/issues). The [wiki](https://github.com/ErikEdits/JustQuests/wiki/Server-Plugin) explains every option.

## 📄 License

Released under the **GNU LGPL v3**. Source: https://github.com/ErikEdits/JustQuests
