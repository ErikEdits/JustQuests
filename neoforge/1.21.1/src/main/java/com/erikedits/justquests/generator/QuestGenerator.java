package com.erikedits.justquests.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The procedural quest generator (Phase 6 v1). No AI/LLM and no dependencies:
 * it picks a template + a target from curated, achievability-checked pools +
 * a random count, and emits the exact same quest JSON the datapack loader
 * understands (so it reuses the Codec validation path). An optional AI text
 * layer may come later; the core is deterministic, built-in rules.
 */
public final class QuestGenerator {
    private QuestGenerator() {}

    /** A generated quest before it's parsed: id path, dedupe signature, quest JSON. */
    public record GenQuest(String idPath, String signature, JsonObject json) {}

    /** One generation template: objective type, its target field, target pool, count range, verb. */
    private record Template(String objType, String field, String[] pool, int minCount, int maxCount, String verb) {}

    // Curated, achievable pools (baked-in constraints — nothing oversized/technical).
    private static final Template[] TEMPLATES = {
        new Template("justquests:collect_item", "item", new String[]{
            "minecraft:oak_log", "minecraft:cobblestone", "minecraft:dirt", "minecraft:sand",
            "minecraft:wheat", "minecraft:coal", "minecraft:iron_ore", "minecraft:sugar_cane",
            "minecraft:kelp", "minecraft:apple"}, 12, 48, "Gather"),
        new Template("justquests:mine_block", "block", new String[]{
            "minecraft:stone", "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:copper_ore",
            "minecraft:andesite", "minecraft:granite", "minecraft:diorite", "minecraft:deepslate",
            "minecraft:gravel", "minecraft:sandstone"}, 16, 64, "Mine"),
        new Template("justquests:craft_item", "item", new String[]{
            "minecraft:bread", "minecraft:torch", "minecraft:stick", "minecraft:chest",
            "minecraft:furnace", "minecraft:ladder", "minecraft:crafting_table", "minecraft:bowl",
            "minecraft:bookshelf"}, 4, 16, "Craft"),
        new Template("justquests:kill_mob", "entity", new String[]{
            "minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper",
            "minecraft:husk", "minecraft:drowned", "minecraft:slime"}, 4, 12, "Defeat"),
    };

    private static final String[] REWARD_ITEMS = {
        "minecraft:bread", "minecraft:cooked_beef", "minecraft:iron_ingot", "minecraft:gold_ingot",
        "minecraft:emerald", "minecraft:coal", "minecraft:apple", "minecraft:experience_bottle"};

    /**
     * Generate up to {@code count} distinct quests, skipping any signature in
     * {@code recentSigs} (the 6-day history) and avoiding duplicates within the set.
     */
    public static List<GenQuest> generate(int count, Set<String> recentSigs, Random rng, long seedTag) {
        List<GenQuest> out = new ArrayList<>();
        Set<String> usedThisRun = new java.util.HashSet<>();
        int attempts = 0;
        int index = 0;
        while (out.size() < count && attempts < count * 25 + 25) {
            attempts++;
            Template t = TEMPLATES[rng.nextInt(TEMPLATES.length)];
            String target = t.pool()[rng.nextInt(t.pool().length)];
            String sig = t.objType() + "|" + target;
            if (recentSigs.contains(sig) || usedThisRun.contains(sig)) continue;
            usedThisRun.add(sig);

            int qty = t.minCount() + rng.nextInt(t.maxCount() - t.minCount() + 1);
            String rewardItem = REWARD_ITEMS[rng.nextInt(REWARD_ITEMS.length)];
            int rewardQty = Math.max(1, qty / 8 + rng.nextInt(3));

            JsonObject q = buildQuest(t, target, qty, rewardItem, rewardQty);
            out.add(new GenQuest(seedTag + "_" + (index++), sig, q));
        }
        return out;
    }

    private static JsonObject buildQuest(Template t, String target, int qty, String rewardItem, int rewardQty) {
        String name = niceName(target);
        JsonObject q = new JsonObject();
        q.addProperty("title", titleCase(t.verb() + " " + name));
        q.addProperty("description", t.verb() + " " + qty + "x " + name + ".");
        q.addProperty("category", "generated");

        JsonObject obj = new JsonObject();
        obj.addProperty("type", t.objType());
        obj.addProperty(t.field(), target);
        obj.addProperty("count", qty);
        JsonArray objectives = new JsonArray();
        objectives.add(obj);
        q.add("objectives", objectives);

        JsonObject reward = new JsonObject();
        reward.addProperty("type", "justquests:give_item");
        reward.addProperty("item", rewardItem);
        reward.addProperty("count", rewardQty);
        JsonArray rewards = new JsonArray();
        rewards.add(reward);
        q.add("rewards", rewards);
        return q;
    }

    /** "minecraft:oak_log" -> "oak log". */
    private static String niceName(String id) {
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return path.replace('_', ' ');
    }

    private static String titleCase(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean up = true;
        for (char c : s.toCharArray()) {
            if (c == ' ') { up = true; b.append(c); }
            else if (up) { b.append(Character.toUpperCase(c)); up = false; }
            else b.append(c);
        }
        return b.toString();
    }
}
