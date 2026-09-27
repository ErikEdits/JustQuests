#!/usr/bin/env python3
"""Copy the generator v2 core + data from generator-v2/ into every JustQuests version tree.

Run from the repository root after changing generator-v2/justquests-generator-v2:
    python scripts/sync_generator_v2.py

Copies (replacing) in each tree listed in settings.gradle:
    src/main/java/com/erikedits/justquests/generator/v2/**   (core, identical everywhere)
    src/main/resources/justquests_genv2/**                   (catalogs, templates, balance)
The per-build adapter (generator/GenV2Host.java, generator/GenV2.java) is never touched.
"""
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "generator-v2", "justquests-generator-v2")
CORE = os.path.join("src", "main", "java", "com", "erikedits", "justquests", "generator", "v2")
DATA = os.path.join("src", "main", "resources", "justquests_genv2")


def version_trees():
    """Registered trees from settings.gradle: each ['1.20.4', ...].each { v -> ... } block maps
    to the loader directory named in its file("<loader>/${v}") call."""
    text = open(os.path.join(ROOT, "settings.gradle"), encoding="utf-8").read()
    trees = []
    for m in re.finditer(r"\[([^\]]*)\]\.each\s*\{\s*v\s*->", text):
        window = text[m.end():m.end() + 600]
        found = re.search(r'file\("(neoforge|fabric|forge)/\$\{v\}"\)', window)
        if found:
            trees += [(found.group(1), v) for v in re.findall(r"'([0-9.]+)'", m.group(1))]
    return trees


def main():
    if not os.path.isdir(os.path.join(SRC, CORE)):
        sys.exit("generator-v2 project not found at " + SRC)
    trees = version_trees()
    if not trees:
        sys.exit("no version trees found in settings.gradle")
    for loader, v in trees:
        tree = os.path.join(ROOT, loader, v)
        for rel in (CORE, DATA):
            dst = os.path.join(tree, rel)
            if os.path.isdir(dst):
                shutil.rmtree(dst)   # drop files that no longer exist upstream
            shutil.copytree(os.path.join(SRC, rel), dst)
        print(f"synced {loader}/{v}")
    print(f"done: {len(trees)} trees")


if __name__ == "__main__":
    main()
