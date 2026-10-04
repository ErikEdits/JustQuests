package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.internal.catalog.Localization;
import com.erikedits.justquests.generator.v2.internal.catalog.ObjectiveType;
import com.erikedits.justquests.generator.v2.internal.catalog.Templates;
import com.erikedits.justquests.generator.v2.internal.util.English;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Titles and descriptions from templates (§9.6). Titles aim for &le; 22 characters (hard max 32)
 * and are unique within a set; descriptions carry the catalog's hints and stay &le; 140 characters.
 *
 * <p>All choices (which title template, phrase template, theme text) are made once, in English; the
 * same choices are then rendered in every extra {@link Localization} by {@link #localize}.
 */
public final class TextBuilder {
    public static final int TITLE_IDEAL = 22;
    public static final int TITLE_MAX = 32;
    public static final int DESCRIPTION_MAX = 140;
    public static final String ENGLISH = "en_us";

    static final int SRC_THEME = 0;
    static final int SRC_COMBO = 1;
    static final int SRC_TYPE = 2;
    static final int SRC_FALLBACK = 3;
    static final int SRC_NUMERAL = 4;
    private static final String[] NUMERALS = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
    private static final Pattern COLON_LIST = Pattern.compile("[:：]\\s*\\{list\\}");

    private final Templates t;
    private final Localization en;
    private final List<String> messages;
    private final List<Localization> languages;

    public TextBuilder(Templates templates) {
        this(templates, List.of(), List.of());
    }

    /**
     * @param templates English templates
     * @param messages  English reward messages (index-aligned with each language's messages)
     * @param languages extra languages rendered by {@link #localize}
     */
    public TextBuilder(Templates templates, List<String> messages, List<Localization> languages) {
        this.t = templates;
        this.en = Localization.english(templates);
        this.messages = List.copyOf(messages);
        this.languages = List.copyOf(languages);
    }

    /** One title option and where it came from (so other languages can render the same pick). */
    private record Option(String text, int src, int idx) {
    }

    /** Picks a title not in {@code used} (lower-case set; not modified). Returns null if none fits. */
    public String title(QuestDraft d, Set<String> used, Rng rng) {
        List<Option> options = new ArrayList<>();
        for (int i : order(d.themeNames.size(), rng)) {
            options.add(new Option(d.themeNames.get(i), SRC_THEME, i));
        }
        QuestDraft.Objective main = d.objectives.get(0);
        if (d.comboFamily != null) {
            for (int i : order(t.comboTitles.size(), rng)) {
                String s = fill(en, t.comboTitles.get(i), main).replace("{Family}", familyName(en, d.comboFamily));
                options.add(new Option(s, SRC_COMBO, i));
            }
        }
        List<String> typeTitles = typeTitles(t, main);
        for (int i : order(typeTitles.size(), rng)) {
            options.add(new Option(fill(en, typeTitles.get(i), main), SRC_TYPE, i));
        }
        for (int i : order(t.fallbackTitles.size(), rng)) {
            options.add(new Option(fill(en, t.fallbackTitles.get(i), main), SRC_FALLBACK, i));
        }
        Option pick = firstFitting(options, used, TITLE_IDEAL);
        if (pick == null) {
            pick = firstFitting(options, used, TITLE_MAX);
        }
        if (pick == null) {
            // last resort: shortened name plus a numeral
            String base = shorten(main.c().name(), TITLE_MAX - 4);
            for (int s = 0; s < NUMERALS.length; s++) {
                String cand = base + NUMERALS[s];
                if (!used.contains(cand.toLowerCase(Locale.ROOT))) {
                    pick = new Option(cand, SRC_NUMERAL, s);
                    break;
                }
            }
        }
        if (pick == null) {
            return null;
        }
        d.titleSource = pick.src();
        d.titleIndex = pick.idx();
        return pick.text();
    }

    private static List<String> typeTitles(Templates tpl, QuestDraft.Objective main) {
        List<String> typeTitles = tpl.titles.getOrDefault(main.c().type(), List.of());
        List<String> variantTitles = tpl.variant(main.c().hints(), "titles");
        return variantTitles != null ? variantTitles : typeTitles;
    }

    private static Option firstFitting(List<Option> options, Set<String> used, int max) {
        for (Option o : options) {
            String s = o.text();
            if (s.length() <= max && !s.contains("{") && !used.contains(s.toLowerCase(Locale.ROOT))) {
                return o;
            }
        }
        return null;
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max).trim();
    }

    /** A shuffled index order 0..n-1 (same random draws as shuffling a list of n elements). */
    private static int[] order(int n, Rng rng) {
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int x = out[i];
            out[i] = out[j];
            out[j] = x;
        }
        return out;
    }

    /** Fills a title template; a title always starts upper case ("{Dimension} Trip" → "The Nether Trip"). */
    private String fill(Localization l, String tpl, QuestDraft.Objective o) {
        Candidate c = o.c();
        if (l.english) {
            return English.capitalize(tpl.replace("{Name}", c.name())
                .replace("{Names}", c.plural())
                .replace("{Dimension}", dimensionName(l, c.target())));
        }
        String name = name(l, c);
        String inSentence = l.inSentence(name);
        return Localization.capitalize(tpl.replace("{Name}", name)
            .replace("{Names}", name)
            .replace("{name}", inSentence)
            .replace("{de_name}", l.de(inSentence))
            .replace("{Dimension}", dimensionName(l, c.target())));
    }

    public String familyName(String family) {
        return familyName(en, family);
    }

    private String familyName(Localization l, String family) {
        String n = l.templates.familyNames.get(family);
        if (n != null) {
            return n;
        }
        if (!l.english) {
            n = t.familyNames.get(family);
            if (n != null) {
                return n;
            }
        }
        String f = family;
        int us = f.indexOf('_');
        if (us > 0 && us <= 7 && f.length() > us + 1) {
            f = f.substring(us + 1); // drop a short mod prefix such as "fd_" or "create_"
        }
        return English.titleCase(f.replace('_', ' '));
    }

    public String dimensionName(String dim) {
        return dimensionName(en, dim);
    }

    private String dimensionName(Localization l, String dim) {
        String n = l.templates.dimensionNames.get(dim);
        if (n == null && !l.english) {
            n = t.dimensionNames.get(dim);
        }
        return n != null ? n : Ids.prettify(dim);
    }

    private String dimensionForm(Localization l, Map<String, String> forms, String dim) {
        String n = forms.get(dim);
        return n != null ? n : dimensionName(l, dim);
    }

    /** Display name of a candidate in a language: official name, tag noun, or the English name. */
    private static String name(Localization l, Candidate c) {
        if (l.english) {
            return c.name();
        }
        String n = c.isTag() ? l.tagNouns.get(c.def().tagConcept()) : l.names.get(nameKey(c));
        return n != null && !n.isBlank() ? n : c.name();
    }

    /** Key into {@link Localization#names}: the target id, plus {@code {potion}} for a potion filter. */
    static String nameKey(Candidate c) {
        return c.def().potion() != null ? c.target() + "{" + c.def().potion() + "}" : c.target();
    }

    private static List<String> phrasePool(Templates tpl, Candidate c) {
        List<String> pool = tpl.phrases.getOrDefault(c.type(), List.of());
        List<String> variant = tpl.variant(c.hints(), "phrases");
        return variant != null ? variant : pool;
    }

    /** Objective phrase, e.g. "Mine 16 iron ore". */
    public String phrase(QuestDraft.Objective o, Rng rng) {
        List<String> pool = phrasePool(t, o.c());
        return phrase(en, o, pool.isEmpty() ? -1 : rng.nextInt(pool.size()));
    }

    private String phrase(Localization l, QuestDraft.Objective o, int idx) {
        Candidate c = o.c();
        List<String> pool = phrasePool(l.templates, c);
        String tpl;
        if (idx >= 0 && idx < pool.size()) {
            tpl = pool.get(idx);
        } else if (!pool.isEmpty() && !l.english) {
            tpl = pool.get(0);
        } else {
            tpl = l.english ? defaultPhrase(c.type()) : phrase(en, o, idx);
        }
        if (!l.english) {
            String n = l.inSentence(name(l, c));
            return tpl.replace("{count}", Integer.toString(o.count()))
                .replace("{names}", n)
                .replace("{name}", n)
                .replace("{a_name}", n)
                .replace("{de_name}", l.de(n))
                .replace("{dimension_to}", dimensionForm(l, l.dimensionTo, c.target()))
                .replace("{dimension_in}", dimensionForm(l, l.dimensionIn, c.target()))
                .replace("{dimension}", dimensionName(l, c.target()));
        }
        List<String> variant = t.variant(c.hints(), "phrases");
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
            .replace("{dimension}", dimensionName(en, c.target()));
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
        int[] phraseIdx = new int[d.objectives.size()];
        for (int i = 0; i < phraseIdx.length; i++) {
            List<String> pool = phrasePool(t, d.objectives.get(i).c());
            phraseIdx[i] = pool.isEmpty() ? -1 : rng.nextInt(pool.size());
        }
        int themeIdx = themeTemplates != null && !themeTemplates.isEmpty() ? rng.nextInt(themeTemplates.size()) : -1;
        d.phraseChoice = phraseIdx;
        d.themeDescriptionChoice = themeIdx;
        return description(en, d, themeTemplates);
    }

    private String description(Localization l, QuestDraft d, List<String> themeTemplates) {
        List<String> phrases = new ArrayList<>();
        for (int i = 0; i < d.objectives.size(); i++) {
            int idx = d.phraseChoice != null && i < d.phraseChoice.length ? d.phraseChoice[i] : -1;
            phrases.add(phrase(l, d.objectives.get(i), idx));
        }
        String list = joinPhrases(l, phrases);
        String plain = l.ensureEnd(Localization.capitalize(l.english ? list
            : l.templates.sentence("list", "{list}").replace("{list}", list)));
        List<String> sentences = new ArrayList<>();
        String tpl = themeTemplates != null && d.themeDescriptionChoice >= 0
            && d.themeDescriptionChoice < themeTemplates.size() ? themeTemplates.get(d.themeDescriptionChoice) : null;
        if (tpl != null) {
            boolean lower = l.english ? tpl.contains(": {list}") : l.lowerAfterColon && COLON_LIST.matcher(tpl).find();
            String inList = lower ? Localization.lowerFirst(list) : list;
            sentences.add(l.ensureEnd(Localization.capitalize(tpl.replace("{list}", inList))));
        } else {
            sentences.add(plain);
        }
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
        Set<String> hints = new LinkedHashSet<>();
        for (QuestDraft.Objective o : d.objectives) {
            for (Hint h : hints(o.c())) {
                // only the strongest pickaxe requirement is mentioned
                if (h.kind() == Hint.TOOL && isPickaxe(h.key()) && !h.key().equals(bestTool)) {
                    continue;
                }
                String s = render(l, h, o.c());
                if (s != null) {
                    hints.add(s);
                }
            }
        }
        String sep = l.sentenceSep;
        StringBuilder sb = new StringBuilder();
        for (String s : sentences) {
            append(sb, sep, s);
        }
        if (sb.length() > DESCRIPTION_MAX) {
            // theme flavour too long: fall back to the plain list
            sb.setLength(0);
            append(sb, sep, plain);
        }
        for (String h : hints) {
            if (sb.length() + sep.length() + h.length() <= DESCRIPTION_MAX) {
                append(sb, sep, h);
            }
        }
        String out = sb.toString();
        if (out.length() > DESCRIPTION_MAX) {
            out = out.substring(0, DESCRIPTION_MAX - 3).trim() + "...";
        }
        return out;
    }

    /**
     * Renders the quest's English picks in every extra language into {@code d.titles},
     * {@code d.descriptions} and {@code d.messageTexts} (each starting with {@code en_us}).
     */
    public void localize(QuestDraft d) {
        Map<String, String> titles = new LinkedHashMap<>();
        Map<String, String> descriptions = new LinkedHashMap<>();
        Map<String, String> msgs = new LinkedHashMap<>();
        titles.put(ENGLISH, d.title);
        if (d.description != null) {
            descriptions.put(ENGLISH, d.description);
        }
        String message = null;
        for (QuestDraft.Reward r : d.rewards) {
            if ("message".equals(r.type())) {
                message = r.text();
            }
        }
        int msgIdx = message == null ? -1 : messages.indexOf(message);
        if (message != null) {
            msgs.put(ENGLISH, message);
        }
        for (Localization l : languages) {
            titles.put(l.code, localizedTitle(d, l));
            if (d.description != null && !d.description.isEmpty()) {
                String desc = description(l, d, d.themeKey == null ? null : l.themeDescriptions.get(d.themeKey));
                if (!desc.isEmpty()) {
                    descriptions.put(l.code, desc);
                }
            }
            if (msgIdx >= 0 && msgIdx < l.messages.size()) {
                msgs.put(l.code, l.messages.get(msgIdx));
            }
        }
        d.titles = titles;
        d.descriptions = descriptions;
        d.messageTexts = msgs;
    }

    private String localizedTitle(QuestDraft d, Localization l) {
        QuestDraft.Objective main = d.objectives.get(0);
        int idx = d.titleIndex;
        String s = null;
        switch (d.titleSource) {
            case SRC_THEME:
                s = at(d.themeKey == null ? null : l.themeNames.get(d.themeKey), idx);
                break;
            case SRC_COMBO:
                s = at(l.templates.comboTitles, idx);
                if (s != null) {
                    s = fill(l, s, main).replace("{Family}", familyName(l, d.comboFamily));
                }
                break;
            case SRC_TYPE:
                s = at(typeTitles(l.templates, main), idx);
                s = s == null ? null : fill(l, s, main);
                break;
            case SRC_FALLBACK:
                s = at(l.templates.fallbackTitles, idx);
                s = s == null ? null : fill(l, s, main);
                break;
            default:
                break;
        }
        if (fits(s)) {
            return s;
        }
        if (d.titleSource != SRC_NUMERAL) {
            for (String tpl : l.templates.fallbackTitles) {
                String f = fill(l, tpl, main);
                if (fits(f)) {
                    return f;
                }
            }
        }
        String full = Localization.capitalize(name(l, main.c()));
        if (d.titleSource == SRC_NUMERAL && idx >= 0 && idx < NUMERALS.length) {
            return shortenWords(full, TITLE_MAX - 5) + NUMERALS[idx];
        }
        return fits(full) ? full : shortenWords(full, TITLE_MAX);
    }

    private static boolean fits(String s) {
        return s != null && !s.isBlank() && s.length() <= TITLE_MAX && !s.contains("{");
    }

    /** Shortens at a word boundary where there is one in the second half, else at {@code max}. */
    private static String shortenWords(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        int cut = s.lastIndexOf(' ', max);
        return cut > max / 2 ? s.substring(0, cut).trim() : s.substring(0, max).trim();
    }

    private static String at(List<String> list, int idx) {
        return list != null && idx >= 0 && idx < list.size() ? list.get(idx) : null;
    }

    private static void append(StringBuilder sb, String sep, String s) {
        if (sb.length() > 0) {
            sb.append(sep);
        }
        sb.append(s);
    }

    private static String joinPhrases(Localization l, List<String> phrases) {
        if (phrases.size() == 1) {
            return phrases.get(0);
        }
        List<String> lower = new ArrayList<>();
        for (int i = 0; i < phrases.size(); i++) {
            String p = phrases.get(i);
            lower.add(i == 0 ? p : Localization.lowerFirst(p));
        }
        if (lower.size() == 2) {
            return lower.get(0) + l.listAnd + lower.get(1);
        }
        return String.join(l.listSep, lower.subList(0, lower.size() - 1)) + l.listAnd + lower.get(lower.size() - 1);
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

    private static boolean isPickaxe(String tool) {
        return "stone".equals(tool) || "iron".equals(tool) || "diamond".equals(tool) || "netherite".equals(tool);
    }

    /**
     * One hint about a candidate, independent of language.
     *
     * @param kind {@link #CATALOG}, {@link #TAG}, {@link #TOOL}, {@link #WORD} or {@link #DIMENSION}
     * @param key  catalog hint text, tool, hint word or dimension
     */
    private record Hint(int kind, String key) {
        static final int CATALOG = 0;
        static final int TAG = 1;
        static final int TOOL = 2;
        static final int WORD = 3;
        static final int DIMENSION = 4;
    }

    /** Hints for one candidate, most useful first. */
    private List<Hint> hints(Candidate c) {
        List<Hint> out = new ArrayList<>();
        if (c.def().hint() != null && !c.def().hint().isBlank()) {
            out.add(new Hint(Hint.CATALOG, c.def().hint().trim()));
        }
        if (c.isTag() && c.tagNoun() != null) {
            out.add(new Hint(Hint.TAG, c.def().tagConcept()));
        }
        String tool = c.tool();
        if (tool != null && !tool.equals("none") && !tool.equals("wood") && t.tools.get(tool) != null) {
            out.add(new Hint(Hint.TOOL, tool));
        }
        for (String h : c.hints()) {
            if (t.hints.get(h) != null) {
                out.add(new Hint(Hint.WORD, h));
            }
        }
        if (c.type() != ObjectiveType.VISIT_DIMENSION && c.dimension() != null
            && !"minecraft:overworld".equals(c.dimension())) {
            out.add(new Hint(Hint.DIMENSION, c.dimension()));
        }
        return out;
    }

    /** A hint as a sentence in a language; null when the language has no text for it. */
    private String render(Localization l, Hint h, Candidate c) {
        Templates lt = l.templates;
        switch (h.kind()) {
            case Hint.CATALOG: {
                String s = l.english ? h.key() : l.hints.get(h.key());
                return s == null || s.isBlank() ? null : l.ensureEnd(s.trim());
            }
            case Hint.TAG: {
                if (l.english) {
                    return t.sentence("tag", "Any {noun} counts.").replace("{noun}", c.tagNoun());
                }
                String tpl = lt.sentences.get("tag");
                return tpl == null ? null : l.ensureEnd(tpl.replace("{noun}", l.inSentence(name(l, c))));
            }
            case Hint.TOOL: {
                String s = lt.tools.get(h.key());
                return s == null ? null : l.ensureEnd(s);
            }
            case Hint.WORD: {
                String s = lt.hints.get(h.key());
                return s == null ? null : l.ensureEnd(s);
            }
            default: {
                if (l.english) {
                    return t.sentence("dimension", "Found in {dimension}.").replace("{dimension}", dimensionName(l, h.key()));
                }
                String tpl = lt.sentences.get("dimension");
                return tpl == null ? null : l.ensureEnd(tpl
                    .replace("{dimension_in}", dimensionForm(l, l.dimensionIn, h.key()))
                    .replace("{dimension}", dimensionName(l, h.key())));
            }
        }
    }

    /** English hint sentences for one candidate, most useful first. */
    public List<String> hintSentences(Candidate c) {
        List<String> out = new ArrayList<>();
        for (Hint h : hints(c)) {
            out.add(render(en, h, c));
        }
        return out;
    }
}
