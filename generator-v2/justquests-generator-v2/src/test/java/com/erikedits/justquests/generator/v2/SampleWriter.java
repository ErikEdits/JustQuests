package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

/**
 * Writes {@code samples/}: for world seeds 1, 2, 3 × each difficulty a set of 10 quests with vanilla
 * only and a set of 10 with both mod profiles active, plus the explain() output of every quest.
 * Run with {@code ./gradlew samples}.
 */
public final class SampleWriter {
    /** Fixed "now": 2026-09-27 12:00 UTC. */
    static final long NOW = 1_790_510_400_000L;

    private SampleWriter() {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "samples");
        for (String kind : new String[]{"vanilla", "modded", "mc-1.18.2"}) {
            Files.createDirectories(root.resolve(kind));
            for (long seed = 1; seed <= 3; seed++) {
                for (Difficulty d : Difficulty.values()) {
                    FakeHost host = new FakeHost();
                    if (kind.equals("modded")) {
                        host.withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
                        host.content.dimensions.add("twilightforest:twilight_forest");
                    }
                    if (kind.equals("mc-1.18.2")) {
                        // an old version: content released later (mangrove, cherry, armadillo, ...) never appears
                        host.content.version = "1.18.2";
                        host.content.loader = "forge";
                    }
                    host.now = NOW;
                    host.world.seed = seed;
                    host.world.day = 30;
                    host.world.online = 2;
                    host.world.shares.put("minecraft:story/enter_the_nether", 1.0);
                    host.world.shares.put("minecraft:story/enter_the_end", 0.5);
                    GeneratorConfig cfg = GeneratorConfig.builder().difficulty(d).questsPerCycle(10)
                        .zone(ZoneId.of("UTC")).build();
                    QuestGeneratorV2 gen = new QuestGeneratorV2(host, cfg);
                    gen.start(Map.of());
                    String base = "seed" + seed + "-" + d.settingsValue();
                    JsonObject quests = new JsonObject();
                    StringBuilder explain = new StringBuilder();
                    explain.append("# ").append(kind).append(" | world seed ").append(seed).append(" | ")
                        .append(d).append(" | 10 quests | day 30, Nether and End unlocked\n\n")
                        .append(gen.status()).append("\n\n");
                    for (Map.Entry<String, JsonObject> e : gen.servedQuests().entrySet()) {
                        quests.add(e.getKey(), e.getValue());
                        explain.append(gen.explain(e.getKey())).append("\n\n");
                    }
                    write(root.resolve(kind).resolve(base + ".json"), Json.pretty(quests) + "\n");
                    write(root.resolve(kind).resolve(base + ".explain.txt"), explain.toString());
                    System.out.println(String.format(Locale.ROOT, "%s/%s: %d quests", kind, base, quests.size()));
                }
            }
        }
    }

    private static void write(Path p, String text) throws IOException {
        Files.writeString(p, text, StandardCharsets.UTF_8);
    }
}
