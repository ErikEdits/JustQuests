"""Biomes O' Plenty profile (by Forstride, Adubbz and the Glitchfiend team; All Rights Reserved).

Ids, names and recipes were checked against the released jars from Modrinth for Minecraft 1.18.2, 1.20.1,
1.21.1 and 1.21.11 (lang/en_us.json, crafting and smelting recipes, block loot tables). Only ids and names
are used, which are facts; no code or assets of the mod are copied or shipped.

Most targets are wood, flowers and sand from the mod's biomes, so the entries carry the biome hints.
Rose quartz chunks drop from rose quartz clusters from 1.20 on.
"""
from catalog_dsl import E, T

B = "biomesoplenty:"
BIOME = "Biomes O' Plenty adds it in its own biomes."

BOP_ENTRIES = [
    E("bop_timber", "bop_timber", 0, [
        T("mine_block", B + "fir_log", 0.13, 16, 64, hint="Firs grow in coniferous forests."),
        T("mine_block", B + "redwood_log", 0.12, 16, 64, hint="Redwoods tower in redwood forests."),
        T("mine_block", B + "jacaranda_log", 0.16, 8, 48, hint="Jacarandas bloom in jacaranda glades."),
        T("mine_block", B + "mahogany_log", 0.16, 8, 48, hint="Mahogany grows in rainforests."),
        T("mine_block", B + "willow_log", 0.15, 8, 48, hint="Willows hang over bayous and marshes."),
        T("mine_block", B + "palm_log", 0.15, 8, 48, hint="Palms line tropical beaches."),
        T("mine_block", B + "dead_log", 0.18, 8, 32, hint="Dead trees stand in wastelands and dead forests."),
        T("mine_block", B + "magic_log", 0.45, 4, 16, hint="Magic trees glow blue in mystic groves."),
    ], hints=["rare"]),
    E("bop_carpentry", "bop_carpentry", 0, [
        T("craft_item", B + "fir_planks", 0.035, 16, 64, hint="One fir log makes four planks."),
        T("craft_item", B + "redwood_planks", 0.035, 16, 64, hint="One redwood log makes four planks."),
        T("craft_item", B + "jacaranda_planks", 0.045, 16, 64, hint="One jacaranda log makes four planks."),
        T("craft_item", B + "fir_boat", 1.2, 1, 2, hint="Five fir planks."),
        T("craft_item", B + "redwood_boat", 1.2, 1, 2, hint="Five redwood planks."),
    ]),
    E("bop_bouquet", "bop_bouquet", 0, [
        T("collect_item", B + "lavender", 0.35, 4, 16, hint="Lavender fields are purple from afar."),
        T("collect_item", B + "violet", 0.35, 4, 16, hint=BIOME),
        T("collect_item", B + "pink_daffodil", 0.4, 4, 16, hint=BIOME),
        T("collect_item", B + "goldenrod", 0.4, 4, 16, hint="Goldenrod grows in prairies and fields."),
        T("collect_item", B + "orange_cosmos", 0.4, 4, 16, hint=BIOME),
        T("collect_item", B + "blue_hydrangea", 0.45, 2, 12, hint=BIOME),
        T("collect_item", B + "pink_hibiscus", 0.5, 2, 12, hint="Hibiscus grows in tropical biomes."),
        T("collect_item", B + "glowflower", 0.6, 2, 8, hint="Glowflowers light up mystic groves."),
        T("collect_item", B + "wilted_lily", 0.6, 2, 8, hint="Wilted lilies droop in eerie, foggy biomes."),
    ]),
    E("bop_sands", "bop_sands", 0, [
        T("collect_item", B + "white_sand", 0.1, 16, 64, hint="White sand covers tropical beaches."),
        T("collect_item", B + "orange_sand", 0.1, 16, 64, hint="Orange sand covers lush deserts."),
        T("collect_item", B + "black_sand", 0.12, 16, 64, hint="Black sand covers volcanic beaches."),
        T("craft_item", B + "white_sandstone", 0.15, 8, 32, hint="Four white sand."),
        T("craft_item", B + "orange_sandstone", 0.15, 8, 32, hint="Four orange sand."),
        T("smelt_item", B + "smooth_white_sandstone", 0.25, 8, 32, hint="Smelt white sandstone in a furnace."),
    ], tool="none"),
    E("bop_wetlands", "bop_wetlands", 1, [
        T("collect_item", B + "cattail", 0.35, 4, 16, hint="Cattails grow at the edges of marshes."),
        T("collect_item", B + "reed", 0.35, 4, 16, hint="Reeds stand in shallow water."),
        T("collect_item", B + "spanish_moss", 0.4, 4, 16, hint="Spanish moss hangs from bayou trees."),
        T("collect_item", B + "watergrass", 0.4, 4, 16, hint="Watergrass grows under water in wetlands."),
        T("collect_item", B + "barley", 0.35, 4, 16, plural="Barley", hint="Wild barley grows on the prairie."),
        T("collect_item", B + "glowshroom", 0.8, 2, 8, hint="Glowshrooms light up glowing grottos and caves."),
    ], hints=["swamp"]),
    E("bop_rose_quartz", "bop_rose_quartz", 2, [
        T("mine_block", B + "rose_quartz_cluster", 1.4, 2, 8,
          hint="Rose quartz clusters grow in crystal caves underground."),
        T("collect_item", B + "rose_quartz_chunk", 1.0, 4, 16, since="1.20",
          hint="Rose quartz clusters drop chunks."),
        T("craft_item", B + "rose_quartz_block", 4.5, 1, 4, since="1.20", minDifficulty="normal",
          hint="Four rose quartz chunks."),
    ], hints=["caves", "rare"]),
    E("bop_nether", "bop_nether", 3, [
        T("mine_block", B + "brimstone", 0.25, 8, 32, hint="Brimstone crusts the fumaroles of Nether biomes."),
        T("collect_item", B + "flesh", 0.35, 8, 32, plural="Flesh", hint="Flesh covers the Visceral Heap."),
        T("mine_block", B + "hellbark_log", 0.3, 8, 32, hint="Hellbark grows in the Nether's undergrowth."),
        T("collect_item", B + "burning_blossom", 0.6, 2, 8, hint="Burning blossoms grow in the Nether."),
    ], dim="minecraft:the_nether"),
]

