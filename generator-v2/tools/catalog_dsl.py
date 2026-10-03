"""Tiny DSL used to write the bundled catalog JSON files (the JSON files are the source of truth
shipped in the core; this script only makes the initial authoring less error-prone)."""
import json

def T(type, id=None, effort=None, min=1, max=16, **kw):
    t = {"type": type}
    if id is not None:
        t["id"] = id
    for k in ("tag",):
        if k in kw:
            t[k] = kw.pop(k)
    t["effort"] = effort
    if type != "visit_dimension":
        t["min"] = min
        t["max"] = max
    for k in ("alt", "tier", "tool", "hints", "name", "plural", "hint", "stack", "dimension", "minDifficulty",
              "tamable", "note", "weight", "since", "potion"):
        if k in kw:
            t[k] = kw.pop(k)
    assert not kw, kw
    return t

def E(key, family, tier, targets, **kw):
    e = {"key": key}
    if "name" in kw:
        e["name"] = kw.pop("name")
    e["family"] = family
    e["tier"] = tier
    e["dimension"] = kw.pop("dim", "minecraft:overworld")
    e["tool"] = kw.pop("tool", "none")
    hints = kw.pop("hints", [])
    if hints:
        e["hints"] = hints
    for k in ("requires", "since", "notes", "exclusions", "weight"):
        if k in kw:
            e[k] = kw.pop(k)
    assert not kw, kw
    e["targets"] = targets
    return e

def dump(obj, path):
    with open(path, "w") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
