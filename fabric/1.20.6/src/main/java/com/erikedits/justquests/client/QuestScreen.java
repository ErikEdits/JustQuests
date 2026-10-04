package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The quest book, rendered from the JustQuests v2-full pixel textures (no vanilla widgets — manual
 * blit + click hit-testing so the look is fully custom). Left: the quest list, grouped by category or
 * by status, one item icon per quest, with a search field once there are many quests. Right: the
 * selected quest (active ones can be pinned to the HUD), or the player's stats. Reads the synced
 * {@link ClientQuestData} and runs /quest accept|abandon for actions. Textures live in
 * assets/justquests/textures/gui/. Fixed 280x184 window.
 */
public class QuestScreen extends Screen {
    private static final int W = 280, H = 184;
    private static final int ROWS = 6, ROW_W = 112, ROW_H = 18;
    /** The search field shows up from this many quests on. */
    private static final int SEARCH_MIN = 15;
    // darker text reads clearly on the light-grey panes; full alpha, since 1.21.6+ skips text with alpha 0
    private static final int TITLE_DARK = 0xFF161616, TEXT = 0xFF282828, MUTED = 0xFF4C4C4C, HEAD = 0xFF24395C,
        GOOD = 0xFF2E7D32, WHITE = 0xFFFFFFFF, LIGHT = 0xFFC6C6C6;

    /** Where a quest stands for this player; also the order of the status groups. */
    private enum Status {
        ACTIVE("active", "glyph_exclamation"), AVAILABLE("available", "glyph_star"),
        LOCKED("locked", "glyph_lock"), COMPLETED("completed", "glyph_check");

        final String label, icon;

