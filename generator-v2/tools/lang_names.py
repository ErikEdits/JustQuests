"""Collects the official translated names of every catalog target for the generator's other languages.

Reads the vanilla language files from a local Minecraft install (assets/indexes + assets/objects) and the
mod language files from downloaded mod jars (or loose lang JSON files), then writes
i18n/names/<lang>.json: {target id: name}. Only names are taken, nothing else.

Usage: python lang_names.py <.minecraft dir> <mod jars dir>
  The mod jars dir is searched recursively for *.jar and for loose files named <lang>*.json
  (e.g. de_de-1.21.1.json, used for mods whose translations come from their source repository).
Newer sources win: asset indexes in descending order, jars by file name in descending order.
"""
import glob
import json
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "justquests-generator-v2", "src", "main", "resources", "justquests_genv2")
OUT = os.path.join(HERE, "i18n", "names")
LANGS = ["de_de", "fr_fr", "es_es", "ja_jp"]
ENTITY_TYPES = {"kill_mob", "breed_animal", "tame_animal"}
BLOCK_TYPES = {"mine_block", "place_block"}


def catalog_targets():
    """(type, id, potion) for every catalog target, alternates included."""
    files = [os.path.join(RES, "catalog", "vanilla.json")]
    files += sorted(f for f in glob.glob(os.path.join(RES, "catalog", "profiles", "*.json"))
                    if not f.endswith("index.json"))
    out = []
    for f in files:
        with open(f, encoding="utf-8") as fh:
            profile = json.load(fh)
        for e in profile.get("entries", []):
            for t in e.get("targets", []):
                if t["type"] == "visit_dimension":
                    continue
                ids = [t["id"]] + ([t["alt"]] if isinstance(t.get("alt"), str) else list(t.get("alt") or []))
                for i in ids:
                    out.append((t["type"], i, t.get("potion")))
    return out


def vanilla_langs(mc_dir):
    """lang -> merged dict, newest asset index first."""
    merged = {l: {} for l in LANGS}
    idx_dir = os.path.join(mc_dir, "assets", "indexes")
    indexes = [f for f in os.listdir(idx_dir) if re.fullmatch(r"\d+\.json", f)]
    for name in sorted(indexes, key=lambda f: -int(f[:-5])):
        with open(os.path.join(idx_dir, name), encoding="utf-8") as fh:
            objects = json.load(fh)["objects"]
        for lang in LANGS:
            entry = objects.get("minecraft/lang/%s.json" % lang)
            if not entry:
                continue
            h = entry["hash"]
            path = os.path.join(mc_dir, "assets", "objects", h[:2], h)
            if os.path.exists(path):
                with open(path, encoding="utf-8") as fh:
                    for k, v in json.load(fh).items():
                        merged[lang].setdefault(k, v)
    return merged


def parse_lang(raw):
    text = raw.decode("utf-8", "replace").lstrip("﻿")
    try:
        return json.loads(text)
    except ValueError:
        return {}


def mod_langs(mods_dir):
    merged = {l: {} for l in LANGS}
    jars = sorted(glob.glob(os.path.join(mods_dir, "**", "*.jar"), recursive=True), reverse=True)
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            for n in z.namelist():
                m = re.fullmatch(r"assets/[a-z0-9_.-]+/lang/([a-z]{2}_[a-z]{2})\.json", n)
                if m and m.group(1) in LANGS:
                    for k, v in parse_lang(z.read(n)).items():
                        merged[m.group(1)].setdefault(k, v)
    loose = sorted(glob.glob(os.path.join(mods_dir, "**", "*.json"), recursive=True), reverse=True)
    for f in loose:
        lang = os.path.basename(f)[:5]
        if lang in LANGS:
            with open(f, "rb") as fh:
                for k, v in parse_lang(fh.read()).items():
                    merged[lang].setdefault(k, v)
    return merged


def keys_for(type_, target, potion):
    ns, path = target.split(":", 1)
    if potion:
        p = potion.split(":", 1)[1]
        return ["item.minecraft.%s.effect.%s" % (path, p)]
    if type_ in ENTITY_TYPES:
        return ["entity.%s.%s" % (ns, path)]
    if type_ in BLOCK_TYPES:
        return ["block.%s.%s" % (ns, path), "item.%s.%s" % (ns, path)]
    return ["item.%s.%s" % (ns, path), "block.%s.%s" % (ns, path)]


def clean(name):
    # strip formatting codes some mod translations carry
    return re.sub("§.", "", name).strip()


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    van = vanilla_langs(sys.argv[1])
    mods = mod_langs(sys.argv[2])
    targets = catalog_targets()
    os.makedirs(OUT, exist_ok=True)
    for lang in LANGS:
        names, missing = {}, []
        for type_, target, potion in targets:
            key = target + ("{%s}" % potion if potion else "")
            if key in names:
                continue
            table = van[lang] if target.startswith("minecraft:") else mods[lang]
            found = next((table[k] for k in keys_for(type_, target, potion) if table.get(k)), None)
            if found:
                names[key] = clean(found)
            else:
                missing.append(key)
        with open(os.path.join(OUT, lang + ".json"), "w", encoding="utf-8", newline="\n") as fh:
            json.dump(dict(sorted(names.items())), fh, ensure_ascii=False, indent=1)
            fh.write("\n")
        print(lang, "names", len(names), "missing", len(missing), sorted(set(missing))[:40])


if __name__ == "__main__":
    main()
