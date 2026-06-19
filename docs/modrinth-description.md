# JustQuests

**A lightweight quest book for NeoForge.**
No GUI bloat, no heavy dependencies — just quests and a handful of commands.
Built as a focused, server-friendly alternative to FTB Quests and HQM for
packs that want questing without the weight.

> **Version 0.1 is command-only.** A full in-game GUI is on the way (see
> the roadmap below). Everything here works right now on **NeoForge 1.21.1**.

---

## ✨ What JustQuests does

- **Ready to play** — comes with a built-in quest progression you can start
  right away, no setup required.
- **Lightweight** — tiny and fast, with no heavy dependencies.
- **Persistent progress** — your progress is saved across sessions and kept
  even after death.
- **No conflicts** — being command-based, it stays out of the way of other
  mods and UI overhauls.

---

## 🎮 Commands

| Command | What it does |
|---------|--------------|
| `/quest list` | List all quests with their description, goal and reward |
| `/quest accept <id>` | Start a quest (with tab completion) |
| `/quest progress` | Show your active quests and how far along you are |
| `/quest abandon <id>` | Drop an active quest (suggests your active ones) |
| `/quest reload` | *(OP)* reload quest definitions |

Quest ids support **tab completion** — `accept` suggests every available
quest, `abandon` suggests only the ones you've taken.

---

## 📦 Built-in quests

JustQuests ships with a ready-to-play starter progression so you can jump
in right away.

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
| Lumberjack | collect 32 logs (any kind) | iron axe |

> **Good to know:** objectives track items **picked up from the ground** —
> mining blocks, harvesting crops, collecting mob drops. Crafted items go
> straight into the inventory and do not count toward a goal.

---

## 📥 Installation

1. Install **NeoForge 1.21.1** (21.1.143 or newer)
2. Put `justquests-0.1.0.jar` into your `mods` folder
3. Launch the game and run `/quest list`

---

## ✅ Compatibility

- **Minecraft:** 1.21.1
- **Loader:** NeoForge only (more loaders planned)
- **Environment:** singleplayer

---

## 🗺️ Roadmap

JustQuests is built step by step. Planned for upcoming versions:

- **v0.2** — in-game GUI, more objective types (kill mob, place block,
  reach location, craft item) and more reward types (XP, commands)
- **Custom quest creation** — make and add your own quests
- **v0.3** — quest chains and prerequisites
- **Later** — more Minecraft versions, additional loaders (Fabric/Forge),
  a Paper/Bukkit plugin edition, per-player language display, and an
  optional automatic quest generator

Feedback shapes the priorities — bug reports and suggestions are very
welcome on the [issue tracker](https://github.com/ErikEdits/JustQuests/issues).

---

## 📄 License

Released under the **GNU LGPL v3**. Free to use in any modpack, no
permission needed. Modified versions and forks must stay open source
under the same license.

**Source & issues:** https://github.com/ErikEdits/JustQuests
