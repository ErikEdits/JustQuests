# JustQuests

**A lightweight, datapack-driven quest book for NeoForge.**
No GUI bloat, no heavy dependencies — just plain JSON quests and a handful
of commands. Built as a focused, server-friendly alternative to FTB Quests
and HQM for packs that want questing without the weight.

> **Version 0.1 is command-only.** A full in-game GUI is on the way (see
> the roadmap below). Everything here works right now on **NeoForge 1.21.1**.

---

## ✨ What JustQuests does

- **Datapack-driven** — define quests in plain JSON inside any datapack.
  No code, no extra tools. Reload them live with `/reload`.
- **Pack-maker friendly** — the quest format is tiny and readable; anyone
  can write a quest in under a minute.
- **Server-side friendly** — runs on dedicated servers and in singleplayer.
  Players don't need anything special for command-based play.
- **Per-player progress** — every player has their own progress, saved
  across logouts and kept after death.
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

## 📦 Bundled starter quests

JustQuests ships with a ready-to-play 10-quest progression so you can try
it instantly — or override it with your own datapack.

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

---

## 🛠️ Writing your own quests

Drop a JSON file into
`data/<namespace>/justquests/quests/<id>.json`:

```json
{
  "title": "First Steps",
  "description": "Gather some basic materials to get started.",
  "objectives": [
    {
      "type": "justquests:collect_item",
      "item": "minecraft:oak_log",
      "count": 16
    }
  ],
  "rewards": [
    {
      "type": "justquests:give_item",
      "item": "minecraft:bread",
      "count": 4
    }
  ]
}
```

Run `/reload` and it's live. The file name becomes the quest id
(`yourpack:first_steps`).

### ⚠️ Important design note
Objectives currently track items **picked up from the ground** — mining
blocks, harvesting crops, collecting mob drops. **Crafted items go straight
into the inventory and do not count.** Design your quests around *gathering*,
not crafting. (A dedicated `craft_item` objective is planned — see roadmap.)

---

## 📥 Installation

1. Install **NeoForge 1.21.1** (21.1.143 or newer)
2. Put `justquests-0.1.0.jar` into your `mods` folder
3. Launch the game and run `/quest list`

Works in singleplayer and on dedicated servers.

---

## ✅ Compatibility

- **Minecraft:** 1.21.1
- **Loader:** NeoForge only (more loaders planned)
- **Side:** works on dedicated servers and in singleplayer

---

## 🗺️ Roadmap

JustQuests is built step by step. Planned for upcoming versions:

- **v0.2** — in-game GUI, more objective types (kill mob, place block,
  reach location, craft item) and more reward types (XP, commands)
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
