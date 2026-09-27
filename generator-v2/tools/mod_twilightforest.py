"""The Twilight Forest profile (LGPL-2.1 licensed mod by Benimatic and the TeamTwilight contributors).

Ids, names, recipes and drops were checked against the mod's data on GitHub: lang/en_us.json of the
1.18.x, 1.19.x, 1.20.1 and 1.21.1 branches, and the generated recipes, loot tables and advancements of
1.20.1 and 1.21.1. Only facts are used (ids, names, what drops what, which items have a grid or furnace
recipe); nothing is copied.

Everything but the portal trip happens inside the Twilight Forest dimension, which unlocks like the
Nether: once a share of the online players has the mod's root advancement (made a portal or entered),
or from game day 12.
"""
from catalog_dsl import E, T

TF = "twilightforest:"
DIM = TF + "twilight_forest"
REQ = ["dim:" + DIM]

TF_ENTRIES = [
    E("tf_trip", "tf_travel", 2, [
        T("visit_dimension", DIM, 4.0,
          hint="Throw a diamond into a two-by-two pool of water ringed with flowers or grass."),
    ], dim=DIM, requires=REQ),
    E("tf_wood", "tf_wood", 2, [
        T("mine_block", TF + "twilight_oak_log", 0.12, 16, 64),
        T("mine_block", TF + "canopy_log", 0.14, 16, 64, name="Canopy Tree Log"),
        T("mine_block", TF + "dark_log", 0.22, 8, 32, name="Darkwood Log",
          hint="Darkwood grows in the gloomy Dark Forest."),
        T("craft_item", TF + "twilight_oak_planks", 0.035, 16, 64, hint="One twilight oak log makes four planks."),
        T("craft_item", TF + "canopy_bookshelf", 3.2, 1, 4, minDifficulty="normal",
          hint="Six canopy planks and three books."),
    ], dim=DIM, tool="none", requires=REQ),
    E("tf_forage", "tf_forage", 2, [
        T("collect_item", TF + "torchberries", 0.45, 4, 16,
          hint="Ripe torchberries hang from cave ceilings in the Twilight Forest."),
        T("collect_item", TF + "liveroot", 0.55, 4, 16,
          hint="Liveroot grows in the root tangles underground; break it without Silk Touch."),
        T("collect_item", TF + "raven_feather", 0.9, 2, 8, hint="Forest ravens drop feathers."),
    ], dim=DIM, requires=REQ),
    E("tf_game", "tf_game", 2, [
        T("collect_item", TF + "raw_venison", 0.9, 2, 12, hint="Deer drop venison."),
        T("smelt_item", TF + "cooked_venison", 1.0, 2, 12, name="Venison Steak", plural="Venison Steaks",
          hint="Cook raw venison in a furnace, smoker or campfire."),
        T("consume_item", TF + "cooked_venison", 1.1, 2, 6, name="Venison Steak", plural="Venison Steaks",
          minDifficulty="normal"),
    ], dim=DIM, requires=REQ),
    E("tf_ranch", "tf_ranch", 2, [
        T("breed_animal", TF + "deer", 2.2, 2, 6, hint="Deer breed with wheat."),
        T("breed_animal", TF + "boar", 2.0, 2, 6, hint="Boars breed like pigs: carrots, potatoes or beetroots."),
        T("breed_animal", TF + "bighorn_sheep", 2.0, 2, 6, hint="Bighorn sheep breed with wheat."),
    ], dim=DIM, requires=REQ),
    E("tf_monsters", "tf_monsters", 3, [
        T("kill_mob", TF + "kobold", 0.8, 4, 12, hint="Kobolds roam the Twilight Forest in packs."),
        T("kill_mob", TF + "redcap", 1.0, 4, 12, hint="Redcaps lurk around the hollow hills."),
        T("kill_mob", TF + "skeleton_druid", 1.2, 2, 8, hint="Skeleton druids haunt the swamps and forests."),
        T("kill_mob", TF + "hostile_wolf", 1.0, 2, 8),
        T("kill_mob", TF + "hedge_spider", 1.0, 2, 8, hint="Hedge spiders hide in hedge mazes."),
        T("kill_mob", TF + "minotaur", 2.4, 1, 4, minDifficulty="normal",
          hint="Minotaurs guard the labyrinths below the swamp."),
    ], dim=DIM, requires=REQ),
    E("tf_bosses", "tf_bosses", 4, [
        T("kill_mob", TF + "naga", 18.0, 1, 1, minDifficulty="hard",
          hint="The Naga coils in a stone courtyard in the forest."),
        T("collect_item", TF + "naga_scale", 2.6, 6, 12, minDifficulty="hard", hint="Dropped by the Naga."),
        T("kill_mob", TF + "lich", 32.0, 1, 1, minDifficulty="hard", hint="The Lich rules the tallest tower."),
    ], dim=DIM, requires=REQ, weight=0.6),
    E("tf_ironwood", "tf_ironwood", 3, [
        T("craft_item", TF + "raw_ironwood", 1.6, 2, 8, hint="Liveroot, raw iron and a gold nugget."),
        T("smelt_item", TF + "ironwood_ingot", 1.8, 2, 8, minDifficulty="normal",
          hint="Smelt raw ironwood in a furnace."),
    ], dim=DIM, requires=REQ),
    E("tf_maps", "tf_maps", 3, [
        T("craft_item", TF + "magic_map_focus", 3.0, 1, 2,
          hint="A raven feather, torchberries and glowstone dust."),
        T("craft_item", TF + "magic_map", 4.0, 1, 1, name="Blank Magic Map", plural="Blank Magic Maps",
          minDifficulty="normal", hint="A magic map focus surrounded by paper."),
    ], dim=DIM, requires=REQ),
]

