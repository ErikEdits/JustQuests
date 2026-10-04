"""Checks mod profiles against facts read from the mods' released jars.

    python tools/verify_jars.py <facts_dir>

<facts_dir> holds one <modrinth-slug>.json per mod as written by the jar reader used for 0.4.0 (per
Minecraft version: "names" {id: English name}, "entities", "grid" and "smelt" recipe outputs, "machine"
{recipe type: outputs}, "drops" {block: items}, "mineable" {tag: blocks}). Every target id must exist,
craft targets need a crafting-table recipe, smelt targets a furnace recipe, kill/breed/tame targets an
entity, mined blocks a loot table; the tool of an entry must not be below what the block needs.
Ids that exist only in some versions are reported unless the target carries "since".
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from mod_ae2 import AE2  # noqa: E402
from mod_alexsmobs import ALEXS_MOBS  # noqa: E402
from mod_ars import ARS_NOUVEAU  # noqa: E402
from mod_bop import BIOMES_O_PLENTY  # noqa: E402
from mod_botania import BOTANIA  # noqa: E402
from mod_immersive import IMMERSIVE  # noqa: E402
from mod_mekanism import MEKANISM  # noqa: E402
from mod_tinkers import TINKERS  # noqa: E402

PROFILES = {"ae2": AE2, "alexs-mobs": ALEXS_MOBS, "ars-nouveau": ARS_NOUVEAU, "biomes-o-plenty": BIOMES_O_PLENTY,
            "botania": BOTANIA, "immersiveengineering": IMMERSIVE, "mekanism": MEKANISM, "tinkers-construct": TINKERS}
TOOLS = ["none", "wood", "stone", "iron", "diamond"]
ENTITY_TYPES = {"kill_mob", "breed_animal", "tame_animal"}
# the mods' own crafting-table recipe types (shaped grids with extra behaviour)
GRID_TYPES = {"botania:twig_wand", "mekanism:mek_data"}


def needed_tool(facts, block):
    for tool in ("diamond", "iron", "stone"):
        if block in facts["mineable"].get(f"needs_{tool}_tool", []):
            return tool
    return "none"


def crafted(facts, rid):
    if rid in facts["grid"]:
        return True
    return any(rid in outs for t, outs in facts["machine"].items() if "crafting" in t or t in GRID_TYPES)


def check(slug, profile, data):
    problems, notes, targets = [], [], 0
    versions = list(data)
    for entry in profile["entries"]:
        tool = entry.get("tool", "none")
        for t in entry["targets"]:
            targets += 1
            rid, typ = t.get("id"), t["type"]
            if typ == "visit_dimension" or rid is None:
                continue
            key = "entities" if typ in ENTITY_TYPES else "names"
            present = [v for v in versions if rid in data[v][key]]
            where = f"{entry['key']} {typ} {rid}"
            if not present:
                problems.append(f"{where}: id unknown in every studied version")
                continue
            since = t.get("since") or entry.get("since")
            if len(present) < len(versions) and not since:
                notes.append(f"{where}: only in {', '.join(present)}")
            for v in present:
                f = data[v]
                if typ == "craft_item" and not crafted(f, rid):
                    problems.append(f"{where}: no crafting-table recipe in {v}")
                if typ == "smelt_item" and rid not in f["smelt"]:
                    problems.append(f"{where}: no furnace recipe in {v}")
                if typ == "mine_block":
                    if rid not in f["drops"]:
                        problems.append(f"{where}: no block loot table in {v}")
                    need = needed_tool(f, rid)
                    if TOOLS.index(need) > TOOLS.index(tool if tool in TOOLS else "none"):
                        problems.append(f"{where}: needs a {need} tool in {v}, entry says {tool}")
    for r in profile.get("rewards", {}).get("items", []):
        if not any(r["id"] in data[v]["names"] for v in versions):
            problems.append(f"reward {r['id']}: id unknown")
    return targets, problems, notes


def main():
    facts_dir = sys.argv[1] if len(sys.argv) > 1 else "facts"
    total_targets, total_problems = 0, 0
    for slug, profile in PROFILES.items():
        path = os.path.join(facts_dir, slug + ".json")
        if not os.path.exists(path):
            print(f"{profile['id']}: no facts ({path})")
            continue
        data = json.load(open(path, encoding="utf-8"))
        targets, problems, notes = check(slug, profile, data)
        total_targets += targets
        total_problems += len(problems)
        print(f"{profile['id']}: {targets} targets, {len(problems)} problems, versions {', '.join(data)}")
        for p in problems:
            print("  PROBLEM", p)
        for n in notes:
            print("  note   ", n)
    print(f"total: {total_targets} targets, {total_problems} problems")
    sys.exit(1 if total_problems else 0)


main()
