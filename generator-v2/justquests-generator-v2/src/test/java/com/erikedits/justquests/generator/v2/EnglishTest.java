package com.erikedits.justquests.generator.v2;

import com.erikedits.justquests.generator.v2.internal.util.English;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Plurals and articles of generated text, including the modded names of the bundled profiles. */
class EnglishTest {
    @Test
    void itemPlurals() {
        assertEquals("Oak Logs", English.plural("Oak Log"));
        assertEquals("Raw Iron", English.plural("Raw Iron"));
        assertEquals("Raw Osmium", English.plural("Raw Osmium"));
        assertEquals("Raw Tin", English.plural("Raw Tin"));
        assertEquals("Raw Uranium", English.plural("Raw Uranium"));
        assertEquals("Leads", English.plural("Lead"), "the leash item keeps its plural");
        assertEquals("Liveroot", English.plural("Liveroot"));
        assertEquals("Raw Venison", English.plural("Raw Venison"));
        // steak is a mass noun, as for vanilla steak; the Twilight Forest profile sets its own plural
        assertEquals("Venison Steak", English.plural("Venison Steak"));
        assertEquals("Raw Ironwood", English.plural("Raw Ironwood"));
        assertEquals("Ironwood Ingots", English.plural("Ironwood Ingot"));
        assertEquals("Torchberries", English.plural("Torchberries"));
        assertEquals("Canopy Bookshelves", English.plural("Canopy Bookshelf"));
        assertEquals("Crimson Fungi", English.plural("Crimson Fungus"));
        assertEquals("Magic Map Focuses", English.plural("Magic Map Focus"));
        assertEquals("Glass Panes", English.plural("Glass Pane"));
        assertEquals("Potatoes", English.plural("Potato"));
        assertEquals("Cherries", English.plural("Cherry"));
        assertEquals("Sculk", English.plural("Sculk"));
        assertEquals("Crimson Nylium", English.plural("Crimson Nylium"));
        assertEquals("Glow Lichen", English.plural("Glow Lichen"));
        assertEquals("Flint and Steel", English.plural("Flint and Steel"));
        assertEquals("Sculk Sensors", English.plural("Sculk Sensor"));
    }

    @Test
    void entityPlurals() {
        assertEquals("Deer", English.plural("Deer", true));
        assertEquals("Bighorn Sheep", English.plural("Bighorn Sheep", true));
        assertEquals("Boars", English.plural("Boar", true));
        assertEquals("Chickens", English.plural("Chicken", true), "entities ignore the item mass nouns");
        assertEquals("Drowned", English.plural("Drowned", true));
        assertEquals("Wolves", English.plural("Wolf", true));
    }

    @Test
    void articles() {
        assertEquals("an iron ingot", English.withArticle("iron ingot"));
        assertEquals("a lich", English.withArticle("lich"));
        assertEquals("an osmium ingot", English.withArticle("osmium ingot"));
    }
}
