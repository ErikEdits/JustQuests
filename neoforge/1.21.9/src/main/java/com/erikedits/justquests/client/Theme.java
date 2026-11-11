package com.erikedits.justquests.client;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Quest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The look of the quest book, the tracker and the popups, as resource packs set it (0.5.0, #21).
 * <p>
 * Pictures live in {@code assets/justquests/textures/gui/}: a picture is looked up in the new folders
 * first ({@code quest_book/book.png}, {@code buttons/claim_hover.png} ...), then under its old flat name
 * ({@code window.png}, {@code button_claim_hover.png} ...), which is also where the mod keeps its own. A
 * picture may be a whole multiple of its size (HD) and may be animated with a {@code .png.mcmeta}.
 * Settings come from one {@code settings.json} per folder; a missing value keeps the default.
 * Everything is read again after the game reloads its resources.
 */
public final class Theme {
    public static final String NS = "justquests";
    private static final ResourceLocation FONT = ResourceLocation.tryBuild(NS, "book");
    private static Theme current;
    private static Language loadedFor;

    // --- quest book (quest_book/settings.json) ---
    public final int bookW, bookH, listX, listY, rowW, rowH, rows, pageX, pageY, pageW, pageBottom;
    private final Map<String, int[]> buttons = new HashMap<>();
    public final int title, text, muted, heading, link, done, warning, group, groupCount;
    public final boolean blur, bookShadow;
    public final String bgMode;
    public final int bgColor;
    // --- progress bars (quest_book/progress/settings.json, tracker/progress/settings.json) ---
    public final int barW, barH, trackerBarW, trackerBarH;
    // --- tracker (tracker/settings.json) ---
    public final int tMaxText, tBorder, tIconX, tIconY, tTitleX, tTitleY, tGoalX, tGoalY, tLine, tMaxGoals, tOffX, tOffY;
    public final boolean tBar, tShadow;
    public final int tTitle, tGoal, tDone, tMore, tClaim, tBox;
    // --- popups (popup/settings.json) ---
    public final String pCorner;
    public final int pOffX, pOffY, pW, pH, pIconX, pIconY, pTitleX, pTitleY, pLineX, pLineY, pTitle, pLine;
    public final long pMillis;
    public final boolean pShadow;

    private final Map<String, ResourceLocation> resolved = new HashMap<>();
    private final Map<ResourceLocation, Boolean> present = new HashMap<>();
    private final Map<ResourceLocation, int[]> anims = new HashMap<>();
    private final Map<String, Integer> states = new HashMap<>();
    private final Map<ResourceLocation, int[]> sizes = new HashMap<>();

    /** The theme of the resource packs now loaded. */
    public static Theme get() {
        // the game puts a new Language in place on every resource reload
        if (current == null || loadedFor != Language.getInstance()) {
            loadedFor = Language.getInstance();
            current = new Theme();
        }
        return current;
    }

    private Theme() {
        JsonObject b = settings("quest_book");
        bookW = num(b, 280, "size", 0);
        bookH = num(b, 184, "size", 1);
        listX = num(b, 7, "list", "x");
        listY = num(b, 44, "list", "y");
        rowW = num(b, 112, "list", "row_width");
        rowH = num(b, 18, "list", "row_height");
        rows = Math.max(1, num(b, 6, "list", "rows"));
        pageX = num(b, 137, "page", "x");
        pageY = num(b, 24, "page", "y");
        pageW = num(b, 134, "page", "width");
        pageBottom = num(b, 153, "page", "bottom");
        button(b, "title", 9, 7);
        button(b, "hint", 48, 7);
        button(b, "action", 137, 156);
        button(b, "pin", 213, 156);
        button(b, "close", 260, 6);
        button(b, "stats", 212, 3);
        button(b, "corner", 228, 3);
        button(b, "hud", 244, 3);
        button(b, "sort", 7, 22);
        button(b, "filter", 25, 22);
        button(b, "search", 44, 23);
        button(b, "page_prev", 8, 164);
        button(b, "page_next", 108, 164);
        // the popup button (0.5.1) sits left of the stats button, also in packs that moved that one
        int[] stats = buttons.get("stats");
        button(b, "popup", stats[0] - 16, stats[1]);
        title = color(b, 0xFF161616, "colors", "title");
        text = color(b, 0xFF282828, "colors", "text");
        muted = color(b, 0xFF4C4C4C, "colors", "muted");
        heading = color(b, 0xFF24395C, "colors", "heading");
        link = color(b, 0xFF24395C, "colors", "link");
        done = color(b, 0xFF2E7D32, "colors", "done");
        warning = color(b, 0xFF9E2A2A, "colors", "warning");
        group = color(b, 0xFFFFFFFF, "colors", "group");
        groupCount = color(b, 0xFFC6C6C6, "colors", "group_count");
        blur = bool(b, true, "background", "blur");
        bgMode = str(b, "none", "background", "mode");
        bgColor = color(b, 0x80000000, "background", "color");
        bookShadow = bool(b, false, "text_shadow");

        JsonObject p = settings("quest_book/progress");
        barW = num(p, 100, "size", 0);
        barH = Math.max(1, num(p, 6, "size", 1));
        JsonObject tp = settings("tracker/progress");
        trackerBarW = num(tp, 0, "size", 0);   // 0 = as wide as the goal text
        trackerBarH = Math.max(1, num(tp, 3, "size", 1));

        JsonObject t = settings("tracker");
        tMaxText = num(t, 140, "max_text_width");
        tBorder = num(t, 3, "border");
        tIconX = num(t, 4, "icon", "x");
        tIconY = num(t, 4, "icon", "y");
        tTitleX = num(t, 23, "title", "x");
        tTitleY = num(t, 5, "title", "y");
        tGoalX = num(t, 23, "goal", "x");
        tGoalY = num(t, 15, "goal", "y");
        tLine = Math.max(1, num(t, 9, "goal", "line_height"));
        tMaxGoals = Math.max(1, num(t, 3, "max_goals"));
        tBar = bool(t, false, "progress_bar");
        tOffX = num(t, 4, "offset", 0);
        tOffY = num(t, 4, "offset", 1);
        tShadow = bool(t, true, "text_shadow");
        tTitle = color(t, 0xFFFFFFFF, "colors", "title");
        tGoal = color(t, 0xFFD0D0D0, "colors", "goal");
        tDone = color(t, 0xFF55FF55, "colors", "done");
        tMore = color(t, 0xFFA0A0A0, "colors", "more");
        tClaim = color(t, 0xFFFFAA00, "colors", "claim");
        tBox = color(t, 0xAA000000, "colors", "box");

        JsonObject u = settings("popup");
        pCorner = str(u, "top_right", "corner");
        pOffX = num(u, 4, "offset", 0);
        pOffY = num(u, 4, "offset", 1);
        pMillis = Math.max(1, num(u, 5, "seconds")) * 1000L;
        pW = num(u, 160, "size", 0);
        pH = num(u, 32, "size", 1);
        pIconX = num(u, 8, "icon", "x");
        pIconY = num(u, 8, "icon", "y");
        pTitleX = num(u, 30, "title", "x");
        pTitleY = num(u, 7, "title", "y");
        pLineX = num(u, 30, "line", "x");
        pLineY = num(u, 18, "line", "y");
        pTitle = color(u, 0xFFFFFF00, "colors", "title");
        pLine = color(u, 0xFFFFFFFF, "colors", "line");
        pShadow = bool(u, false, "text_shadow");
    }

    // ------------------------------------------------------------------ text

    /** The style of every JustQuests text: the font justquests:book (the normal font unless a pack sets one). */
    public static Style style() {
        return Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(FONT));
    }

    public static MutableComponent text(String s) {
        return Component.literal(s).withStyle(style());
    }

    public static MutableComponent text(Component c) {
        return c.copy().withStyle(style());
    }

    // ------------------------------------------------------------------ settings

    /** A button's place on the book, from the book's top left: {x, y}. */
    public int[] button(String name) {
        return buttons.getOrDefault(name, new int[2]);
    }

    private void button(JsonObject o, String name, int x, int y) {
        buttons.put(name, new int[] {num(o, x, "buttons", name, 0), num(o, y, "buttons", name, 1)});
    }

    private static JsonObject settings(String folder) {
        ResourceLocation id = ResourceLocation.tryBuild(NS, "textures/gui/" + folder + "/settings.json");
        Optional<Resource> r = id == null ? Optional.empty() : Minecraft.getInstance().getResourceManager().getResource(id);
        if (r.isEmpty()) return new JsonObject();
        try (Reader in = r.get().openAsReader()) {
            JsonElement e = JsonParser.parseReader(in);
            return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not read {}: {}", id, e.getMessage());
            return new JsonObject();
        }
    }

    /** The value at a path of keys (strings) and array places (ints), or null. */
    private static JsonElement at(JsonObject o, Object... path) {
        JsonElement e = o;
        for (Object step : path) {
            if (step instanceof String key && e != null && e.isJsonObject()) e = e.getAsJsonObject().get(key);
            else if (step instanceof Integer i && e != null && e.isJsonArray()) {
                JsonArray a = e.getAsJsonArray();
                e = i < a.size() ? a.get(i) : null;
            } else return null;
        }
        return e;
    }

    private static int num(JsonObject o, int def, Object... path) {
        JsonElement e = at(o, path);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
        } catch (Exception ex) {
            return def;
        }
    }

    private static boolean bool(JsonObject o, boolean def, Object... path) {
        JsonElement e = at(o, path);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    private static String str(JsonObject o, String def, Object... path) {
        JsonElement e = at(o, path);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    /** "#RRGGBB" (opaque) or "#AARRGGBB". */
    private static int color(JsonObject o, int def, Object... path) {
        String s = str(o, null, path);
        if (s == null) return def;
        s = s.startsWith("#") ? s.substring(1) : s;
        try {
            long v = Long.parseLong(s, 16);
            return s.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ------------------------------------------------------------------ pictures

    /** Whether a pack (or the mod) has this picture, by its new path ("quest_book/separator"). */
    public boolean has(String name) {
        ResourceLocation id = loc(newPath(name));
        return id != null && exists(id);
    }

    /** The picture to draw for a name: new path, else old flat name, else the category default. */
    public ResourceLocation tex(String name) {
        ResourceLocation id = resolved.get(name);
        if (id == null) {
            // not computeIfAbsent: a category without an icon looks up "cat_custom" in the same map
            id = find(name);
            resolved.put(name, id);
        }
        return id;
    }

    private ResourceLocation find(String n) {
        ResourceLocation neu = loc(newPath(n));
        if (neu != null && exists(neu)) return neu;
        ResourceLocation old = loc(n);
        // a category without its own icon: the "custom" one
        if (n.startsWith("cat_") && !n.equals("cat_custom") && (old == null || !exists(old))) return tex("cat_custom");
        return old != null ? old : tex("cat_custom");
    }

    /** Where an old flat name lives in the new folders (names already new stay as they are). */
    private static String newPath(String n) {
        if (n.equals("window")) return "quest_book/book";
        if (n.startsWith("quest_row_")) return "quest_book/rows/" + n.substring(10);
        if (n.startsWith("progress_")) return "quest_book/progress/" + n.substring(9);
        if (n.startsWith("button_")) return "buttons/" + n.substring(7);
        if (n.startsWith("page_") || n.startsWith("search_")) return "buttons/" + n;
        if (n.startsWith("glyph_")) return "icons/glyphs/" + n.substring(6);
        if (n.startsWith("cat_")) return "icons/categories/" + n.substring(4);
        return n;
    }

    private static ResourceLocation loc(String path) {
        return ResourceLocation.tryBuild(NS, "textures/gui/" + path + ".png");
    }

    private boolean exists(ResourceLocation id) {
        return present.computeIfAbsent(id, i -> Minecraft.getInstance().getResourceManager().getResource(i).isPresent());
    }

    /** A quest's own picture (icons/quests/<namespace>/<path>.png), or null. */
    public ResourceLocation questIcon(ResourceLocation quest) {
        String key = "icons/quests/" + quest.getNamespace() + "/" + quest.getPath();
        ResourceLocation id = loc(key);
        return id != null && exists(id) ? id : null;
    }

    /** The quest's icon: its own picture from a pack, else its item. */
    public void icon(GuiGraphics g, ResourceLocation quest, Quest q, int x, int y) {
        ResourceLocation own = questIcon(quest);
        if (own != null) draw(g, own, x, y, 16, 16, 16, 16, 16, 16);
        else g.renderItem(QuestIcons.of(quest, q), x, y);
    }

    /** The w x h top-left part of a picture shown at its size texW x texH (one frame if animated). */
    public void blit(GuiGraphics g, String name, int x, int y, int w, int h, int texW, int texH) {
        draw(g, tex(name), x, y, w, h, w, h, texW, texH);
    }

    /** The whole picture (size texW x texH) stretched to w x h. */
    public void stretch(GuiGraphics g, String name, int x, int y, int w, int h, int texW, int texH) {
        draw(g, tex(name), x, y, w, h, texW, texH, texW, texH);
    }

    /** Draws the region (0,0)-(uW,vH) of the current frame, scaled to w x h. */
    private void draw(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h, int uW, int vH, int texW, int texH) {
        int[] a = anim(id, texW, texH);
        int frame = a[0] > 1 ? (int) (System.currentTimeMillis() / 50 / a[1] % a[0]) : 0;
        Draw.blit(g, id, x, y, w, h, 0, frame * texH, uW, vH, texW, texH * a[0]);
    }

    /**
     * A picture stretched like a frame: the outer {@code border} pixels stay, the middle grows. The
     * picture's own pixel size counts (tracker.png can be any size).
     */
    public void frame(GuiGraphics g, String name, int x, int y, int w, int h, int border) {
        ResourceLocation id = tex(name);
        int[] size = size(id);
        int tw = size[0], th = size[1];
        int b = Math.max(0, Math.min(border, Math.min(Math.min(tw, th) / 2, Math.min(w, h) / 2)));
        int mw = tw - 2 * b, mh = th - 2 * b, iw = w - 2 * b, ih = h - 2 * b;
        int[] xs = {x, x + b, x + w - b}, ws = {b, iw, b}, us = {0, b, tw - b}, uws = {b, mw, b};
        int[] ys = {y, y + b, y + h - b}, hs = {b, ih, b}, vs = {0, b, th - b}, vhs = {b, mh, b};
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (ws[c] <= 0 || hs[r] <= 0 || uws[c] <= 0 || vhs[r] <= 0) continue;
                Draw.blit(g, id, xs[c], ys[r], ws[c], hs[r], us[c], vs[r], uws[c], vhs[r], tw, th);
            }
        }
    }

    /** A picture repeated over an area at its own pixel size. */
    public void tile(GuiGraphics g, String name, int x, int y, int w, int h) {
        ResourceLocation id = tex(name);
        int[] size = size(id);
        int tw = Math.max(1, size[0]), th = Math.max(1, size[1]);
        for (int ty = y; ty < y + h; ty += th) {
            for (int tx = x; tx < x + w; tx += tw) {
                int cw = Math.min(tw, x + w - tx), ch = Math.min(th, y + h - ty);
                Draw.blit(g, id, tx, ty, cw, ch, 0, 0, cw, ch, tw, th);
            }
        }
    }

    /**
     * A progress bar at a fraction (0 to 1): a picture per step if the pack has state_1 ... state_N,
     * else the empty bar with the full one cut off from the left.
     */
    public void progress(GuiGraphics g, boolean tracker, int x, int y, int w, int h, float fraction) {
        // the tracker's own pictures if it has them, else the book's ("progress_track" finds those)
        String track = tracker && has("tracker/progress/track") ? "tracker/progress/track" : "progress_track";
        String fill = tracker && has("tracker/progress/fill") ? "tracker/progress/fill" : "progress_fill";
        String folder = tracker && states("tracker/progress") > 0 ? "tracker/progress" : "quest_book/progress";
        int n = states(folder);
        float f = Math.max(0f, Math.min(1f, fraction));
        if (n > 0) {
            int step = (int) Math.ceil(f * n);
            stretch(g, step == 0 ? track : folder + "/state_" + step, x, y, w, h, w, h);
            return;
        }
        stretch(g, track, x, y, w, h, w, h);
        int fw = (int) (w * f);
        if (fw > 0) blit(g, fill, x, y, fw, h, w, h);
    }

    /** How many state_N pictures a progress folder has (1 to 100 in a row), 0 for none. */
    private int states(String folder) {
        return states.computeIfAbsent(folder, f -> {
            int n = 0;
            while (n < 100 && has(f + "/state_" + (n + 1))) n++;
            return n;
        });
    }

    /** Frames and ticks per frame of a picture; one frame without a .png.mcmeta. */
    private int[] anim(ResourceLocation id, int texW, int texH) {
        return anims.computeIfAbsent(id, i -> {
            ResourceLocation meta = ResourceLocation.tryBuild(i.getNamespace(), i.getPath() + ".mcmeta");
            Optional<Resource> r = meta == null ? Optional.empty() : Minecraft.getInstance().getResourceManager().getResource(meta);
            if (r.isEmpty() || texW <= 0 || texH <= 0) return new int[] {1, 1};
            int frametime = 1;
            try (Reader in = r.get().openAsReader()) {
                JsonElement e = JsonParser.parseReader(in);
                frametime = Math.max(1, num(e.getAsJsonObject(), 1, "animation", "frametime"));
            } catch (Exception e) {
                JustQuests.LOG.warn("Could not read {}: {}", meta, e.getMessage());
            }
            int[] size = size(i);
            // one frame has the picture's width and the shown aspect ratio
            int frameH = Math.max(1, size[0] * texH / texW);
            return new int[] {Math.max(1, size[1] / frameH), frametime};
        });
    }

    /** A picture's pixel size, read once. */
    private int[] size(ResourceLocation id) {
        return sizes.computeIfAbsent(id, Theme::pngSize);
    }

    /** A PNG's width and height from its header (16 x 16 if it cannot be read). */
    private static int[] pngSize(ResourceLocation id) {
        Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(id);
        if (r.isPresent()) {
            try (InputStream in = r.get().open()) {
                byte[] head = in.readNBytes(24);
                if (head.length == 24) {
                    int w = (head[16] & 0xFF) << 24 | (head[17] & 0xFF) << 16 | (head[18] & 0xFF) << 8 | head[19] & 0xFF;
                    int h = (head[20] & 0xFF) << 24 | (head[21] & 0xFF) << 16 | (head[22] & 0xFF) << 8 | head[23] & 0xFF;
                    if (w > 0 && h > 0) return new int[] {w, h};
                }
            } catch (Exception e) {
                JustQuests.LOG.warn("Could not read the size of {}", id);
            }
        }
        return new int[] {16, 16};
    }
}
