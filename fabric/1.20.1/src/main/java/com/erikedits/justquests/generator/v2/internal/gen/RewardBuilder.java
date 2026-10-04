package com.erikedits.justquests.generator.v2.internal.gen;

import com.erikedits.justquests.generator.v2.api.ContentKind;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.RewardOptions;
import com.erikedits.justquests.generator.v2.internal.catalog.Balance;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.RewardDefs;
import com.erikedits.justquests.generator.v2.internal.util.Rng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * Value-based rewards (§9.4): budget = estimated minutes × reward rate (× multi-objective premium),
 * clamped per difficulty; one item reward + XP derived from the remaining budget; sometimes an
 * effect or a curated loot table on Normal/Hard; total within the balance tolerance of the budget.
 */
public final class RewardBuilder {
    private final Catalog catalog;
    private final Balance balance;
    private final Balance.Level level;
    private final Difficulty difficulty;
    private final Set<String> activeProfiles;
    private final BiPredicate<ContentKind, String> exists;
    private final ToIntFunction<String> hostStackSize;
    private final Set<String> rewardTypes;
    private final RewardOptions options;
    /** Multiplies item and XP caps: the balance's (scaled sets) times the reward scale. */
    private final double capScale;

    public RewardBuilder(Catalog catalog, Difficulty difficulty, Set<String> activeProfiles,
                         BiPredicate<ContentKind, String> exists, ToIntFunction<String> hostStackSize,
                         Set<String> rewardTypes) {
        this(catalog, difficulty, activeProfiles, exists, hostStackSize, rewardTypes, RewardOptions.STANDARD);
    }

    public RewardBuilder(Catalog catalog, Difficulty difficulty, Set<String> activeProfiles,
                         BiPredicate<ContentKind, String> exists, ToIntFunction<String> hostStackSize,
                         Set<String> rewardTypes, RewardOptions options) {
        this.catalog = catalog;
        this.balance = catalog.balance;
        this.level = balance.level(difficulty);
        this.difficulty = difficulty;
        this.activeProfiles = activeProfiles;
        this.exists = exists;
        this.hostStackSize = hostStackSize;
        this.rewardTypes = rewardTypes;
        this.options = options == null ? RewardOptions.STANDARD : options;
        this.capScale = balance.rewardCapScale * this.options.scale();
    }

    /** Budget for a quest of the given estimated minutes and objective count. */
    public double budget(double estMinutes, int objectives) {
        double premium = balance.multiRewardPremium[Math.max(0, Math.min(2, objectives - 1))];
        double b = estMinutes * level.rewardRate * premium;
        if (options.scale() != 1.0) {
            b *= options.scale();
            return Math.max(level.budgetMin * options.scale(), Math.min(level.budgetMax * options.scale(), b));
        }
        return Math.max(level.budgetMin, Math.min(level.budgetMax, b));
    }

