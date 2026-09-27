package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.Templates;
import com.erikedits.justquests.generator.v2.internal.util.English;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Rng;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Titles and descriptions from templates (§9.6). Titles aim for &le; 22 characters (hard max 32)
 * and are unique within a set; descriptions carry the catalog's hints and stay &le; 140 characters.
 */
public final class TextBuilder {
    public static final int TITLE_IDEAL = 22;
    public static final int TITLE_MAX = 32;
    public static final int DESCRIPTION_MAX = 140;

    private final Templates t;

    public TextBuilder(Templates templates) {
        this.t = templates;
    }

    /** Picks a title not in {@code used} (lower-case set; not modified). Returns null if none fits. */
    public String title(QuestDraft d, Set<String> used, Rng rng) {
        List<String> options = new ArrayList<>(shuffled(d.themeNames, rng));
        QuestDraft.Objective main = d.objectives.get(0);
        if (d.comboFamily != null) {
            for (String tpl : shuffled(t.comboTitles, rng)) {
                options.add(fill(tpl, main).replace("{Family}", familyName(d.comboFamily)));
            }
        }
        List<String> typeTitles = t.titles.getOrDefault(main.c().type(), List.of());
        List<String> variantTitles = t.variant(main.c().hints(), "titles");
        if (variantTitles != null) {
            typeTitles = variantTitles;
        }
        for (String tpl : shuffled(typeTitles, rng)) {
            options.add(fill(tpl, main));
        }
        for (String tpl : shuffled(t.fallbackTitles, rng)) {
            options.add(fill(tpl, main));
        }
        String pick = firstFitting(options, used, TITLE_IDEAL);
        if (pick == null) {
            pick = firstFitting(options, used, TITLE_MAX);
        }
        if (pick == null) {
            // last resort: shortened name plus a numeral
            String base = shorten(main.c().name(), TITLE_MAX - 4);
            for (String suffix : new String[]{"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"}) {
                String cand = base + suffix;
                if (!used.contains(cand.toLowerCase(Locale.ROOT))) {
                    pick = cand;
                    break;
                }
            }
        }
        return pick;
    }

