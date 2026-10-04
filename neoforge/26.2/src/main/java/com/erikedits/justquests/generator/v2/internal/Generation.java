package com.erikedits.justquests.generator.v2.internal;

import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.GeneratorHost;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.gen.CandidateResolver;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.gen.QuestDraft;
import com.erikedits.justquests.generator.v2.internal.gen.RewardBuilder;
import com.erikedits.justquests.generator.v2.internal.gen.SetBuilder;
import com.erikedits.justquests.generator.v2.internal.gen.TextBuilder;
import com.erikedits.justquests.generator.v2.internal.util.Ids;
import com.erikedits.justquests.generator.v2.internal.util.Rng;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One side-effect-free generation run (used by rotation, reroll, preview and the tests). */
public final class Generation {
    private Generation() {
    }

    /**
     * Result of a run.
     *
     * @param drafts      accepted quests (JSON in {@code draft.json})
     * @param relaxations relaxation counters
     * @param relaxNotes  human-readable relaxation notes
     * @param rejections  rejected candidates per pipeline step
     * @param pool        the resolved candidate pool
     */
    public record Result(List<QuestDraft> drafts, Map<String, Long> relaxations, List<String> relaxNotes,
                         Map<String, Long> rejections, CandidateResolver.Pool pool) {
    }

    /**
     * Generates {@code n} quests.
     *
     * @param catalog     loaded catalog
     * @param host        host adapter (content, capabilities, validator, log)
     * @param config      sanitised config (difficulty, modded share, disabled profiles, adaptive balancing)
     * @param progression progression snapshot (not modified)
     * @param calibration family|type multipliers (applied only if adaptive balancing is on)
     * @param history     signatures blocked by the no-repeat window
     * @param kept        quests that stay in the set
     * @param n           number of new quests
     * @param cycleId     cycle id used for provisional ids passed to the validator
     * @param seed        generation seed
     * @param firstIndex  index of the first new quest
     * @return the run result
     */
    public static Result run(Catalog catalog, GeneratorHost host, GeneratorConfig config, Progression progression,
                             Map<String, Double> calibration, Set<String> history, List<SetBuilder.Kept> kept, int n,
                             long cycleId, long seed, int firstIndex) {
        Map<String, Double> cal = config.adaptiveBalancing() ? calibration : Map.of();
        CandidateResolver resolver = new CandidateResolver(catalog, host.content(), host.capabilities(), host.log(),
            config.difficulty(), progression, cal, config.disabledProfiles());
        CandidateResolver.Pool pool = resolver.resolve();
        Set<String> rewardTypes;
        try {
            rewardTypes = host.capabilities().rewardTypes();
        } catch (RuntimeException e) {
            rewardTypes = Set.of();
        }
        RewardBuilder rewards = new RewardBuilder(catalog, config.difficulty(), CandidateResolver.ids(pool.activeProfiles()),
            resolver::exists, id -> {
                try {
                    return host.content().maxStackSize(id);
                } catch (RuntimeException e) {
                    return -1;
                }
            }, rewardTypes);
        TextBuilder text = new TextBuilder(catalog.templates, catalog.messages, catalog.languages);
        SetBuilder.Validator validator = (draft, json, ordinal) ->
            host.validator().validate(Ids.GEN_PREFIX + cycleId + "_" + (firstIndex + ordinal), json.deepCopy());
        SetBuilder builder = new SetBuilder(catalog, config.difficulty(), pool, new Rng(seed), history, rewards, text,
            validator, host.log(), host.capabilities(), progression);
        List<QuestDraft> drafts = builder.build(n, kept, kept.size() + n, config.moddedShare());
        Map<String, Long> rejections = new LinkedHashMap<>(resolver.rejections());
        builder.rejections().forEach((k, v) -> rejections.merge(k, v, Long::sum));
        return new Result(drafts, builder.relaxations(), builder.relaxNotes(), rejections, pool);
    }
}