# tier 3: a reward may be one tier above its quest, so these stay off the earliest quests
TF_REWARDS = [
    {"id": TF + "torchberries", "value": 0.5, "tier": 3, "max": 16, "family": "tf_forage"},
    {"id": TF + "raven_feather", "value": 0.9, "tier": 3, "max": 8, "family": "tf_forage"},
    {"id": TF + "cooked_venison", "value": 1.1, "tier": 3, "max": 16, "family": "tf_game"},
    {"id": TF + "liveroot", "value": 0.6, "tier": 3, "max": 16, "family": "tf_forage"},
    {"id": TF + "ironwood_ingot", "value": 2.2, "tier": 3, "max": 8, "family": "tf_ironwood"},
    {"id": TF + "naga_scale", "value": 3.0, "tier": 4, "max": 6, "family": "tf_bosses", "minDifficulty": "hard"},
]

TF_THEMES = [
    {"key": "tf_expedition", "names": ["Twilight Expedition", "Into the Twilight", "Forest Beyond"],
     "descriptions": ["The Twilight Forest is waiting: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["tf_forage"]},
               {"types": ["mine_block"], "keys": ["tf_wood"]}]},
    {"key": "tf_hunters", "names": ["Twilight Hunt", "Forest Patrol", "Goblin Trouble"],
     "descriptions": ["Clear the paths of the Twilight Forest: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["kill_mob"], "keys": ["tf_monsters"]},
               {"types": ["kill_mob"], "keys": ["tf_monsters"]}]},
    {"key": "tf_ironwood_smith", "names": ["Ironwood Smith", "Living Metal", "Root and Iron"],
     "descriptions": ["Ironwood grows from roots and iron: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["tf_forage"]},
               {"types": ["craft_item", "smelt_item"], "keys": ["tf_ironwood"]}]},
    {"key": "tf_venison_feast", "names": ["Venison Feast", "Hunter's Supper"],
     "descriptions": ["Supper comes from the twilight woods: {list}."],
     "minDifficulty": "normal",
     "slots": [{"types": ["collect_item"], "keys": ["tf_game"]},
               {"types": ["smelt_item", "consume_item"], "keys": ["tf_game"]}]},
]

TWILIGHT_FOREST = {
    "format": 1,
    "id": "twilightforest",
    "name": "The Twilight Forest",
    "requiresMod": ["twilightforest"],
    "minecraft": {"min": "1.18.2"},
    "dimensions": [
        {"id": DIM, "advancement": TF + "root", "share": 0.25, "day": 12, "tier": 2},
    ],
    "entries": TF_ENTRIES,
    "rewards": {"items": TF_REWARDS},
    "themes": TF_THEMES,
}

TF_FAMILY_NAMES = {
    "tf_travel": "Twilight", "tf_wood": "Twilight Timber", "tf_forage": "Twilight Forager",
    "tf_game": "Venison", "tf_ranch": "Twilight Ranch", "tf_monsters": "Twilight Hunt", "tf_bosses": "Boss",
    "tf_ironwood": "Ironwood", "tf_maps": "Cartographer",
}

TF_DIMENSION_NAMES = {DIM: "the Twilight Forest"}
