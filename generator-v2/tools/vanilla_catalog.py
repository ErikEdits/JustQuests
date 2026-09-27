from catalog_dsl import T, E

OW = "minecraft:overworld"
NE = "minecraft:the_nether"
EN = "minecraft:the_end"

def wood(key, log, tier=0, hints=None, since=None, weight=1.0):
    kw = {}
    if since: kw["since"] = since
    return E(key, "wood", tier, [
        T("mine_block", log, 0.10, 16, 64, tool="none"),
        T("collect_item", log, 0.10, 16, 64),
    ], tool="none", hints=hints or [], weight=weight, **kw)

ENTRIES = [
    # ------------------------------------------------------------ wood
    E("logs_any", "wood", 0, [
        T("collect_item", "minecraft:oak_log", 0.09, 16, 96, tag="logs", hint="Chop any kind of tree."),
    ], notes="Uses the item tag #minecraft:logs where the build allows tags; falls back to oak logs."),
    wood("oak_wood", "minecraft:oak_log"),
    wood("birch_wood", "minecraft:birch_log"),
    wood("spruce_wood", "minecraft:spruce_log", hints=["taiga"]),
    wood("jungle_wood", "minecraft:jungle_log", hints=["jungle"]),
    wood("acacia_wood", "minecraft:acacia_log", hints=["savanna"]),
    wood("dark_oak_wood", "minecraft:dark_oak_log", hints=["dark_forest"]),
    wood("mangrove_wood", "minecraft:mangrove_log", hints=["mangrove_swamp"], since="1.19"),
    wood("cherry_wood", "minecraft:cherry_log", hints=["cherry_grove"], since="1.20", weight=0.8),
    wood("pale_oak_wood", "minecraft:pale_oak_log", hints=["pale_garden"], since="1.21.4", weight=0.6),
    E("saplings", "forestry", 0, [
        T("place_block", "minecraft:oak_sapling", 0.18, 8, 32, hint="Plant saplings to regrow the forest."),
        T("place_block", "minecraft:birch_sapling", 0.18, 8, 32, hint="Plant saplings to regrow the forest."),
        T("place_block", "minecraft:spruce_sapling", 0.2, 8, 32, hints=["taiga"]),
    ]),
    E("charcoal", "wood", 0, [
        T("smelt_item", "minecraft:charcoal", 0.15, 8, 64, hint="Smelt logs in a furnace."),
    ]),
    # ------------------------------------------------------------ stone & soil
    E("cobblestone", "stone", 0, [
        T("collect_item", "minecraft:cobblestone", 0.04, 32, 128, hint="Mine stone with a pickaxe."),
        T("mine_block", "minecraft:stone", 0.045, 32, 128),
        T("place_block", "minecraft:cobblestone", 0.05, 32, 128, hint="Build something with it."),
    ], tool="wood"),
    E("smooth_stone", "stone", 0, [
        T("smelt_item", "minecraft:stone", 0.08, 16, 64, hint="Smelt cobblestone in a furnace."),
        T("smelt_item", "minecraft:smooth_stone", 0.15, 8, 64, hint="Smelt stone a second time."),
    ]),
    E("stone_bricks", "stonework", 0, [
        T("craft_item", "minecraft:stone_bricks", 0.13, 16, 64, hint="Craft from smelted stone."),
        T("place_block", "minecraft:stone_bricks", 0.15, 16, 64),
    ]),
    E("andesite", "stone", 0, [T("mine_block", "minecraft:andesite", 0.05, 32, 96)], tool="wood"),
    E("granite", "stone", 0, [T("mine_block", "minecraft:granite", 0.05, 32, 96)], tool="wood"),
    E("diorite", "stone", 0, [T("mine_block", "minecraft:diorite", 0.05, 32, 96)], tool="wood"),
    E("deepslate", "stone", 1, [
        T("mine_block", "minecraft:deepslate", 0.08, 32, 96),
        T("collect_item", "minecraft:cobbled_deepslate", 0.08, 32, 96),
        T("smelt_item", "minecraft:deepslate", 0.12, 16, 64, hint="Smelt cobbled deepslate."),
    ], tool="wood", hints=["deep"], since="1.17"),
    E("tuff", "stone", 1, [T("mine_block", "minecraft:tuff", 0.08, 16, 64)], tool="wood", hints=["deep"]),
    E("calcite", "stone", 1, [T("mine_block", "minecraft:calcite", 0.25, 8, 32)], tool="wood", hints=["geode"]),
    E("dripstone", "stone", 1, [T("mine_block", "minecraft:dripstone_block", 0.15, 16, 48)], tool="wood",
      hints=["dripstone_caves"]),
    E("sandstone", "sand", 0, [
        T("mine_block", "minecraft:sandstone", 0.06, 16, 64),
        T("smelt_item", "minecraft:smooth_sandstone", 0.12, 16, 64, hint="Smelt sandstone."),
    ], tool="wood", hints=["desert"]),
    E("sand", "sand", 0, [
        T("collect_item", "minecraft:sand", 0.04, 32, 128, hint="Dig sand with a shovel."),
        T("mine_block", "minecraft:sand", 0.04, 32, 128),
    ], hints=["beach"]),
    E("red_sand", "sand", 0, [T("collect_item", "minecraft:red_sand", 0.06, 16, 96)], hints=["badlands"]),
    E("glass", "glass", 0, [
        T("smelt_item", "minecraft:glass", 0.1, 16, 64, hint="Smelt sand in a furnace."),
        T("place_block", "minecraft:glass", 0.12, 16, 64),
        T("craft_item", "minecraft:glass_pane", 0.05, 16, 64, hint="Craft panes from smelted glass."),
    ]),
    E("dirt", "soil", 0, [T("collect_item", "minecraft:dirt", 0.03, 32, 128)]),
    E("gravel", "soil", 0, [
        T("mine_block", "minecraft:gravel", 0.05, 32, 96),
        T("collect_item", "minecraft:flint", 0.4, 4, 32, hint="Gravel sometimes drops flint."),
    ]),
    E("clay", "soil", 0, [
        T("collect_item", "minecraft:clay_ball", 0.08, 16, 64, hint="Dig clay in rivers and swamps."),
        T("smelt_item", "minecraft:brick", 0.12, 8, 64, hint="Smelt clay balls."),
        T("smelt_item", "minecraft:terracotta", 0.35, 8, 32, hint="Smelt clay blocks."),
    ], hints=["river"]),
    E("bricks", "stonework", 0, [
        T("craft_item", "minecraft:bricks", 0.9, 4, 16, hint="Four bricks make a brick block."),
        T("place_block", "minecraft:bricks", 0.95, 4, 16),
        T("craft_item", "minecraft:flower_pot", 0.4, 2, 8),
    ]),
    E("terracotta", "badlands", 0, [T("mine_block", "minecraft:terracotta", 0.07, 16, 64)], tool="wood",
      hints=["badlands"]),
    E("mud", "soil", 0, [T("mine_block", "minecraft:mud", 0.05, 16, 64)], hints=["mangrove_swamp"], since="1.19"),
    E("moss", "lush", 1, [T("mine_block", "minecraft:moss_block", 0.12, 16, 48)], hints=["lush_caves"], since="1.17"),
    E("snow", "snow", 0, [
        T("collect_item", "minecraft:snowball", 0.03, 16, 64, hint="Dig snow with a shovel."),
        T("mine_block", "minecraft:ice", 0.12, 8, 48),
        T("mine_block", "minecraft:packed_ice", 0.2, 8, 32, hints=["rare"]),
    ], tool="none", hints=["snowy"]),
    E("grass", "plants", 0, [
        T("mine_block", "minecraft:short_grass", 0.02, 32, 128, alt=["minecraft:grass"], name="Grass",
          hint="Break tall grass anywhere."),
        T("collect_item", "minecraft:wheat_seeds", 0.05, 16, 64, hint="Break grass to find seeds."),
    ], notes="short_grass was called grass before 1.20.3; the alternative id covers older builds."),
    # ------------------------------------------------------------ coal & metals
    E("coal", "coal", 0, [
        T("mine_block", "minecraft:coal_ore", 0.25, 8, 64),
        T("collect_item", "minecraft:coal", 0.25, 8, 64, hint="Mine coal ore."),
    ], tool="wood", hints=["caves"]),
    E("copper", "metals", 1, [
        T("mine_block", "minecraft:copper_ore", 0.3, 8, 48),
        T("collect_item", "minecraft:raw_copper", 0.1, 16, 96, hint="Copper ore drops raw copper."),
        T("smelt_item", "minecraft:copper_ingot", 0.13, 16, 64, hint="Smelt raw copper in a furnace."),
    ], tool="stone", hints=["caves"], since="1.17"),
    E("iron", "metals", 1, [
        T("mine_block", "minecraft:iron_ore", 0.5, 4, 48),
        T("mine_block", "minecraft:deepslate_iron_ore", 0.6, 4, 32, hints=["deep"]),
        T("collect_item", "minecraft:raw_iron", 0.5, 4, 48, hint="Iron ore drops raw iron."),
        T("smelt_item", "minecraft:iron_ingot", 0.55, 4, 48, hint="Smelt raw iron in a furnace."),
    ], tool="stone", hints=["caves"],
      exclusions=["collect_item minecraft:iron_ore (drops raw_iron, needs Silk Touch)"]),
    E("gold", "metals", 2, [
        T("mine_block", "minecraft:gold_ore", 1.2, 2, 24),
        T("mine_block", "minecraft:deepslate_gold_ore", 1.2, 2, 24, hints=["deep"]),
        T("collect_item", "minecraft:raw_gold", 1.2, 2, 24, hint="Gold ore drops raw gold."),
        T("smelt_item", "minecraft:gold_ingot", 1.3, 2, 24, hint="Smelt raw gold in a furnace."),
    ], tool="iron", hints=["deep"],
      exclusions=["collect_item minecraft:gold_ore (drops raw_gold, needs Silk Touch)"]),
    E("lapis", "gems", 2, [
        T("mine_block", "minecraft:lapis_ore", 1.2, 2, 16),
        T("collect_item", "minecraft:lapis_lazuli", 0.2, 8, 64, hint="Lapis ore drops several pieces."),
    ], tool="stone", hints=["deep"]),
    E("redstone", "redstone", 2, [
        T("mine_block", "minecraft:redstone_ore", 0.9, 4, 24),
        T("collect_item", "minecraft:redstone", 0.2, 8, 64, name="Redstone Dust", hint="Redstone ore drops dust."),
    ], tool="iron", hints=["deep"]),
    E("diamond", "gems", 2, [
        T("mine_block", "minecraft:deepslate_diamond_ore", 5.0, 1, 4),
        T("collect_item", "minecraft:diamond", 5.0, 1, 4, hint="Dig deep, around y=-58."),
    ], tool="iron", hints=["deep"], exclusions=["mine_block minecraft:diamond_ore (rare above y=0; deepslate variant used)"]),
    E("emerald", "gems", 2, [
        T("mine_block", "minecraft:emerald_ore", 7.0, 1, 3, minDifficulty="hard"),
    ], tool="iron", hints=["mountains", "rare"],
      exclusions=["collect_item minecraft:emerald (villager trades do not count as pickups)"]),
    E("amethyst", "amethyst", 1, [
        T("collect_item", "minecraft:amethyst_shard", 0.35, 4, 32, hint="Break amethyst clusters."),
    ], tool="wood", hints=["geode"], since="1.17"),
    E("obsidian", "obsidian", 2, [
        T("mine_block", "minecraft:obsidian", 1.0, 2, 16, hint="Pour water on lava pools, then mine it."),
    ], tool="diamond"),
    # ------------------------------------------------------------ plants & crops
    E("wheat", "crops", 0, [
        T("collect_item", "minecraft:wheat", 0.15, 8, 64, hint="Harvest fully grown wheat."),
        T("craft_item", "minecraft:hay_block", 1.4, 2, 12, hint="Nine wheat make a hay bale."),
        T("place_block", "minecraft:hay_block", 1.45, 2, 12),
    ], hints=["farm"], exclusions=["mine_block minecraft:wheat (breaking crops at any stage counts)"]),
    E("carrots", "crops", 0, [
        T("collect_item", "minecraft:carrot", 0.15, 8, 64),
        T("consume_item", "minecraft:carrot", 0.45, 4, 8),
    ], hints=["village", "farm"]),
    E("potatoes", "crops", 0, [
        T("collect_item", "minecraft:potato", 0.15, 8, 64),
        T("smelt_item", "minecraft:baked_potato", 0.2, 8, 48, hint="Bake potatoes in a furnace or smoker.", hints=["cook"]),
        T("consume_item", "minecraft:baked_potato", 0.75, 2, 8),
    ], hints=["village", "farm"]),
    E("beetroot", "crops", 0, [
        T("collect_item", "minecraft:beetroot", 0.2, 8, 48),
        T("consume_item", "minecraft:beetroot_soup", 2.0, 1, 3, hint="Six beetroots make a soup."),
    ], hints=["village", "farm"]),
    E("melon", "fruit", 0, [
        T("collect_item", "minecraft:melon_slice", 0.06, 16, 96),
        T("mine_block", "minecraft:melon", 0.4, 4, 24),
        T("consume_item", "minecraft:melon_slice", 0.28, 4, 16),
    ], hints=["jungle"]),
    E("pumpkin", "fruit", 0, [
        T("collect_item", "minecraft:pumpkin", 0.4, 4, 24),
        T("craft_item", "minecraft:pumpkin_pie", 1.0, 2, 8, hint="Pumpkin, sugar and an egg."),
        T("consume_item", "minecraft:pumpkin_pie", 1.9, 1, 4),
    ], hints=["plains"]),
    E("apple", "fruit", 0, [
        T("collect_item", "minecraft:apple", 2.0, 1, 4, hint="Oak and dark oak leaves sometimes drop apples."),
        T("consume_item", "minecraft:apple", 2.4, 1, 3),
    ], weight=0.5, notes="~0.5% leaf drop: counts kept tiny."),
    E("sugar_cane", "plants", 0, [
        T("collect_item", "minecraft:sugar_cane", 0.1, 16, 64),
        T("craft_item", "minecraft:paper", 0.1, 8, 64, hint="Three sugar cane make three paper."),
    ], hints=["river"]),
    E("cactus", "plants", 0, [
        T("collect_item", "minecraft:cactus", 0.12, 8, 64),
        T("smelt_item", "minecraft:green_dye", 0.16, 8, 48, hint="Smelt cactus."),
    ], hints=["desert"]),
    E("bamboo", "plants", 0, [
        T("collect_item", "minecraft:bamboo", 0.05, 32, 128),
        T("craft_item", "minecraft:scaffolding", 0.12, 8, 64, hint="Bamboo and string."),
    ], hints=["jungle"]),
    E("kelp", "ocean", 0, [
        T("collect_item", "minecraft:kelp", 0.06, 16, 96),
        T("smelt_item", "minecraft:dried_kelp", 0.1, 16, 64, hint="Smelt kelp.", hints=["cook"]),
        T("consume_item", "minecraft:dried_kelp", 0.22, 8, 16),
    ], hints=["ocean"]),
    E("sweet_berries", "fruit", 0, [
        T("collect_item", "minecraft:sweet_berries", 0.12, 16, 64),
        T("consume_item", "minecraft:sweet_berries", 0.35, 4, 16),
    ], hints=["taiga"]),
    E("glow_berries", "lush", 1, [
        T("collect_item", "minecraft:glow_berries", 0.25, 8, 48),
        T("consume_item", "minecraft:glow_berries", 0.5, 4, 8),
    ], hints=["lush_caves"], since="1.17"),
    E("cocoa", "plants", 0, [T("collect_item", "minecraft:cocoa_beans", 0.3, 8, 32)], hints=["jungle"]),
    E("mushrooms", "mushrooms", 0, [
        T("collect_item", "minecraft:brown_mushroom", 0.3, 4, 32),
        T("collect_item", "minecraft:red_mushroom", 0.35, 4, 32),
        T("consume_item", "minecraft:mushroom_stew", 1.4, 1, 4, hint="A bowl with a red and a brown mushroom."),
    ], hints=["dark_forest"]),
    E("flowers", "flowers", 0, [
        T("collect_item", "minecraft:dandelion", 0.1, 8, 32, tag="small_flowers", hint="Pick flowers."),
        T("collect_item", "minecraft:poppy", 0.1, 8, 32),
        T("collect_item", "minecraft:cornflower", 0.25, 4, 24, hints=["flower_forest"]),
        T("collect_item", "minecraft:lily_of_the_valley", 0.35, 4, 16, hints=["flower_forest"]),
    ]),
    E("lily_pad", "plants", 0, [T("collect_item", "minecraft:lily_pad", 0.2, 4, 32)], hints=["swamp"]),
    E("bees", "bees", 0, [
        T("collect_item", "minecraft:honeycomb", 1.0, 3, 16, tool="shears", hint="Shear a full bee nest."),
        T("consume_item", "minecraft:honey_bottle", 1.8, 1, 4, hints=["drink"], hint="Use a glass bottle on a full hive."),
        T("craft_item", "minecraft:candle", 1.2, 1, 6),
        T("breed_animal", "minecraft:bee", 1.5, 2, 8, hint="Bees breed with flowers."),
    ], hints=["bees"]),
    # ------------------------------------------------------------ farm animals
    E("cows", "livestock", 0, [
        T("breed_animal", "minecraft:cow", 1.0, 2, 12, hint="Cows breed with wheat."),
        T("collect_item", "minecraft:beef", 0.3, 8, 48, name="Raw Beef"),
        T("collect_item", "minecraft:leather", 0.4, 4, 32),
        T("smelt_item", "minecraft:cooked_beef", 0.4, 8, 48, name="Steak", hint="Cook raw beef.", hints=["cook"]),
        T("consume_item", "minecraft:cooked_beef", 1.25, 2, 6, name="Steak"),
    ], hints=["plains"]),
    E("pigs", "livestock", 0, [
        T("breed_animal", "minecraft:pig", 1.0, 2, 12, hint="Pigs breed with carrots, potatoes or beetroots."),
        T("collect_item", "minecraft:porkchop", 0.3, 8, 48, name="Raw Porkchop"),
        T("smelt_item", "minecraft:cooked_porkchop", 0.4, 8, 48, hint="Cook raw porkchops.", hints=["cook"]),
        T("consume_item", "minecraft:cooked_porkchop", 1.25, 2, 6),
    ]),
    E("chickens", "livestock", 0, [
        T("breed_animal", "minecraft:chicken", 0.8, 2, 12, hint="Chickens breed with seeds."),
        T("collect_item", "minecraft:feather", 0.3, 8, 32),
        T("collect_item", "minecraft:egg", 0.7, 4, 16, stack=16, hint="Chickens lay eggs over time."),
        T("smelt_item", "minecraft:cooked_chicken", 0.4, 8, 48, hint="Cook raw chicken.", hints=["cook"]),
    ]),
    E("sheep", "wool", 0, [
        T("breed_animal", "minecraft:sheep", 1.0, 2, 12, hint="Sheep breed with wheat."),
        T("collect_item", "minecraft:white_wool", 0.25, 8, 64, tag="wool", tool="shears", hint="Shear sheep."),
        T("collect_item", "minecraft:mutton", 0.35, 8, 32, name="Raw Mutton"),
        T("smelt_item", "minecraft:cooked_mutton", 0.45, 8, 32, hints=["cook"]),
        T("place_block", "minecraft:white_wool", 0.3, 8, 32),
    ]),
    E("rabbits", "wild_animals", 0, [
        T("breed_animal", "minecraft:rabbit", 2.0, 2, 8, hint="Rabbits breed with carrots or dandelions."),
        T("collect_item", "minecraft:rabbit_hide", 1.0, 2, 12),
        T("consume_item", "minecraft:rabbit_stew", 4.0, 1, 2),
    ], hints=["desert"]),
    E("goats", "wild_animals", 0, [T("breed_animal", "minecraft:goat", 2.0, 2, 8, hint="Goats breed with wheat.")],
      hints=["mountains"], since="1.17"),
    E("llamas", "wild_animals", 0, [T("breed_animal", "minecraft:llama", 2.5, 2, 6, hint="Llamas breed with hay bales.")],
      hints=["savanna"], exclusions=["tame_animal minecraft:llama (not counted on Fabric)"]),
    E("turtles", "wild_animals", 0, [T("breed_animal", "minecraft:turtle", 3.0, 2, 6, hint="Turtles breed with seagrass.")],
      hints=["beach"]),
    E("frogs", "wild_animals", 1, [T("breed_animal", "minecraft:frog", 2.5, 2, 6, hint="Frogs breed with slimeballs.")],
      hints=["swamp"], since="1.19"),
    E("foxes", "wild_animals", 0, [T("breed_animal", "minecraft:fox", 4.0, 2, 4, minDifficulty="normal",
      hint="Sneak up on foxes with sweet berries.")], hints=["taiga"]),
    E("armadillos", "wild_animals", 0, [T("breed_animal", "minecraft:armadillo", 3.0, 2, 6,
      hint="Armadillos breed with spider eyes.")], hints=["savanna"], since="1.20.5"),
    E("camels", "wild_animals", 0, [T("breed_animal", "minecraft:camel", 4.0, 2, 4, hint="Camels breed with cactus.")],
      hints=["desert", "village"], since="1.20"),
    E("horses", "horses", 1, [
        T("breed_animal", "minecraft:horse", 4.0, 2, 4, hint="Horses breed with golden carrots or apples."),
        T("breed_animal", "minecraft:donkey", 4.0, 2, 4, hint="Donkeys breed with golden carrots or apples."),
    ], hints=["plains"], exclusions=["tame_animal minecraft:horse (horses/donkeys/llamas are not counted on Fabric)"]),
    E("wolves", "pets", 0, [
        T("tame_animal", "minecraft:wolf", 3.0, 1, 3, tamable=True, hint="Tame wolves with bones."),
        T("breed_animal", "minecraft:wolf", 1.5, 2, 6, hint="Tamed wolves breed with meat."),
    ], hints=["taiga"]),
    E("cats", "pets", 0, [
        T("tame_animal", "minecraft:cat", 4.0, 1, 2, tamable=True, hint="Tame stray cats with raw cod or salmon."),
        T("breed_animal", "minecraft:cat", 2.0, 2, 4, hint="Tamed cats breed with raw fish."),
    ], hints=["village"], exclusions=["tame_animal minecraft:ocelot (ocelots only trust players; not a TamableAnimal)"]),
    E("parrots", "pets", 0, [
        T("tame_animal", "minecraft:parrot", 6.0, 1, 2, tamable=True, hint="Tame parrots with seeds."),
    ], hints=["jungle"]),
    E("villagers", "villagers", 0, [T("kill_mob", "minecraft:zombie_villager", 5.0, 1, 2, minDifficulty="hard")],
      hints=["rare", "night"], exclusions=["breed_animal minecraft:villager (not an Animal; not counted on Fabric)"],
      weight=0.3),
    # ------------------------------------------------------------ fishing & ocean
    E("fishing", "fishing", 0, [
        T("collect_item", "minecraft:cod", 0.8, 4, 24, name="Raw Cod", tool="fishing_rod"),
        T("collect_item", "minecraft:salmon", 1.4, 2, 16, name="Raw Salmon", tool="fishing_rod"),
        T("smelt_item", "minecraft:cooked_cod", 0.9, 4, 16, hints=["cook"]),
        T("smelt_item", "minecraft:cooked_salmon", 1.5, 2, 12, hints=["cook"]),
        T("consume_item", "minecraft:cooked_cod", 1.5, 2, 6),
    ], tool="fishing_rod"),
    E("squid", "ocean", 0, [
        T("collect_item", "minecraft:ink_sac", 0.5, 4, 24, hint="Squid drop ink sacs."),
        T("collect_item", "minecraft:glow_ink_sac", 1.0, 2, 12, tier=1, hint="Glow squid live in dark water."),
    ], hints=["ocean"]),
    # ------------------------------------------------------------ monsters
    E("zombies", "undead", 0, [
        T("kill_mob", "minecraft:zombie", 0.7, 4, 24),
        T("collect_item", "minecraft:rotten_flesh", 0.5, 8, 32),
    ], hints=["night"]),
    E("skeletons", "undead", 0, [
        T("kill_mob", "minecraft:skeleton", 0.9, 4, 20),
        T("collect_item", "minecraft:bone", 0.6, 8, 32),
        T("collect_item", "minecraft:arrow", 0.6, 8, 32, hint="Skeletons drop arrows."),
    ], hints=["night"]),
    E("husks", "undead", 0, [T("kill_mob", "minecraft:husk", 1.0, 4, 16)], hints=["desert", "night"]),
    E("strays", "undead", 1, [T("kill_mob", "minecraft:stray", 1.2, 4, 12)], hints=["snowy", "night"]),
    E("drowned", "undead", 0, [T("kill_mob", "minecraft:drowned", 1.2, 4, 12)], hints=["ocean", "night"]),
    E("bogged", "undead", 1, [T("kill_mob", "minecraft:bogged", 2.0, 2, 8)], hints=["swamp"], since="1.21"),
    E("spiders", "arthropods", 0, [
        T("kill_mob", "minecraft:spider", 0.8, 4, 20),
        T("collect_item", "minecraft:string", 0.5, 8, 32),
        T("collect_item", "minecraft:spider_eye", 1.2, 2, 12),
    ], hints=["night"]),
    E("cave_spiders", "arthropods", 1, [T("kill_mob", "minecraft:cave_spider", 1.5, 4, 12)], hints=["mineshaft"]),
    E("creepers", "monsters", 0, [
        T("kill_mob", "minecraft:creeper", 1.0, 3, 16),
        T("collect_item", "minecraft:gunpowder", 1.0, 3, 16),
        T("craft_item", "minecraft:tnt", 3.0, 1, 4, name="TNT", plural="TNT", hint="Five gunpowder and four sand."),
    ], hints=["night"]),
    E("endermen", "monsters", 2, [
        T("kill_mob", "minecraft:enderman", 2.5, 2, 8),
        T("collect_item", "minecraft:ender_pearl", 3.0, 1, 6, stack=16),
    ], hints=["night"]),
    E("slimes", "monsters", 1, [
        T("kill_mob", "minecraft:slime", 1.5, 4, 16),
        T("collect_item", "minecraft:slime_ball", 1.5, 2, 16, name="Slimeball"),
    ], hints=["swamp"]),
    E("witches", "monsters", 1, [T("kill_mob", "minecraft:witch", 5.0, 1, 3, minDifficulty="normal")],
      hints=["swamp", "rare"]),
    E("pillagers", "illagers", 1, [T("kill_mob", "minecraft:pillager", 2.5, 2, 8, minDifficulty="normal")],
      hints=["outpost"]),
    E("guardians", "ocean", 2, [
        T("kill_mob", "minecraft:guardian", 2.5, 2, 8),
        T("collect_item", "minecraft:prismarine_shard", 1.5, 4, 16),
    ], hints=["monument"], tool="iron"),
    E("breezes", "trial", 2, [T("kill_mob", "minecraft:breeze", 5.0, 1, 3, minDifficulty="hard")],
      hints=["trial_chambers"], since="1.21"),
    # ------------------------------------------------------------ crafting
    E("torches", "lighting", 0, [T("craft_item", "minecraft:torch", 0.07, 16, 128, hint="Coal or charcoal on a stick.")],
      exclusions=["place_block minecraft:torch (wall torches are a different block id)"]),
    E("lanterns", "lighting", 1, [
        T("craft_item", "minecraft:lantern", 0.8, 2, 8),
        T("place_block", "minecraft:lantern", 0.85, 2, 8),
    ]),
    E("campfires", "lighting", 0, [T("craft_item", "minecraft:campfire", 0.5, 2, 8)]),
    E("woodwork", "woodwork", 0, [
        T("craft_item", "minecraft:chest", 0.35, 2, 16),
        T("craft_item", "minecraft:barrel", 0.35, 2, 16),
        T("craft_item", "minecraft:ladder", 0.06, 8, 64),
        T("craft_item", "minecraft:composter", 0.3, 2, 8),
        T("place_block", "minecraft:oak_planks", 0.04, 32, 128, hint="Build with oak planks."),
    ]),
    E("furnaces", "stonework", 0, [
        T("craft_item", "minecraft:furnace", 0.35, 2, 12),
        T("craft_item", "minecraft:smoker", 0.8, 1, 6),
        T("craft_item", "minecraft:blast_furnace", 3.0, 1, 3, tier=1),
    ]),
    E("baking", "baking", 0, [
        T("craft_item", "minecraft:bread", 0.45, 4, 32, hint="Three wheat make a loaf."),
        T("consume_item", "minecraft:bread", 1.0, 2, 8),
        T("craft_item", "minecraft:cookie", 0.1, 8, 64, hint="Wheat and cocoa beans."),
        T("consume_item", "minecraft:cookie", 0.32, 4, 16),
        T("craft_item", "minecraft:cake", 3.0, 1, 2, tier=1, hint="Needs milk, sugar, wheat and an egg."),
    ], exclusions=["consume_item minecraft:cake (cake is eaten as a placed block, not a use action)"]),
    E("library", "library", 0, [
        T("craft_item", "minecraft:book", 1.2, 2, 8),
        T("craft_item", "minecraft:bookshelf", 4.5, 1, 4, hint="Each shelf needs three books."),
        T("place_block", "minecraft:bookshelf", 4.6, 1, 4),
    ], notes="v1 underestimated bookshelves; each needs three books (paper + leather)."),
    E("archery", "archery", 0, [
        T("craft_item", "minecraft:bow", 1.0, 1, 3),
        T("craft_item", "minecraft:arrow", 0.15, 8, 64, hint="Flint, stick and feather."),
        T("craft_item", "minecraft:fishing_rod", 1.0, 1, 3),
    ]),
    E("iron_tools", "iron_tools", 1, [
        T("craft_item", "minecraft:iron_pickaxe", 1.8, 1, 2),
        T("craft_item", "minecraft:iron_axe", 1.8, 1, 2),
        T("craft_item", "minecraft:iron_shovel", 0.7, 1, 3),
        T("craft_item", "minecraft:iron_sword", 1.2, 1, 2),
        T("craft_item", "minecraft:shears", 1.1, 1, 3),
        T("craft_item", "minecraft:shield", 1.2, 1, 2),
    ]),
    E("iron_armor", "iron_armor", 1, [
        T("craft_item", "minecraft:iron_helmet", 2.9, 1, 1),
        T("craft_item", "minecraft:iron_chestplate", 4.5, 1, 1),
        T("craft_item", "minecraft:iron_leggings", 4.0, 1, 1),
        T("craft_item", "minecraft:iron_boots", 2.3, 1, 1),
    ]),
    E("iron_works", "iron_works", 1, [
        T("craft_item", "minecraft:bucket", 1.6, 1, 3),
        T("craft_item", "minecraft:iron_chain", 0.7, 2, 16, alt=["minecraft:chain"]),
        T("craft_item", "minecraft:iron_bars", 0.25, 8, 32),
        T("craft_item", "minecraft:rail", 0.25, 16, 64),
        T("place_block", "minecraft:rail", 0.3, 16, 64, hint="Build a railway."),
        T("craft_item", "minecraft:minecart", 2.6, 1, 2),
        T("craft_item", "minecraft:hopper", 3.5, 1, 2),
    ], notes="minecraft:chain was renamed to iron_chain in 1.21.9; both ids are tried."),
    E("milk", "iron_works", 1, [T("consume_item", "minecraft:milk_bucket", 0.6, 1, 4, hints=["drink"], hint="Milk a cow with a bucket.")]),
    E("redstone_gadgets", "redstone", 2, [
        T("craft_item", "minecraft:piston", 1.2, 2, 8),
        T("craft_item", "minecraft:compass", 2.4, 1, 1),
        T("craft_item", "minecraft:clock", 5.0, 1, 1),
    ]),
    E("decor", "decor", 0, [
        T("craft_item", "minecraft:item_frame", 0.7, 2, 8),
        T("craft_item", "minecraft:painting", 0.6, 2, 8),
        T("craft_item", "minecraft:white_bed", 1.0, 1, 4, tag="beds", hint="Three wool and three planks."),
        T("craft_item", "minecraft:armor_stand", 0.8, 1, 4),
    ]),
    E("copper_works", "copper_works", 1, [
        T("craft_item", "minecraft:spyglass", 2.0, 1, 2),
        T("craft_item", "minecraft:lightning_rod", 0.4, 2, 8),
    ], since="1.17"),
    E("golden_food", "golden_food", 2, [
        T("craft_item", "minecraft:golden_carrot", 1.5, 2, 12),
        T("consume_item", "minecraft:golden_carrot", 2.2, 1, 4),
        T("craft_item", "minecraft:golden_apple", 10.0, 1, 2, minDifficulty="hard"),
    ]),
    # ------------------------------------------------------------ diamond gear (hard)
    E("diamond_gear", "diamond_gear", 2, [
        T("craft_item", "minecraft:diamond_pickaxe", 16.0, 1, 1, minDifficulty="hard"),
        T("craft_item", "minecraft:diamond_sword", 11.0, 1, 1, minDifficulty="hard"),
    ], weight=0.5),
    # ------------------------------------------------------------ nether
    E("nether_trip", "nether_travel", 3, [T("visit_dimension", "minecraft:the_nether", 3.0)], dim=NE),
    E("netherrack", "nether_blocks", 3, [
        T("mine_block", "minecraft:netherrack", 0.03, 32, 128),
        T("smelt_item", "minecraft:nether_brick", 0.08, 16, 64, hint="Smelt netherrack."),
    ], dim=NE, tool="wood"),
    E("soul_sand", "nether_blocks", 3, [
        T("mine_block", "minecraft:soul_sand", 0.05, 16, 64),
        T("mine_block", "minecraft:soul_soil", 0.05, 16, 64),
    ], dim=NE, hints=["soul_sand_valley"]),
    E("basalt", "nether_blocks", 3, [
        T("mine_block", "minecraft:basalt", 0.05, 16, 64),
        T("mine_block", "minecraft:blackstone", 0.05, 16, 64),
    ], dim=NE, tool="wood", hints=["basalt_deltas"]),
    E("nether_quartz", "nether_ores", 3, [
        T("mine_block", "minecraft:nether_quartz_ore", 0.3, 8, 48),
        T("collect_item", "minecraft:quartz", 0.3, 8, 48, name="Nether Quartz"),
    ], dim=NE, tool="wood"),
    E("nether_gold", "nether_ores", 3, [T("mine_block", "minecraft:nether_gold_ore", 0.35, 8, 32)], dim=NE, tool="wood"),
    E("glowstone", "nether_ores", 3, [T("collect_item", "minecraft:glowstone_dust", 0.25, 8, 48)], dim=NE),
    E("ancient_debris", "nether_ores", 3, [
        T("mine_block", "minecraft:ancient_debris", 10.0, 1, 2, minDifficulty="hard"),
        T("smelt_item", "minecraft:netherite_scrap", 11.0, 1, 2, minDifficulty="hard"),
    ], dim=NE, tool="diamond", hints=["rare"]),
    E("crimson_forest", "nether_plants", 3, [
        T("mine_block", "minecraft:crimson_stem", 0.12, 16, 64),
        T("collect_item", "minecraft:crimson_fungus", 0.3, 4, 16),
    ], dim=NE, hints=["crimson_forest"]),
    E("warped_forest", "nether_plants", 3, [
        T("mine_block", "minecraft:warped_stem", 0.12, 16, 64),
        T("collect_item", "minecraft:warped_fungus", 0.3, 4, 16),
    ], dim=NE, hints=["warped_forest"]),
    E("nether_wart", "nether_plants", 3, [T("collect_item", "minecraft:nether_wart", 0.4, 8, 32)], dim=NE,
      hints=["fortress"]),
    E("blazes", "nether_mobs", 3, [
        T("kill_mob", "minecraft:blaze", 1.5, 3, 12),
        T("collect_item", "minecraft:blaze_rod", 1.3, 2, 12),
    ], dim=NE, hints=["fortress"]),
    E("wither_skeletons", "nether_mobs", 3, [T("kill_mob", "minecraft:wither_skeleton", 3.0, 2, 6, minDifficulty="hard")],
      dim=NE, hints=["fortress"]),
    E("ghasts", "nether_mobs", 3, [
        T("kill_mob", "minecraft:ghast", 3.0, 1, 4),
        T("collect_item", "minecraft:ghast_tear", 4.0, 1, 3, minDifficulty="hard"),
    ], dim=NE, hints=["soul_sand_valley"]),
    E("magma_cubes", "nether_mobs", 3, [
        T("kill_mob", "minecraft:magma_cube", 1.0, 4, 16),
        T("collect_item", "minecraft:magma_cream", 1.4, 2, 12),
    ], dim=NE, hints=["basalt_deltas"]),
    E("piglins", "nether_mobs", 3, [
        T("kill_mob", "minecraft:zombified_piglin", 0.6, 4, 16, hint="They attack as a group once angered."),
        T("kill_mob", "minecraft:hoglin", 2.0, 2, 6, hints=["crimson_forest"]),
    ], dim=NE),
    # ------------------------------------------------------------ end
    E("end_trip", "end_travel", 4, [T("visit_dimension", "minecraft:the_end", 6.0, minDifficulty="normal")], dim=EN),
    E("end_stone", "end_blocks", 4, [
        T("mine_block", "minecraft:end_stone", 0.04, 32, 128),
        T("mine_block", "minecraft:purpur_block", 0.12, 16, 64, hints=["end_city"]),
    ], dim=EN, tool="wood"),
    E("chorus", "end_plants", 4, [
        T("collect_item", "minecraft:chorus_fruit", 0.15, 16, 64),
        T("smelt_item", "minecraft:popped_chorus_fruit", 0.25, 8, 48),
        T("consume_item", "minecraft:chorus_fruit", 0.2, 4, 16),
    ], dim=EN, hints=["outer_islands"]),
    E("end_mobs", "end_mobs", 4, [
        T("kill_mob", "minecraft:enderman", 0.4, 8, 24),
        T("collect_item", "minecraft:ender_pearl", 0.6, 4, 16, stack=16),
        T("kill_mob", "minecraft:shulker", 2.5, 2, 6, hints=["end_city"], minDifficulty="normal"),
        T("collect_item", "minecraft:shulker_shell", 3.0, 2, 6, hints=["end_city"], minDifficulty="hard"),
    ], dim=EN),
    E("ender_eyes", "end_prep", 3, [
        T("craft_item", "minecraft:ender_eye", 5.0, 1, 4, name="Eye of Ender", plural="Eyes of Ender",
          minDifficulty="hard", hint="Blaze powder and an ender pearl."),
    ], requires=["nether"]),
]

from vanilla_more import MORE  # noqa: E402  (second batch, extension phase)
ENTRIES = ENTRIES + MORE

VANILLA = {
    "format": 1,
    "id": "vanilla",
    "name": "Minecraft",
    "entries": ENTRIES,
}
