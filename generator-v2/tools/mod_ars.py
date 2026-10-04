"""Ars Nouveau profile (GPL-3.0 licensed mod by baileyholl and contributors).

Ids, names and recipes were checked against the released jars from Modrinth for Minecraft 1.18.2, 1.19.2,
1.20.1 and 1.21.1 (lang/en_us.json, crafting recipes, block loot tables). Only facts are used (ids,
names, which items have a crafting-grid recipe, what drops what); nothing is copied.

Source gems come from the imbuement chamber, not a grid, so the pieces built from them are Normal or
Hard and say so in their hint. The archwood trees grow in the mod's archwood forests.
"""
from catalog_dsl import E, T

AN = "ars_nouveau:"
ARCHWOOD = "Archwood trees grow in archwood forests."

AN_ENTRIES = [
    E("an_archwood", "an_archwood", 0, [
        T("mine_block", AN + "blue_archwood_log", 0.2, 8, 32, name="Cascading Archwood Log", hint=ARCHWOOD),
        T("mine_block", AN + "red_archwood_log", 0.2, 8, 32, name="Blazing Archwood Log", hint=ARCHWOOD),
        T("mine_block", AN + "purple_archwood_log", 0.2, 8, 32, name="Vexing Archwood Log", hint=ARCHWOOD),
        T("mine_block", AN + "green_archwood_log", 0.2, 8, 32, name="Flourishing Archwood Log", hint=ARCHWOOD),
        T("craft_item", AN + "archwood_planks", 0.05, 16, 64, hint="One archwood log makes four planks."),
    ], hints=["rare"]),
    E("an_forage", "an_forage", 0, [
        T("collect_item", AN + "sourceberry_bush", 0.5, 4, 16, name="Sourceberry", plural="Sourceberries",
          hint="Sourceberry bushes grow in taigas and archwood forests."),
        T("collect_item", AN + "magebloom", 1.0, 2, 8,
          hint="Magebloom grows from magebloom seeds; its flower drops when harvested."),
        T("craft_item", AN + "magebloom_fiber", 1.1, 2, 8, hint="One magebloom."),
    ]),
    E("an_first_spells", "an_first_spells", 1, [
        T("craft_item", AN + "worn_notebook", 3.0, 1, 1, name="Tattered Tome", plural="Tattered Tomes",
          hint="A book and lapis lazuli."),
        T("craft_item", AN + "novice_spell_book", 6.0, 1, 1, minDifficulty="normal",
          hint="A book and four iron tools: shovel, pickaxe, axe and sword."),
        T("craft_item", AN + "dowsing_rod", 2.0, 1, 2, hint="A gold ingot and archwood planks."),
    ]),
    E("an_workshop", "an_workshop", 2, [
        T("craft_item", AN + "scribes_table", 3.0, 1, 1, name="Scribe's Table",
          hint="Archwood slabs, gold nuggets and archwood logs."),
        T("craft_item", AN + "imbuement_chamber", 3.5, 1, 1, hint="Archwood planks and gold ingots."),
        T("craft_item", AN + "archwood_chest", 2.0, 1, 2, hint="Archwood planks around a gold nugget."),
        T("craft_item", AN + "source_jar", 3.0, 1, 2, hint="Archwood slabs and glass."),
        T("craft_item", AN + "blank_parchment", 3.0, 1, 4, minDifficulty="normal",
          hint="Paper surrounded by magebloom fiber."),
    ]),
    E("an_wilden", "an_wilden", 3, [
        T("kill_mob", AN + "wilden_hunter", 1.8, 2, 6, hint="Wilden hunters prowl archwood forests at night."),
        T("kill_mob", AN + "wilden_stalker", 1.8, 2, 6, hint="Wilden stalkers swoop down from above."),
        T("kill_mob", AN + "wilden_guardian", 2.0, 2, 6, hint="Wilden guardians roll up behind their spikes."),
        T("collect_item", AN + "wilden_horn", 2.0, 1, 4, hint="Wilden hunters drop horns."),
        T("collect_item", AN + "wilden_spike", 2.0, 1, 4, hint="Wilden guardians drop spikes."),
        T("collect_item", AN + "wilden_wing", 2.0, 1, 4, hint="Wilden stalkers drop wings."),
    ]),
]

AN_REWARDS = [
    {"id": AN + "source_gem", "value": 2.0, "tier": 2, "max": 8, "family": "an_workshop"},
    {"id": AN + "sourceberry_bush", "value": 0.5, "tier": 1, "max": 16, "family": "an_forage"},
    {"id": AN + "magebloom_fiber", "value": 1.2, "tier": 2, "max": 8, "family": "an_forage"},
    {"id": AN + "archwood_planks", "value": 0.06, "tier": 0, "max": 64, "family": "an_archwood"},
]

AN_THEMES = [
    {"key": "an_apprentice", "names": ["Apprentice Mage", "First Spells", "Arcane Studies"],
     "descriptions": ["Every mage starts with a tome: {list}."],
     "slots": [{"types": ["craft_item"], "keys": ["an_first_spells"]},
               {"types": ["collect_item", "craft_item"], "keys": ["an_forage", "an_workshop"]}]},
    {"key": "an_woodland", "names": ["Archwood Grove", "Magic Woods"],
     "descriptions": ["The archwood forest hides more than wood: {list}."],
     "slots": [{"types": ["mine_block"], "keys": ["an_archwood"]},
               {"types": ["collect_item"], "keys": ["an_forage"]}]},
]

ARS_NOUVEAU = {
    "format": 1,
    "id": "ars_nouveau",
    "name": "Ars Nouveau",
    "requiresMod": ["ars_nouveau"],
    "minecraft": {"min": "1.18.2"},
    "entries": AN_ENTRIES,
    "rewards": {"items": AN_REWARDS},
    "themes": AN_THEMES,
}

AN_FAMILY_NAMES = {
    "an_archwood": "Archwood", "an_forage": "Sourceberry", "an_first_spells": "Apprentice", "an_workshop": "Arcane",
    "an_wilden": "Wilden",
}
