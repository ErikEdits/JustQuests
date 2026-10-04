"""Alex's Mobs profile (by sbom_xela; Forge 1.16.5–1.20.1).

Ids, names, recipes and mobs were checked against the released jars from Modrinth for Minecraft 1.18.2,
1.19.2 and 1.20.1 (lang/en_us.json, crafting and smelting recipes). Only ids and names are used, which
are facts; nothing of the mod is copied or shipped.

The mod has no grid recipe for most of its drops, so the profile hunts the animals for their drops,
cooks their meat and crafts the few gear pieces that are made on a crafting table.
"""
from catalog_dsl import E, T

AM = "alexsmobs:"

AM_ENTRIES = [
    E("am_savanna", "am_savanna", 1, [
        T("collect_item", AM + "kangaroo_hide", 1.4, 2, 8, hint="Kangaroos hop around savannas and deserts."),
        T("collect_item", AM + "kangaroo_meat", 1.2, 2, 8, plural="Kangaroo Meat", hint="Kangaroos drop meat."),
        T("collect_item", AM + "roadrunner_feather", 1.6, 2, 6, hint="Roadrunners race across badlands and deserts."),
        T("collect_item", AM + "emu_feather", 1.4, 2, 8, hint="Emus roam savannas and badlands."),
    ], hints=["savanna"]),
    E("am_northlands", "am_northlands", 1, [
        T("collect_item", AM + "moose_antler", 2.4, 1, 4, hint="Moose roam snowy forests."),
        T("collect_item", AM + "moose_ribs", 1.6, 2, 6, name="Raw Moose Ribs", plural="Raw Moose Ribs",
          hint="Moose drop ribs."),
        T("collect_item", AM + "bear_fur", 2.0, 1, 4, name="Hair of Bear", plural="Hair of Bear",
          hint="Grizzly bears live in forests and taigas."),
        T("collect_item", AM + "bison_fur", 1.4, 2, 8, plural="Bison Fur", hint="Bison roam snowy plains."),
    ], hints=["taiga"]),
    E("am_cookout", "am_cookout", 1, [
        T("smelt_item", AM + "cooked_kangaroo_meat", 1.3, 2, 8, plural="Cooked Kangaroo Meat",
          hint="Cook kangaroo meat in a furnace, smoker or campfire."),
        T("smelt_item", AM + "cooked_moose_ribs", 1.7, 2, 6, plural="Cooked Moose Ribs",
          hint="Cook moose ribs in a furnace, smoker or campfire."),
        T("smelt_item", AM + "cooked_lobster_tail", 1.5, 2, 6,
          hint="Lobsters crawl on beaches; cook their tails."),
        T("craft_item", AM + "kangaroo_burger", 2.0, 1, 4, hint="Cooked kangaroo meat between two breads."),
    ]),
    E("am_predators", "am_predators", 2, [
        T("kill_mob", AM + "crocodile", 2.0, 1, 4, hint="Crocodiles lurk in swamps, rivers and mangroves."),
        T("kill_mob", AM + "rattlesnake", 1.4, 2, 6, hint="Rattlesnakes coil in deserts and badlands."),
        T("kill_mob", AM + "komodo_dragon", 2.4, 1, 3, minDifficulty="normal",
          hint="Komodo dragons hunt in jungles."),
        T("kill_mob", AM + "centipede_head", 2.0, 1, 3, name="Cave Centipede", minDifficulty="normal",
          hint="Cave centipedes crawl in dark caves."),
        T("collect_item", AM + "crocodile_scute", 2.2, 2, 8, hint="Crocodiles drop scutes."),
    ], hints=["swamp"]),
    E("am_gear", "am_gear", 2, [
        T("craft_item", AM + "roadrunner_boots", 10.0, 1, 1, plural="Roadrunner Boots",
          minDifficulty="normal", hint="Roadrunner feathers, leather boots and chiseled sandstone."),
        T("craft_item", AM + "moose_headgear", 10.0, 1, 1, name="Antler Headdress", minDifficulty="normal",
          hint="Moose antlers, an iron ingot and string."),
        T("craft_item", AM + "crocodile_chestplate", 16.0, 1, 1, minDifficulty="hard",
          hint="Seven crocodile scutes."),
        T("craft_item", AM + "shark_tooth_arrow", 1.6, 4, 16, hint="A shark tooth, a stick and kelp."),
    ]),
    E("am_nether", "am_nether", 3, [
        T("kill_mob", AM + "crimson_mosquito", 1.4, 2, 6, hint="Crimson mosquitoes buzz in crimson forests."),
        T("kill_mob", AM + "soul_vulture", 2.0, 1, 4, hint="Soul vultures circle soul sand valleys."),
        T("kill_mob", AM + "dropbear", 2.4, 1, 3, minDifficulty="normal",
          hint="Dropbears hang from the ceilings of Nether wastes."),
    ], dim="minecraft:the_nether"),
]

AM_REWARDS = [
    {"id": AM + "cooked_kangaroo_meat", "value": 1.3, "tier": 1, "max": 16, "family": "am_cookout"},
    {"id": AM + "kangaroo_burger", "value": 2.2, "tier": 2, "max": 8, "family": "am_cookout"},
    {"id": AM + "bison_fur", "value": 1.4, "tier": 1, "max": 16, "family": "am_northlands"},
    {"id": AM + "roadrunner_feather", "value": 1.7, "tier": 2, "max": 8, "family": "am_savanna"},
]

AM_THEMES = [
    {"key": "am_safari", "names": ["Safari", "Wild Kingdom", "Into the Wild"],
     "descriptions": ["The wild animals have plenty to give: {list}."],
     "slots": [{"types": ["collect_item"], "keys": ["am_savanna", "am_northlands"]},
               {"types": ["collect_item", "smelt_item"], "keys": ["am_savanna", "am_northlands", "am_cookout"]}]},
    {"key": "am_hunter", "names": ["Big Game", "Apex Hunter", "Predator Patrol"],
     "descriptions": ["Dangerous animals roam the wilds: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["kill_mob"], "keys": ["am_predators"]},
               {"types": ["kill_mob", "collect_item"], "keys": ["am_predators"]}]},
]

ALEXS_MOBS = {
    "format": 1,
    "id": "alexsmobs",
    "name": "Alex's Mobs",
    "requiresMod": ["alexsmobs"],
    "minecraft": {"min": "1.18.2"},
    "entries": AM_ENTRIES,
    "rewards": {"items": AM_REWARDS},
    "themes": AM_THEMES,
}

AM_FAMILY_NAMES = {
    "am_savanna": "Savanna", "am_northlands": "Northland", "am_cookout": "Bush Cook", "am_predators": "Predator",
    "am_gear": "Outfitter", "am_nether": "Nether Hunter",
}
