package com.erikedits.justquests.generator.v2.internal.catalog;

import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GenLog;
import com.erikedits.justquests.generator.v2.api.StateStore;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads the bundled data under {@code /justquests_genv2/} and the optional world overrides:
 * {@code generator_v2/balance.json} (deep-merged over the bundled balance) and extra profiles in
 * {@code generator_v2/profiles/} (a world profile with the same id replaces the bundled one).
 * Bad records are skipped with a warning; loading never throws.
 */
public final class CatalogLoader {
    /** Root of the bundled resources (not a Java package, so module systems do not hide it). */
    public static final String ROOT = "/justquests_genv2/";
    public static final String WORLD_DIR = "generator_v2/";

    private final GenLog log;
    private final StateStore store;
    private final List<String> warnings = new ArrayList<>();

    public CatalogLoader(GenLog log, StateStore store) {
        this.log = log;
        this.store = store;
    }

    public Catalog load() {
        Map<String, TagConcept> tags = parseTags(resourceJson("tags.json"));
        List<ProfileDef> profiles = new ArrayList<>();
        ProfileDef vanilla = parseProfile(resourceJson("catalog/vanilla.json"), ROOT + "catalog/vanilla.json", tags, true);
        if (vanilla != null) {
            profiles.add(vanilla);
        }
        JsonObject index = resourceJson("catalog/profiles/index.json");
        for (String file : Json.strings(index, "profiles")) {
            ProfileDef p = parseProfile(resourceJson("catalog/profiles/" + file), ROOT + "catalog/profiles/" + file, tags, false);
            if (p != null) {
                addOrReplace(profiles, p);
            }
        }
        for (String file : worldProfileFiles()) {
            String name = WORLD_DIR + "profiles/" + file;
            Optional<String> text = safeRead(name);
            if (text.isPresent()) {
                JsonObject o = parseText(text.get(), name);
                ProfileDef p = parseProfile(o, "world:" + name, tags, false);
                if (p != null) {
                    addOrReplace(profiles, p);
                    log.info("[GenV2] Loaded world profile " + p.id() + " from " + name);
                }
            }
        }

        JsonObject balanceJson = resourceJson("balance.json");
        Optional<String> worldBalance = safeRead(WORLD_DIR + "balance.json");
        if (worldBalance.isPresent()) {
            JsonObject o = parseText(worldBalance.get(), WORLD_DIR + "balance.json");
            if (o != null) {
                balanceJson = balanceJson == null ? o : Json.deepMerge(balanceJson, o);
                log.info("[GenV2] Applied world balance overrides from " + WORLD_DIR + "balance.json");
            }
        }
        Balance balance = Balance.parse(balanceJson);
        Templates templates = Templates.parse(resourceJson("templates.json"));

        JsonObject rewards = resourceJson("rewards.json");
        List<RewardDefs.Item> items = new ArrayList<>(parseItems(Json.objects(rewards, "items"), "vanilla"));
        List<RewardDefs.Effect> effects = new ArrayList<>(parseEffects(Json.objects(rewards, "effects"), "vanilla"));
        List<RewardDefs.Loot> loot = new ArrayList<>(parseLoot(Json.objects(rewards, "lootTables"), "vanilla"));
        List<String> messages = Json.strings(rewards, "messages");
        List<ThemeDef> themes = new ArrayList<>(parseThemes(Json.objects(resourceJson("themes.json"), "themes"), "vanilla"));
        for (ProfileDef p : profiles) {
            items.addAll(p.items());
            effects.addAll(p.effects());
            loot.addAll(p.loot());
            themes.addAll(p.themes());
        }
        List<Localization> languages = new ArrayList<>();
        for (String code : Localization.CODES) {
            String text = readResource("lang/" + code + ".json");
            Localization l = null;
            try {
                l = text == null ? null : Localization.parse(code, Json.parseObject(text));
            } catch (RuntimeException e) {
                l = null;
            }
            if (l != null) {
                languages.add(l);
            } else {
                warn("bundled resource missing or invalid: " + ROOT + "lang/" + code + ".json");
            }
        }
        return new Catalog(profiles, items, effects, loot, messages, themes, tags, templates, balance, warnings,
            languages);
    }