        Status(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    /** One line of the list: a group header, a quest, or an empty spacer. */
    private record Entry(String header, String icon, String count, ResourceLocation id, Quest quest) {
        static final Entry SPACER = new Entry(null, null, null, null, null);

        boolean isHeader() { return header != null; }
        boolean isQuest() { return id != null; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private ResourceLocation selected;
    private boolean showStats;
    private int page = 0, left, top;
    private int shownSync = -1;
    private EditBox search;
    private String query = "";

    public QuestScreen() {
        super(Component.translatable("justquests.book.title"));
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("justquests", "textures/gui/" + name + ".png");
    }

    /** Draw a whole texture PNG (size natW x natH) at (x,y). */
    private void blit(GuiGraphics g, String name, int x, int y, int natW, int natH) {
        blitPart(g, name, x, y, natW, natH, natW, natH);
    }

    /**
     * Draw the top-left w x h region of a texW x texH texture. This is the ONLY
     * place that calls GuiGraphics.blit, so porting to another MC version only
     * needs this one line changed (the blit signature shifts at 1.21.2/1.21.4).
     */
    private void blitPart(GuiGraphics g, String name, int x, int y, int w, int h, int texW, int texH) {
        g.blit(tex(name), x, y, 0, 0, w, h, texW, texH);
    }

    private String lang() {
        return Minecraft.getInstance().options.languageCode;
    }

    private PlayerQuestData data() {
        // client-side synced copy (works on servers and in singleplayer)
        return ClientQuestData.getData();
    }

    @Override
    protected void init() {
        ClientSettings.load();
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        // drawn by us on the pixel field texture; the box only handles typing
        search = new EditBox(this.font, left + 47, top + 26, 56, 9, Component.translatable("justquests.book.search"));
        search.setBordered(false);
        search.setMaxLength(40);
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            page = 0;
            rebuild();
        });
        search.visible = searchShown();
        addWidget(search);
        refresh();
    }

    private boolean searchShown() {
        return ClientQuestData.getQuests().size() >= SEARCH_MIN;
    }

    /**
     * Rebuild the list from the latest sync. Called on open and every frame, but only does work when
     * a sync has arrived since (progress moves quests between the status groups, so every sync counts).
     */
    private void refresh() {
        int s = ClientQuestData.syncCount();
        if (s == shownSync) return;
        shownSync = s;
        QuestHud.prunePins();
        rebuild();
    }

    private void rebuild() {
        entries.clear();
        Map<ResourceLocation, Quest> all = ClientQuestData.getQuests();
        if (selected != null && !all.containsKey(selected)) selected = null;
        List<Map.Entry<ResourceLocation, Quest>> sorted = new ArrayList<>(all.entrySet());
        sorted.sort(Comparator
            .comparing((Map.Entry<ResourceLocation, Quest> e) -> e.getValue().category(), String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(e -> e.getValue().sort())
            .thenComparing(e -> e.getKey().toString()));

        // group key -> quests; status groups keep the enum order, categories the sorted order
        Map<String, List<Map.Entry<ResourceLocation, Quest>>> groups = new LinkedHashMap<>();
        if (ClientSettings.byStatus) {
            for (Status st : Status.values()) groups.put(st.name(), new ArrayList<>());
        }
        Map<String, int[]> perCategory = new LinkedHashMap<>();   // category -> {done, total}
        String q = searchShown() ? query.trim().toLowerCase(Locale.ROOT) : "";
        for (Map.Entry<ResourceLocation, Quest> e : sorted) {
            Status st = status(e.getKey(), e.getValue());
            int[] c = perCategory.computeIfAbsent(e.getValue().category(), k -> new int[2]);
            c[1]++;
            if (data().isCompleted(e.getKey())) c[0]++;
            if (ClientSettings.hideCompleted && st == Status.COMPLETED) continue;
            if (!matches(e.getValue(), q)) continue;
            String key = ClientSettings.byStatus ? st.name() : e.getValue().category();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<String, List<Map.Entry<ResourceLocation, Quest>>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            Entry header;
            if (ClientSettings.byStatus) {
                Status st = Status.valueOf(group.getKey());
                header = new Entry(I18n.get("justquests.book.status." + st.label), st.icon, String.valueOf(group.getValue().size()), null, null);
            } else {
                int[] c = perCategory.get(group.getKey());
                header = new Entry(QuestIcons.categoryName(group.getKey()), QuestIcons.categoryIcon(group.getKey()),
                    c[0] + "/" + c[1], null, null);
            }
            // never leave a header alone on the last line of a page
            if (entries.size() % ROWS == ROWS - 1) entries.add(Entry.SPACER);
            entries.add(header);
            for (Map.Entry<ResourceLocation, Quest> e : group.getValue()) {
                entries.add(new Entry(null, null, null, e.getKey(), e.getValue()));
            }
        }
        if (page > pages() - 1) page = Math.max(0, pages() - 1);
    }

    /** Search: title, category and goals, so "diam" finds every quest with diamonds. */
    private boolean matches(Quest quest, String q) {
        if (q.isEmpty()) return true;
        if (quest.title().get(lang()).toLowerCase(Locale.ROOT).contains(q)) return true;
        if (QuestIcons.categoryName(quest.category()).toLowerCase(Locale.ROOT).contains(q)) return true;
        for (QuestObjective o : quest.objectives()) {
            if (QuestIcons.label(o).getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private int pages() {
        return Math.max(1, (entries.size() + ROWS - 1) / ROWS);
    }

    // --- quest state ---
    private Status status(ResourceLocation id, Quest q) {
        PlayerQuestData d = data();
        if (d.isActive(id)) return Status.ACTIVE;
        if (takenByOther(id)) return Status.LOCKED;
        if (d.isCompleted(id) && (!q.repeatable() || cooldownLeft(id, q) > 0)) return Status.COMPLETED;
        if (!missing(q).isEmpty()) return Status.LOCKED;
        return Status.AVAILABLE;
    }

    /** Milliseconds until a repeatable quest can be taken again (0 = now). */
    private long cooldownLeft(ResourceLocation id, Quest q) {
        Long done = data().completed.get(id);
        if (done == null || q.cooldownHours().isEmpty()) return 0;
        return Math.max(0, done + q.cooldownHours().get() * 3_600_000L - System.currentTimeMillis());
    }

    /** Required quests the player has not completed yet. */
    private List<ResourceLocation> missing(Quest q) {
        List<ResourceLocation> out = new ArrayList<>();
        for (ResourceLocation req : q.requires()) if (!data().isCompleted(req)) out.add(req);
        return out;
    }

    /** A generated quest another player holds (or finished) under exclusive claims. */
    private static boolean takenByOther(ResourceLocation id) {
        ClientQuestData.Claim c = ClientQuestData.claim(id);
        return c != null && !c.mine();
    }

    // --- geometry helpers ---
    private int listX() { return left + 7; }
    private int rowY(int i) { return top + 44 + i * ROW_H; }
    private int sortX() { return left + 7; }
    private int filterX() { return left + 25; }
    private int toolY() { return top + 22; }
    private int prevX() { return left + 8; }
    private int nextX() { return left + 8 + ROW_W - 12; }
    private int navY() { return top + H - 20; }
    private int closeX() { return left + W - 20; }
    private int closeY() { return top + 6; }
    private int statsX() { return left + W - 52; }
    private int hudX() { return left + W - 36; }
    private int barY() { return top + 3; }
    private int detailX() { return left + 137; }
    private int detailW() { return W - 137 - 9; }
    private int actionX() { return detailX(); }
    private int actionY() { return top + H - 28; }
    private int pinX() { return actionX() + 76; }
    private int clearX() { return left + 44 + 74 - 12; }
    private boolean hasPrev() { return page > 0; }
    private boolean hasNext() { return page < pages() - 1; }

    /** Truncate to width with an ellipsis so titles never run past their row. */
    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        return this.font.plainSubstrByWidth(s, maxW - this.font.width("...")) + "...";
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        refresh();
        this.renderBackground(g, mouseX, mouseY, pt);
        blit(g, "window", left, top, W, H);
        g.drawString(this.font, Component.translatable("justquests.book.title"), left + 9, closeY() + 1, TITLE_DARK, false);

        // title-bar buttons: stats page, HUD on/off, close
        blit(g, showStats ? "button_stats_on" : in(mouseX, mouseY, statsX(), barY(), 14, 14) ? "button_stats_hover" : "button_stats_normal",
            statsX(), barY(), 14, 14);
        blit(g, ClientSettings.hud ? "button_hud_on" : in(mouseX, mouseY, hudX(), barY(), 14, 14) ? "button_hud_hover" : "button_hud_normal",
            hudX(), barY(), 14, 14);
        blit(g, in(mouseX, mouseY, closeX(), closeY(), 11, 11) ? "button_close_hover" : "button_close_normal",
            closeX(), closeY(), 11, 11);

        // list tools: grouping, filter and (with many quests) the search field
        boolean overSort = in(mouseX, mouseY, sortX(), toolY(), 16, 16);
        boolean overFilter = in(mouseX, mouseY, filterX(), toolY(), 16, 16);
        blit(g, overSort ? "button_sort_hover" : "button_sort_normal", sortX(), toolY(), 16, 16);
        blit(g, ClientSettings.hideCompleted ? "button_filter_on" : overFilter ? "button_filter_hover" : "button_filter_normal",
            filterX(), toolY(), 16, 16);
        search.visible = searchShown();
        if (search.visible) {
            blit(g, search.isFocused() ? "search_focused" : "search_normal", left + 44, top + 23, 74, 14);
            if (query.isEmpty() && !search.isFocused()) g.drawString(this.font, I18n.get("justquests.book.search"), left + 47, top + 26, 0xFF8B8B8B, false);
            search.render(g, mouseX, mouseY, pt);
            if (!query.isEmpty()) blit(g, "search_clear", clearX(), top + 25, 10, 10);
        } else {
            g.drawString(this.font, I18n.get(ClientSettings.byStatus ? "justquests.book.by_status" : "justquests.book.by_category"), left + 46, toolY() + 4, TEXT, false);
        }

        // what the hovered button does, next to the title
        boolean pinShown = !showStats && selected != null && data().isActive(selected);
        String hint = overSort ? (ClientSettings.byStatus ? "group_category" : "group_status")
            : overFilter ? (ClientSettings.hideCompleted ? "show_completed" : "hide_completed")
            : in(mouseX, mouseY, statsX(), barY(), 14, 14) ? (showStats ? "back" : "stats")
            : in(mouseX, mouseY, hudX(), barY(), 14, 14) ? (ClientSettings.hud ? "hide_hud" : "show_hud")
            : pinShown && in(mouseX, mouseY, pinX(), actionY(), 20, 20) ? (ClientSettings.isPinned(selected) ? "unpin" : "pin")
            : null;
        if (hint != null) {
            g.drawString(this.font, fit("- " + I18n.get("justquests.book.hint." + hint), W - 48 - 56), left + 48, closeY() + 1, MUTED, false);
        }

        if (entries.isEmpty()) {
            g.drawString(this.font, Component.translatable(ClientQuestData.getQuests().isEmpty() ? "justquests.book.empty"
                    : search.visible && !query.isBlank() ? "justquests.book.no_match" : "justquests.book.nothing"),
                listX() + 2, rowY(0) + 2, LIGHT, false);
        } else {
            int start = page * ROWS;
            for (int i = 0; i < ROWS && start + i < entries.size(); i++) {
                Entry e = entries.get(start + i);
                if (e.isHeader()) renderHeader(g, e, rowY(i));
                else if (e.isQuest()) renderRow(g, e, rowY(i), in(mouseX, mouseY, listX(), rowY(i), ROW_W, ROW_H));
            }
        }
        // page arrows and number
        blit(g, hasPrev() ? (in(mouseX, mouseY, prevX(), navY(), 12, 12) ? "page_prev_hover" : "page_prev_normal") : "page_prev_disabled",
            prevX(), navY(), 12, 12);
        blit(g, hasNext() ? (in(mouseX, mouseY, nextX(), navY(), 12, 12) ? "page_next_hover" : "page_next_normal") : "page_next_disabled",
            nextX(), navY(), 12, 12);
        String pg = (page + 1) + "/" + pages();
        g.drawString(this.font, pg, left + 8 + ROW_W / 2 - this.font.width(pg) / 2, navY() + 2, TEXT, false);

        if (showStats) renderStats(g, mouseX, mouseY);
        else renderDetail(g, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g, Entry e, int y) {
        int x = listX();
        blit(g, e.icon(), x + 1, y, 16, 16);
        g.drawString(this.font, fit(e.header(), ROW_W - 50), x + 20, y + 4, WHITE, true);
        g.drawString(this.font, e.count(), x + ROW_W - 3 - this.font.width(e.count()), y + 4, LIGHT, true);
        g.fill(x, y + 16, x + ROW_W, y + 17, 0xFF555555);
    }

    private void renderRow(GuiGraphics g, Entry e, int y, boolean hover) {
        int x = listX();
        ResourceLocation id = e.id();
        Quest q = e.quest();
        Status st = status(id, q);
        boolean sel = id.equals(selected);
        boolean cooldown = st == Status.COMPLETED && q.repeatable();
        String state = sel ? "selected"
            : st == Status.ACTIVE ? "active"
            : st == Status.LOCKED ? "locked"
            : st == Status.COMPLETED && !cooldown ? "completed"
            : hover ? "hover" : "available";
        blit(g, "quest_row_" + state, x, y, ROW_W, ROW_H);
        // completed and locked rows have their glyph in the texture; the others get one drawn on top
        String glyph = cooldown ? "glyph_clock"
            : st == Status.ACTIVE && ClientSettings.isPinned(id) ? "glyph_pin"
            : st == Status.AVAILABLE && q.repeatable() ? "glyph_repeat"
            : sel && st == Status.COMPLETED ? "glyph_check"
            : sel && st == Status.LOCKED ? "glyph_lock" : null;
        if (glyph != null) blit(g, glyph, x + ROW_W - 18, y + 1, 16, 16);
        boolean hasGlyph = glyph != null || state.equals("completed") || state.equals("locked");
        g.renderItem(QuestIcons.of(id, q), x + 3, y + 1);
        String title = fit(q.title().get(lang()), ROW_W - 23 - (hasGlyph ? 18 : 4));
        g.drawString(this.font, title, x + 22, y + 5, st == Status.LOCKED ? MUTED : TITLE_DARK, false);
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        int dx = detailX(), dy = top + 24, dw = detailW();
        if (selected == null) {
            g.drawString(this.font, Component.translatable("justquests.book.select_1"), dx, dy, MUTED, false);
            g.drawString(this.font, Component.translatable("justquests.book.select_2"), dx, dy + 11, MUTED, false);
            return;
        }
        Quest q = ClientQuestData.get(selected);
        if (q == null) return;
        PlayerQuestData d = data();
        QuestProgress prog = d.active.get(selected);
        Status st = status(selected, q);

        // icon + title (wraps next to the icon)
        g.renderItem(QuestIcons.of(selected, q), dx, dy);
        var titleLines = this.font.split(Component.literal(q.title().get(lang())), dw - 20);
        int ty = titleLines.size() == 1 ? dy + 4 : dy;
        for (var line : titleLines) {
            g.drawString(this.font, line, dx + 20, ty, TITLE_DARK, false);
            ty += 10;
        }
        dy = Math.max(dy + 19, ty + 2);

        // why it cannot be taken right now
        ClientQuestData.Claim claim = ClientQuestData.claim(selected);
        List<ResourceLocation> missing = missing(q);
        long wait = st == Status.COMPLETED && q.repeatable() ? cooldownLeft(selected, q) : 0;
        String note = null;
        int noteColor = MUTED;
        if (claim != null && !(claim.mine() && claim.completed())) {
            String who = claim.by().isEmpty() ? I18n.get("justquests.another_player") : claim.by();
            note = claim.mine() ? I18n.get("justquests.book.reserved") : I18n.get(claim.completed() ? "justquests.book.completed_by" : "justquests.book.taken_by", who);
            if (claim.mine()) noteColor = GOOD;
        } else if (st == Status.LOCKED && !missing.isEmpty()) {
            Quest req = ClientQuestData.get(missing.get(0));
            note = I18n.get("justquests.book.needs", req != null ? req.title().get(lang()) : missing.get(0).getPath())
                + (missing.size() > 1 ? " +" + (missing.size() - 1) : "");
        } else if (wait > 0) {
            note = I18n.get("justquests.book.again_in", duration(wait));
        } else if (st == Status.COMPLETED) {
            // rewards are paid out the moment a quest completes (a claim button comes with 0.4.0)
            note = I18n.get("justquests.book.rewards_received");
            noteColor = GOOD;
        }
        if (note != null) {
            g.drawString(this.font, fit(note, dw), dx, dy, noteColor, false);
            dy += 11;
        }

        String desc = q.description().get(lang());
        if (!desc.isBlank()) {
            for (var line : this.font.split(Component.literal(desc), dw)) {
                g.drawString(this.font, line, dx, dy, MUTED, false);
                dy += 9;
            }
        }
        dy += 3;
        g.drawString(this.font, Component.translatable("justquests.book.objectives"), dx, dy, HEAD, false);
        dy += 11;
        List<QuestObjective> objs = q.objectives();
        boolean finished = st == Status.COMPLETED;
        for (int i = 0; i < objs.size() && dy < actionY() - 12; i++) {
            int need = objs.get(i).requiredCount();
            int cur = finished ? need : prog != null ? Math.min(prog.get(i), need) : 0;
            boolean done = cur >= need;
            g.drawString(this.font, fit((done ? "✓ " : cur + "/" + need + " ") + QuestIcons.label(objs.get(i)).getString(), dw),
                dx, dy, done ? GOOD : TEXT, false);
            dy += 10;
            // progress bar
            int barW = Math.min(100, dw);
            blit(g, "progress_track", dx, dy, barW, 6);
            int fillW = need > 0 ? (int) (barW * (cur / (float) need)) : 0;
            if (fillW > 0) blitPart(g, "progress_fill", dx, dy, fillW, 6, 100, 6);
            dy += 9;
        }
        dy += 2;
        if (dy < actionY() - 10) {
            g.drawString(this.font, Component.translatable("justquests.book.rewards"), dx, dy, HEAD, false);
            dy += 11;
            for (QuestReward r : q.rewards()) {
                if (dy >= actionY() - 2) break;
                g.drawString(this.font, fit(r.display().getString(), dw), dx, dy, TEXT, false);
                dy += 10;
            }
        }

        // accept / abandon button
        if (st == Status.ACTIVE) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_abandon_hover" : "button_abandon_normal",
                actionX(), actionY(), 72, 20);
            centered(g, I18n.get("justquests.book.abandon"), actionX() + 36, actionY() + 6, TEXT);
            // pin to the HUD
            boolean pinned = ClientSettings.isPinned(selected);
            blit(g, pinned ? "button_pin_on" : in(mouseX, mouseY, pinX(), actionY(), 20, 20) ? "button_pin_hover" : "button_pin_normal",
                pinX(), actionY(), 20, 20);
            if (pinned) g.drawString(this.font, Component.translatable("justquests.book.pinned"), pinX() + 23, actionY() + 6, GOOD, false);
        } else if (st == Status.AVAILABLE) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_claim_hover" : "button_claim_normal",
                actionX(), actionY(), 72, 20);
            centered(g, I18n.get("justquests.book.accept"), actionX() + 36, actionY() + 6, TEXT);
        } else if (st == Status.LOCKED || wait > 0) {
            String label = I18n.get(takenByOther(selected) ? "justquests.book.taken" : wait > 0 ? "justquests.book.wait" : "justquests.book.locked");
            blit(g, "button_claim_disabled", actionX(), actionY(), 72, 20);
            centered(g, label, actionX() + 36, actionY() + 6, MUTED);
        }
    }

    /** The stats page: totals, per-category progress (hover an icon for its name), dates and server rank. */
    private void renderStats(GuiGraphics g, int mouseX, int mouseY) {
        int dx = detailX(), dy = top + 24, dw = detailW();
        PlayerQuestData d = data();
        Map<ResourceLocation, Quest> all = ClientQuestData.getQuests();
        int total = all.size();
        int done = (int) all.keySet().stream().filter(d::isCompleted).count();
        int pct = total > 0 ? done * 100 / total : 0;

        g.drawString(this.font, Component.translatable("justquests.book.stats.title"), dx, dy, TITLE_DARK, false);
        dy += 13;
        g.drawString(this.font, fit(I18n.get("justquests.book.stats.completed", done, total, pct), dw), dx, dy, TEXT, false);
        dy += 10;
        blit(g, "progress_track", dx, dy, 100, 6);
        if (total > 0 && done > 0) blitPart(g, "progress_fill", dx, dy, Math.max(1, 100 * done / total), 6, 100, 6);
        dy += 9;
        ClientQuestData.Rank rank = ClientQuestData.rank();
        String line = I18n.get("justquests.book.stats.active", d.active.size())
            + (rank != null ? "   " + I18n.get("justquests.book.stats.rank", rank.pos(), rank.of()) : "");
        g.drawString(this.font, fit(line, dw), dx, dy, TEXT, false);
        dy += 12;

        // per-category grid: icon + done/total, three to a row
        Map<String, int[]> cats = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        all.forEach((id, q) -> {
            int[] c = cats.computeIfAbsent(q.category(), k -> new int[2]);
            c[1]++;
            if (d.isCompleted(id)) c[0]++;
        });
        int headY = dy;
        dy += 11;
        String hovered = null;
        int col = 0, cellW = dw / 3, shown = 0, maxCells = 12;
        for (Map.Entry<String, int[]> c : cats.entrySet()) {
            if (shown == maxCells - 1 && cats.size() > maxCells) {
                g.drawString(this.font, "+" + (cats.size() - shown), dx + col * cellW + 4, dy + 4, MUTED, false);
                break;
            }
            int cx = dx + col * cellW;
            blit(g, QuestIcons.categoryIcon(c.getKey()), cx, dy, 16, 16);
            g.drawString(this.font, c.getValue()[0] + "/" + c.getValue()[1], cx + 18, dy + 4,
                c.getValue()[0] == c.getValue()[1] ? GOOD : TEXT, false);
            if (in(mouseX, mouseY, cx, dy, cellW, 16)) hovered = QuestIcons.categoryName(c.getKey());
            shown++;
            if (++col == 3) {
                col = 0;
                dy += 17;
            }
        }
        if (col != 0) dy += 17;
        g.drawString(this.font, fit(hovered != null ? I18n.get("justquests.book.stats.by_category_named", hovered) : I18n.get("justquests.book.stats.by_category"), dw), dx, headY, HEAD, false);

        // first and last completion
        List<Long> times = d.completed.values().stream().filter(t -> t > 0L).sorted().toList();
        if (!times.isEmpty()) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            dy += 2;
            g.drawString(this.font, I18n.get("justquests.book.stats.first", fmt.format(new java.util.Date(times.get(0)))), dx, dy, MUTED, false);
            dy += 9;
            g.drawString(this.font, I18n.get("justquests.book.stats.last", fmt.format(new java.util.Date(times.get(times.size() - 1)))), dx, dy, MUTED, false);
        }
    }

