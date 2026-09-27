"""Mekanism profile (MIT licensed mod by Aidan C. Brady and contributors).

Ids, names and recipes were checked against the mod's generated data on GitHub (branches 1.18.x, 1.19.x,
1.20.x and 1.21.x: lang/en_us.json, recipes, loot tables, minecraft:needs_stone_tool). Only facts are
used (ids, names, which items have a crafting-grid or furnace recipe); nothing is copied.

Mekanism names its materials "ingot_osmium", "block_osmium" and so on. A dedicated server has no mod
lang files, so prettified ids would read "Ingot Osmium"; every such target carries its English name.
"""
from catalog_dsl import E, T

M = "mekanism:"

MEK_ENTRIES = [
    E("mek_osmium", "mek_osmium", 1, [
        T("mine_block", M + "osmium_ore", 0.7, 4, 24),
        T("mine_block", M + "deepslate_osmium_ore", 0.8, 4, 24, hints=["deep"]),
        T("collect_item", M + "raw_osmium", 0.7, 4, 24, hint="Osmium ore drops raw osmium."),
        T("smelt_item", M + "ingot_osmium", 0.75, 4, 32, tag="osmium_ingots", name="Osmium Ingot",
          hint="Smelt raw osmium in a furnace."),
        T("craft_item", M + "block_osmium", 6.8, 1, 2, name="Block of Osmium", plural="Blocks of Osmium",
          minDifficulty="hard"),
    ], tool="stone", hints=["caves"],
      exclusions=["collect_item mekanism:osmium_ore (drops raw osmium, needs Silk Touch)"]),
    E("mek_tin", "mek_tin", 1, [
        T("mine_block", M + "tin_ore", 0.7, 4, 24),
        T("mine_block", M + "deepslate_tin_ore", 0.8, 4, 24, hints=["deep"]),
        T("collect_item", M + "raw_tin", 0.7, 4, 24, hint="Tin ore drops raw tin."),
        T("smelt_item", M + "ingot_tin", 0.75, 4, 32, tag="tin_ingots", name="Tin Ingot",
          hint="Smelt raw tin in a furnace."),
    ], tool="stone", hints=["caves"]),
    E("mek_lead", "mek_lead", 1, [
        T("mine_block", M + "lead_ore", 0.8, 4, 24),
        T("mine_block", M + "deepslate_lead_ore", 0.9, 4, 24, hints=["deep"]),
        T("collect_item", M + "raw_lead", 0.8, 4, 24, name="Raw Lead", plural="Raw Lead",
          hint="Lead ore drops raw lead."),
        T("smelt_item", M + "ingot_lead", 0.85, 4, 32, tag="lead_ingots", name="Lead Ingot",
          hint="Smelt raw lead in a furnace."),
    ], tool="stone", hints=["caves"]),
    E("mek_uranium", "mek_uranium", 2, [
        T("mine_block", M + "uranium_ore", 1.1, 2, 16),
        T("mine_block", M + "deepslate_uranium_ore", 1.2, 2, 16, hints=["deep"]),
        T("smelt_item", M + "ingot_uranium", 1.2, 2, 16, name="Uranium Ingot", minDifficulty="normal",
          hint="Smelt raw uranium in a furnace."),
    ], tool="stone", hints=["caves"]),
    E("mek_fluorite", "mek_fluorite", 1, [
        T("mine_block", M + "fluorite_ore", 0.9, 2, 16),
        T("mine_block", M + "deepslate_fluorite_ore", 1.0, 2, 16, hints=["deep"]),
        T("collect_item", M + "fluorite_gem", 0.32, 8, 48, name="Fluorite", plural="Fluorite",
          hint="Fluorite ore drops two to four gems."),
        T("craft_item", M + "block_fluorite", 3.2, 1, 2, name="Block of Fluorite", plural="Blocks of Fluorite",
          minDifficulty="hard"),
    ], tool="stone", hints=["caves"]),
    E("mek_salt", "mek_salt", 0, [
        T("mine_block", M + "block_salt", 0.35, 4, 24, name="Salt Block",
          hint="Salt forms in small patches under shallow water, like clay."),
        T("collect_item", M + "salt", 0.1, 8, 48, name="Salt", plural="Salt", minDifficulty="normal",
          hint="Salt blocks drop salt."),
    ], tool="wood", hints=["river"]),
    E("mek_gear", "mek_gear", 1, [
        T("craft_item", M + "metallurgic_infuser", 5.0, 1, 1,
          hint="Iron, osmium, redstone and two furnaces: the first Mekanism machine."),
        T("craft_item", M + "gauge_dropper", 2.5, 1, 2, hint="An osmium ingot over five glass panes."),
        T("craft_item", M + "canteen", 3.4, 1, 1, hint="Four tin ingots around a bowl."),
    ]),
    E("mek_radiation", "mek_radiation", 1, [
        T("craft_item", M + "dosimeter", 3.6, 1, 1, hint="Four lead ingots around redstone."),
        T("craft_item", M + "hazmat_mask", 4.5, 1, 1, hint="Five lead ingots and orange dye."),
    ]),
    E("mek_machines", "mek_machines", 2, [
        T("craft_item", M + "steel_casing", 22.0, 1, 1, minDifficulty="hard",
          hint="Steel comes from the metallurgic infuser: infuse iron with carbon twice."),
        T("craft_item", M + "enrichment_chamber", 34.0, 1, 1, minDifficulty="hard",
          hint="A steel casing, basic alloys and a basic control circuit."),
        T("craft_item", M + "energized_smelter", 34.0, 1, 1, minDifficulty="hard",
          hint="A steel casing, basic alloys, glass and a basic control circuit."),
        T("craft_item", M + "configurator", 16.0, 1, 1, minDifficulty="hard",
          hint="Iron, an osmium ingot and a steel ingot."),
    ], exclusions=["craft_item mekanism:ingot_steel (grid recipe only converts nuggets and blocks)",
                   "craft_item mekanism:basic_control_circuit (made in the metallurgic infuser, not a grid)",
                   "craft_item mekanism:crusher (needs lava buckets and a circuit; kept out to stay short)"]),
]

