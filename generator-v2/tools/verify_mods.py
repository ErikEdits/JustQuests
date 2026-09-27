"""Verifies the mod profiles: every id against the en_us files of all studied mod versions, and every
craft_item/smelt_item target against the mod's own recipe data (Farmer's Delight 1.20, Create mc1.20.1 and
Mekanism 1.20.x/1.21.x, Twilight Forest 1.20.1/1.21.1, Botania 1.20.x generated data, sparse clones in the research dir).
Usage: python3 tools/verify_mods.py [research_dir]"""
import json, os, sys, glob
R = sys.argv[1] if len(sys.argv) > 1 else '/home/user/research'
RES = os.path.join(os.path.dirname(__file__), '..', 'justquests-generator-v2', 'src', 'main', 'resources', 'justquests_genv2')
LANGS = {'farmersdelight': ['fd-1.18.2.json', 'fd-1.19.json', 'fd-1.20.json', 'fd-1.20.4.json', 'fd-1.21.json', 'fd-26.1.json'],
         'create': ['create-0.5.1-1.18.json', 'createfabric-1.18.json', 'create-0.5.1-1.19.json', 'create-1.20.1.json',
                    'createfabric-1.20.1.json', 'create-1.21.1.json', 'createfabric-1.21.1.json'],
         'mekanism': ['mek-1.18.x.json', 'mek-1.19.x.json', 'mek-1.20.x.json', 'mek-1.21.x.json'],
         'twilightforest': ['tf-1.18.x.json', 'tf-1.19.x.json', 'tf-1.20.1.json', 'tf-1.21.1.json'],
         'botania': ['botania-1.18.x.json', 'botania-1.19.x.json', 'botania-1.20.x.json']}
SRC = {'farmersdelight': ['fd-1.20/src'], 'create': ['create-1.20.1/src'],
       'mekanism': ['mek-1.20.x/src/datagen/generated', 'mek-1.21.x/src/datagen/generated'],
       'twilightforest': ['tf-1.20.1/src/generated/resources/data', 'tf-1.21.1/src/generated/resources/data'],
       'botania': ['botania-1.20.x/Xplat/src/generated/resources/data']}
# grid recipes with a mod-specific serializer (the result still comes out of a crafting grid)
GRID_TYPES = ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless', 'mekanism:mek_data', 'botania:twig_wand')

def lang_ids(f):
    j = json.load(open(f'{R}/lang/{f}'))
    s = set()
    for k in j:
        p = k.split('.')
        if len(p) >= 3 and p[0] in ('item', 'block', 'entity'):
            s.add(p[1] + ':' + p[2])
    return s

def recipes(srcs):
    """Recipe outputs; with several sources an output must exist in every one (all studied versions)."""
    sets = [recipes_one(s) for s in srcs]
    return {k: set.intersection(*[x[k] for x in sets]) for k in ('craft', 'smelt')}

def recipes_one(src):
    out = {'craft': set(), 'smelt': set()}
    for p in glob.glob(f'{R}/{src}/**/recipe/**/*.json', recursive=True) + \
             glob.glob(f'{R}/{src}/**/recipes/**/*.json', recursive=True):
        try:
            j = json.load(open(p))
        except Exception:
            continue
        t = j.get('type', '')
        r = j.get('result')
        rid = r if isinstance(r, str) else (r.get('item') or r.get('id')) if isinstance(r, dict) else None
        if not rid or any(k in j for k in ('conditions', 'forge:conditions', 'neoforge:conditions', 'fabric:load_conditions')):
            continue
        if t.startswith(GRID_TYPES):
            out['craft'].add(rid)
        elif t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking'):
            out['smelt'].add(rid)
    return out

problems = []
for pid in ('farmersdelight', 'create', 'mekanism', 'twilightforest', 'botania'):
    prof = json.load(open(f'{RES}/catalog/profiles/{pid}.json'))
    langs = {f: lang_ids(f) for f in LANGS[pid]}
    rec = recipes(SRC[pid])
    ids = []
    n = 0
    for e in prof['entries']:
        for t in e['targets']:
            n += 1
            if t['type'] == 'visit_dimension':
                ns, path = t['id'].split(':', 1)
                if not any(glob.glob(f'{R}/{src}/**/{ns}/dimension/{path}.json', recursive=True) for src in SRC[pid]):
                    problems.append(f"{pid} {t['id']}: no dimension file in the mod data")
                continue
            ids.append(t['id'])
            if t['type'] in ('craft_item', 'smelt_item'):
                key = 'craft' if t['type'] == 'craft_item' else 'smelt'
                if t['id'] not in rec[key] and not t['id'].startswith('minecraft:'):
                    problems.append(f"{pid} {e['key']} {t['type']} {t['id']}: no {key} recipe in the mod data")
    for r in prof.get('rewards', {}).get('items', []):
        ids.append(r['id'])
    for i in sorted(set(ids)):
        if i.startswith('minecraft:'):
            continue
        missing = [f for f, s in langs.items() if i not in s]
        if len(missing) == len(langs):
            problems.append(f"{pid} {i}: in NO version")
        elif missing:
            print(f"  note {pid} {i}: missing in {missing}")
    print(f"{pid}: {n} targets checked")
print(f"{len(problems)} problems"); [print('  P', p) for p in problems]
