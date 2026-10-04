package com.erikedits.justquests.generator.v2.internal.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything loaded from the bundled data files plus world-level overrides. Immutable after load. */
public final class Catalog {
    public final List<ProfileDef> profiles;
    public final List<RewardDefs.Item> items;
    public final List<RewardDefs.Effect> effects;
    public final List<RewardDefs.Loot> loot;
    public final List<String> messages;
    public final List<ThemeDef> themes;
    public final Map<String, TagConcept> tags;
    public final Templates templates;
    public final Balance balance;
    public final List<String> loadWarnings;
    /** Extra languages for generated quest text (lang/*.json); may be empty. */
    public final List<Localization> languages;

    /** The same catalog with another balance (scaled sets, see {@link Balance#scaled}). */
    public Catalog withBalance(Balance other) {
        return new Catalog(profiles, items, effects, loot, messages, themes, tags, templates, other, loadWarnings,
            languages);
    }

    public Catalog(List<ProfileDef> profiles, List<RewardDefs.Item> items, List<RewardDefs.Effect> effects,
                   List<RewardDefs.Loot> loot, List<String> messages, List<ThemeDef> themes,
                   Map<String, TagConcept> tags, Templates templates, Balance balance, List<String> loadWarnings) {
        this(profiles, items, effects, loot, messages, themes, tags, templates, balance, loadWarnings, List.of());
    }

    public Catalog(List<ProfileDef> profiles, List<RewardDefs.Item> items, List<RewardDefs.Effect> effects,
                   List<RewardDefs.Loot> loot, List<String> messages, List<ThemeDef> themes,
                   Map<String, TagConcept> tags, Templates templates, Balance balance, List<String> loadWarnings,
                   List<Localization> languages) {
        this.profiles = List.copyOf(profiles);
        this.items = List.copyOf(items);
        this.effects = List.copyOf(effects);
        this.loot = List.copyOf(loot);
        this.messages = List.copyOf(messages);
        this.themes = List.copyOf(themes);
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        this.templates = templates;
        this.balance = balance;
        this.loadWarnings = List.copyOf(loadWarnings);
        this.languages = List.copyOf(languages);
    }

    public ProfileDef profile(String id) {
        for (ProfileDef p : profiles) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public List<EntryDef> allEntries() {
        List<EntryDef> out = new ArrayList<>();
        for (ProfileDef p : profiles) {
            out.addAll(p.entries());
        }
        return out;
    }

    public int targetCount(String profileId) {
        int n = 0;
        for (ProfileDef p : profiles) {
            if (profileId == null || p.id().equals(profileId)) {
                for (EntryDef e : p.entries()) {
                    n += e.targets().size();
                }
            }
        }
        return n;
    }
}