MEK_REWARDS = [
    {"id": M + "ingot_osmium", "value": 0.9, "tier": 1, "max": 32, "family": "mek_osmium"},
    {"id": M + "ingot_tin", "value": 0.85, "tier": 1, "max": 32, "family": "mek_tin"},
    {"id": M + "ingot_lead", "value": 0.95, "tier": 1, "max": 32, "family": "mek_lead"},
    {"id": M + "raw_osmium", "value": 0.7, "tier": 1, "max": 32, "family": "mek_osmium"},
    {"id": M + "fluorite_gem", "value": 0.35, "tier": 1, "max": 32, "family": "mek_fluorite"},
    {"id": M + "salt", "value": 0.12, "tier": 0, "max": 32, "family": "mek_salt"},
    {"id": M + "ingot_steel", "value": 3.0, "tier": 2, "max": 16, "family": "mek_machines"},
    {"id": M + "alloy_infused", "value": 3.5, "tier": 2, "max": 8, "family": "mek_machines"},
    {"id": M + "basic_control_circuit", "value": 4.0, "tier": 2, "max": 8, "family": "mek_machines"},
]

MEK_THEMES = [
    {"key": "mek_osmium_rush", "names": ["Osmium Rush", "Blue Metal", "Osmium Supply"],
     "descriptions": ["Every Mekanism machine needs osmium: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block", "collect_item"], "keys": ["mek_osmium"]},
               {"types": ["smelt_item"], "keys": ["mek_osmium"]}]},
    {"key": "mek_first_machine", "names": ["First Machine", "Infuser Build", "Tech Start"],
     "descriptions": ["Get the workshop going: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["smelt_item"], "keys": ["mek_osmium"]},
               {"types": ["craft_item"], "keys": ["mek_gear"]}]},
    {"key": "mek_metal_survey", "names": ["Metal Survey", "Ore Survey", "New Metals"],
     "descriptions": ["Chart the new ores underground: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["mine_block"], "keys": ["mek_tin", "mek_lead"]},
               {"types": ["mine_block"], "keys": ["mek_osmium", "mek_fluorite", "mek_uranium"]}]},
    {"key": "mek_radiation_safety", "names": ["Radiation Safety", "Hazmat Prep", "Lead Shielding"],
     "descriptions": ["Before anything glows, be ready: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["smelt_item", "collect_item"], "keys": ["mek_lead"]},
               {"types": ["craft_item"], "keys": ["mek_radiation"]}]},
]

MEKANISM = {
    "format": 1,
    "id": "mekanism",
    "name": "Mekanism",
    "requiresMod": ["mekanism"],
    "minecraft": {"min": "1.18.2"},
    "entries": MEK_ENTRIES,
    "rewards": {"items": MEK_REWARDS},
    "themes": MEK_THEMES,
}

MEK_FAMILY_NAMES = {
    "mek_osmium": "Osmium", "mek_tin": "Tin", "mek_lead": "Lead", "mek_uranium": "Uranium",
    "mek_fluorite": "Fluorite", "mek_salt": "Salt", "mek_gear": "Workshop", "mek_radiation": "Hazmat",
    "mek_machines": "Machine",
}

MEK_TAGS = {
    "osmium_ingots": {"kind": "item", "candidates": ["c:ingots/osmium", "forge:ingots/osmium"],
                      "name": "osmium ingot", "plural": "osmium ingots"},
    "tin_ingots": {"kind": "item", "candidates": ["c:ingots/tin", "forge:ingots/tin"],
                   "name": "tin ingot", "plural": "tin ingots"},
    "lead_ingots": {"kind": "item", "candidates": ["c:ingots/lead", "forge:ingots/lead"],
                    "name": "lead ingot", "plural": "lead ingots"},
}
