package com.erikedits.justquests.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Minimap mods (0.5.1, #24). They draw their map in a corner of the screen, on top of what JustQuests
 * draws there. With one of them loaded, the tracker and the popups move down below the map when they
 * share its corner (the popups only unless the player has placed them). The corners are where each mod
 * puts its map by default; a player who moved it can move the tracker and the popups as well.
 */
public final class Minimaps {
    /**
     * Mod id, the corner of its map, and how far down the map with its text reaches by default: a share
     * of the screen's height (percent) plus GUI pixels. Xaero's was measured; the others are estimated
     * on the safe side.
     */
    private static final Object[][] MODS = {
        {"journeymap", "top_right", 36, 24}, {"ftbchunks", "top_right", 30, 20}, {"voxelmap", "top_right", 30, 16},
        {"xaerominimap", "top_left", 30, 14}, {"xaerominimapfair", "top_left", 30, 14}, {"map_atlases", "top_left", 30, 16}};
    private static Set<String> corners;

    private Minimaps() {}

    /** Whether a known minimap mod is loaded. */
    public static boolean present() {
        return !corners().isEmpty();
    }

    /** Whether a loaded minimap mod draws its map in this corner ("top_right" ...). */
    public static boolean at(String corner) {
        return corners().contains(corner);
    }

    /** Where something in this corner starts below the minimaps there (0 without one). */
    public static int below(String corner, int screenHeight) {
        int y = 0;
        for (Object[] mod : MODS) {
            if (mod[1].equals(corner) && loaded((String) mod[0])) {
                y = Math.max(y, screenHeight * (Integer) mod[2] / 100 + (Integer) mod[3]);
            }
        }
        return y;
    }

    /** The corners with a minimap, looked up once. */
    private static Set<String> corners() {
        if (corners == null) {
            Set<String> found = new HashSet<>();
            for (Object[] mod : MODS) {
                if (loaded((String) mod[0])) found.add((String) mod[1]);
            }
            corners = found;
        }
        return corners;
    }

    private static boolean loaded(String id) {
        try {
            return net.neoforged.fml.ModList.get().isLoaded(id);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