    /** Fills {@code d.rewards}; returns false if no acceptable reward set exists. */
    public boolean build(QuestDraft d, Rng rng) {
        d.rewards.clear();
        double budget = budget(d.estMinutes, d.objectives.size());
        d.budget = budget;
        Set<String> families = d.families();
        Set<String> targets = d.targets();
        int questTier = d.tier();

        List<QuestDraft.Reward> extras = new ArrayList<>();
        double extraValue = 0;
        if (allowed("justquests:loot_table") && rng.chance(level.lootChance)) {
            RewardDefs.Loot loot = pickLoot(budget, questTier, rng);
            if (loot != null) {
                extras.add(new QuestDraft.Reward("loot_table", loot.id(), 1, 0, null, loot.value()));
                extraValue += loot.value();
            }
        }
        if (extras.isEmpty() && allowed("justquests:effect") && rng.chance(level.effectChance)) {
            QuestDraft.Reward eff = pickEffect(budget * 0.25, rng);
            if (eff != null) {
                extras.add(eff);
                extraValue += eff.value();
            }
        }
        double itemBudget = Math.max(0, (budget - extraValue) * balance.itemShare);
        QuestDraft.Reward item = allowed("justquests:give_item") ? pickItem(itemBudget, families, targets, null, questTier, rng) : null;
        if (item != null) {
            d.rewards.add(item);
            double left = itemBudget - item.value();
            // a second item when the first one cannot carry the item budget (keeps XP sane)
            if (extras.isEmpty() && left > itemBudget * 0.4 && left > 2.0) {
                QuestDraft.Reward second = pickItem(left, families, targets, item.id(), questTier, rng);
                if (second != null) {
                    d.rewards.add(second);
                }
            }
        }
        d.rewards.addAll(extras);
        double used = d.totalRewardValue();
        if (allowed("justquests:xp")) {
            int xp = xpFor(budget - used);
            // trim the item if XP alone already overshoots after rounding
            d.rewards.add(new QuestDraft.Reward("xp", null, xp, 0, null, xp / balance.xpPointsPerValue));
        }
        fixTolerance(d, budget);
        if (d.rewards.size() < 3 && allowed("justquests:message") && !catalog.messages.isEmpty()
            && rng.chance(level.messageChance)) {
            d.rewards.add(new QuestDraft.Reward("message", null, 0, 0, rng.pick(catalog.messages), 0.0));
        }
        if (options.choice()) {
            offerChoice(d, rng);
        }
        d.rewardValue = d.totalRewardValue();
        return !d.rewards.isEmpty() && within(d.rewardValue, budget);
    }

    /**
     * Turns the first item reward into a choice of up to three options of about its value: that
     * item, another item, and a loot table or an effect. Nothing changes when fewer than two options
     * can be found.
     */
    private void offerChoice(QuestDraft d, Rng rng) {
        int i = indexOf(d, "give_item");
        if (i < 0 || !allowed("justquests:choice")) {
            return;
        }
        QuestDraft.Reward first = d.rewards.get(i);
        double value = first.value();
        List<QuestDraft.Reward> options = new ArrayList<>();
        options.add(first);
        Set<String> exclude = new java.util.HashSet<>(d.targets());
        for (QuestDraft.Reward r : d.rewards) {
            if (r.id() != null) {
                exclude.add(r.id());
            }
        }
        QuestDraft.Reward second = pickItem(value, d.families(), exclude, first.id(), d.tier(), rng);
        if (second != null) {
            options.add(second);
        }
        boolean hasLoot = indexOf(d, "loot_table") >= 0;
        RewardDefs.Loot loot = !hasLoot && allowed("justquests:loot_table") ? pickLoot(value / 0.6, d.tier(), rng) : null;
        if (loot != null) {
            options.add(new QuestDraft.Reward("loot_table", loot.id(), 1, 0, null, loot.value()));
        } else if (indexOf(d, "effect") < 0 && allowed("justquests:effect")) {
            QuestDraft.Reward eff = pickEffect(value, rng);
            if (eff != null) {
                options.add(eff);
            }
        }
        if (options.size() < 2) {
            return;
        }
        d.choice.clear();
        d.choice.addAll(options);
        d.rewards.set(i, new QuestDraft.Reward("choice", null, options.size(), 0, null, value));
    }

    /** An item's count cap: its catalog maximum and stack size, raised by {@link #capScale}. */
    private int capOf(RewardDefs.Item it, String id) {
        int cap = Math.max(1, Math.min(it.max(), stackOf(it, id)));
        return capScale == 1.0 ? cap : Math.max(1, (int) Math.min(100_000L, Math.round(cap * capScale)));
    }

    private boolean allowed(String rewardType) {
        return rewardTypes == null || rewardTypes.isEmpty() || rewardTypes.contains(rewardType);
    }

    private int xpFor(double value) {
        double pts = Math.max(0, value) * balance.xpPointsPerValue;
        int rounded = pts >= 50 ? (int) (Math.round(pts / 5.0) * 5) : (int) Math.round(pts);
        int max = capScale == 1.0 ? balance.maxXp : (int) Math.min(1_000_000L, Math.round(balance.maxXp * capScale));
        return Math.max(balance.minXp, Math.min(max, rounded));
    }

