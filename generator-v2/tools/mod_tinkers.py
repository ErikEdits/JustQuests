"""Tinkers' Construct profile (MIT licensed mod by mDiyo, KnightMiner and the SlimeKnights team).

Ids, names and recipes were checked against the released jars from Modrinth for Minecraft 1.18.2, 1.19.2
and 1.20.1 (lang/en_us.json, crafting and smelting recipes, block loot tables). Only facts are used (ids,
names, which items have a crafting-grid or furnace recipe); nothing is copied.

Tools are built in the part builder and the tinker station, which the generator cannot count; the
profile covers the way there: grout, seared bricks, patterns, the stations, the first smeltery parts,
slime islands and cobalt. The stations use the mod's own crafting-table recipe type, which counts as
crafting.
"""
from catalog_dsl import E, T

TC = "tconstruct:"

TC_ENTRIES = [
    E("tc_grout", "tc_grout", 1, [
        T("craft_item", TC + "grout", 0.3, 8, 32, plural="Grout", hint="Clay, sand and gravel."),
        T("smelt_item", TC + "seared_brick", 0.45, 8, 32, hint="Smelt grout in a furnace."),
        T("craft_item", TC + "seared_bricks", 2.0, 2, 8, plural="Seared Bricks", hint="Four seared bricks."),
    ]),
    E("tc_stations", "tc_stations", 1, [
        T("craft_item", TC + "pattern", 0.4, 4, 16, hint="Two planks and two sticks."),
        T("craft_item", TC + "crafting_station", 1.6, 1, 1, hint="A pattern on top of a log or crafting table."),
        T("craft_item", TC + "part_builder", 2.0, 1, 1, hint="Two patterns on top of two planks."),
        T("craft_item", TC + "tinker_station", 2.4, 1, 1, hint="Three patterns on top of planks."),
        T("craft_item", TC + "materials_and_you", 1.2, 1, 1, name="Materials and You", plural="Materials and You",
          hint="A book and a pattern."),
    ]),
    E("tc_smeltery", "tc_smeltery", 2, [
        T("craft_item", TC + "seared_heater", 5.0, 1, 1, minDifficulty="normal", hint="Eight seared bricks."),
        T("craft_item", TC + "seared_faucet", 2.2, 1, 2, minDifficulty="normal", hint="Three seared bricks."),
        T("craft_item", TC + "seared_table", 4.5, 1, 1, name="Seared Casting Table", minDifficulty="normal",
          hint="Seven seared bricks."),
        T("craft_item", TC + "seared_basin", 4.5, 1, 1, name="Seared Casting Basin", minDifficulty="normal",
          hint="Seven seared bricks."),
        T("craft_item", TC + "seared_melter", 6.5, 1, 1, minDifficulty="hard",
          hint="Seared bricks around a seared tank."),
    ]),
    E("tc_slime_isles", "tc_slime_isles", 2, [
        T("mine_block", TC + "skyroot_log", 0.6, 4, 16, hint="Skyroot grows on the sky slime islands floating high up."),
        T("collect_item", TC + "sky_slime_ball", 1.0, 4, 16, hint="Sky slimes on the slime islands drop skyslime balls."),
        T("kill_mob", TC + "sky_slime", 0.9, 4, 12, name="Skyslime", hint="Skyslimes bounce on the sky slime islands."),
        T("craft_item", TC + "sky_slime", 9.0, 1, 1, name="Skyslime Block", minDifficulty="normal",
          hint="Nine skyslime balls."),
        T("mine_block", TC + "greenheart_log", 0.5, 4, 16, hint="Greenheart grows on earth slime islands in the sea."),
    ], tool="stone", hints=["rare"]),
    E("tc_cobalt", "tc_cobalt", 3, [
        T("mine_block", TC + "cobalt_ore", 2.0, 2, 8, name="Nether Cobalt Ore",
          hint="Cobalt ore hides in the Nether's netherrack, high and low."),
        T("collect_item", TC + "raw_cobalt", 2.0, 2, 8, plural="Raw Cobalt", hint="Cobalt ore drops raw cobalt."),
    ], dim="minecraft:the_nether", tool="diamond"),
]

TC_REWARDS = [
    {"id": TC + "seared_brick", "value": 0.5, "tier": 1, "max": 32, "family": "tc_grout"},
    {"id": TC + "grout", "value": 0.3, "tier": 1, "max": 32, "family": "tc_grout"},
    {"id": TC + "sky_slime_ball", "value": 1.0, "tier": 2, "max": 16, "family": "tc_slime_isles"},
    {"id": TC + "cobalt_ingot", "value": 3.5, "tier": 3, "max": 4, "family": "tc_cobalt"},
]

TC_THEMES = [
    {"key": "tc_tinker", "names": ["Apprentice Tinker", "Tinker's Bench", "Patterns and Parts"],
     "descriptions": ["Every tinker starts with a pattern: {list}."],
     "slots": [{"types": ["craft_item"], "keys": ["tc_stations"]},
               {"types": ["craft_item", "smelt_item"], "keys": ["tc_grout", "tc_stations"]}]},
    {"key": "tc_smelter", "names": ["Smeltery Builder", "Seared and Ready", "Molten Plans"],
     "descriptions": ["A smeltery is built brick by brick: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["smelt_item", "craft_item"], "keys": ["tc_grout"]},
               {"types": ["craft_item"], "keys": ["tc_smeltery"]}]},
]

TINKERS = {
    "format": 1,
    "id": "tconstruct",
    "name": "Tinkers' Construct",
    "requiresMod": ["tconstruct"],
    "minecraft": {"min": "1.18.2"},
    "entries": TC_ENTRIES,
    "rewards": {"items": TC_REWARDS},
    "themes": TC_THEMES,
}

TC_FAMILY_NAMES = {
    "tc_grout": "Seared", "tc_stations": "Tinker", "tc_smeltery": "Smeltery", "tc_slime_isles": "Slime Island",
    "tc_cobalt": "Cobalt",
}
