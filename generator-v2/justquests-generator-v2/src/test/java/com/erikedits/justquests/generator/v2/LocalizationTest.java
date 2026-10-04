package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.EntryDef;
import com.erikedits.justquests.generator.v2.internal.catalog.Localization;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.ProfileDef;
import com.erikedits.justquests.generator.v2.internal.catalog.TargetDef;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The extra languages (lang/*.json): every one loads, every catalog target has a translated name,
 * and generated quests carry all five languages in title, description and message, with no English
 * hint left in a translated description.
 */
class LocalizationTest {
    private static final List<String> ALL = List.of("en_us", "de_de", "fr_fr", "es_es", "ja_jp");

    @Test
    void everyLanguageLoads() {
        Catalog c = TestSupport.catalog();
        List<String> codes = new ArrayList<>();
        for (Localization l : c.languages) {
            codes.add(l.code);
            assertTrue(l.names.size() > 800, l.code + " names: " + l.names.size());
            assertTrue(!l.templates.titles.isEmpty() && !l.templates.phrases.isEmpty(), l.code + " templates");
            assertEquals(c.messages.size(), l.messages.size(), l.code + " messages");
        }
        assertEquals(Localization.CODES, codes);
    }

    @Test
    void everyTargetHasATranslatedName() {
        Catalog c = TestSupport.catalog();
        List<String> missing = new ArrayList<>();
        for (Localization l : c.languages) {
            for (ProfileDef p : c.profiles) {
                for (EntryDef e : p.entries()) {
                    for (TargetDef t : e.targets()) {
                        if (t.type() == ObjectiveType.VISIT_DIMENSION) {
                            continue;
                        }
                        String key = t.potion() != null ? t.id() + "{" + t.potion() + "}" : t.id();
                        if (!l.names.containsKey(key)) {
                            missing.add(l.code + " " + key);
                        }
                        if (t.tagConcept() != null && !l.tagNouns.containsKey(t.tagConcept())) {
                            missing.add(l.code + " tag " + t.tagConcept());
                        }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "missing names: " + missing);
    }

    @Test
    void generatedQuestsCarryAllLanguages() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania",
            "ae2", "immersiveengineering", "biomesoplenty", "alexsmobs", "tconstruct", "ars_nouveau");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        Set<String> englishHints = new HashSet<>();
        for (ProfileDef pr : TestSupport.catalog().profiles) {
            for (EntryDef e : pr.entries()) {
                for (TargetDef t : e.targets()) {
                    if (t.hint() != null && !t.hint().isBlank()) {
                        englishHints.add(t.hint().trim());
                    }
                }
            }
        }
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 25; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 20).withModdedShare(0.5), p,
                    seed * 17L, 20).drafts()) {
                    checked++;
                    check(q.json, "title", englishHints, failures);
                    if (q.json.has("description")) {
                        check(q.json, "description", englishHints, failures);
                    }
                    for (JsonElement r : q.json.getAsJsonArray("rewards")) {
                        JsonObject o = r.getAsJsonObject();
                        if (o.has("message")) {
                            check(o, "message", Set.of(), failures);
                        }
                    }
                }
            }
        }
        assertTrue(checked > 500, "quests checked: " + checked);
        assertTrue(failures.isEmpty(), failures.size() + " problems, first: "
            + failures.subList(0, Math.min(20, failures.size())));
    }

    private static void check(JsonObject o, String key, Set<String> englishHints, List<String> failures) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonObject()) {
            failures.add(key + " not a language map: " + e);
            return;
        }
        JsonObject m = e.getAsJsonObject();
        for (String lang : ALL) {
            if (!m.has(lang) || m.get(lang).getAsString().isBlank()) {
                failures.add(key + " lacks " + lang + ": " + m);
                continue;
            }
            String s = m.get(lang).getAsString();
            if (s.contains("{") || s.contains("}") || s.contains("  ") || !s.equals(s.trim())) {
                failures.add(key + " " + lang + " malformed: " + s);
            }
            if (!lang.equals("en_us")) {
                for (String h : englishHints) {
                    if (s.contains(h)) {
                        failures.add(key + " " + lang + " contains the English hint '" + h + "': " + s);
                    }
                }
            }
        }
    }
}
