# JustQuests Create Addon

**Quest goals for Create's machines in [JustQuests](https://modrinth.com/mod/justquests).** "Press 16 Iron Sheets", "Mix 8 Andesite Alloy", "Assemble a Precision Mechanism": the goal only counts what that machine really makes, and the quest book says so in plain words.

> **For now:** NeoForge 1.21.1 with Create 6.0.10. Needs **JustQuests 0.4.6 or newer**. Install it on the **client and the server**.

Without this addon, a quest about Create can only look at what a player holds in the inventory (`have_item`). With it, the quest knows that the sheet really came out of a press. The main mod stays small for everyone who doesn't play with Create.

---

## ⚙️ New objectives

| Objective | Counts what comes out of | The quest book shows |
|---|---|---|
| `justquests_create:press_item` | a Mechanical Press on a belt, a depot or the ground | Press Iron Sheet |
| `justquests_create:compact_item` | a Mechanical Press over a basin | Compact Blaze Cake Base |
| `justquests_create:mix_item` | a Mechanical Mixer over a basin | Mix Andesite Alloy |
| `justquests_create:mill_item` | a Millstone | Mill Wheat Flour |
| `justquests_create:crush_item` | Crushing Wheels | Crush Crushed Iron Ore |
| `justquests_create:saw_item` | a Mechanical Saw | Saw Stripped Oak Log |
| `justquests_create:deploy_item` | a Deployer on a belt or a depot | Make Andesite Casing with a Deployer |
| `justquests_create:fill_item` | a Spout | Make Water Bucket with a Spout |
| `justquests_create:mechanical_craft_item` | Mechanical Crafters | Craft Crushing Wheel with Mechanical Crafters |
| `justquests_create:wash_item` | a fan through water | Wash out Iron Nugget |
| `justquests_create:smoke_item` | a fan through fire | Smoke Cooked Beef with a Fan |
| `justquests_create:blast_item` | a fan through lava | Blast Iron Ingot with a Fan |
| `justquests_create:haunt_item` | a fan through soul fire | Haunt Soul Sand with a Fan |
| `justquests_create:assemble_item` | the last step of a Sequenced Assembly | Assemble Precision Mechanism |

The item is always what comes **out** of the machine.

---

## ✍️ Writing a quest

They work like every other JustQuests objective, in a datapack or in `custom-quests.json`:

```json
{
  "title": { "en_us": "Press Iron Sheets", "de_de": "Eisenbleche pressen" },
  "category": "create",
  "objectives": [
    { "type": "justquests_create:press_item", "item": "create:iron_sheet", "count": 16 }
  ],
  "rewards": [
    { "type": "justquests:xp", "amount": 50 }
  ]
}
```

- `item` takes an id or a tag (`#c:plates`), like everywhere in JustQuests.
- `count` is how many have to come out of the machine.
- `"distinct": true` counts every kind of item only once.
- `label` and `group` work too, for your own goal text and for "this **or** that".

A full [example datapack](https://github.com/ErikEdits/JustQuests/tree/main/addons/create/example-datapack) with one quest for every objective is on GitHub.

---

## 👤 Who gets the progress?

Create's machines run without a player, so someone has to get the credit:

- **The player who placed the machine**, wherever they are on the server. Your factory counts while you're at the other end of the world.
- **A machine without a known owner** (placed before the addon was installed, or built by a schematic) counts for the **nearest player within 32 blocks**.
- An owner who is **offline** gets nothing, and nobody else gets it in their place.

For crushing wheels, the owner of a wheel counts. For a press or mixer over a basin, the owner of the press or mixer counts, otherwise the owner of the basin. Team quests work as usual.

---

## 📥 Installation

1. Install [Create](https://modrinth.com/mod/create) 6.0.10 and [JustQuests](https://modrinth.com/mod/justquests) 0.4.6 or newer.
2. Put this addon into the `mods` folder, on the client **and** the server.
3. Write quests with the new objectives.

---

## ⚠️ Good to know

- Only items count, not fluids a mixer makes.
- A saw cutting down a tree and a deployer working on a block in the world don't count. A saw or deployer working on an item does.
- Applying an item to a block by hand doesn't count: that isn't a machine.
- Quests with these objectives are skipped with a message in the log if the addon is missing on the server. A client without the addon can still join, but those quests stay off its quest book.

---

## 💬 Community

**[Join the JustQuests Discord](https://discord.gg/cMTGE9QCja)** for support and news. Bug reports and ideas are very welcome on the [issue tracker](https://github.com/ErikEdits/JustQuests/issues).

**Source:** https://github.com/ErikEdits/JustQuests/tree/main/addons/create

## 📄 License

LGPL-3.0-only. This addon is not made by or affiliated with the Create team.
