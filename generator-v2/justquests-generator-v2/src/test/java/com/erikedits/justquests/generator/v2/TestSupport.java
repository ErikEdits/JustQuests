package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import com.erikedits.justquests.generator.v2.internal.gen.Progression;
import com.erikedits.justquests.generator.v2.internal.util.Json;
import com.google.gson.JsonObject;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared helpers for the tests. */
public final class TestSupport {
    private static Catalog catalog;

    private TestSupport() {
    }

    /** The bundled catalog (loaded once). */
    public static synchronized Catalog catalog() {
        if (catalog == null) {
            FakeHost h = new FakeHost();
            catalog = new CatalogLoader(h.log, h.store).load();
        }
        return catalog;
    }

    public static GeneratorConfig config(Difficulty d, int n) {
        return GeneratorConfig.builder().difficulty(d).questsPerCycle(n).zone(ZoneId.of("UTC")).build().sanitized(null);
    }

    /** Progression with the Nether and the End unlocked (by game day). */
    public static Progression unlockedProgression(FakeHost host) {
        Progression p = new Progression();
        long day = host.world.day;
        host.world.day = 100;
        p.update(host.world, catalog().balance, catalog().profiles);
        host.world.day = day;
        return p;
    }

    /** Progression of a fresh world (day 0, nobody online). */
    public static Progression freshProgression() {
        return new Progression();
    }

    /** One side-effect-free generation run. */
    public static Generation.Result generate(FakeHost host, GeneratorConfig cfg, Progression p, long seed, int n) {
        return Generation.run(catalog(), host, cfg, p, Map.of(), Set.of(), List.of(), n, 1_790_000_000L, seed, 0);
    }

    public static String objectiveTarget(JsonObject objective) {
        for (String f : new String[]{"item", "block", "entity", "dimension"}) {
            if (objective.has(f)) {
                // an item filter object ({"id": ..., "potion": ...}) targets its id
                return objective.get(f).isJsonObject() ? objective.getAsJsonObject(f).get("id").getAsString()
                    : objective.get(f).getAsString();
            }
        }
        return null;
    }

    public static String shortType(JsonObject objective) {
        return Json.str(objective, "type", "").replace("justquests:", "");
    }
}
