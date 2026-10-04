package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Text quality over a few thousand generated quests with all bundled profiles active: spacing,
 * punctuation, articles, repeated words or sentences, and counts of one written as numbers.
 */
class TextQualityTest {
    private static final Pattern REPEATED_WORD = Pattern.compile("(?i)\\b([a-z]+) \\1\\b");
    private static final Pattern A_BEFORE_VOWEL = Pattern.compile("\\ba ([aeiouAEIOU]\\w*)");
    private static final Pattern AN_BEFORE_CONSONANT = Pattern.compile("\\ban ([b-df-hj-np-tv-zB-DF-HJ-NP-TV-Z]\\w*)");
    private static final Pattern ONE_AS_NUMBER = Pattern.compile("\\b1 [a-z]");
    private static final Set<String> A_EXCEPTIONS = Set.of("uni", "use", "one", "eu", "ur");
    private static final Set<String> AN_EXCEPTIONS = Set.of("hour", "honest", "honor", "heir");

    private static List<String> problems(String title, String description) {
        List<String> p = new ArrayList<>();
        for (String[] part : new String[][]{{"title", title}, {"description", description}}) {
            String s = part[1];
            if (s.contains("  ") || s.contains(" .") || s.contains(" ,") || s.contains("..") || s.contains(",.")) {
                p.add(part[0] + " spacing/punctuation");
            }
            if (!s.equals(s.trim())) {
                p.add(part[0] + " leading/trailing space");
            }
            Matcher m = REPEATED_WORD.matcher(s);
            while (m.find()) {
                p.add(part[0] + " repeated word '" + m.group() + "'");
            }
            m = A_BEFORE_VOWEL.matcher(s);
            while (m.find()) {
                String w = m.group(1).toLowerCase(Locale.ROOT);
                if (A_EXCEPTIONS.stream().noneMatch(w::startsWith)) {
                    p.add(part[0] + " 'a " + m.group(1) + "'");
                }
            }
            m = AN_BEFORE_CONSONANT.matcher(s);
            while (m.find()) {
                String w = m.group(1).toLowerCase(Locale.ROOT);
                if (AN_EXCEPTIONS.stream().noneMatch(w::startsWith)) {
                    p.add(part[0] + " 'an " + m.group(1) + "'");
                }
            }
            if (ONE_AS_NUMBER.matcher(s).find()) {
                p.add(part[0] + " count of one written as a number");
            }
        }
        if (!description.isEmpty()) {
            if (!Character.isUpperCase(description.charAt(0))) {
                p.add("description starts lower case");
            }
            if (!description.endsWith(".") && !description.endsWith("!")) {
                p.add("description without final period");
            }
            Set<String> sentences = new HashSet<>();
            for (String sentence : description.split("(?<=[.!]) ")) {
                if (!sentences.add(sentence.toLowerCase(Locale.ROOT))) {
                    p.add("sentence repeated: " + sentence);
                }
            }
        }
        if (!title.isEmpty() && !Character.isUpperCase(title.charAt(0)) && !Character.isDigit(title.charAt(0))) {
            p.add("title starts lower case");
        }
        return p;
    }

    @Test
    void generatedTextReadsCleanly() {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        Progression p = TestSupport.unlockedProgression(host);
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Difficulty d : Difficulty.values()) {
            for (long seed = 1; seed <= 60; seed++) {
                for (QuestDraft q : TestSupport.generate(host, TestSupport.config(d, 20), p, seed * 31L, 20).drafts()) {
                    checked++;
                    String title = Json.text(q.json, "title", "");
                    String description = Json.text(q.json, "description", "");
                    for (String problem : problems(title, description)) {
                        if (failures.size() < 40) {
                            failures.add(problem + " | " + title + " | " + description);
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 3000, "checked " + checked);
        assertTrue(failures.isEmpty(), failures.size() + " text problems, e.g.\n" + String.join("\n", failures));
    }

    @Test
    void heuristicsCatchTheirTargets() {
        assertTrue(problems("Iron Run", "Mine 8 iron ore.  Found in caves.").contains("description spacing/punctuation"));
        assertTrue(problems("Iron Run", "Craft a iron ingot.").stream().anyMatch(s -> s.contains("'a iron'")));
        assertTrue(problems("Iron Run", "Craft an bucket.").stream().anyMatch(s -> s.contains("'an bucket'")));
        assertTrue(problems("Iron Run", "Craft 1 bucket.").contains("description count of one written as a number"));
        assertTrue(problems("Iron Run", "Found in caves. Found in caves.").stream().anyMatch(s -> s.startsWith("sentence")));
        assertTrue(problems("Iron Run", "Craft a useful bucket. An hour of work.").isEmpty());
    }

    @Test
    void travelQuestTitlesStartUpperCase() {
        // only visit_dimension allowed: the sets consist of the Nether, End and Twilight Forest trips
        FakeHost host = new FakeHost().withMods("twilightforest");
        host.content.dimensions.add("twilightforest:twilight_forest");
        host.caps.objectiveTypes.clear();
        host.caps.objectiveTypes.add("justquests:visit_dimension");
        Progression p = TestSupport.unlockedProgression(host);
        int seen = 0;
        for (long seed = 1; seed <= 40; seed++) {
            for (QuestDraft q : TestSupport.generate(host, TestSupport.config(Difficulty.HARD, 3).withModdedShare(0.34), p,
                seed, 3).drafts()) {
                String title = Json.text(q.json, "title", "");
                assertTrue(Character.isUpperCase(title.charAt(0)), "title '" + title + "'");
                seen++;
            }
        }
        assertTrue(seen >= 40, "travel quests generated: " + seen);
    }
}
