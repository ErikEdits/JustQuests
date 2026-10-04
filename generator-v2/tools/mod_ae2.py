"""Applied Energistics 2 profile (code LGPL-3.0, assets CC BY-NC-SA; by AlgorithmX2 and the AE2 team).

Ids, names and recipes were checked against the released jars from Modrinth for Minecraft 1.18.2, 1.19.2,
1.20.1 and 1.21.1 (lang/en_us.json, crafting and smelting recipes, block loot tables). Only facts are used
(ids, names, which items have a crafting-grid or furnace recipe); nothing is copied.

Certus quartz grows on budding quartz in meteorites from 1.19 on (the 1.18.2 release had ores instead);
the cluster and budding targets carry "since". Processors need the Inscriber, so everything built from
them is Hard.
"""
from catalog_dsl import E, T

A = "ae2:"
METEOR = "Meteorites are craters of sky stone; a meteorite compass points to the nearest one."

AE2_ENTRIES = [
    E("ae2_meteor", "ae2_meteor", 1, [
        T("mine_block", A + "sky_stone_block", 0.6, 8, 32, name="Sky Stone", plural="Sky Stone", hint=METEOR),
        T("mine_block", A + "quartz_cluster", 1.2, 2, 8, name="Certus Quartz Cluster", since="1.19",
          hint="Certus clusters grow on the budding quartz inside meteorites."),
        T("collect_item", A + "certus_quartz_crystal", 1.0, 4, 16, hint=METEOR),
    ], tool="iron", hints=["rare"]),
    E("ae2_skystone", "ae2_skystone", 1, [
        T("smelt_item", A + "smooth_sky_stone_block", 0.8, 4, 16, name="Sky Stone Block",
          hint="Smelt sky stone in a furnace."),
        T("craft_item", A + "sky_stone_brick", 0.3, 8, 32, hint="Four sky stone blocks (smelted sky stone)."),
        T("craft_item", A + "sky_stone_chest", 5.0, 1, 2, minDifficulty="normal", hint="Eight sky stone."),
    ]),
    E("ae2_quartz_tools", "ae2_quartz_tools", 2, [
        T("craft_item", A + "certus_quartz_wrench", 3.0, 1, 1, hint="Five certus quartz crystals."),
        T("craft_item", A + "certus_quartz_cutting_knife", 3.0, 1, 1,
          hint="A stick, an iron ingot and certus quartz."),
        T("craft_item", A + "nether_quartz_wrench", 2.5, 1, 1, hint="Five nether quartz."),
        T("craft_item", A + "certus_quartz_pickaxe", 4.0, 1, 1, minDifficulty="normal",
          hint="Three certus quartz crystals and two sticks."),
        T("craft_item", A + "certus_quartz_sword", 3.5, 1, 1, minDifficulty="normal",
          hint="Two certus quartz crystals and a stick."),
    ]),
    E("ae2_fluix", "ae2_fluix", 2, [
        T("collect_item", A + "fluix_crystal", 2.0, 2, 8,
          hint="Charged certus quartz, redstone and nether quartz dropped into water turn into fluix."),
        T("craft_item", A + "fluix_block", 9.0, 1, 2, name="Fluix Block", minDifficulty="normal",
          hint="Four fluix crystals."),
        T("craft_item", A + "fluix_pearl", 10.0, 1, 1, minDifficulty="hard",
          hint="An ender pearl surrounded by fluix crystals and fluix dust."),
    ]),
    E("ae2_power", "ae2_power", 2, [
        T("craft_item", A + "charger", 6.0, 1, 1, hint="Iron and copper ingots."),
        T("craft_item", A + "inscriber", 7.0, 1, 1, minDifficulty="normal",
          hint="Iron ingots, pistons and a copper ingot."),
        T("craft_item", A + "energy_acceptor", 6.5, 1, 1, minDifficulty="normal",
          hint="Iron, quartz glass and copper."),
        T("craft_item", A + "quartz_glass", 1.0, 4, 8, minDifficulty="normal",
          hint="Glass and quartz dust, which the Inscriber grinds from quartz."),
    ]),
    E("ae2_network", "ae2_network", 3, [
        T("craft_item", A + "fluix_glass_cable", 1.2, 4, 16, name="Fluix ME Glass Cable",
          hint="Quartz fiber and two fluix crystals."),
        T("craft_item", A + "drive", 14.0, 1, 1, minDifficulty="hard",
          hint="Iron, engineering processors and fluix cables; processors come from the Inscriber."),
        T("craft_item", A + "item_cell_housing", 4.0, 1, 2, name="ME Item Cell Housing", minDifficulty="normal",
          hint="Quartz glass, redstone and iron."),
        T("craft_item", A + "cell_component_1k", 8.0, 1, 2, name="1k ME Storage Component",
          minDifficulty="hard", hint="Certus quartz, redstone and a logic processor from the Inscriber."),
    ]),
]

AE2_REWARDS = [
    {"id": A + "certus_quartz_crystal", "value": 1.2, "tier": 2, "max": 16, "family": "ae2_meteor"},
    {"id": A + "fluix_crystal", "value": 2.2, "tier": 2, "max": 8, "family": "ae2_fluix"},
    {"id": A + "sky_stone_block", "value": 0.6, "tier": 1, "max": 32, "family": "ae2_meteor"},
    {"id": A + "silicon", "value": 2.0, "tier": 3, "max": 8, "family": "ae2_power"},
]

AE2_THEMES = [
    {"key": "ae2_meteor_hunt", "names": ["Meteor Hunt", "Fallen Star", "Sky Stone"],
     "descriptions": ["A meteorite fell somewhere near: {list}."],
     "slots": [{"types": ["mine_block"], "keys": ["ae2_meteor"]},
               {"types": ["collect_item", "smelt_item"], "keys": ["ae2_meteor", "ae2_skystone"]}]},
    {"key": "ae2_engineer", "names": ["Network Engineer", "Wired Up", "Matter to Energy"],
     "descriptions": ["Every network starts on a workbench: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["craft_item"], "keys": ["ae2_quartz_tools", "ae2_power"]},
               {"types": ["craft_item", "collect_item"], "keys": ["ae2_fluix", "ae2_network"]}]},
]

AE2 = {
    "format": 1,
    "id": "ae2",
    "name": "Applied Energistics 2",
    "requiresMod": ["ae2"],
    "minecraft": {"min": "1.18.2"},
    "entries": AE2_ENTRIES,
    "rewards": {"items": AE2_REWARDS},
    "themes": AE2_THEMES,
}

AE2_FAMILY_NAMES = {
    "ae2_meteor": "Meteorite", "ae2_skystone": "Sky Stone", "ae2_quartz_tools": "Quartz Smith",
    "ae2_fluix": "Fluix", "ae2_power": "Power", "ae2_network": "Network",
}
