"""Builds lang/<code>.json (the generator's extra languages) from tools/i18n and checks it against the English.

Sources (all hand-written except names/):
  i18n/templates/<lang>.json   grammar + title/phrase templates, index-aligned with templates.json
  i18n/themes.json             theme key -> language -> [names..., description]
  i18n/common.json             tools, hint words, tag nouns, messages, dimensions, family names, name overrides
  i18n/hints/*.json            English catalog hint -> [de_de, fr_fr, es_es, ja_jp]
  i18n/names/<lang>.json       official target names (written by lang_names.py)
Any mismatch with the English data (missing key, list of a different length, unknown placeholder) stops the build.
"""
import glob
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
I18N = os.path.join(HERE, "i18n")
LANGS = ["de_de", "fr_fr", "es_es", "ja_jp"]
PLACEHOLDERS = {"{Name}", "{Names}", "{name}", "{names}", "{a_name}", "{de_name}", "{count}", "{Dimension}",
                "{dimension}", "{dimension_to}", "{dimension_in}", "{Family}", "{list}", "{noun}"}


def _load(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def _check_placeholders(where, text, problems):
    for p in re.findall(r"\{[^}]*\}", text):
        if p not in PLACEHOLDERS:
            problems.append("%s: unknown placeholder %s in %r" % (where, p, text))


def _aligned(where, english, local, problems):
    """Same keys and list lengths as the English structure (dict of lists or list)."""
    if isinstance(english, list):
        if not isinstance(local, list) or len(local) != len(english):
            problems.append("%s: expected %d entries, got %r" % (where, len(english), local))
            return
        for i, s in enumerate(local):
            if not isinstance(s, str) or not s.strip():
                problems.append("%s[%d]: empty" % (where, i))
            else:
                _check_placeholders("%s[%d]" % (where, i), s, problems)
        return
    for k, v in english.items():
        if k not in (local or {}):
            problems.append("%s: missing %s" % (where, k))
        else:
            _aligned("%s.%s" % (where, k), v, local[k], problems)


def _pick(table, idx, where, keys, problems):
    out = {}
    for k in keys:
        row = table.get(k)
        if not isinstance(row, list) or len(row) != len(LANGS):
            problems.append("%s: missing or malformed %s" % (where, k))
            continue
        if row[idx] is not None:
            out[k] = row[idx]
    return out


def catalog_hints(profiles):
    hints = []
    for p in profiles:
        for e in p.get("entries", []):
            for t in e.get("targets", []):
                h = (t.get("hint") or "").strip()
                if h and h not in hints:
                    hints.append(h)
    return hints


def build(templates, themes, profiles, messages, tags):
    """Returns {lang: lang json}; raises SystemExit listing every problem."""
    problems = []
    common = _load(os.path.join(I18N, "common.json"))
    theme_text = _load(os.path.join(I18N, "themes.json"))
    hint_rows = {}
    for f in sorted(glob.glob(os.path.join(I18N, "hints", "*.json"))):
        for k, v in _load(f).items():
            if not k.startswith("_"):
                hint_rows[k] = v
    wanted_hints = catalog_hints(profiles)
    for h in wanted_hints:
        if h not in hint_rows:
            problems.append("hints: no translation for %r" % h)
    all_themes = list(themes)
    for p in profiles:
        all_themes += p.get("themes", [])
    out = {}
    for idx, lang in enumerate(LANGS):
        local = _load(os.path.join(I18N, "templates", lang + ".json"))
        where = "templates/%s" % lang
        for key in ("titles", "phrases"):
            _aligned("%s.%s" % (where, key), templates[key], local.get(key), problems)
        _aligned(where + ".variants", templates["variants"], local.get("variants"), problems)
        _aligned(where + ".fallbackTitles", templates["fallbackTitles"], local.get("fallbackTitles"), problems)
        _aligned(where + ".comboTitles", templates["comboTitles"], local.get("comboTitles"), problems)
        sentences = local.get("sentences", {})
        for k in ("tag", "dimension", "list"):
            if not sentences.get(k):
                problems.append("%s.sentences: missing %s" % (where, k))
        tpl = {
            "titles": local.get("titles"),
            "phrases": local.get("phrases"),
            "variants": local.get("variants"),
            "fallbackTitles": local.get("fallbackTitles"),
            "comboTitles": local.get("comboTitles"),
            "sentences": sentences,
            "tools": _pick(common["tools"], idx, "common.tools", templates["tools"], problems),
            "hints": _pick(common["hints"], idx, "common.hints", templates["hints"], problems),
            "familyNames": _pick(common["familyNames"], idx, "common.familyNames", templates["familyNames"], problems),
            "dimensionNames": _pick(common["dimensionNames"], idx, "common.dimensionNames", templates["dimensionNames"], problems),
            "dimensionTo": _pick(common["dimensionTo"], idx, "common.dimensionTo", templates["dimensionNames"], problems),
            "dimensionIn": _pick(common["dimensionIn"], idx, "common.dimensionIn", templates["dimensionNames"], problems),
        }
        theme_out = {}
        for th in all_themes:
            row = theme_text.get(th["key"], {}).get(lang)
            n, d = len(th["names"]), len(th["descriptions"])
            if d != 1:
                problems.append("theme %s: the i18n format expects one English description, found %d" % (th["key"], d))
            if not isinstance(row, list) or len(row) != n + 1:
                problems.append("themes.%s.%s: expected %d names + 1 description, got %r" % (th["key"], lang, n, row))
                continue
            if "{list}" not in row[-1]:
                problems.append("themes.%s.%s: description lacks {list}" % (th["key"], lang))
            for s in row:
                _check_placeholders("themes.%s.%s" % (th["key"], lang), s, problems)
            theme_out[th["key"]] = {"names": row[:-1], "descriptions": row[-1:]}
        rows = common["messages"]
        if len(rows) != len(messages):
            problems.append("common.messages: expected %d, got %d" % (len(messages), len(rows)))
        names = _load(os.path.join(I18N, "names", lang + ".json"))
        for target, row in common["nameOverrides"].items():
            if row[idx] is not None:
                names[target] = row[idx]
        out[lang] = {
            "format": 1,
            "language": lang,
            "grammar": local.get("grammar", {}),
            "templates": tpl,
            "themes": theme_out,
            "tagNouns": _pick(common["tagNouns"], idx, "common.tagNouns", tags["concepts"], problems),
            "hints": {h: hint_rows[h][idx] for h in wanted_hints if h in hint_rows},
            "messages": [r[idx] for r in rows],
            "names": dict(sorted(names.items())),
        }
    if problems:
        raise SystemExit("language data problems:\n  " + "\n  ".join(dict.fromkeys(problems)))
    return out
