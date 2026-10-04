package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.internal.state.V1Migration;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulates 14 days (28 rotations) of a small server with 8 players who take generated quests,
 * finish them after 0.7–2.5× the estimated time or give up, and writes
 * {@code samples/simulation/<difficulty>.md} with every cycle's new quests, variety figures and the
 * resulting {@code stats().toText()}. Deterministic. Run with {@code ./gradlew simulate}.
 */
public final class Simulation {
    private static final long MIN = 60_000L;
    private static final Pattern EST = Pattern.compile("estimated ([0-9.]+) min");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("EEE dd HH:mm", Locale.ROOT);

    private Simulation() {
    }

    /** One simulated player. */
    private static final class Player {
        final UUID id;
        String quest;
        long doneAt;
        boolean willAbandon;

        Player(int n) {
            id = new UUID(0x5157L, n);
        }
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "samples/simulation");
        Files.createDirectories(root);
        for (Difficulty d : Difficulty.values()) {
            Files.writeString(root.resolve(d.settingsValue() + ".md"), run(d), StandardCharsets.UTF_8);
            System.out.println("simulated " + d);
        }
    }

    static String run(Difficulty difficulty) {
        FakeHost host = new FakeHost().withMods("farmersdelight", "create", "mekanism", "twilightforest", "botania");
        host.content.dimensions.add("twilightforest:twilight_forest");
        host.now = SampleWriter.NOW;
        host.world.seed = 2026L;
        host.world.day = 30;
        host.world.online = 8;
        host.world.shares.put("minecraft:story/enter_the_nether", 0.75);
        host.world.shares.put("minecraft:story/enter_the_end", 0.25);
        GeneratorConfig cfg = GeneratorConfig.builder().difficulty(difficulty).questsPerCycle(10)
            .zone(ZoneId.of("UTC")).build();
        QuestGeneratorV2 gen = new QuestGeneratorV2(host, cfg);
        gen.start(Map.of());
        Random rnd = new Random(42L);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            players.add(new Player(i));
        }
        StringBuilder cycles = new StringBuilder();
        Map<String, Integer> types = new TreeMap<>();
        Map<String, Integer> families = new TreeMap<>();
        Map<String, Long> lastSeen = new HashMap<>();
        Map<String, Integer> profiles = new TreeMap<>();
        int quests = 0;
        int repeatsInWindow = 0;
        int completed = 0;
        int abandoned = 0;
        int denied = 0;
        appendCycle(cycles, gen, host, gen.servedQuests().keySet(), types, families, profiles, lastSeen);
        quests += gen.servedQuests().size();
        long end = host.now + 14 * 24 * 60 * MIN;
        while (host.now < end) {
            host.now += 5 * MIN; // the mod calls tick() every ~5 minutes
            RotationResult r = gen.tick();
            if (r.changed() && !r.added().isEmpty()) {
                for (String id : r.added()) {
                    String sig = V1Migration.signatureOf(gen.servedQuests().get(id));
                    Long prev = lastSeen.get(sig);
                    if (prev != null && host.now - prev < 6L * 24 * 60 * MIN) {
                        repeatsInWindow++;
                    }
                }
                appendCycle(cycles, gen, host, r.added(), types, families, profiles, lastSeen);
                quests += r.added().size();
            }
            for (Player p : players) {
                if (p.quest != null && host.now >= p.doneAt) {
                    if (p.willAbandon) {
                        gen.onAbandon(p.quest, p.id);
                        abandoned++;
                    } else {
                        gen.onComplete(p.quest, p.id);
                        completed++;
                    }
                    p.quest = null;
                } else if (p.quest == null && rnd.nextDouble() < 0.035) {
                    List<String> open = new ArrayList<>();
                    for (String id : gen.servedQuests().keySet()) {
                        if (gen.claim(id).state() == ClaimState.AVAILABLE) {
                            open.add(id);
                        }
                    }
                    if (open.isEmpty()) {
                        continue;
                    }
                    String pick = open.get(rnd.nextInt(open.size()));
                    ClaimResult c = gen.tryClaim(pick, p.id);
                    if (c == ClaimResult.OK) {
                        Matcher m = EST.matcher(gen.explain(pick));
                        double est = m.find() ? Double.parseDouble(m.group(1)) : 15.0;
                        p.quest = pick;
                        p.willAbandon = rnd.nextDouble() < 0.12;
                        double factor = 0.7 + rnd.nextDouble() * 1.8;
                        p.doneAt = host.now + (long) (est * factor * MIN);
                    } else {
                        denied++;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Simulation — ").append(difficulty).append("\n\n");
        sb.append("14 days (28 rotations of 10 quests) on a server with 8 players, Farmer's Delight, Create, ")
            .append("Mekanism, the Twilight Forest and Botania installed, day 30, Nether and End unlocked. Every 5 simulated minutes each idle player takes ")
            .append("a random free quest with probability 0.035 and finishes it after 0.7–2.5× the estimated time ")
            .append("(12 % give up instead). Generated by `./gradlew simulate` (deterministic).\n\n");
        sb.append("## Variety\n\n");
        sb.append("- quests generated: ").append(quests).append('\n');
        sb.append("- signature repeats within 6 days: ").append(repeatsInWindow).append('\n');
        sb.append("- distinct signatures: ").append(lastSeen.size()).append('\n');
        sb.append("- completed ").append(completed).append(", abandoned ").append(abandoned)
            .append(", claims denied (race/one-active) ").append(denied).append('\n');
        sb.append("- objective types: ").append(types).append('\n');
        sb.append("- profiles: ").append(profiles).append('\n');
        sb.append("- top families: ").append(top(families, 15)).append("\n\n");
        sb.append("## Statistics (`stats().toText()`)\n\n```\n").append(gen.stats().toText()).append("```\n\n");
        sb.append("## New quests per rotation\n").append(cycles);
        return sb.toString();
    }

    private static void appendCycle(StringBuilder sb, QuestGeneratorV2 gen, FakeHost host, Iterable<String> ids,
                                    Map<String, Integer> types, Map<String, Integer> families,
                                    Map<String, Integer> profiles, Map<String, Long> lastSeen) {
        sb.append("\n### ").append(ZonedDateTime.ofInstant(Instant.ofEpochMilli(host.now), ZoneId.of("UTC")).format(FMT))
            .append("\n\n");
        for (String id : ids) {
            JsonObject q = gen.servedQuests().get(id);
            if (q == null) {
                continue;
            }
            lastSeen.put(V1Migration.signatureOf(q), host.now);
            List<String> objs = new ArrayList<>();
            for (JsonElement e : q.getAsJsonArray("objectives")) {
                JsonObject o = e.getAsJsonObject();
                String t = TestSupport.shortType(o);
                types.merge(t, 1, Integer::sum);
                String target = TestSupport.objectiveTarget(o);
                objs.add(t + " " + target + (o.has("count") ? " ×" + o.get("count").getAsInt() : ""));
                String ns = target.startsWith("#") ? "tag" : target.substring(0, target.indexOf(':'));
                profiles.merge(List.of("farmersdelight", "create", "mekanism", "twilightforest", "botania").contains(ns) ? ns : "vanilla", 1,
                    Integer::sum);
            }
            String explain = gen.explain(id);
            Matcher fam = Pattern.compile("family (\\w+)").matcher(explain);
            while (fam.find()) {
                families.merge(fam.group(1), 1, Integer::sum);
            }
            sb.append("- **").append(Json.text(q, "title", "")).append("** (").append(q.get("sort").getAsInt())
                .append(" min) — ").append(String.join("; ", objs)).append('\n');
        }
    }

    private static String top(Map<String, Integer> m, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(m.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : list.subList(0, Math.min(n, list.size()))) {
            out.put(e.getKey(), e.getValue());
        }
        return out.toString();
    }
}
