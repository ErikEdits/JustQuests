"""Copies the language files from lang/ into every build (assets/justquests/lang/).

lang/ is the one place to edit translations: en_us.json is the source, the other languages
translate its keys. Run from the repo root:  python scripts/sync_lang.py
"""
import json
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "scripts"))
from sync_generator_v2 import version_trees  # noqa: E402

SRC = os.path.join(ROOT, "lang")
DST = os.path.join("src", "main", "resources", "assets", "justquests", "lang")


def main():
    files = sorted(f for f in os.listdir(SRC) if f.endswith(".json"))
    english = json.load(open(os.path.join(SRC, "en_us.json"), encoding="utf-8"))
    for f in files:
        keys = json.load(open(os.path.join(SRC, f), encoding="utf-8"))
        missing = [k for k in english if k not in keys]
        extra = [k for k in keys if k not in english]
        if f != "en_us.json":
            print(f"{f}: {len(keys)} keys, {len(missing)} missing, {len(extra)} unknown")
    trees = version_trees()
    for loader, v in trees:
        dst = os.path.join(ROOT, loader, v, DST)
        os.makedirs(dst, exist_ok=True)
        for f in files:
            shutil.copyfile(os.path.join(SRC, f), os.path.join(dst, f))
    print(f"{len(files)} language file(s) copied into {len(trees)} builds")


if __name__ == "__main__":
    main()