    private boolean within(double value, double budget) {
        return Math.abs(value - budget) <= budget * balance.rewardTolerance + 1e-9;
    }

    /** Adjusts the item count, then drops extras, until the total is within tolerance (if possible). */
    private void fixTolerance(QuestDraft d, double budget) {
        for (int guard = 0; guard < 8 && !within(d.totalRewardValue(), budget); guard++) {
            double total = d.totalRewardValue();
            int itemIdx = indexOf(d, "give_item");
            int xpIdx = indexOf(d, "xp");
            if (total > budget) {
                if (itemIdx >= 0 && d.rewards.get(itemIdx).count() > 1) {
                    QuestDraft.Reward it = d.rewards.get(itemIdx);
                    double unit = it.value() / it.count();
                    int drop = (int) Math.ceil((total - budget * (1 + balance.rewardTolerance * 0.5)) / unit);
                    int n = Math.max(1, it.count() - Math.max(1, drop));
                    d.rewards.set(itemIdx, new QuestDraft.Reward("give_item", it.id(), n, 0, null, unit * n));
                } else if (d.rewards.size() > 2) {
                    removeExtra(d);
                } else if (xpIdx >= 0 && d.rewards.get(xpIdx).count() > balance.minXp) {
                    double others = total - d.rewards.get(xpIdx).value();
                    int xp = xpFor(budget - others);
                    d.rewards.set(xpIdx, new QuestDraft.Reward("xp", null, xp, 0, null, xp / balance.xpPointsPerValue));
                    if (xp == d.rewards.get(xpIdx).count() && others > budget) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                // too low: XP is capped; raise the item count if possible
                if (itemIdx >= 0) {
                    QuestDraft.Reward it = d.rewards.get(itemIdx);
                    double unit = it.value() / it.count();
                    int cap = maxCount(it.id());
                    int add = (int) Math.ceil((budget * (1 - balance.rewardTolerance * 0.5) - total) / unit);
                    int n = Math.min(cap, it.count() + Math.max(1, add));
                    if (n == it.count()) {
                        break;
                    }
                    d.rewards.set(itemIdx, new QuestDraft.Reward("give_item", it.id(), n, 0, null, unit * n));
                } else {
                    break;
                }
            }
            if (xpIdx >= 0) {
                double others = d.totalRewardValue() - d.rewards.get(xpIdx).value();
                int xp = xpFor(budget - others);
                d.rewards.set(xpIdx, new QuestDraft.Reward("xp", null, xp, 0, null, xp / balance.xpPointsPerValue));
            }
        }
    }

    private static int indexOf(QuestDraft d, String type) {
        for (int i = 0; i < d.rewards.size(); i++) {
            if (d.rewards.get(i).type().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    private static void removeExtra(QuestDraft d) {
        for (int i = d.rewards.size() - 1; i >= 0; i--) {
            String t = d.rewards.get(i).type();
            if (t.equals("effect") || t.equals("loot_table")) {
                d.rewards.remove(i);
                return;
            }
        }
    }

    private int maxCount(String id) {
        for (RewardDefs.Item it : catalog.items) {
            if (it.id().equals(id) || it.alts().contains(id)) {
                return capOf(it, id);
            }
        }
        return 1;
    }

    private int stackOf(RewardDefs.Item it, String resolvedId) {
        int host = hostStackSize.applyAsInt(resolvedId);
        return host > 0 ? host : it.stack() > 0 ? it.stack() : 64;
    }

    private QuestDraft.Reward pickItem(double itemBudget, Set<String> families, Set<String> targets, String exclude,
                                       int questTier, Rng rng) {
        if (itemBudget <= 0) {
            return null;
        }
        List<RewardDefs.Item> ok = new ArrayList<>();
        List<String> resolved = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (RewardDefs.Item it : catalog.items) {
            if (!activeProfiles.contains(it.profile()) || it.minDifficulty() > difficulty.ordinal()) {
                continue;
            }
            if (it.tier() > questTier + 1) {
                continue;
            }
            if (families.contains(it.family())) {
                continue;
            }
            boolean clash = false;
            for (String f : it.avoidFamilies()) {
                if (families.contains(f)) {
                    clash = true;
                }
            }
            if (clash) {
                continue;
            }
            String id = resolveId(it);
            if (id == null || targets.contains(id) || id.equals(exclude)) {
                continue;
            }
            if (it.value() > itemBudget * (1 + balance.rewardTolerance)) {
                continue;
            }
            int cap = capOf(it, id);
            // capacity: share of the item budget this reward can carry (1 = fully)
            double fit = Math.min(1.0, cap * it.value() / itemBudget);
            fit = fit * fit;
            ok.add(it);
            resolved.add(id);
            weights.add(it.weight() * fit);
        }
        if (ok.isEmpty()) {
            return null;
        }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < ok.size(); i++) {
            idx.add(i);
        }
        Integer choice = rng.weighted(idx, weights::get);
        if (choice == null) {
            return null;
        }
        RewardDefs.Item it = ok.get(choice);
        String id = resolved.get(choice);
        int cap = capOf(it, id);
        int count = (int) Math.max(1, Math.min(cap, Math.round(itemBudget / it.value())));
        return new QuestDraft.Reward("give_item", id, count, 0, null, count * it.value());
    }

    private String resolveId(RewardDefs.Item it) {
        if (exists.test(ContentKind.ITEM, it.id())) {
            return it.id();
        }
        for (String a : it.alts()) {
            if (exists.test(ContentKind.ITEM, a)) {
                return a;
            }
        }
        return null;
    }

    private RewardDefs.Loot pickLoot(double budget, int questTier, Rng rng) {
        List<RewardDefs.Loot> ok = new ArrayList<>();
        for (RewardDefs.Loot l : catalog.loot) {
            if (activeProfiles.contains(l.profile()) && l.minDifficulty() <= difficulty.ordinal()
                && l.tier() <= questTier + 1 && l.value() <= budget * 0.6) {
                ok.add(l);
            }
        }
        return rng.weighted(ok, RewardDefs.Loot::weight);
    }

    private QuestDraft.Reward pickEffect(double value, Rng rng) {
        List<RewardDefs.Effect> ok = new ArrayList<>();
        for (RewardDefs.Effect e : catalog.effects) {
            if (activeProfiles.contains(e.profile()) && e.minDifficulty() <= difficulty.ordinal()) {
                ok.add(e);
            }
        }
        RewardDefs.Effect e = rng.weighted(ok, RewardDefs.Effect::weight);
        if (e == null) {
            return null;
        }
        double perMinute = e.valuePerMinute() * (e.amplifier() + 1);
        int seconds = (int) Math.round(value / perMinute * 60.0 / 30.0) * 30;
        seconds = Math.max(e.minSeconds(), Math.min(e.maxSeconds(), seconds));
        double v = perMinute * seconds / 60.0;
        return new QuestDraft.Reward("effect", e.id(), seconds, e.amplifier(), null, v);
    }

    /** Explain line for the rewards. */
    public static String describe(QuestDraft d) {
        StringBuilder sb = new StringBuilder("reward ");
        boolean first = true;
        for (QuestDraft.Reward r : d.rewards) {
            if (!first) {
                sb.append(" + ");
            }
            first = false;
            switch (r.type()) {
                case "give_item":
                    sb.append(String.format(Locale.ROOT, "give_item %s ×%d (value %.1f)", r.id(), r.count(), r.value()));
                    break;
                case "xp":
                    sb.append(String.format(Locale.ROOT, "xp %d (value %.1f)", r.count(), r.value()));
                    break;
                case "effect":
                    sb.append(String.format(Locale.ROOT, "effect %s %ds (value %.1f)", r.id(), r.count(), r.value()));
                    break;
                case "loot_table":
                    sb.append(String.format(Locale.ROOT, "loot_table %s (value %.1f)", r.id(), r.value()));
                    break;
                default:
                    sb.append("message");
                    break;
            }
        }
        sb.append(String.format(Locale.ROOT, "  → %.1f / budget %.1f", d.totalRewardValue(), d.budget));
        return sb.toString();
    }
}