BOP_REWARDS = [
    {"id": B + "white_sand", "value": 0.1, "tier": 0, "max": 64, "family": "bop_sands"},
    {"id": B + "magic_log", "value": 0.5, "tier": 1, "max": 16, "family": "bop_timber"},
    {"id": B + "glowshroom", "value": 0.8, "tier": 1, "max": 8, "family": "bop_wetlands"},
    {"id": B + "lavender", "value": 0.35, "tier": 0, "max": 16, "family": "bop_bouquet"},
    {"id": B + "rose_quartz_chunk", "value": 1.2, "tier": 2, "max": 8, "family": "bop_rose_quartz"},
]

BOP_THEMES = [
    {"key": "bop_explorer", "names": ["Biome Hopper", "Plenty to See", "Wanderlust"],
     "descriptions": ["The world got bigger: {list}."],
     "slots": [{"types": ["mine_block"], "keys": ["bop_timber"]},
               {"types": ["collect_item"], "keys": ["bop_bouquet", "bop_wetlands"]}]},
    {"key": "bop_florist", "names": ["Wild Bouquet", "Meadow Colours", "Flower Hunt"],
     "descriptions": ["Gather the wild flowers: {list}."],
     "slots": [{"types": ["collect_item"], "keys": ["bop_bouquet"]},
               {"types": ["collect_item"], "keys": ["bop_bouquet"]}]},
    {"key": "bop_carpenter", "names": ["Exotic Carpenter", "Fine Timber"],
     "descriptions": ["Exotic wood for the workshop: {list}."],
     "slots": [{"types": ["mine_block"], "keys": ["bop_timber"]},
               {"types": ["craft_item"], "keys": ["bop_carpentry"]}]},
]

BIOMES_O_PLENTY = {
    "format": 1,
    "id": "biomesoplenty",
    "name": "Biomes O' Plenty",
    "requiresMod": ["biomesoplenty"],
    "minecraft": {"min": "1.18.2"},
    "entries": BOP_ENTRIES,
    "rewards": {"items": BOP_REWARDS},
    "themes": BOP_THEMES,
}

BOP_FAMILY_NAMES = {
    "bop_timber": "Exotic Timber", "bop_carpentry": "Carpenter", "bop_bouquet": "Wildflower", "bop_sands": "Sand",
    "bop_wetlands": "Wetland", "bop_rose_quartz": "Rose Quartz", "bop_nether": "Nether Botanist",
}
