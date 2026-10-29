# JustQuests (server plugin)

**A lightweight quest system for Spigot, Paper and Purpur — players join with a plain vanilla client.**
Datapack-driven quests, a chest-menu quest book, a boss-bar tracker, a quest generator built for servers, team quests and a weekly server goal. No client mod, no heavy dependencies.

> One jar per range of versions: **1.19 – 1.20.6**, **1.21 – 1.21.11** and **26.1 – 26.3**. Also available as a mod for NeoForge, Fabric and Forge (JustQuests under Mods).

---

## ✨ Features

- **📖 Quest book as a chest menu** — new players get a quest book item; right-click it or type `/quest` (also `/quests`, `/jq`). Stats, quests by status and by category, and a page per quest with its goals, rewards and the Accept / Abandon / Claim button.
- **📍 Boss-bar tracker** — the pinned (or first active) quest and its progress; progress also shows above the hotbar.
- **🎁 Claim your rewards** — finished quests wait until the player claims them; a quest can offer a **choice of rewards**. Or switch to instant rewards in `config.yml`.
- **👥 Team quests** — a quest marked `"team": true` belongs to a whole team (`/quest team` or a scoreboard team): everyone's actions count and every member gets the rewards, also members who were offline.
- **🎒 Ready to play** — 25 built-in quests in **English, German, French, Spanish and Japanese**; every player reads their own game language, and item, block and mob names are translated by their game.
- **✍️ Your own quests** — in `custom-quests.json`, one file per quest in `plugins/JustQuests/quests/`, or in a datapack. 15 objective types, 9 reward types (including money), tags and item filters.

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

---

## 🎮 Commands

- `/quest` – open the quest book
- `/quest list [category]`, `/quest progress`, `/quest accept <id>`, `/quest abandon <id>`
- `/quest accept all` – start every quest you can take now
- `/quest autoaccept on|off` – whether unlocked quests start by themselves (on by default)
- `/quest claim [id]` – take the rewards of finished quests
- `/quest track <id>` / `/quest bossbar` – choose or hide the boss bar
- `/quest book` – a new quest book item
- `/quest goal` – the server goal
- `/quest team [create|invite|accept|leave|kick]` – your team for team quests
- `/quest stats` / `/quest leaderboard`

**Operators:** `/quest reload`, `/quest reroll`, `/quest difficulty`, `/quest generator …`, `/quest mainquests on|off`, `/quest npc set|remove`, `/quest admin view|reset|complete`, `/quest test`.

---

## 📥 Installation

1. Pick the jar for your server version:
   - `JustQuests-plugin-1.19` — Minecraft 1.19 – 1.20.6 (Java 17+)
   - `JustQuests-plugin-1.21` — Minecraft 1.21 – 1.21.11 (Java 21+)
   - `JustQuests-plugin-26.1` — Minecraft 26.1 – 26.3 (Java 25)
2. Put it into `plugins/` and start the server. Everything is set in `plugins/JustQuests/config.yml`.

Players need nothing — any vanilla client works.

---

## 💬 Community

**[Join the JustQuests Discord](https://discord.gg/cMTGE9QCja)** for support and update news. Bug reports and ideas: [issue tracker](https://github.com/ErikEdits/JustQuests/issues). The [README](https://github.com/ErikEdits/JustQuests#server-plugin-spigot-paper-purpur) explains every option.

## 📄 License

Released under the **GNU LGPL v3**. Source: https://github.com/ErikEdits/JustQuests
