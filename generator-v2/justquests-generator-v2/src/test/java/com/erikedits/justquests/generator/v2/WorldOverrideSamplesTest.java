package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.internal.Generation;
import com.erikedits.justquests.generator.v2.internal.catalog.Catalog;
import com.erikedits.justquests.generator.v2.internal.catalog.CatalogLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The example world overrides in samples/world-overrides load cleanly and take effect. */
class WorldOverrideSamplesTest {
    private static final Path ROOT = Path.of("samples/world-overrides");

    private static FakeHost hostWithSamples() throws IOException {
        FakeHost host = new FakeHost().withMods("examplemod");
        for (String f : List.of("generator_v2/balance.json", "generator_v2/profiles/examplemod.json")) {
            host.store.files.put(f, Files.readString(ROOT.resolve(f), StandardCharsets.UTF_8));
        }
        return host;
    }

    @Test
    void samplesLoadWithoutWarningsAndApply() throws IOException {
        FakeHost host = hostWithSamples();
        Catalog catalog = new CatalogLoader(host.log, host.store).load();
        assertEquals(List.of(), catalog.loadWarnings, "the samples must load without warnings");
        assertNotNull(catalog.profile("examplemod"));
        assertEquals(3.0, catalog.balance.level(Difficulty.EASY).minMinutes, 1e-9);
        assertEquals(0.4, catalog.balance.maxPerModShare, 1e-9);
        // with every id present, the example ore shows up in a Normal set
        Generation.Result r = Generation.run(catalog, host, TestSupport.config(Difficulty.NORMAL, 10),
            TestSupport.unlockedProgression(host), Map.of(), Set.of(), List.of(), 10, 1L, 3L, 0);
        assertTrue(r.drafts().stream().anyMatch(q -> q.profiles().contains("examplemod")), "one quest per active mod");
        // the dimension does not exist on this host, so its mob is rejected
        assertTrue(r.pool().candidates().stream().noneMatch(c -> c.target().equals("examplemod:glow_bat")));
    }

    @Test
    void facadeServesTheExampleProfile() throws IOException {
        FakeHost host = hostWithSamples();
        QuestGeneratorV2 g = new QuestGeneratorV2(host, TestSupport.config(Difficulty.NORMAL, 10));
        g.start(Map.of());
        assertTrue(g.status().contains("examplemod"), g.status());
        assertTrue(g.servedQuests().values().stream().anyMatch(q -> q.toString().contains("examplemod:")));
        assertTrue(g.selfTest().isEmpty(), g.selfTest().toString());
    }
}
