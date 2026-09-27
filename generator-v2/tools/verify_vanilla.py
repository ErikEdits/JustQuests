"""Verifies vanilla ids/recipes/tags/loot tables/effects of the bundled data against misode/mcmeta
registry summaries (all 17 Minecraft versions of the 34 builds) and recipe data (4 key versions).
Usage: python3 tools/verify_vanilla.py [research_dir]"""
import json, os, sys, glob
R = sys.argv[1] if len(sys.argv) > 1 else '/home/user/research/mcmeta'
VERSIONS = ['1.18.2', '1.19.2', '1.19.4', '1.20.1', '1.20.4', '1.20.6', '1.21', '1.21.1', '1.21.2', '1.21.3',
            '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10']
RECIPE_VERSIONS = ['1.18.2', '1.20.1', '1.21.1', '1.21.10']
RES = os.path.join(os.path.dirname(__file__), '..', 'justquests-generator-v2', 'src', 'main', 'resources', 'justquests_genv2')

def vkey(v):
    return tuple(int(x) for x in v.split('.'))

reg = {v: json.load(open(f'{R}/reg-{v}.json')) for v in VERSIONS}
def names(v, key):
    return set(reg[v].get(key, []))

def recipes(v):
    out = {'craft': set(), 'smelt': set()}
    base = f'{R}/data-{v}/data/minecraft'
    for p in glob.glob(base + '/recipe*/**/*.json', recursive=True):
        j = json.load(open(p))
        t = j.get('type', '')
        r = j.get('result')
        rid = r if isinstance(r, str) else (r.get('item') or r.get('id')) if isinstance(r, dict) else None
        if not rid:
            continue
        rid = rid.replace('minecraft:', '')
        if t.startswith('minecraft:crafting_shaped') or t.startswith('minecraft:crafting_shapeless') or t == 'minecraft:crafting_transmute':
            out['craft'].add(rid)
        elif t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking'):
            out['smelt'].add(rid)
    return out
REC = {v: recipes(v) for v in RECIPE_VERSIONS}

KIND = {'collect_item': 'item', 'craft_item': 'item', 'smelt_item': 'item', 'consume_item': 'item',
        'mine_block': 'block', 'place_block': 'block', 'kill_mob': 'entity_type', 'breed_animal': 'entity_type',
        'tame_animal': 'entity_type', 'visit_dimension': 'dimension'}
DIMS = {'the_nether', 'the_end', 'overworld'}

def present(kind, rid):
    if kind == 'dimension':
        return {v for v in VERSIONS} if rid in DIMS else set()
    return {v for v in VERSIONS if rid in names(v, kind)}

problems, notes = [], []
def first(vs):
    return min(vs, key=vkey) if vs else None

cat = json.load(open(f'{RES}/catalog/vanilla.json'))
tags = json.load(open(f'{RES}/tags.json'))['concepts']
count = 0
for e in cat['entries']:
    since = e.get('since')
    for t in e['targets']:
        count += 1
        typ = t['type']; kind = KIND[typ]
        ids = [t['id']] + t.get('alt', [])
        avail = set()
        for i in ids:
            if not i.startswith('minecraft:'):
                problems.append(f"{e['key']}: non-vanilla id {i}"); continue
            avail |= present(kind, i.split(':', 1)[1])
        where = f"{e['key']} {typ} {t['id']}"
        if not avail:
            problems.append(f"{where}: exists in NO version"); continue
        f = first(avail)
        if f != '1.18.2':
            s = t.get('since', since)
            if not s:
                problems.append(f"{where}: only from {f} but no 'since'")
            else:
                idx = VERSIONS.index(f)
                prev = VERSIONS[idx - 1] if idx > 0 else None
                if prev and vkey(s) <= vkey(prev):
                    problems.append(f"{where}: since {s} but registry from {f}")
        s2 = t.get('since', since)
        if s2 and vkey(s2) > vkey(f):
            notes.append(f"{where}: in the registry from {f} (experimental) but released {s2} -> since-guard needed")
        missing_after = [v for v in VERSIONS if vkey(v) >= vkey(f) and v not in avail]
        if missing_after:
            notes.append(f"{where}: disappears in {missing_after}")
        if typ in ('craft_item', 'smelt_item'):
            key = 'craft' if typ == 'craft_item' else 'smelt'
            for v in RECIPE_VERSIONS:
                if v not in avail:
                    continue
                ok = any(i.split(':', 1)[1] in REC[v][key] for i in ids if i.split(':', 1)[1] in names(v, 'item'))
                if not ok:
                    problems.append(f"{where}: no {key} recipe in {v}")
        tag = t.get('tag')
        if tag:
            for cand_ok_v in VERSIONS:
                tl = names(cand_ok_v, 'tag/item')
                if not any(c.replace('minecraft:', '') in tl for c in tags[tag]['candidates'] if c.startswith('minecraft:')) and all(c.startswith('minecraft:') for c in tags[tag]['candidates']):
                    problems.append(f"{where}: tag {tag} missing in {cand_ok_v}")
rw = json.load(open(f'{RES}/rewards.json'))
for it in rw['items']:
    i = it['id'].split(':', 1)[1]
    if not present('item', i):
        problems.append(f"reward item {it['id']} missing")
    elif first(present('item', i)) != '1.18.2':
        notes.append(f"reward item {it['id']} only from {first(present('item', i))} (runtime check skips it earlier)")
for ef in rw['effects']:
    i = ef['id'].split(':', 1)[1]
    miss = [v for v in VERSIONS if i not in names(v, 'mob_effect')]
    if miss:
        problems.append(f"effect {ef['id']} missing in {miss}")
for lt in rw['lootTables']:
    i = lt['id'].split(':', 1)[1]
    miss = [v for v in VERSIONS if i not in names(v, 'loot_table')]
    if miss:
        problems.append(f"loot table {lt['id']} missing in {miss}")
print(f"checked {count} vanilla targets, {len(rw['items'])} reward items")
print(f"{len(problems)} problems:"); [print('  P', p) for p in problems]
print(f"{len(notes)} notes:"); [print('  N', n) for n in notes]
