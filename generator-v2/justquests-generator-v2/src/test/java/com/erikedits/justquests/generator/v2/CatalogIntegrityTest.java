package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** §16.17 — the bundled data is internally consistent (checked on the raw JSON, not through the loader). */
class CatalogIntegrityTest {
    private static final Set<String> TYPES = Set.of("collect_item", "mine_block", "craft_item", "smelt_item", "kill_mob",
        "breed_animal", "tame_animal", "consume_item", "place_block", "visit_dimension", "enchant_item", "use_item");
    private static final Set<String> ITEM_TYPES = Set.of("collect_item", "craft_item", "smelt_item", "consume_item",
        "enchant_item", "use_item");
    private static final Set<String> BLOCK_TYPES = Set.of("mine_block", "place_block");
    private static final Set<String> ENTITY_TYPES = Set.of("kill_mob", "breed_animal", "tame_animal");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_]+)}");

    private static JsonObject res(String path) {
        String text = CatalogLoader.readResource(path);
        assertTrue(text != null, "missing resource " + path);
        return Json.parseObject(text);
    }

    private static List<JsonObject> profiles() {
        List<JsonObject> out = new ArrayList<>();
        out.add(res("catalog/vanilla.json"));
        for (String f : Json.strings(res("catalog/profiles/index.json"), "profiles")) {
            out.add(res("catalog/profiles/" + f));
        }
        return out;
    }

    @Test
    void entriesAndTargetsAreWellFormed() {
        JsonObject tags = Json.obj(res("tags.json"), "concepts");
        JsonObject templates = res("templates.json");
        Set<String> hintTexts = Json.obj(templates, "hints").keySet();
        Set<String> toolTexts = Json.obj(templates, "tools").keySet();
        Set<String> hintKeys = Json.obj(res("balance.json"), "hintMultipliers").keySet();
        Set<String> variants = Json.obj(templates, "variants").keySet();
        List<String> problems = new ArrayList<>();
        for (JsonObject p : profiles()) {
            String pid = Json.str(p, "id", "?");
            if (!pid.equals("vanilla")) {
                assertTrue(!Json.strings(p, "requiresMod").isEmpty(), pid + " needs requiresMod");
            }
            Set<String> keys = new HashSet<>();
            for (JsonObject e : Json.objects(p, "entries")) {
                String where = pid + ":" + Json.str(e, "key", "?");
                if (!keys.add(Json.str(e, "key", ""))) {
                    problems.add(where + " duplicate key");
                }
                if (Json.str(e, "family", "").isBlank()) {
                    problems.add(where + " no family");
                }
                int tier = Json.integer(e, "tier", -1);
                if (tier < 0 || tier > 4) {
                    problems.add(where + " tier " + tier);
                }
                String tool = Json.str(e, "tool", "none");
                if (!tool.equals("none") && !tool.equals("wood") && !toolTexts.contains(tool)) {
                    problems.add(where + " tool without text " + tool);
                }
                for (String h : Json.strings(e, "hints")) {
                    if (!hintTexts.contains(h)) {
                        problems.add(where + " hint without text " + h);
                    }
                    if (!hintKeys.contains(h) && !h.equals("night") && !h.equals("caves") && !h.equals("farm")
                        && !h.equals("plains") && !h.equals("river")) {
                        problems.add(where + " hint without multiplier " + h);
                    }
                }
                if (!ID.matcher(Json.str(e, "dimension", "minecraft:overworld")).matches()) {
                    problems.add(where + " dimension");
                }
                for (JsonObject t : Json.objects(e, "targets")) {
                    String type = Json.str(t, "type", "");
                    String id = Json.str(t, "id", null);
                    String tw = where + " " + type + " " + id;
                    if (!TYPES.contains(type)) {
                        problems.add(tw + " type not allowed");
                    }
                    if (id == null || !ID.matcher(id).matches()) {
                        problems.add(tw + " bad id");
                    }
                    for (String a : Json.strings(t, "alt")) {
                        if (!ID.matcher(a).matches()) {
                            problems.add(tw + " bad alt " + a);
                        }
                    }
                    if (!(Json.dbl(t, "effort", 0) > 0)) {
                        problems.add(tw + " effort");
                    }
                    if (!type.equals("visit_dimension")) {
                        int min = Json.integer(t, "min", -1);
                        int max = Json.integer(t, "max", -1);
                        if (min < 1 || max < min) {
                            problems.add(tw + " min/max " + min + "/" + max);
                        }
                    }
                    String tag = Json.str(t, "tag", null);
                    String kind = ITEM_TYPES.contains(type) ? "item" : BLOCK_TYPES.contains(type) ? "block"
                        : ENTITY_TYPES.contains(type) ? "entity" : null;
                    if (tag != null && (!tags.has(tag) || kind == null
                        || !kind.equals(Json.str(Json.obj(tags, tag), "kind", "item")))) {
                        problems.add(tw + " bad tag " + tag);
                    }
                    String potion = Json.str(t, "potion", null);
                    if (potion != null && (!ITEM_TYPES.contains(type) || !ID.matcher(potion).matches())) {
                        problems.add(tw + " bad potion " + potion);
                    }
                    if (type.equals("tame_animal") && !Json.bool(t, "tamable", false)) {
                        problems.add(tw + " tame target not flagged tamable");
                    }
                    for (String h : Json.strings(t, "hints")) {
                        if (!hintTexts.contains(h) && !variants.contains(h)) { // variant hints only switch the wording
                            problems.add(tw + " hint without text " + h);
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void templatePlaceholdersAreResolvable() {
        JsonObject t = res("templates.json");
        Set<String> titleOk = Set.of("Name", "Names", "Dimension", "Family");
        Set<String> phraseOk = Set.of("count", "names", "name", "a_name", "dimension");
        List<String> problems = new ArrayList<>();
        checkPlaceholders(Json.obj(t, "titles"), titleOk, problems);
        checkPlaceholders(Json.obj(t, "phrases"), phraseOk, problems);
        for (String s : Json.strings(t, "fallbackTitles")) {
            check(s, titleOk, problems);
        }
        for (String s : Json.strings(t, "comboTitles")) {
            check(s, titleOk, problems);
        }
        for (String h : Json.obj(t, "hints").keySet()) {
            check(Json.str(Json.obj(t, "hints"), h, ""), Set.of(), problems);
        }
        check(Json.str(Json.obj(t, "sentences"), "tag", ""), Set.of("noun"), problems);
        for (Map.Entry<String, JsonElement> v : Json.obj(t, "variants").entrySet()) {
            for (String s : Json.strings(v.getValue().getAsJsonObject(), "phrases")) {
                check(s, phraseOk, problems);
            }
            for (String s : Json.strings(v.getValue().getAsJsonObject(), "titles")) {
                check(s, titleOk, problems);
            }
        }
        check(Json.str(Json.obj(t, "sentences"), "dimension", ""), Set.of("dimension"), problems);
        List<JsonObject> themes = new ArrayList<>(Json.objects(res("themes.json"), "themes"));
        for (JsonObject p : profiles()) {
            themes.addAll(Json.objects(p, "themes"));
        }
        for (JsonObject th : themes) {
            for (String d : Json.strings(th, "descriptions")) {
                check(d, Set.of("list"), problems);
            }
            for (String n : Json.strings(th, "names")) {
                if (n.length() > 32 || n.contains("{")) {
                    problems.add("theme name " + n);
                }
            }
        }
        for (String m : Json.strings(res("rewards.json"), "messages")) {
            check(m, Set.of(), problems);
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static void checkPlaceholders(JsonObject byType, Set<String> ok, List<String> problems) {
        for (Map.Entry<String, JsonElement> e : byType.entrySet()) {
            if (!TYPES.contains(e.getKey())) {
                problems.add("template for unknown type " + e.getKey());
            }
            for (JsonElement s : e.getValue().getAsJsonArray()) {
                check(s.getAsString(), ok, problems);
            }
        }
    }

    private static void check(String s, Set<String> ok, List<String> problems) {
        Matcher m = PLACEHOLDER.matcher(s);
        while (m.find()) {
            if (!ok.contains(m.group(1))) {
                problems.add("unresolvable placeholder " + m.group() + " in \"" + s + "\"");
            }
        }
    }

    @Test
    void themesReferenceExistingContent() {
        Set<String> keys = new HashSet<>();
        Set<String> families = new HashSet<>();
        for (JsonObject p : profiles()) {
            for (JsonObject e : Json.objects(p, "entries")) {
                keys.add(Json.str(e, "key", ""));
                families.add(Json.str(e, "family", ""));
            }
        }
        List<JsonObject> themes = new ArrayList<>(Json.objects(res("themes.json"), "themes"));
        for (JsonObject p : profiles()) {
            themes.addAll(Json.objects(p, "themes"));
        }
        List<String> problems = new ArrayList<>();
        for (JsonObject th : themes) {
            for (JsonObject s : Json.objects(th, "slots")) {
                for (String k : Json.strings(s, "keys")) {
                    if (!keys.contains(k)) {
                        problems.add(Json.str(th, "key", "?") + " unknown key " + k);
                    }
                }
                for (String f : Json.strings(s, "families")) {
                    if (!families.contains(f)) {
                        problems.add(Json.str(th, "key", "?") + " unknown family " + f);
                    }
                }
                for (String t : Json.strings(s, "types")) {
                    if (!TYPES.contains(t)) {
                        problems.add(Json.str(th, "key", "?") + " unknown type " + t);
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void rewardTableIsWellFormed() {
        List<String> problems = new ArrayList<>();
        List<JsonObject> items = new ArrayList<>(Json.objects(res("rewards.json"), "items"));
        for (JsonObject p : profiles()) {
            items.addAll(Json.objects(Json.obj(p, "rewards"), "items"));
        }
        for (JsonObject it : items) {
            String id = Json.str(it, "id", "");
            if (!ID.matcher(id).matches() || !(Json.dbl(it, "value", 0) > 0) || Json.str(it, "family", "").isBlank()) {
                problems.add("reward item " + id);
            }
        }
        for (JsonObject l : Json.objects(res("rewards.json"), "lootTables")) {
            if (!ID.matcher(Json.str(l, "id", "")).matches()) {
                problems.add("loot table " + l);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void loaderAcceptsEverythingAndCoverageTargetsAreMet() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        var catalog = new CatalogLoader(host.log, host.store).load();
        assertEquals(List.of(), catalog.loadWarnings, "bundled data must load without warnings");
        var r = Generation.run(catalog, host, TestSupport.config(Difficulty.HARD, 1), TestSupport.unlockedProgression(host),
            Map.of(), Set.of(), List.of(), 1, 1L, 1L, 0);
        long vanilla = r.pool().candidates().stream().filter(c -> c.profile().equals("vanilla")).count();
        long fd = r.pool().candidates().stream().filter(c -> c.profile().equals("farmersdelight")).count();
        long create = r.pool().candidates().stream().filter(c -> c.profile().equals("create")).count();
        // usable = passes the achievability pipeline on a build where all ids exist (HARD: every tier, all unlocked)
        int vanillaTargets = catalog.targetCount("vanilla");
        assertTrue(vanillaTargets >= 150, "vanilla catalog targets " + vanillaTargets);
        assertTrue(vanilla >= 150, "usable vanilla targets " + vanilla);
        assertTrue(fd >= 40, "usable Farmer's Delight targets " + fd);
        assertTrue(create >= 40, "usable Create targets " + create);
        for (String[] m : new String[][]{{"mekanism", "25"}, {"twilightforest", "25"}, {"botania", "12"}}) {
            long usable = r.pool().candidates().stream().filter(c -> c.profile().equals(m[0])).count();
            assertTrue(usable >= Long.parseLong(m[1]), "usable " + m[0] + " targets " + usable);
        }
        long fdThemes = catalog.themes.stream().filter(t -> t.profile().equals("farmersdelight")).count();
        long createThemes = catalog.themes.stream().filter(t -> t.profile().equals("create")).count();
        assertTrue(fdThemes >= 3 && createThemes >= 3, "mod themes " + fdThemes + "/" + createThemes);
        assertTrue(catalog.themes.stream().anyMatch(t -> t.profile().equals("farmersdelight")
            && t.slots().stream().anyMatch(s -> s.profiles().contains("vanilla") || s.families().contains("crops"))),
            "a theme mixing vanilla and a mod");
    }
}
