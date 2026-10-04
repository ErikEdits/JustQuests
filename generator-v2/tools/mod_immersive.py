"""Immersive Engineering profile (BluSunrize and contributors; "Blu's License of Common Sense").

Ids, names and recipes were checked against the released jars from Modrinth for Minecraft 1.18.2, 1.19.2,
1.20.1 and 1.21.1 (lang/en_us.json, crafting and smelting recipes, block loot tables). Only facts are used
(ids, names, which items have a crafting-grid or furnace recipe, what drops what); nothing is copied.

Like Mekanism, IE names its materials "ingot_lead", "plate_iron" and so on; every target carries its
English name for dedicated servers. Coke, steel and treated wood come from multiblocks or fluids, so the
profile counts the grid steps around them.
"""
from catalog_dsl import E, T

IE = "immersiveengineering:"

IE_ENTRIES = [
    E("ie_bauxite", "ie_bauxite", 1, [
        T("mine_block", IE + "ore_aluminum", 0.7, 4, 24, name="Bauxite Ore"),
        T("mine_block", IE + "deepslate_ore_aluminum", 0.8, 4, 24, name="Deepslate Bauxite Ore", hints=["deep"]),
        T("collect_item", IE + "raw_aluminum", 0.7, 4, 24, name="Raw Bauxite", plural="Raw Bauxite",
          hint="Bauxite ore drops raw bauxite."),
        T("smelt_item", IE + "ingot_aluminum", 0.75, 4, 32, name="Aluminium Ingot",
          hint="Smelt raw bauxite in a furnace."),
    ], tool="stone", hints=["caves"]),
    E("ie_lead", "ie_lead", 1, [
        T("mine_block", IE + "ore_lead", 0.8, 4, 24, name="Lead Ore"),
        T("collect_item", IE + "raw_lead", 0.8, 4, 24, name="Raw Lead", plural="Raw Lead",
          hint="Lead ore drops raw lead."),
        T("smelt_item", IE + "ingot_lead", 0.85, 4, 32, name="Lead Ingot", hint="Smelt raw lead in a furnace."),
    ], tool="iron", hints=["caves"]),
    E("ie_silver", "ie_silver", 2, [
        T("mine_block", IE + "ore_silver", 1.0, 4, 16, name="Silver Ore"),
        T("collect_item", IE + "raw_silver", 1.0, 4, 16, name="Raw Silver", plural="Raw Silver",
          hint="Silver ore drops raw silver."),
        T("smelt_item", IE + "ingot_silver", 1.05, 4, 24, name="Silver Ingot", hint="Smelt raw silver in a furnace."),
    ], tool="iron", hints=["caves", "deep"]),
    E("ie_nickel", "ie_nickel", 2, [
        T("mine_block", IE + "ore_nickel", 1.0, 4, 16, name="Nickel Ore"),
        T("smelt_item", IE + "ingot_nickel", 1.05, 4, 24, name="Nickel Ingot", hint="Smelt raw nickel in a furnace."),
        T("collect_item", IE + "raw_nickel", 1.0, 4, 16, name="Raw Nickel", plural="Raw Nickel",
          hint="Nickel ore drops raw nickel."),
    ], tool="iron", hints=["caves"],
      exclusions=["craft_item immersiveengineering:ingot_constantan (made in the kiln or from crusher dusts)"]),
    E("ie_hemp", "ie_hemp", 1, [
        T("collect_item", IE + "seed", 0.5, 2, 8, name="Industrial Hemp Seeds", plural="Industrial Hemp Seeds",
          hint="Breaking tall grass sometimes drops hemp seeds."),
        T("collect_item", IE + "hemp_fiber", 0.4, 8, 32, name="Industrial Hemp Fiber",
          hint="Grown hemp drops fiber."),
        T("craft_item", IE + "hemp_fabric", 1.0, 2, 8, name="Tough Fabric", hint="Hemp fiber around a stick."),
    ]),
    E("ie_workshop", "ie_workshop", 1, [
        T("craft_item", IE + "hammer", 2.5, 1, 1, name="Engineer's Hammer", plural="Engineer's Hammers",
          hint="Iron ingots, string and sticks."),
        T("craft_item", IE + "manual", 1.5, 1, 1, name="Engineer's Manual", plural="Engineer's Manuals",
          hint="A book and a lever."),
        T("craft_item", IE + "cokebrick", 0.8, 4, 16, name="Coke Brick", hint="Clay, bricks and sandstone."),
        T("craft_item", IE + "alloybrick", 0.8, 4, 16, name="Kiln Brick", hint="Two bricks and two sandstone."),
        T("craft_item", IE + "craftingtable", 3.0, 1, 1, name="Engineer's Crafting Table",
          hint="Treated wood slabs, treated sticks and a crafting table."),
    ]),
    E("ie_metalwork", "ie_metalwork", 2, [
        T("craft_item", IE + "plate_iron", 1.6, 2, 8, name="Iron Plate", hint="An iron ingot hit with the Engineer's Hammer."),
        T("craft_item", IE + "stick_iron", 1.0, 4, 16, name="Iron Rod", hint="Iron ingots in a column."),
        T("craft_item", IE + "wire_copper", 1.2, 4, 16, name="Copper Wire",
          hint="A copper plate and the Engineer's Wire Cutters."),
        T("craft_item", IE + "component_iron", 3.0, 1, 4, name="Iron Mechanical Component", minDifficulty="normal",
          hint="Iron plates and a copper ingot."),
        T("craft_item", IE + "conveyor_basic", 1.2, 4, 16, name="Conveyor Belt", minDifficulty="normal",
          hint="Leather, iron and redstone."),
    ]),
    E("ie_treated", "ie_treated", 2, [
        T("craft_item", IE + "stick_treated", 0.5, 8, 32, name="Treated Stick",
          hint="Two treated wood planks; planks soak creosote from a coke oven."),
        T("craft_item", IE + "treated_fence", 0.6, 4, 16, name="Treated Wood Fence", minDifficulty="normal",
          hint="Treated wood planks and treated sticks."),
        T("craft_item", IE + "treated_scaffold", 0.8, 4, 16, name="Treated Wood Scaffolding",
          plural="Treated Wood Scaffolding", minDifficulty="normal", hint="Treated wood planks and treated sticks."),
    ]),
]

