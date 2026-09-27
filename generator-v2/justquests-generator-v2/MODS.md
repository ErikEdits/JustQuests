# MODS — the five supported mods

Generator v2 ships data profiles for **Farmer's Delight**, **Create**, **Mekanism**, **The Twilight
Forest** and **Botania**. A profile only references facts about a mod: item/block/entity/dimension
**ids**, their English names, and which items have a crafting-grid or furnace recipe. No code,
textures, models, recipes or other assets of any mod are bundled, copied or depended on. The profiles
activate only when the mod is installed; every id is existence-checked at runtime.

Research note: Modrinth (`modrinth.com`, `api.modrinth.com`) and CurseForge were blocked by this
environment's network policy. All facts below come from the mods' public **GitHub source
repositories** (license files, `gradle.properties`, generated recipe/loot/worldgen data and `en_us`
language files of every release branch), read in September 2026. Download figures are approximate
and from general knowledge; everything that could not be verified is marked as such.

Contents

1. [Selection](#1-selection)
2. [Farmer's Delight](#2-farmers-delight)
3. [Create](#3-create)
4. [Mekanism](#4-mekanism)
5. [The Twilight Forest](#5-the-twilight-forest)
6. [Botania](#6-botania)
7. [How the ids were verified](#7-how-the-ids-were-verified)

---

## 1. Selection

Criteria from the specification (§14.1) plus the maintainer's explicit request: **open source with a
licence that causes no problems**. Scoring 0–3 per criterion:

| Mod | Popularity & health | Loader/version coverage | Quest-friendliness (our hooks) | Stable ids | License (code) | Verdict |
|---|---|---|---|---|---|---|
| **Farmer's Delight** (+ Refabricated) | 3 — one of the most downloaded content mods, active | 3 — Forge, NeoForge, Fabric (Refabricated, same namespace) | 3 — crops to collect, wild plants to mine, crafting-table food, smelting, many foods to eat | 3 | **MIT** | **chosen** |
| **Create** (+ Create Fabric) | 3 — top technology mod, active | 2 — Forge 1.18.2–1.20.1, NeoForge 1.21.1, Fabric 1.18.2–1.20.1 (same namespace) | 2 — ores, stone layers, many crafting-table parts; machine outputs are not countable | 3 | **MIT** (assets ARR — we use no assets) | **chosen** |
| Biomes O' Plenty | 3 | 3 | 2 — mostly blocks/wood | 3 | All Rights Reserved | rejected (licence) |
| Supplementaries | 3 | 3 | 2 | 2 | custom "Supplementaries Team License", All Rights Reserved | rejected (licence) |
| Friends & Foes | 2 | 3 | 2 | 3 | CC BY-NC-ND 4.0 | rejected (licence) |
| Alex's Mobs | 3 | 1 — Forge only, ends at 1.20.1 | 3 | 3 | no licence file found | rejected (coverage, licence unclear) |
| **The Twilight Forest** | 3 | 1 — Forge/NeoForge only | 3 — own dimension, mobs, bosses, drops | 3 | **LGPL-2.1** | **chosen in the extension** (see below) |
| The Aether | 2 | 1 — Forge/NeoForge only | 2 | 2 | LGPL-3.0 | not added (overlaps the Twilight Forest's role) |
| **Mekanism** | 3 | 1 — Forge/NeoForge only | 2 — five ores, raw metals, first machines | 3 | **MIT** | **chosen in the extension** |
| **Botania** | 3 | 3 — Forge, NeoForge and Fabric (same namespace) | 2 — flowers, petals, first tools | 3 | **Botania License** (open; attribution) | **chosen in the extension** |
| Tinkers' Construct | 3 | 1 — Forge 1.18.2–1.20.1 only | 1 — tools are built in the part builder, not a grid | 3 | MIT | not added (few countable targets) |
| Storage Drawers | 3 | 3 | 1 — only crafting | 3 | MIT | not added (little quest value) |
| Naturalist | 2 | 2 | 2 — animals | 2 | split licence (resources restricted) | rejected |
| Oh The Biomes We've Gone | 2 | 2 | 1 — namespace changed `byg` → `biomeswevegone` | 1 | LGPL (code) | rejected (id churn) |

Farmer's Delight and Create complement each other: one is farming/cooking (crops, foods, kitchen
tools), the other mining/engineering (zinc, stone layers, kinetic parts), so the family rule keeps
sets varied and the "one quest per active mod" rule has plenty of choice.

**Extension (September 2026).** The maintainer asked for more content. The first selection had
demanded Fabric coverage, but that turned out to be unnecessary: a profile whose mod is missing is
simply inactive, so a Forge/NeoForge-only mod costs nothing on the Fabric builds. With that criterion
relaxed, three more open-source mods were added, each filling a gap:

- **Mekanism** (MIT): a second technology mod with its own ores (osmium, tin, lead, uranium, fluorite),
  which is what mining quests need.
- **The Twilight Forest** (LGPL-2.1): an adventure dimension with its own mobs, bosses and forage.
  It unlocks per world like the Nether.
- **Botania** (Botania License): magic and flowers, with an early game that is almost entirely
  gathering and grid crafting. It runs on all three loaders.

With five profiles, "one quest per active mod" could fill a small set with modded content only. The
guarantee is therefore capped at half of the set (`setRules.maxPerModShare`), and the subset of mods
it covers rotates from cycle to cycle (`DESIGN.md`, assumption 21).

Licence check (files read from the repositories):

- `vectorwing/FarmersDelight` — `LICENSE`: MIT License, © 2020 vectorwing.
- `MehVahdJukaar/FarmersDelightRefabricated` — `LICENSE`: MIT License (same text).
- `Creators-of-Create/Create` — `LICENSE.md`: code MIT, `src/main/resources/assets/` All Rights
  Reserved. Generator v2 uses neither code nor assets — only ids, which are facts.
- `mekanism/Mekanism` — `LICENSE`: MIT License, © 2017-2025 Aidan C. Brady.
- `TeamTwilight/twilightforest` — `LICENSE`: GNU LGPL 2.1 or later, © 2012-2017 Ben Mazur /
  Benimatic and contributors.
- `VazkiiMods/Botania` — `LICENSE.txt`: the Botania License. You may use, share and adapt it, with
  attribution to Vazkii as the creator of Botania, and without charging for distributions of the mod.
  Generator v2 distributes no part of Botania; the profile names Vazkii in this file.
- JustQuests itself is LGPL-3.0-only. Referencing the ids of these mods creates no licensing
  obligation either way.

---

## 2. Farmer's Delight

| | |
|---|---|
| Name | Farmer's Delight (Forge/NeoForge) · Farmer's Delight Refabricated (Fabric) |
| Modrinth | https://modrinth.com/mod/farmers-delight · https://modrinth.com/mod/farmers-delight-refabricated |
| Source | https://github.com/vectorwing/FarmersDelight · https://github.com/MehVahdJukaar/FarmersDelightRefabricated |
| Mod id (all loaders) | `farmersdelight` (Refabricated keeps the id and namespace) |
| Namespace | `farmersdelight` |
| Profile | `catalog/profiles/farmersdelight.json` — `requiresMod: ["farmersdelight"]`, `minecraft.min: 1.18.2` |
| Studied | FD 1.2.3 (1.18.2), 1.2.4 (1.19.2), 1.3.4 (1.20.1, main data source), 1.2.4-beta.3 (NeoForge 1.20.4), 1.3.4 (NeoForge 1.21.1), 1.3.0 (26.1); Refabricated 2.5.7 (1.20.1) … 3.6.16 (1.21.11) |

### 2.1 Coverage of our 34 builds

✓ = a release branch exists for exactly this version · ~ = a neighbouring version's build (usually
works, unverified) · ✗ = none found.

| Loader | Build | FD | Source |
|---|---|---|---|
| Forge | 1.18.2 | ✓ | FD 1.2.3 |
| Forge | 1.19.2 | ✓ | FD 1.2.4 |
| Forge | 1.19.4 | ✗ | — |
| Forge | 1.20.1 | ✓ | FD 1.3.4 |
| NeoForge | 1.20.4 | ✓ | FD 1.2.4-beta.3 |
| NeoForge | 1.20.6 | ✗ | — |
| NeoForge | 1.21 | ~ | FD for 1.21.1 declares `[1.21.1]` only |
| NeoForge | 1.21.1 | ✓ | FD 1.3.4 |
| NeoForge | 1.21.2 – 1.21.10 | ✗ | FD skipped to 26.1 |
| Fabric | 1.18.2, 1.19.2 | ? | an older community Fabric port existed; not verifiable here — if it uses the `farmersdelight` id, the profile activates automatically |
| Fabric | 1.19.4, 1.20.4, 1.20.6, 1.21 | ✗ | — |
| Fabric | 1.20.1 | ✓ | Refabricated 2.5.7 (LTS) |
| Fabric | 1.21.1 | ✓ | Refabricated 3.4.0 |
| Fabric | 1.21.2, 1.21.3 | ✗ | — |
| Fabric | 1.21.4 | ✓ | Refabricated 3.2.5 |
| Fabric | 1.21.5 | ✓ | Refabricated 3.2.5 |
| Fabric | 1.21.6, 1.21.7 | ~ | Refabricated 1.21.8 build |
| Fabric | 1.21.8 | ✓ | Refabricated 3.3.3 |
| Fabric | 1.21.9 | ~ | Refabricated 1.21.10 build |
| Fabric | 1.21.10 | ✓ | Refabricated 3.4.1 |

Verified: 10 builds (+ 4 likely compatible). On all other builds the profile is simply inactive.

### 2.2 Id differences

All 102 ids used by the profile (targets and rewards) exist in every studied version (checked
against the `en_us` files of 1.18.2, 1.19, 1.20, 1.20.4, 1.21 and 26.1) **except**
`farmersdelight:onion_soup`, `wooden_basket` and `bamboo_basket`, which appear in the 1.20.1 (1.3.x)
and 1.21+ branches only. No alternative ids were needed; the runtime existence check drops them on
older versions.

### 2.3 Included content and why it works with our hooks

| Entries (family) | Objective types | Why it is countable on all loaders |
|---|---|---|
| Cabbage, tomato, onion, rice (`fd_crops`) | `collect_item` crops, `craft_item` crates/bale, `consume_item` | Harvested crops pop as item entities (pickup hook); crates are crafting-grid recipes (9 crops) |
| Wild cabbages/beetroots/tomatoes/onions/carrots/potatoes/rice (`fd_wild`) | `mine_block` | Naturally generated plants (biome modifiers: beaches, warm dry biomes, most biomes, wet biomes); breaking counts once |
| Straw, rope, canvas, straw bale (`fd_straw`) | `collect_item`, `craft_item`, `place_block` | Straw drops as an item when grass/wheat is cut with a knife (loot modifier) |
| Knives (`fd_tools`), cutting board, cooking pot, skillet, stove (`fd_kitchen`), cabinets (`fd_furniture`) | `craft_item`, `place_block` | Shaped crafting-table recipes |
| Ham, bacon, patties, cuts (`fd_butchery`), fish slices (`fd_seafood`), fried egg (`fd_breakfast`) | `collect_item` ham, `smelt_item` outputs | Ham drops from pigs/hoglins killed with a knife; the cooked cuts have furnace/smoker recipes (smoked ham: smoker only — the smoker output slot counts) |
| Sandwiches, burgers, wraps (`fd_meals`), salads (`fd_salads`), cookies, pies, popsicle (`fd_sweets`), melon juice, milk bottle (`fd_drinks`) | `craft_item`, `consume_item` | Shapeless crafting-table recipes; foods finish a use action |
| Cooking-pot meals and drinks: stews, soups, fried rice, pasta, ratatouille, cider, hot cocoa, custard (`fd_stews`, `fd_plates`, `fd_drinks`) | `consume_item` only | Eating counts on every loader, however the food was made |
| Feast blocks (`fd_feasts`, Hard only) | `craft_item` | Shapeless crafting-table recipes (roast chicken, shepherd's pie, honey glazed ham, rice roll medley) |
| Wheat dough, pie crust, bread from dough (`baking`) | `craft_item`, `smelt_item minecraft:bread` | Dough is a crafting recipe; baking dough into vanilla bread exists only with FD, hence in the profile |
| Organic compost (`fd_soil`) | `craft_item` | Shapeless crafting recipe |

Totals: 27 entries, 121 targets (all usable when every id exists), 12 themes (Farmer's Market,
Harvest Festival (vanilla + FD crops), Chef's Special, Butcher's Order (vanilla meat + FD), Kitchen
Setup, Wild Forager, Village Bakery (FD + vanilla baking), Breakfast Club, Picnic Basket, Rope Maker,
Crate Stock, Fishmonger), 20 reward items.

### 2.4 Excluded content and why

- **Cooking-pot outputs as `craft_item`** — the cooking pot's output slot is not a crafting grid;
  these meals are `consume_item` only.
- **Cutting-board results** (slices, cuts, bark, straw from the board) as `craft_item` — the board is
  a block interaction, not a crafting grid. (They do pop as item entities, but we do not rely on
  that.)
- **Rich soil** — produced over time from organic compost, not crafted or dropped reliably.
- **Mushroom colonies** — mostly grown by players from mushrooms on rich soil; natural generation is
  rare (mushroom fields).
- **Canvas signs** — 1.20+ only and pure decoration; kept out to keep the pool focused.
- **Dog food / horse feed** — fed to animals, not eaten by players.
- **Knife-only drops other than straw and ham** (leather, feathers via "scavenging") — duplicates of
  vanilla drops.
- **Crates of vanilla crops** (carrot, potato, beetroot crates) — their recipes are conditional on a
  config option (recipe condition `farmersdelight:vanilla_crates_enabled`) and may be missing; the FD
  crop crates stay.

### 2.5 Progression

Almost everything is tier 0 (surface farming). Tier 1: the cooking pot, skillet, stove (iron) and
cooking-pot meals (need a pot). Tier 2: diamond knife (Hard). Tier 3: nether salad (fungi). No
dimensions, no advancements needed.

### 2.6 Limitations

- Coverage gaps on NeoForge 1.20.6/1.21.2–1.21.10 and several Fabric versions (§2.1).
- The Fabric edition's behaviour of straw/ham loot modifiers was not tested in game; both are
  standard loot modifiers in the Refabricated source.
- Effort values for cooking-pot meals include building the pot's heat source; players without a pot
  will find these quests long (they are tier 1 and weighted like other consume quests).

---

## 3. Create

| | |
|---|---|
| Name | Create (Forge/NeoForge) · Create Fabric |
| Modrinth | https://modrinth.com/mod/create · https://modrinth.com/mod/create-fabric |
| Source | https://github.com/Creators-of-Create/Create · https://github.com/Fabricators-of-Create/create |
| Mod id (all loaders) | `create` |
| Namespace | `create` |
| Profile | `catalog/profiles/create.json` — `requiresMod: ["create"]`, `minecraft.min: 1.18.2` |
| Studied | Create 0.5.1.a (Forge 1.18.2, 1.19.2), 6.0.8 (Forge 1.20.1, main data source), 6.0.11 (NeoForge 1.21.1); Create Fabric 0.5.1-i (1.18.2, 1.19.2), 6.0.8.1 (1.20.1), 6.0.0.0 dev branch (1.21.1) |

### 3.1 Coverage of our 34 builds

| Loader | Build | Create | Source |
|---|---|---|---|
| Forge | 1.18.2 | ✓ | 0.5.1.a |
| Forge | 1.19.2 | ✓ | 0.5.1.a |
| Forge | 1.19.4 | ✗ | — |
| Forge | 1.20.1 | ✓ | 6.0.8 |
| NeoForge | 1.20.4, 1.20.6, 1.21 | ✗ | — |
| NeoForge | 1.21.1 | ✓ | 6.0.11 |
| NeoForge | 1.21.2 – 1.21.10 | ✗ | — |
| Fabric | 1.18.2 | ✓ | Create Fabric 0.5.1-i |
| Fabric | 1.19.2 | ✓ | Create Fabric 0.5.1-i |
| Fabric | 1.20.1 | ✓ | Create Fabric 6.0.8.1 |
| Fabric | 1.21.1 | ? | development branch only (release not verified) |
| Fabric | others | ✗ | — |

Verified: 7 builds (+ 1 in development). Create concentrates on long-lived versions; on the others
the profile is inactive.

### 3.2 Id differences

All 67 ids used by the profile (targets and rewards) exist in every studied version (checked against the generated
`en_us` files of Create 0.5.1 for 1.18.2 and 1.19.2, Create 6 for 1.20.1 and 1.21.1, and Create
Fabric for 1.18.2, 1.20.1 and 1.21.1). No alternative ids were needed. The stone-layer blocks
(asurine, crimsite, ochrum, veridium, limestone, scoria, scorchia) and zinc ore exist since 0.5.0.

### 3.3 Included content and why it works with our hooks

| Entries (family) | Objective types | Why it is countable on all loaders |
|---|---|---|
| Zinc (`create_zinc`) | `mine_block` zinc ore/deepslate zinc ore, `collect_item` raw zinc, `smelt_item` zinc ingot (tag concept `zinc_ingots` where supported), `craft_item` zinc block (Hard) | Ore blocks generate y −63..70 (needs an iron pickaxe: `needs_iron_tool`); raw zinc is the ore's drop; furnace/blast-furnace recipe |
| Stone layers (`create_stone`) | `mine_block` asurine, crimsite, ochrum, veridium, limestone, scoria (Overworld), scorchia (Nether) | Generated by the `striated_ores_*` features (rare thick layers) |
| Cut stone (`create_masonry`) | `place_block` only | Made on a stonecutter (not countable as crafting) but placing counts |
| Andesite alloy, casings, basin (`create_alloy`) | `craft_item`, `place_block` | Andesite + iron/zinc nuggets in a 2×2 grid; casings are made by applying alloy to stripped logs (in-world), placing them counts |
| Shafts, cogwheels, hand crank, gearbox (`create_kinetics`) | `craft_item`, `place_block` | Crafting-table recipes (2 alloy → 8 shafts; shaft + planks → cogwheel) |
| Water wheels, sails, millstone (`create_power`) | `craft_item` | Crafting-table recipes |
| Belt connector, funnels, tunnels, depot (`create_logistics`) | `craft_item` | Crafting-table recipes (dried kelp, alloy) |
| Mechanical press, drill, mixer, fan, saw (`create_machines`, Normal+) | `craft_item` | Crafting-table recipes; some inputs (iron sheets, whisk, propeller) need a press first — the quest counts only the final crafting step |
| Wrench, goggles, super glue (`create_tools`, Normal+) | `craft_item` | Crafting-table recipes with pressed sheets |
| Seats (`create_decor`) | `craft_item`, `place_block` | Wool on a wooden slab (tier 0 — the only Easy-friendly Create craft) |
| Chocolate, honeyed apple, builder's tea, sweet roll, glazed berries (`create_sweets`, Hard) | `consume_item` | Made by mixers/spouts (not countable) but eating counts |
| Rose quartz (`create_quartz`) | `craft_item` | Shapeless: nether quartz + 8 redstone (needs the Nether) |

Totals: 18 entries, 70 targets, 10 themes (Zinc Rush, Engineer's Start, Water Mill, Stone Layers,
Metalworks (vanilla iron/copper + Create zinc), Logistics Line, Contraption Crew, Layered Masonry,
Andesite Age, Engineer's Kit), 17 reward items.

### 3.4 Excluded content and why

- **Everything produced by Create machines** as `craft_item`/`smelt_item`: brass (mixing), sheets
  (pressing), crushed ores (crushing), flour (milling), polished rose quartz (sandpaper), precision
  mechanisms (sequenced assembly), chocolate/sweets (mixing/filling). Machine outputs do not trigger
  the crafting-grid or furnace-slot hooks on any loader.
- **Mechanical crafting** recipes (crushing wheel, extendo grip, potato cannon, wand of symmetry) — the
  mechanical crafter is not a crafting grid.
- **Cut/polished stone as `craft_item`** — only stonecutter recipes (the crafting recipes are slab
  recycling).
- **`collect_item` for stone layers** — duplicates the `mine_block` targets.
- **Create 6-only content** (packager, stock ticker, table cloths, postboxes, cardboard) — missing on
  0.5.1 builds and logistics-heavy; left out to keep one profile valid everywhere.
- **Trains and contraption parts** — long, build-heavy goals that do not fit 4–35 minute quests.

### 3.5 Progression

Tier 1: zinc (iron pickaxe), stone layers, alloy and kinetic parts, logistics. Tier 2: machines and
tools that need pressed sheets or an iron block, and the sweets (Hard). Tier 3: scorchia (Nether
layers) and rose quartz (nether quartz). Create has no dimensions or advancements the profile needs.

### 3.6 Limitations

- The stone layers are rare (1 in 18 chunks); the `layers` hint adds ×1.3 effort and a hint, but a
  world may still need exploring.
- Casings and machines assume the player uses Create's basic workflow; effort values are estimates for
  a player who already has a small Create setup (tier 2 targets are Normal/Hard only).
- Create Fabric for 1.21.1 was only seen as a development branch.
- Create 0.5.1 (1.18.2/1.19.2) and Create 6 recipes differ slightly (e.g. andesite alloy is also made by
  mixing); the profile only uses recipes present in both.

---

## 4. Mekanism

| | |
|---|---|
| Name | Mekanism (Forge/NeoForge) |
| Modrinth | https://modrinth.com/mod/mekanism |
| Source | https://github.com/mekanism/Mekanism |
| Mod id / namespace | `mekanism` |
| Profile | `catalog/profiles/mekanism.json`, with `requiresMod: ["mekanism"]` and `minecraft.min: 1.18.2` |
| Studied | branches `1.18.x` (10.2.5, Forge 1.18.2), `1.19.x` (10.3.9, Forge 1.19.2), `1.20.x` (10.4.16, Forge 1.20.1), `1.21.x` (10.7.19, NeoForge 1.21.1) |

### 4.1 Coverage

Forge 1.18.2, 1.19.2 and 1.20.1 and NeoForge 1.21.1 have releases from the studied branches.
Mekanism has no Fabric edition, so the profile stays inactive on the Fabric builds.

### 4.2 Ids

All 34 ids (targets and rewards) exist with the same meaning in all four branches. Mekanism names its
materials the other way round (`ingot_osmium`, `block_osmium`, `fluorite_gem`). A dedicated server has
no mod language files, so prettified ids would read "Ingot Osmium". Every such target therefore
carries its English name in the profile ("Osmium Ingot", "Block of Osmium", "Fluorite").

### 4.3 Included content

| Entries (family) | Objective types | Why it is countable |
|---|---|---|
| Osmium, tin, lead (`mek_osmium`, `mek_tin`, `mek_lead`) | `mine_block` ore and deepslate ore, `collect_item` raw metal, `smelt_item` ingot (tag concepts `osmium_ingots`, `tin_ingots`, `lead_ingots`), `craft_item` block of osmium (Hard) | Ores need a stone pickaxe (`minecraft:needs_stone_tool`); the loot tables drop raw metal; furnace and blast-furnace recipes exist in every branch without conditions |
| Uranium (`mek_uranium`, tier 2) | `mine_block`, `smelt_item` | Same as above; rarer, so Normal and Hard only |
| Fluorite (`mek_fluorite`) | `mine_block`, `collect_item` fluorite (2–4 per ore), `craft_item` block (Hard) | Loot table with a 2–4 count; nine gems make a block in a grid |
| Salt (`mek_salt`) | `mine_block` salt block, `collect_item` salt (Normal+) | Mekanism's world generation places salt patches under shallow water, like clay (a config option); the salt block's loot table drops salt |
| First workshop (`mek_gear`) | `craft_item` metallurgic infuser, gauge dropper, canteen | Shaped grid recipes from iron, osmium, tin, redstone, furnaces, glass panes |
| Hazmat (`mek_radiation`) | `craft_item` dosimeter, hazmat mask | Shaped grid recipes from lead |
| Machines (`mek_machines`, Hard only) | `craft_item` steel casing, enrichment chamber, energized smelter, configurator | Grid recipes; they need steel and circuits from the metallurgic infuser, so each is one long quest |

Totals: 9 entries, 31 targets, 4 themes (Osmium Rush, First Machine, Metal Survey, Radiation Safety),
9 reward items (ingots, raw osmium, fluorite, salt; steel, infused alloy and basic control circuits on
tier 2).

### 4.4 Excluded content and why

- **Everything made by Mekanism machines** (dusts, clumps, shards, crystals, enriched materials,
  alloys, circuits, steel, bio fuel). Machine outputs do not trigger the crafting-grid or furnace hook.
- **Steel ingots as `craft_item`**: the only grid recipes convert nuggets and blocks.
- **Crusher, osmium compressor, higher-tier machines**: they need infused or reinforced alloys from
  machines. The four Hard machines are the practical first ones.
- **Radioactive materials, fission, fusion**: long-term projects, not 4–35 minute quests.

### 4.5 Limitations

- Ore generation can be disabled in Mekanism's world config. The quests then still appear, but the
  ores are missing. The existence check cannot see world generation, so pack makers with ores turned
  off should add `mekanism` to `generatorDisabledProfiles` or override the entries in a world profile.
- Effort values for the machines assume the player learns the steel chain while doing the quest.

---

## 5. The Twilight Forest

| | |
|---|---|
| Name | The Twilight Forest (Forge/NeoForge) |
| Modrinth | https://modrinth.com/mod/the-twilight-forest |
| Source | https://github.com/TeamTwilight/twilightforest |
| Mod id / namespace | `twilightforest` |
| Profile | `catalog/profiles/twilightforest.json`, with `requiresMod: ["twilightforest"]`, `minecraft.min: 1.18.2` and a `dimensions` unlock rule |
| Studied | branches `1.18.x` (4.1, Forge 1.18.2), `1.19.x` (4.2, 1.19), `1.20.1` (4.3, 1.20.1), `1.21.1` (4.8, NeoForge 1.21.1) |

### 5.1 Coverage and unlock

Forge 1.18.2, 1.19.x and 1.20.1 and NeoForge 1.21.1 have releases. There is no official Fabric
edition. The dimension `twilightforest:twilight_forest` unlocks for the world once 25 % of the
online players have the mod's root advancement `twilightforest:root` (granted for building a portal
or entering), or from game day 12. Until then no Twilight Forest objective is generated. When the
dimension does not exist (for example, the mod's data pack is disabled), nothing from the profile
appears.

### 5.2 Ids

All 26 ids (targets and rewards) exist in all four branches. The dimension was checked against its
`dimension/twilight_forest.json`. English names where the id reads badly: "Canopy Tree Log",
"Darkwood Log" (`dark_log`), "Venison Steak" (`cooked_venison`), "Blank Magic Map".

### 5.3 Included content

| Entries (family) | Objective types | Why it is countable |
|---|---|---|
| Portal trip (`tf_travel`) | `visit_dimension` | Dimension change hook; the hint explains the portal (a diamond thrown into a flower-ringed pool) |
| Wood (`tf_wood`) | `mine_block` twilight oak, canopy and darkwood logs, `craft_item` twilight oak planks, canopy bookshelf | Logs generate in the dimension; plank and bookshelf recipes are shapeless/shaped grid recipes |
| Forage (`tf_forage`) | `collect_item` torchberries, liveroot, raven feathers | Loot tables: ripe torchberry plants, liveroot blocks (without Silk Touch) and ravens drop them |
| Venison (`tf_game`) | `collect_item` raw venison, `smelt_item` and `consume_item` venison steak | Deer drop venison; furnace, smoker and campfire recipes |
| Breeding (`tf_ranch`) | `breed_animal` deer, boar, bighorn sheep | The mod's tempt tags (`deer_tempt_items`: wheat, apples; `boar_tempt_items`: carrots, potatoes, beetroots); bighorn sheep behave like sheep |
| Monsters (`tf_monsters`, tier 3) | `kill_mob` kobold, redcap, skeleton druid, hostile wolf, hedge spider, minotaur | Kill hook |
| Bosses (`tf_bosses`, Hard only) | `kill_mob` Naga and Lich, `collect_item` naga scales | Kill hook; the Naga's loot table drops scales |
| Ironwood (`tf_ironwood`) | `craft_item` raw ironwood, `smelt_item` ironwood ingot | Shapeless grid recipe (liveroot, raw iron, gold nugget); furnace and blast-furnace recipes |
| Maps (`tf_maps`) | `craft_item` magic map focus, blank magic map | Grid recipes (raven feather, torchberries, glowstone dust; focus and paper) |

Totals: 9 entries, 28 targets, 4 themes (Twilight Expedition, Twilight Hunt, Ironwood Smith, Venison
Feast), 6 reward items (tier 3 and 4, so they never land on the earliest quests).

### 5.4 Excluded content and why

- **Uncrafting table outputs and the uncrafting table itself**: it needs a maze map focus from a
  minotaur, and uncrafting is not a crafting-grid hook.
- **Later bosses and their trophies** (hydra, knight phantoms, ur-ghast, snow queen, final castle):
  they sit behind the progression locks of the dimension, and some take hours.
- **Steeleaf and knightmetal**: steeleaf comes from specific leaves and knightmetal needs armor shards
  from the knight stronghold; both are too far into the progression.
- **Carminite, fiery ingots**: they need items from later bosses or the dark tower.

### 5.5 Limitations

- The Twilight Forest's progression locks (biomes that hurt or block players until an earlier boss is
  defeated) are not modelled. Everything except the bosses is tier 2 or 3 and doable at the edges of
  the first biomes; darkwood logs grow at the rim of the Dark Forest.
- The breeding foods of 1.18.2–1.20.1 were not read from tags (those branches have none) but follow
  the same behaviour.

---

## 6. Botania

| | |
|---|---|
| Name | Botania (Forge, NeoForge, Fabric; one code base, "Xplat") |
| Modrinth | https://modrinth.com/mod/botania |
| Source | https://github.com/VazkiiMods/Botania |
| Mod id / namespace | `botania` |
| Author | Vazkii (credited as the licence asks) |
| Profile | `catalog/profiles/botania.json`, with `requiresMod: ["botania"]` and `minecraft.min: 1.18.2` |
| Studied | branches `1.18.x` (build 435.1, 1.18.2), `1.19.x` (441, 1.19.4), `1.20.x` (457, 1.20.1); `1.21.1-porting` was still a porting branch in September 2026 |

### 6.1 Coverage

Releases exist for Forge and Fabric 1.18.2, 1.19.x and 1.20.1. For 1.21.1 only the porting branch was
seen, and its language file lacked many item names. The runtime existence check decides there.

### 6.2 Ids

All 20 ids (targets and rewards) exist in the three released branches. English names are set in the
profile ("Mystical White Flower", "Lexica Botania", "Wand of the Forest", …) because dedicated servers
have no mod language files.

### 6.3 Included content

| Entries (family) | Objective types | Why it is countable |
|---|---|---|
| Mystical flowers (`bot_flowers`) | `collect_item` six colours | Natural generation; the flower drops itself |
| Petals (`bot_petals`) | `craft_item` white, red, yellow petals | Shapeless grid recipe: one flower makes two petals |
| First steps (`bot_start`) | `craft_item` Lexica Botania, petal apothecary, flower pouch (Normal+) | Grid recipes (book and sapling; cobblestone and a petal; wool and a petal) |
| Shimmering mushrooms (`bot_mushrooms`) | `collect_item` white and red | Generate in caves and drop themselves |
| Livingwood (`bot_livingwood`, tier 2) | `craft_item` livingwood planks, Wand of the Forest | Grid recipes (the wand uses Botania's own grid serializer); livingwood comes from a Pure Daisy, as the hint says |
| Mana (`bot_mana`, Hard only) | `craft_item` mana pool | Grid recipe from livingrock (Pure Daisy on stone) |

Totals: 6 entries, 17 targets, 2 themes (Botanist's Start, Florist), 4 reward items (floral
fertilizer, petals, manasteel ingots, mana pearls).

### 6.4 Excluded content and why

- **Petal apothecary, runic altar, mana infusion, elven trade and brewing outputs**: none of them is a
  crafting grid.
- **Livingwood and livingrock as `collect_item`**: a Pure Daisy converts blocks in place, so nothing
  is picked up.
- **Mana spreader**: its grid recipe differs between versions.
- **Generating and functional flowers**: they are made in the petal apothecary.

---

## 7. How the ids were verified

1. Shallow, sparse `git` clones of `vectorwing/FarmersDelight` (branch `1.20`),
   `Creators-of-Create/Create` (`mc1.20.1/dev`), `mekanism/Mekanism` (`1.20.x`, `1.21.x`: generated
   data), `TeamTwilight/twilightforest` (`1.20.1`, `1.21.1`: generated data) and `VazkiiMods/Botania`
   (`1.20.x`: generated data). Recipe JSONs were classified by type (crafting shaped/shapeless and the
   mods' own grid serializers `mekanism:mek_data` and `botania:twig_wand`, smelting/blasting/smoking,
   machine types) to decide which outputs are countable. Recipes behind conditions (mod-loaded tags,
   config options) do not count. Loot tables, tool tags, tempt tags, dimension files and root
   advancements confirmed drops, tools, breeding foods and unlocks.
2. The `en_us` language files of every studied release branch were downloaded, and every profile id
   was checked against the item/block/entity keys of every version (results in §2.2, §3.2, §4.2, §5.2
   and §6.2).
3. `gradle.properties` / `libs.versions.toml` / `build.gradle.kts` of each branch gave the mod and
   Minecraft versions.
4. `tools/verify_mods.py` repeats steps 1–2 for all five profiles: every id against all language files,
   every `craft_item`/`smelt_item` target against unconditional grid or furnace recipes present in
   **every** studied data branch, and every `visit_dimension` target against a dimension file. Current
   result: 267 targets, 0 problems (three FD ids noted as missing before 1.20.1).
5. The runtime existence check remains authoritative: anything missing on a given build is skipped
   silently and counted as `3_missing_id` in the stats.