    /** A label centred on cx (button texts differ in length between languages). */
    private void centered(GuiGraphics g, String text, int cx, int y, int color) {
        g.drawString(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private static String duration(long ms) {
        long minutes = (ms + 59_999) / 60_000;
        long h = minutes / 60, m = minutes % 60;
        return h > 0 ? I18n.get("justquests.time.hours_minutes", h, m) : I18n.get("justquests.time.minutes", m);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (in(mx, my, closeX(), closeY(), 11, 11)) { onClose(); return true; }
            if (in(mx, my, statsX(), barY(), 14, 14)) { showStats = !showStats; return true; }
            if (in(mx, my, hudX(), barY(), 14, 14)) {
                ClientSettings.hud = !ClientSettings.hud;
                ClientSettings.save();
                return true;
            }
            if (in(mx, my, sortX(), toolY(), 16, 16)) {
                ClientSettings.byStatus = !ClientSettings.byStatus;
                ClientSettings.save();
                page = 0;
                rebuild();
                return true;
            }
            if (in(mx, my, filterX(), toolY(), 16, 16)) {
                ClientSettings.hideCompleted = !ClientSettings.hideCompleted;
                ClientSettings.save();
                rebuild();
                return true;
            }
            if (search.visible && !query.isEmpty() && in(mx, my, clearX(), top + 25, 10, 10)) {
                search.setValue("");
                return true;
            }
            if (hasPrev() && in(mx, my, prevX(), navY(), 12, 12)) { page--; return true; }
            if (hasNext() && in(mx, my, nextX(), navY(), 12, 12)) { page++; return true; }
            int start = page * ROWS;
            for (int i = 0; i < ROWS && start + i < entries.size(); i++) {
                Entry e = entries.get(start + i);
                if (e.isQuest() && in(mx, my, listX(), rowY(i), ROW_W, ROW_H)) {
                    selected = e.id();
                    showStats = false;
                    return true;
                }
            }
            if (!showStats && selected != null && data().isActive(selected) && in(mx, my, pinX(), actionY(), 20, 20)) {
                ClientSettings.togglePin(selected);
                return true;
            }
            if (!showStats && selected != null && in(mx, my, actionX(), actionY(), 72, 20)) {
                Quest q = ClientQuestData.get(selected);
                if (q != null) {
                    Status st = status(selected, q);
                    if (st == Status.ACTIVE) send("quest abandon " + selected);
                    else if (st == Status.AVAILABLE) send("quest accept " + selected);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** The mouse wheel over the list turns the pages. */
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (in(mx, my, listX(), rowY(0), ROW_W, ROWS * ROW_H)) {
            if (scrollY < 0 && hasNext()) page++;
            else if (scrollY > 0 && hasPrev()) page--;
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private void send(String cmd) {
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(cmd);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