IE_REWARDS = [
    {"id": IE + "ingot_aluminum", "value": 0.9, "tier": 2, "max": 16, "family": "ie_bauxite"},
    {"id": IE + "ingot_lead", "value": 1.0, "tier": 2, "max": 16, "family": "ie_lead"},
    {"id": IE + "ingot_silver", "value": 1.3, "tier": 2, "max": 12, "family": "ie_silver"},
    {"id": IE + "hemp_fiber", "value": 0.4, "tier": 1, "max": 32, "family": "ie_hemp"},
    {"id": IE + "coal_coke", "value": 1.2, "tier": 2, "max": 16, "family": "ie_workshop"},
    {"id": IE + "ingot_steel", "value": 3.0, "tier": 3, "max": 8, "family": "ie_metalwork"},
]

IE_THEMES = [
    {"key": "ie_prospector", "names": ["Prospector", "Ore Survey", "Field Engineer"],
     "descriptions": ["An engineer needs metal before machines: {list}."],
     "slots": [{"types": ["mine_block"], "keys": ["ie_bauxite", "ie_lead", "ie_silver", "ie_nickel"]},
               {"types": ["smelt_item", "collect_item"], "keys": ["ie_bauxite", "ie_lead", "ie_silver", "ie_nickel"]}]},
    {"key": "ie_workshop_setup", "names": ["Workshop Setup", "Hammer Time", "Bricks and Iron"],
     "descriptions": ["Set up the workshop: {list}."],
     "slots": [{"types": ["craft_item"], "keys": ["ie_workshop"]},
               {"types": ["craft_item"], "keys": ["ie_metalwork", "ie_hemp"]}]},
]

IMMERSIVE = {
    "format": 1,
    "id": "immersiveengineering",
    "name": "Immersive Engineering",
    "requiresMod": ["immersiveengineering"],
    "minecraft": {"min": "1.18.2"},
    "entries": IE_ENTRIES,
    "rewards": {"items": IE_REWARDS},
    "themes": IE_THEMES,
}

IE_FAMILY_NAMES = {
    "ie_bauxite": "Bauxite", "ie_lead": "Lead", "ie_silver": "Silver", "ie_nickel": "Nickel", "ie_hemp": "Hemp",
    "ie_workshop": "Workshop", "ie_metalwork": "Metalwork", "ie_treated": "Treated Wood",
}