    private static void addOrReplace(List<ProfileDef> profiles, ProfileDef p) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id().equals(p.id())) {
                profiles.set(i, p);
                return;
            }
        }
        profiles.add(p);
    }

    private List<String> worldProfileFiles() {
        Set<String> names = new TreeSet<>();
        try {
            for (String f : store.list(WORLD_DIR + "profiles")) {
                if (f.endsWith(".json") && !f.equals("index.json")) {
                    names.add(f);
                }
            }
        } catch (RuntimeException e) {
            warn("could not list " + WORLD_DIR + "profiles: " + e);
        }
        Optional<String> idx = safeRead(WORLD_DIR + "profiles/index.json");
        if (idx.isPresent()) {
            JsonObject o = parseText(idx.get(), WORLD_DIR + "profiles/index.json");
            for (String f : Json.strings(o, "profiles")) {
                if (f.endsWith(".json") && !f.contains("..") && !f.contains("/")) {
                    names.add(f);
                }
            }
        }
        return new ArrayList<>(names);
    }

    private Optional<String> safeRead(String name) {
        try {
            Optional<String> r = store.read(name);
            return r == null ? Optional.empty() : r;
        } catch (RuntimeException e) {
            warn("could not read " + name + ": " + e);
            return Optional.empty();
        }
    }

    /** Reads a bundled resource as text; null if missing. */
    public static String readResource(String relative) {
        try (InputStream in = QuestGeneratorV2.class.getResourceAsStream(ROOT + relative)) {
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private JsonObject resourceJson(String relative) {
        String text = readResource(relative);
        if (text == null) {
            warn("bundled resource missing: " + ROOT + relative);
            return null;
        }
        return parseText(text, ROOT + relative);
    }

    private JsonObject parseText(String text, String name) {
        try {
            return Json.parseObject(text);
        } catch (RuntimeException e) {
            warn("could not parse " + name + ": " + e.getMessage());
            return null;
        }
    }

    private void warn(String msg) {
        warnings.add(msg);
        log.warn("[GenV2] " + msg);
    }

    // ------------------------------------------------------------------ tags

    private Map<String, TagConcept> parseTags(JsonObject root) {
        Map<String, TagConcept> out = new LinkedHashMap<>();
        JsonObject concepts = Json.obj(root, "concepts");
        if (concepts == null) {
            return out;
        }
        for (Map.Entry<String, JsonElement> e : concepts.entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            JsonObject o = e.getValue().getAsJsonObject();
            ContentKind kind;
            try {
                kind = ContentKind.valueOf(Json.str(o, "kind", "item").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                warn("tag concept " + e.getKey() + ": bad kind");
                continue;
            }
            List<String> candidates = new ArrayList<>();
            for (String c : Json.strings(o, "candidates")) {
                String t = c.startsWith("#") ? c.substring(1) : c;
                if (Ids.isValid(t)) {
                    candidates.add(t);
                } else {
                    warn("tag concept " + e.getKey() + ": bad tag id " + c);
                }
            }
            String name = Json.str(o, "name", e.getKey());
            out.put(e.getKey(), new TagConcept(e.getKey(), kind, candidates, name, Json.str(o, "plural", name + "s")));
        }
        return out;
    }

    // ------------------------------------------------------------------ profiles

    private ProfileDef parseProfile(JsonObject root, String source, Map<String, TagConcept> tags, boolean vanilla) {
        if (root == null) {
            return null;
        }
        String id = Json.str(root, "id", vanilla ? "vanilla" : null);
        if (id == null || id.isBlank()) {
            warn(source + ": profile without id skipped");
            return null;
        }
        id = id.toLowerCase(Locale.ROOT);
        List<String> requiresMod = Json.strings(root, "requiresMod");
        if (!vanilla && requiresMod.isEmpty()) {
            warn(source + ": profile " + id + " has no requiresMod; skipped");
            return null;
        }
        Set<String> loaders = new LinkedHashSet<>();
        for (String l : Json.strings(root, "loaders")) {
            loaders.add(l.toLowerCase(Locale.ROOT));
        }
        JsonObject mc = Json.obj(root, "minecraft");
        String mcMin = Json.str(mc, "min", null);
        String mcMax = Json.str(mc, "max", null);

        List<EntryDef> entries = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (JsonObject e : Json.objects(root, "entries")) {
            EntryDef def = parseEntry(e, id, source, tags);
            if (def != null) {
                if (!keys.add(def.key())) {
                    warn(source + ": duplicate entry key " + def.key() + " skipped");
                    continue;
                }
                entries.add(def);
            }
        }
        JsonObject rewards = Json.obj(root, "rewards");
        List<RewardDefs.Item> items = parseItems(Json.objects(rewards, "items"), id);
        List<RewardDefs.Effect> effects = parseEffects(Json.objects(rewards, "effects"), id);
        List<RewardDefs.Loot> loot = parseLoot(Json.objects(rewards, "lootTables"), id);
        List<ThemeDef> themes = parseThemes(Json.objects(root, "themes"), id);
        Map<String, ProfileDef.DimensionUnlock> dims = new LinkedHashMap<>();
        for (JsonObject d : Json.objects(root, "dimensions")) {
            String dim = Json.str(d, "id", null);
            if (!Ids.isValid(dim)) {
                warn(source + ": bad dimension id " + dim);
                continue;
            }
            String adv = Json.str(d, "advancement", null);
            dims.put(dim, new ProfileDef.DimensionUnlock(dim, Ids.isValid(adv) ? adv : null,
                Json.dbl(d, "share", 0.25), Json.lng(d, "day", 0), clampTier(Json.integer(d, "tier", 3))));
        }
        return new ProfileDef(id, Json.str(root, "name", id), source, List.copyOf(requiresMod), loaders, mcMin, mcMax,
            entries, items, effects, loot, themes, dims);
    }

    private EntryDef parseEntry(JsonObject e, String profile, String source, Map<String, TagConcept> tags) {
        String key = Json.str(e, "key", null);
        if (key == null || key.isBlank()) {
            warn(source + ": entry without key skipped");
            return null;
        }
        String where = source + " entry " + key;
        String family = Json.str(e, "family", null);
        if (family == null || family.isBlank()) {
            warn(where + ": no family; skipped");
            return null;
        }
        int tier = clampTier(Json.integer(e, "tier", 0));
        String dimension = Json.str(e, "dimension", "minecraft:overworld");
        String tool = Json.str(e, "tool", "none");
        List<String> hints = lower(Json.strings(e, "hints"));
        List<TargetDef> targets = new ArrayList<>();
        for (JsonObject t : Json.objects(e, "targets")) {
            TargetDef td = parseTarget(t, where, tags);
            if (td != null) {
                targets.add(td);
            }
        }
        if (targets.isEmpty()) {
            warn(where + ": no usable targets; skipped");
            return null;
        }
        return new EntryDef(profile, key, Json.str(e, "name", null), family, tier, dimension, tool, hints,
            Json.strings(e, "requires"), Json.str(e, "since", null), Json.str(e, "notes", null),
            Json.strings(e, "exclusions"), List.copyOf(targets), Math.max(0.0, Json.dbl(e, "weight", 1.0)));
    }

    private TargetDef parseTarget(JsonObject t, String where, Map<String, TagConcept> tags) {
        String typeName = Json.str(t, "type", null);
        ObjectiveType type = ObjectiveType.parse(typeName);
        if (type == null) {
            String shortName = typeName == null ? "null" : typeName.replace("justquests:", "");
            warn(where + ": " + (ObjectiveType.FORBIDDEN.contains(shortName) ? "forbidden" : "unknown")
                + " objective type " + typeName + "; target skipped");
            return null;
        }
        String id = Json.str(t, "id", null);
        String tag = Json.str(t, "tag", null);
        if (tag != null && !tags.containsKey(tag)) {
            warn(where + ": unknown tag concept " + tag + "; tag ignored");
            tag = null;
        }
        if (tag != null && (!type.taggable() || tags.get(tag).kind() != type.kind())) {
            warn(where + ": tag concept " + tag + " is not a " + type.kind() + " tag; tag ignored");
            tag = null;
        }
        String potion = Json.str(t, "potion", null);
        if (potion != null && (!type.itemBased() || !Ids.isValid(potion))) {
            warn(where + ": potion filter " + potion + " needs an item objective and a valid id; ignored");
            potion = null;
        }
        if (!Ids.isValid(id) && tag == null) {
            warn(where + ": bad or missing id " + id + "; target skipped");
            return null;
        }
        List<String> alts = new ArrayList<>();
        for (String a : Json.strings(t, t.has("alts") ? "alts" : "alt")) {
            if (Ids.isValid(a)) {
                alts.add(a);
            } else {
                warn(where + ": bad alternative id " + a);
            }
        }
        double effort = Json.dbl(t, "effort", -1);
        if (!(effort > 0)) {
            warn(where + ": target " + id + " has no positive effort; skipped");
            return null;
        }
        int min = type.counted() ? Math.max(1, Json.integer(t, "min", 1)) : 1;
        int max = type.counted() ? Json.integer(t, "max", Math.max(min, 16)) : 1;
        if (max < min) {
            warn(where + ": target " + id + " has min > max; skipped");
            return null;
        }
        int minDifficulty = 0;
        String md = Json.str(t, "minDifficulty", null);
        if (md != null) {
            minDifficulty = Difficulty.parse(md).map(Enum::ordinal).orElse(0);
        }
        int tierOverride = t.has("tier") ? clampTier(Json.integer(t, "tier", 0)) : -1;
        String dim = Json.str(t, "dimension", null);
        return new TargetDef(type, Ids.isValid(id) ? id : null, List.copyOf(alts), tag, effort, min, max,
            Math.max(0.0, Json.dbl(t, "weight", 1.0)), tierOverride, Json.str(t, "tool", null),
            lower(Json.strings(t, "hints")), Json.str(t, "name", null), Json.str(t, "plural", null),
            Json.str(t, "hint", null), Json.integer(t, "stack", 0), Ids.isValid(dim) ? dim : null, minDifficulty,
            Json.bool(t, "tamable", false), Json.str(t, "note", null), Json.str(t, "since", null), potion);
    }

    // ------------------------------------------------------------------ rewards & themes

    private List<RewardDefs.Item> parseItems(List<JsonObject> list, String profile) {
        List<RewardDefs.Item> out = new ArrayList<>();
        for (JsonObject o : list) {
            String id = Json.str(o, "id", null);
            double value = Json.dbl(o, "value", -1);
            if (!Ids.isValid(id) || !(value > 0)) {
                warn("reward item " + id + " (" + profile + "): bad id or value; skipped");
                continue;
            }
            out.add(new RewardDefs.Item(profile, id, Json.strings(o, "alt"), value, clampTier(Json.integer(o, "tier", 0)),
                Math.max(1, Json.integer(o, "max", 64)), Json.integer(o, "stack", 64), Json.str(o, "family", ""),
                Json.strings(o, "avoidFamilies"), Math.max(0.0, Json.dbl(o, "weight", 1.0)),
                difficultyIndex(Json.str(o, "minDifficulty", "easy"))));
        }
        return out;
    }

    private List<RewardDefs.Effect> parseEffects(List<JsonObject> list, String profile) {
        List<RewardDefs.Effect> out = new ArrayList<>();
        for (JsonObject o : list) {
            String id = Json.str(o, "id", null);
            double v = Json.dbl(o, "valuePerMinute", -1);
            if (!Ids.isValid(id) || !(v > 0)) {
                warn("effect reward " + id + ": bad id or value; skipped");
                continue;
            }
            int minS = Math.max(10, Json.integer(o, "minSeconds", 60));
            int maxS = Math.max(minS, Json.integer(o, "maxSeconds", 300));
            out.add(new RewardDefs.Effect(profile, id, v, minS, maxS, Math.max(0, Json.integer(o, "amplifier", 0)),
                difficultyIndex(Json.str(o, "minDifficulty", "normal")), Math.max(0.0, Json.dbl(o, "weight", 1.0))));
        }
        return out;
    }

    private List<RewardDefs.Loot> parseLoot(List<JsonObject> list, String profile) {
        List<RewardDefs.Loot> out = new ArrayList<>();
        for (JsonObject o : list) {
            String id = Json.str(o, "id", null);
            double v = Json.dbl(o, "value", -1);
            if (!Ids.isValid(id) || !(v > 0)) {
                warn("loot table reward " + id + ": bad id or value; skipped");
                continue;
            }
            out.add(new RewardDefs.Loot(profile, id, v, clampTier(Json.integer(o, "tier", 1)),
                difficultyIndex(Json.str(o, "minDifficulty", "hard")), Math.max(0.0, Json.dbl(o, "weight", 1.0))));
        }
        return out;
    }

    private List<ThemeDef> parseThemes(List<JsonObject> list, String profile) {
        List<ThemeDef> out = new ArrayList<>();
        for (JsonObject o : list) {
            String key = Json.str(o, "key", null);
            List<String> names = Json.strings(o, "names");
            if (key == null || names.isEmpty()) {
                warn("theme " + key + " (" + profile + "): missing key or names; skipped");
                continue;
            }
            List<ThemeDef.Slot> slots = new ArrayList<>();
            for (JsonObject s : Json.objects(o, "slots")) {
                Set<ObjectiveType> types = EnumSet.noneOf(ObjectiveType.class);
                List<String> typeNames = new ArrayList<>(Json.strings(s, "types"));
                typeNames.addAll(Json.strings(s, "type"));
                boolean bad = false;
                for (String tn : typeNames) {
                    ObjectiveType ot = ObjectiveType.parse(tn);
                    if (ot == null) {
                        warn("theme " + key + ": unknown slot type " + tn);
                        bad = true;
                    } else {
                        types.add(ot);
                    }
                }
                if (bad) {
                    continue;
                }
                slots.add(new ThemeDef.Slot(types, new LinkedHashSet<>(Json.strings(s, "keys")),
                    new LinkedHashSet<>(Json.strings(s, "families")), new LinkedHashSet<>(Json.strings(s, "profiles")),
                    Json.bool(s, "optional", false)));
            }
            if (slots.size() < 2) {
                warn("theme " + key + ": needs at least two slots; skipped");
                continue;
            }
            Set<String> req = new LinkedHashSet<>(Json.strings(o, "requiresProfiles"));
            if (!"vanilla".equals(profile)) {
                req.add(profile);
            }
            List<String> desc = new ArrayList<>(Json.strings(o, "descriptions"));
            desc.addAll(Json.strings(o, "description"));
            out.add(new ThemeDef(profile, key, List.copyOf(names), List.copyOf(desc), List.copyOf(slots), req,
                difficultyIndex(Json.str(o, "minDifficulty", "normal")), difficultyIndex(Json.str(o, "maxDifficulty", "hard")),
                Math.max(0.0, Json.dbl(o, "weight", 1.0))));
        }
        return out;
    }

    private static int difficultyIndex(String s) {
        return Difficulty.parse(s).map(Enum::ordinal).orElse(0);
    }

    private static int clampTier(int t) {
        return Math.max(0, Math.min(4, t));
    }

    private static List<String> lower(List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            out.add(s.toLowerCase(Locale.ROOT));
        }
        return out;
    }
}