    private static String firstFitting(List<String> options, Set<String> used, int max) {
        for (String o : options) {
            if (o.length() <= max && !o.contains("{") && !used.contains(o.toLowerCase(Locale.ROOT))) {
                return o;
            }
        }
        return null;
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max).trim();
    }

    private static List<String> shuffled(List<String> in, Rng rng) {
        List<String> out = new ArrayList<>(in);
        for (int i = out.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            String x = out.get(i);
            out.set(i, out.get(j));
            out.set(j, x);
        }
        return out;
    }

    /** Fills a title template; a title always starts upper case ("{Dimension} Trip" → "The Nether Trip"). */
    private String fill(String tpl, QuestDraft.Objective o) {
        Candidate c = o.c();
        return English.capitalize(tpl.replace("{Name}", c.name())
            .replace("{Names}", c.plural())
            .replace("{Dimension}", dimensionName(c.target())));
    }

    public String familyName(String family) {
        String n = t.familyNames.get(family);
        if (n != null) {
            return n;
        }
        String f = family;
        int us = f.indexOf('_');
        if (us > 0 && us <= 7 && f.length() > us + 1) {
            f = f.substring(us + 1); // drop a short mod prefix such as "fd_" or "create_"
        }
        return English.titleCase(f.replace('_', ' '));
    }

    public String dimensionName(String dim) {
        String n = t.dimensionNames.get(dim);
        return n != null ? n : Ids.prettify(dim);
    }

    /** Objective phrase, e.g. "Mine 16 iron ore". */
    public String phrase(QuestDraft.Objective o, Rng rng) {
        Candidate c = o.c();
        List<String> pool = t.phrases.getOrDefault(c.type(), List.of());
        List<String> variant = t.variant(c.hints(), "phrases");
        if (variant != null) {
            pool = variant;
        }
        String tpl = pool.isEmpty() ? defaultPhrase(c.type()) : pool.get(rng.nextInt(pool.size()));
        String nameSingular = English.inSentence(c.name());
        String names = o.count() == 1 ? nameSingular : English.inSentence(c.plural());
        if (c.isTag() && c.tagNoun() != null) {
            nameSingular = c.tagNoun();
            names = o.count() == 1 ? c.tagNoun() : English.plural(c.tagNoun());
        }
        if (variant != null && c.hints().contains("cook")) {
            // "Cook 16 cooked bacon" → "Cook 16 bacon"
            nameSingular = nameSingular.replaceFirst("^cooked ", "");
            names = names.replaceFirst("^cooked ", "");
        }
        if (o.count() == 1 && tpl.contains("{count} {names}")) {
            tpl = tpl.replace("{count} {names}", "{a_name}");
        }
        return tpl.replace("{count}", Integer.toString(o.count()))
            .replace("{names}", names)
            .replace("{name}", nameSingular)
            .replace("{a_name}", English.withArticle(nameSingular))
            .replace("{dimension}", dimensionName(c.target()));
    }

    private static String defaultPhrase(ObjectiveType type) {
        switch (type) {
            case VISIT_DIMENSION:
                return "Travel to {dimension}";
            case TAME_ANIMAL:
                return "Tame {count} {names}";
            default:
                return "Get {count} {names}";
        }
    }

    /** Builds the description: objective phrases, then hints, trimmed to 140 characters. */
    public String description(QuestDraft d, List<String> themeTemplates, Rng rng) {
        List<String> phrases = new ArrayList<>();
        for (QuestDraft.Objective o : d.objectives) {
            phrases.add(phrase(o, rng));
        }
        String list = joinPhrases(phrases);
        List<String> sentences = new ArrayList<>();
        if (themeTemplates != null && !themeTemplates.isEmpty()) {
            String tpl = themeTemplates.get(rng.nextInt(themeTemplates.size()));
            String inList = tpl.contains(": {list}") ? list.substring(0, 1).toLowerCase(Locale.ROOT) + list.substring(1) : list;
            sentences.add(ensurePeriod(English.capitalize(tpl.replace("{list}", inList))));
        } else {
            sentences.add(ensurePeriod(English.capitalize(list)));
        }
        Set<String> hints = new LinkedHashSet<>();
        String bestTool = null;
        int bestLevel = -1;
        for (QuestDraft.Objective o : d.objectives) {
            String tool = o.c().tool();
            int lvl = toolRank(tool);
            if (lvl > bestLevel && t.tools.containsKey(tool)) {
                bestLevel = lvl;
                bestTool = tool;
            }
        }
        for (QuestDraft.Objective o : d.objectives) {
            for (String h : hintSentences(o.c())) {
                // only the strongest pickaxe requirement is mentioned
                if (isPickaxeHint(h) && (bestTool == null || !h.equals(ensurePeriod(t.tools.get(bestTool))))) {
                    continue;
                }
                hints.add(h);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String s : sentences) {
            append(sb, s);
        }
        if (sb.length() > DESCRIPTION_MAX) {
            // theme flavour too long: fall back to the plain list
            sb.setLength(0);
            append(sb, ensurePeriod(English.capitalize(list)));
        }
        for (String h : hints) {
            if (sb.length() + 1 + h.length() <= DESCRIPTION_MAX) {
                append(sb, h);
            }
        }
        String out = sb.toString();
        if (out.length() > DESCRIPTION_MAX) {
            out = out.substring(0, DESCRIPTION_MAX - 3).trim() + "...";
        }
        return out;
    }

    private static void append(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(s);
    }

    private static String ensurePeriod(String s) {
        return s.endsWith(".") || s.endsWith("!") || s.endsWith("?") ? s : s + ".";
    }

    private static String joinPhrases(List<String> phrases) {
        if (phrases.size() == 1) {
            return phrases.get(0);
        }
        List<String> lower = new ArrayList<>();
        for (int i = 0; i < phrases.size(); i++) {
            String p = phrases.get(i);
            lower.add(i == 0 ? p : p.substring(0, 1).toLowerCase(Locale.ROOT) + p.substring(1));
        }
        if (lower.size() == 2) {
            return lower.get(0) + " and " + lower.get(1);
        }
        return String.join(", ", lower.subList(0, lower.size() - 1)) + " and " + lower.get(lower.size() - 1);
    }

    private static int toolRank(String tool) {
        if (tool == null) {
            return -1;
        }
        switch (tool) {
            case "netherite":
                return 5;
            case "diamond":
                return 4;
            case "iron":
                return 3;
            case "stone":
                return 2;
            default:
                return 0;
        }
    }

    private boolean isPickaxeHint(String sentence) {
        for (String tool : new String[]{"stone", "iron", "diamond", "netherite"}) {
            String p = t.tools.get(tool);
            if (p != null && ensurePeriod(p).equals(sentence)) {
                return true;
            }
        }
        return false;
    }

    /** Hint sentences for one candidate, most useful first. */
    public List<String> hintSentences(Candidate c) {
        List<String> out = new ArrayList<>();
        if (c.def().hint() != null && !c.def().hint().isBlank()) {
            out.add(ensurePeriod(c.def().hint().trim()));
        }
        if (c.isTag() && c.tagNoun() != null) {
            out.add(t.sentence("tag", "Any {noun} counts.").replace("{noun}", c.tagNoun()));
        }
        String tool = c.tool();
        if (tool != null && !tool.equals("none") && !tool.equals("wood")) {
            String phrase = t.tools.get(tool);
            if (phrase != null) {
                out.add(ensurePeriod(phrase));
            }
        }
        for (String h : c.hints()) {
            String s = t.hints.get(h);
            if (s != null) {
                out.add(ensurePeriod(s));
            }
        }
        if (c.type() != ObjectiveType.VISIT_DIMENSION && c.dimension() != null
            && !"minecraft:overworld".equals(c.dimension())) {
            out.add(t.sentence("dimension", "Found in {dimension}.").replace("{dimension}", dimensionName(c.dimension())));
        }
        return out;
    }
}
