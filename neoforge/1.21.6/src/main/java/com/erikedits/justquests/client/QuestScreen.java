package com.erikedits.justquests.client;

import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.ChoiceReward;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.network.ClientQuestData;
import com.erikedits.justquests.player.QuestProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
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
 * {@link ClientQuestData} and runs /quest accept|abandon for actions. Pictures, places, colours,
 * font and sounds come from the resource packs ({@link Theme}, 0.5.0); by default a 280x184 window.
 */
public class QuestScreen extends Screen {
    /** The search field shows up from this many quests on. */
    private static final int SEARCH_MIN = 15;
    /** Radio boxes and the scroll bar (not text). */
    private static final int WHITE = 0xFFFFFFFF, LIGHT = 0xFFC6C6C6;
    /** The resource packs' look, taken again on every frame. */
    private Theme theme = Theme.get();
    // sizes and text colours from the theme (in capitals: they were constants before 0.5.0); text needs
    // full alpha, since 1.21.6+ skips text with alpha 0
    private int W, H, ROWS, ROW_W, ROW_H;
    private int TITLE_DARK, TEXT, MUTED, HEAD, LINK, GOOD, BAD, GROUP, GROUP_COUNT;
    private boolean SHADOW;
    /** The book-open sound plays once, not on every resize. */
    private boolean opened;

    /** Where a quest stands for this player; also the order of the status groups. */
    private enum Status {
        CLAIM("claim", "glyph_gift"), ACTIVE("active", "glyph_exclamation"), AVAILABLE("available", "glyph_star"),
        LOCKED("locked", "glyph_lock"), COMPLETED("completed", "glyph_check");

        final String label, icon;

        Status(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    /** One line of the list: a group header (with the key it folds by), a quest, or an empty spacer. */
    private record Entry(String header, String icon, String count, ResourceLocation id, Quest quest, String fold) {
        static final Entry SPACER = new Entry(null, null, null, null, null, null);

        boolean isHeader() { return header != null; }
        boolean isQuest() { return id != null; }
    }

    /** A required quest's line on a locked quest's page: a click opens that quest. */
    private record Link(int x, int y, int w, int h, ResourceLocation id) {}

    private final List<Entry> entries = new ArrayList<>();
    private ResourceLocation selected;
    private boolean showStats;
    private int page = 0, left, top;
    private int shownSync = -1;
    private EditBox search;
    private String query = "";
    /** The picked option of the selected quest's choice reward (-1 = none yet). */
    private int pick = -1;
    /** How far the quest page is scrolled down, and how far it can go (found while drawing it). */
    private int detailScroll, detailMax;
    /** The HUD is previewed in its corner until then (after a click on the corner button). */
    private long cornerPreviewUntil;
    /** Where the choice options were drawn this frame: x, y, w, h, option index. */
    private final List<int[]> optionHits = new ArrayList<>();
    /** Where the required quests were drawn this frame, as links. */
    private final List<Link> links = new ArrayList<>();
    /** The quest page's scroll bar this frame: x, top, height, thumb height (null while it fits). */
    private int[] scrollBar;
    /** The auto-accept box on the overview page this frame: x, y, w, h (null while not shown). */
    private int[] autoHit;
    /** The popups box on the overview page this frame: x, y, w, h (null while not shown). */
    private int[] popupHit;
    /** The player is picking the popups' place on the screen (0.5.1, #24): the book steps aside. */
    private boolean placing;

    public QuestScreen() {
        super(Component.translatable("justquests.book.title"));
    }

    /** Sizes and colours from the resource packs' theme. */
    private void applyTheme() {
        theme = Theme.get();
        W = theme.bookW;
        H = theme.bookH;
        ROWS = theme.rows;
        ROW_W = theme.rowW;
        ROW_H = theme.rowH;
        TITLE_DARK = theme.title;
        TEXT = theme.text;
        MUTED = theme.muted;
        HEAD = theme.heading;
        LINK = theme.link;
        GOOD = theme.done;
        BAD = theme.warning;
        GROUP = theme.group;
        GROUP_COUNT = theme.groupCount;
        SHADOW = theme.bookShadow;
    }

    /** Draw a whole picture (size natW x natH) at (x,y): the resource pack's, else the mod's. */
    private void blit(GuiGraphics g, String name, int x, int y, int natW, int natH) {
        theme.blit(g, name, x, y, natW, natH, natW, natH);
    }

    /** Draw the top-left w x h region of a picture shown at texW x texH. */
    private void blitPart(GuiGraphics g, String name, int x, int y, int w, int h, int texW, int texH) {
        theme.blit(g, name, x, y, w, h, texW, texH);
    }

    /** Text in the book's font, with the theme's shadow, in a colour. */
    private void text(GuiGraphics g, String s, int x, int y, int color) {
        text(g, Theme.text(s), x, y, color, false);
    }

    private void text(GuiGraphics g, Component c, int x, int y, int color) {
        text(g, c, x, y, color, false);
    }

    /** With a shadow if the theme or the caller (the group headers) wants one. */
    private void text(GuiGraphics g, String s, int x, int y, int color, boolean shadow) {
        text(g, Theme.text(s), x, y, color, shadow);
    }

    private void text(GuiGraphics g, Component c, int x, int y, int color, boolean shadow) {
        g.drawString(this.font, Theme.text(c), x, y, color, shadow || SHADOW);
    }

    /** A line from font.split of a {@link Theme#text} component (it already has the font). */
    private void text(GuiGraphics g, net.minecraft.util.FormattedCharSequence s, int x, int y, int color) {
        g.drawString(this.font, s, x, y, color, SHADOW);
    }

    /** Width of a text in the book's font. */
    private int width(String s) {
        return this.font.width(Theme.text(s));
    }

    /** A handled click: the theme's click sound. */
    private boolean click() {
        QuestEvents.sound("book.click");
        return true;
    }

    /** A page turned: the theme's page sound. */
    private boolean turn() {
        QuestEvents.sound("book.page");
        return true;
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
        applyTheme();
        if (!opened) {
            opened = true;
            QuestEvents.sound("book.open");
        }
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        // drawn by us on the pixel field texture; the box only handles typing
        search = new EditBox(this.font, searchX() + 3, searchY() + 3, 56, 9, Component.translatable("justquests.book.search"));
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
            String fold = (ClientSettings.byStatus ? "status:" : "category:") + group.getKey();
            if (ClientSettings.byStatus) {
                Status st = Status.valueOf(group.getKey());
                header = new Entry(I18n.get("justquests.book.status." + st.label), st.icon, String.valueOf(group.getValue().size()), null, null, fold);
            } else {
                int[] c = perCategory.get(group.getKey());
                header = new Entry(QuestIcons.categoryName(group.getKey()), QuestIcons.categoryIcon(group.getKey()),
                    c[0] + "/" + c[1], null, null, fold);
            }
            // a search shows every match, folded or not
            boolean folded = q.isEmpty() && ClientSettings.folded.contains(fold);
            // never leave a header alone on the last line of a page (a folded one has nothing below it)
            if (!folded && entries.size() % ROWS == ROWS - 1) entries.add(Entry.SPACER);
            entries.add(header);
            if (folded) continue;
            for (Map.Entry<ResourceLocation, Quest> e : group.getValue()) {
                entries.add(new Entry(null, null, null, e.getKey(), e.getValue(), null));
            }
        }
        if (page > pages() - 1) page = Math.max(0, pages() - 1);
    }

    /** Search: title, category and goals, so "diam" finds every quest with diamonds. */
    private boolean matches(Quest quest, String q) {
        if (q.isEmpty()) return true;
        if (quest.title().get(lang()).toLowerCase(Locale.ROOT).contains(q)) return true;
        if (QuestIcons.categoryName(quest.category()).toLowerCase(Locale.ROOT).contains(q)) return true;
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (QuestIcons.label(quest, i).getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    /** The group header on the current page under the mouse, or null. */
    private Entry headerAt(double mx, double my) {
        int start = page * ROWS;
        for (int i = 0; i < ROWS && start + i < entries.size(); i++) {
            Entry e = entries.get(start + i);
            if (e.isHeader() && in(mx, my, listX(), rowY(i), ROW_W, ROW_H)) return e;
        }
        return null;
    }

    /** Quests the "Accept all" button takes: the available ones without team and generated quests (as /quest accept all). */
    private int acceptable() {
        int n = 0;
        for (Map.Entry<ResourceLocation, Quest> e : ClientQuestData.getQuests().entrySet()) {
            Quest q = e.getValue();
            if (!q.team() && !"generated".equals(q.category()) && status(e.getKey(), q) == Status.AVAILABLE) n++;
        }
        return n;
    }

    private int pages() {
        return Math.max(1, (entries.size() + ROWS - 1) / ROWS);
    }

    // --- quest state ---
    private Status status(ResourceLocation id, Quest q) {
        PlayerQuestData d = data();
        if (d.isClaimable(id)) return Status.CLAIM;
        if (d.isActive(id)) return Status.ACTIVE;
        if (takenByOther(id)) return Status.LOCKED;
        if (d.isCompleted(id) && (!q.repeatable() || cooldownLeft(id, q) > 0)) return Status.COMPLETED;
        if (!q.unlocked(data()::isCompleted)) return Status.LOCKED;
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

    /**
     * Selects a quest from its link on another quest's page: a search that hides it is cleared, its
     * folded group opened and the list turned to its page.
     */
    private void open(ResourceLocation id) {
        Quest q = ClientQuestData.get(id);
        if (q == null) return;
        if (searchShown() && !matches(q, query.trim().toLowerCase(Locale.ROOT))) search.setValue("");
        String fold = ClientSettings.byStatus ? "status:" + status(id, q).name() : "category:" + q.category();
        if (ClientSettings.folded.contains(fold)) ClientSettings.toggleFold(fold);
        rebuild();
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id())) page = i / ROWS;
        }
        selected = id;
        pick = -1;
        detailScroll = 0;
        detailMax = 0;
        showStats = false;
    }

    /** A generated quest another player holds (or finished) under exclusive claims. */
    private static boolean takenByOther(ResourceLocation id) {
        ClientQuestData.Claim c = ClientQuestData.claim(id);
        return c != null && !c.mine();
    }

    // --- geometry helpers ---
    // places on the book, from quest_book/settings.json
    private int bx(String button) { return left + theme.button(button)[0]; }
    private int by(String button) { return top + theme.button(button)[1]; }
    private int listX() { return left + theme.listX; }
    private int rowY(int i) { return top + theme.listY + i * ROW_H; }
    private int titleX() { return bx("title"); }
    private int titleY() { return by("title"); }
    private int hintX() { return bx("hint"); }
    private int hintY() { return by("hint"); }
    private int sortX() { return bx("sort"); }
    private int toolY() { return by("sort"); }
    private int filterX() { return bx("filter"); }
    private int filterY() { return by("filter"); }
    private int searchX() { return bx("search"); }
    private int searchY() { return by("search"); }
    private int prevX() { return bx("page_prev"); }
    private int navY() { return by("page_prev"); }
    private int nextX() { return bx("page_next"); }
    private int nextY() { return by("page_next"); }
    private int closeX() { return bx("close"); }
    private int closeY() { return by("close"); }
    private int statsX() { return bx("stats"); }
    private int statsY() { return by("stats"); }
    private int cornerX() { return bx("corner"); }
    private int cornerY() { return by("corner"); }
    private int hudX() { return bx("hud"); }
    private int hudY() { return by("hud"); }
    private int popX() { return bx("popup"); }
    private int popY() { return by("popup"); }
    private int detailX() { return left + theme.pageX; }
    private int detailY() { return top + theme.pageY; }
    private int detailW() { return theme.pageW; }
    private int actionX() { return bx("action"); }
    private int actionY() { return by("action"); }
    /** The quest page ends above the buttons; below that it scrolls. */
    private int pageBottom() { return top + theme.pageBottom; }
    private int pinX() { return bx("pin"); }
    private int pinY() { return by("pin"); }
    private int clearX() { return searchX() + 74 - 12; }
    private boolean hasPrev() { return page > 0; }
    private boolean hasNext() { return page < pages() - 1; }

    /** Truncate to width with an ellipsis so titles never run past their row. */
    private String fit(String s, int maxW) {
        if (width(s) <= maxW) return s;
        return this.font.getSplitter().plainHeadByWidth(s, maxW - width("..."), Theme.style()) + "...";
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        applyTheme();
        refresh();
        if (placing) {
            renderPlacing(g, mouseX, mouseY);
            return;
        }
        // Minecraft draws the blurred background before this; a second blur in one frame would crash
        // the HUD hides while a screen is open: show it in its corner while its position is being picked
        if (in(mouseX, mouseY, cornerX(), cornerY(), 14, 14) || System.currentTimeMillis() < cornerPreviewUntil) QuestHud.preview(g);
        blit(g, "window", left, top, W, H);
        text(g, Component.translatable("justquests.book.title"), titleX(), titleY(), TITLE_DARK);

        // title-bar buttons: stats page, HUD corner, HUD on/off, close
        blit(g, showStats ? "button_stats_on" : in(mouseX, mouseY, statsX(), statsY(), 14, 14) ? "button_stats_hover" : "button_stats_normal",
            statsX(), statsY(), 14, 14);
        blit(g, "button_corner_" + corner() + (in(mouseX, mouseY, cornerX(), cornerY(), 14, 14) ? "_hover" : "_normal"),
            cornerX(), cornerY(), 14, 14);
        blit(g, ClientSettings.hud ? "button_hud_on" : in(mouseX, mouseY, hudX(), hudY(), 14, 14) ? "button_hud_hover" : "button_hud_normal",
            hudX(), hudY(), 14, 14);
        blit(g, in(mouseX, mouseY, popX(), popY(), 14, 14) ? "button_popup_hover" : "button_popup_normal",
            popX(), popY(), 14, 14);
        blit(g, in(mouseX, mouseY, closeX(), closeY(), 11, 11) ? "button_close_hover" : "button_close_normal",
            closeX(), closeY(), 11, 11);

        // list tools: grouping, filter and (with many quests) the search field
        boolean overSort = in(mouseX, mouseY, sortX(), toolY(), 16, 16);
        boolean overFilter = in(mouseX, mouseY, filterX(), filterY(), 16, 16);
        blit(g, overSort ? "button_sort_hover" : "button_sort_normal", sortX(), toolY(), 16, 16);
        blit(g, ClientSettings.hideCompleted ? "button_filter_on" : overFilter ? "button_filter_hover" : "button_filter_normal",
            filterX(), filterY(), 16, 16);
        search.visible = searchShown();
        if (search.visible) {
            blit(g, search.isFocused() ? "search_focused" : "search_normal", searchX(), searchY(), 74, 14);
            if (query.isEmpty() && !search.isFocused()) text(g, I18n.get("justquests.book.search"), searchX() + 3, searchY() + 3, 0xFF8B8B8B);
            search.render(g, mouseX, mouseY, pt);
            if (!query.isEmpty()) blit(g, "search_clear", clearX(), searchY() + 2, 10, 10);
        } else {
            text(g, I18n.get(ClientSettings.byStatus ? "justquests.book.by_status" : "justquests.book.by_category"), searchX() + 2, searchY() + 3, TEXT);
        }

        // what the hovered button does, next to the title
        boolean pinShown = !showStats && selected != null && data().isActive(selected);
        String hint = overSort ? (ClientSettings.byStatus ? "group_category" : "group_status")
            : overFilter ? (ClientSettings.hideCompleted ? "show_completed" : "hide_completed")
            : in(mouseX, mouseY, statsX(), statsY(), 14, 14) ? (showStats ? "back" : "stats")
            : in(mouseX, mouseY, cornerX(), cornerY(), 14, 14) ? "hud_corner"
            : in(mouseX, mouseY, hudX(), hudY(), 14, 14) ? (ClientSettings.hud ? "hide_hud" : "show_hud")
            : in(mouseX, mouseY, popX(), popY(), 14, 14) ? "popups"
            : pinShown && in(mouseX, mouseY, pinX(), pinY(), 20, 20) ? (ClientSettings.isPinned(selected) ? "unpin" : "pin")
            : headerAt(mouseX, mouseY) != null && query.isBlank()
                ? (ClientSettings.folded.contains(headerAt(mouseX, mouseY).fold()) ? "unfold" : "fold")
            : null;
        if (hint != null) {
            String text = hint.equals("hud_corner")
                ? I18n.get("justquests.book.hint.hud_corner", I18n.get("justquests.book.corner." + corner()))
                : I18n.get("justquests.book.hint." + hint);
            text(g, fit("- " + text, Math.min(statsX(), popX()) - hintX() - 4), hintX(), hintY(), MUTED);
        }

        if (entries.isEmpty()) {
            text(g, Component.translatable(ClientQuestData.getQuests().isEmpty() ? "justquests.book.empty"
                    : search.visible && !query.isBlank() ? "justquests.book.no_match" : "justquests.book.nothing"), listX() + 2, rowY(0) + 2, GROUP_COUNT);
        } else {
            int start = page * ROWS;
            for (int i = 0; i < ROWS && start + i < entries.size(); i++) {
                Entry e = entries.get(start + i);
                if (e.isHeader()) renderHeader(g, e, rowY(i), in(mouseX, mouseY, listX(), rowY(i), ROW_W, ROW_H));
                else if (e.isQuest()) renderRow(g, e, rowY(i), in(mouseX, mouseY, listX(), rowY(i), ROW_W, ROW_H));
            }
        }
        // page arrows and number
        blit(g, hasPrev() ? (in(mouseX, mouseY, prevX(), navY(), 12, 12) ? "page_prev_hover" : "page_prev_normal") : "page_prev_disabled",
            prevX(), navY(), 12, 12);
        blit(g, hasNext() ? (in(mouseX, mouseY, nextX(), nextY(), 12, 12) ? "page_next_hover" : "page_next_normal") : "page_next_disabled",
            nextX(), nextY(), 12, 12);
        String pg = (page + 1) + "/" + pages();
        text(g, pg, (prevX() + nextX() + 12) / 2 - width(pg) / 2, navY() + 2, TEXT);

        if (showStats) renderStats(g, mouseX, mouseY);
        else renderDetail(g, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g, Entry e, int y, boolean hover) {
        int x = listX();
        boolean folded = query.isBlank() && ClientSettings.folded.contains(e.fold());
        if (theme.has("quest_book/rows/header")) blit(g, "quest_book/rows/header", x, y, ROW_W, ROW_H);
        blit(g, e.icon(), x + 1, y, 16, 16);
        text(g, fit(e.header(), ROW_W - 58), x + 20, y + 4, GROUP, true);
        text(g, e.count(), x + ROW_W - 13 - width(e.count()), y + 4, GROUP_COUNT, true);
        if (query.isBlank()) arrow(g, x + ROW_W - 9, y + 5, folded, hover ? GROUP : GROUP_COUNT);
        separator(g, x, y + 16, true);
    }

    /** The line under a group header and between quest rows: separator.png, else a grey line under headers. */
    private void separator(GuiGraphics g, int x, int y, boolean header) {
        if (theme.has("quest_book/separator")) blit(g, "quest_book/separator", x, y, ROW_W, 1);
        else if (header) g.fill(x, y, x + ROW_W, y + 1, 0xFF555555);
    }

    /** The fold arrow of a group header: pointing down while the group is open, right while it is folded. */
    private static void arrow(GuiGraphics g, int x, int y, boolean folded, int color) {
        for (int i = 0; i < 4; i++) {
            if (folded) g.fill(x + i + 1, y + i, x + i + 2, y + 7 - i, color);
            else g.fill(x + i, y + i + 1, x + 7 - i, y + i + 2, color);
        }
    }

    private void renderRow(GuiGraphics g, Entry e, int y, boolean hover) {
        int x = listX();
        ResourceLocation id = e.id();
        Quest q = e.quest();
        Status st = status(id, q);
        boolean sel = id.equals(selected);
        boolean cooldown = st == Status.COMPLETED && q.repeatable();
        String state = sel ? "selected"
            : st == Status.CLAIM ? "claimable"
            : st == Status.ACTIVE ? "active"
            : st == Status.LOCKED ? "locked"
            : st == Status.COMPLETED && !cooldown ? "completed"
            : hover ? "hover" : "available";
        blit(g, "quest_row_" + state, x, y, ROW_W, ROW_H);
        // completed and locked rows have their glyph in the texture; the others get one drawn on top
        String glyph = st == Status.CLAIM ? "glyph_gift"
            : cooldown ? "glyph_clock"
            : st == Status.ACTIVE && ClientSettings.isPinned(id) ? "glyph_pin"
            : st == Status.AVAILABLE && q.repeatable() ? "glyph_repeat"
            : sel && st == Status.COMPLETED ? "glyph_check"
            : sel && st == Status.LOCKED ? "glyph_lock" : null;
        if (glyph != null) blit(g, glyph, x + ROW_W - 18, y + 1, 16, 16);
        boolean hasGlyph = glyph != null || state.equals("completed") || state.equals("locked");
        theme.icon(g, id, q, x + 3, y + 1);
        String title = fit(q.title().get(lang()), ROW_W - 23 - (hasGlyph ? 18 : 4));
        text(g, title, x + 22, y + 5, st == Status.LOCKED ? MUTED : TITLE_DARK);
        separator(g, x, y + ROW_H - 1, false);
    }

    private void renderDetail(GuiGraphics g, int mouseX, int mouseY) {
        optionHits.clear();
        links.clear();
        scrollBar = null;
        autoHit = null;
        popupHit = null;
        int dx = detailX(), dy = detailY(), dw = detailW();
        if (selected == null) {
            text(g, Component.translatable("justquests.book.select_1"), dx, dy, MUTED);
            text(g, Component.translatable("justquests.book.select_2"), dx, dy + 11, MUTED);
            dy += 30;
            // accepting unlocked quests by themselves: the player's setting on the server
            int boxY = dy;
            dy = lines(g, I18n.get("justquests.book.auto_accept"), dx + 11, dy, dw - 11, TEXT);
            autoHit = new int[] {dx, boxY - 1, dw, dy - boxY};
            radio(g, dx + 1, boxY, ClientQuestData.autoAccept(), in(mouseX, mouseY, dx, boxY - 1, dw, dy - boxY));
            // popups for completed and new quests: the player's own choice, kept in this game
            int popY = dy + 3;
            dy = lines(g, I18n.get("justquests.book.popups"), dx + 11, popY, dw - 11, TEXT);
            popupHit = new int[] {dx, popY - 1, dw, dy - popY};
            radio(g, dx + 1, popY, ClientSettings.popups, in(mouseX, mouseY, dx, popY - 1, dw, dy - popY));
            int n = acceptable();
            if (n > 0) {
                lines(g, I18n.get("justquests.book.acceptable", n), dx, dy + 6, dw, MUTED);
                blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_claim_hover" : "button_claim_normal",
                    actionX(), actionY(), 72, 20);
                centered(g, I18n.get("justquests.book.accept_all"), actionX() + 36, actionY() + 6, TEXT);
            }
            return;
        }
        Quest q = ClientQuestData.get(selected);
        if (q == null) return;
        PlayerQuestData d = data();
        QuestProgress prog = d.active.get(selected);
        Status st = status(selected, q);

        // everything above the buttons scrolls with the mouse wheel once it is taller than the page
        int viewTop = dy, viewBottom = pageBottom();
        if (detailMax > 0) dw -= 6;   // room for the scroll bar
        detailScroll = Math.min(detailScroll, detailMax);
        g.enableScissor(dx, viewTop, dx + detailW(), viewBottom);
        dy -= detailScroll;

        // icon + title (wraps next to the icon)
        theme.icon(g, selected, q, dx, dy);
        var titleLines = this.font.split(Theme.text(q.title().get(lang())), dw - 20);
        int ty = titleLines.size() == 1 ? dy + 4 : dy;
        for (var line : titleLines) {
            text(g, line, dx + 20, ty, TITLE_DARK);
            ty += 10;
        }
        dy = Math.max(dy + 19, ty + 2);

        // why it cannot be taken right now
        ClientQuestData.Claim claim = ClientQuestData.claim(selected);
        List<ResourceLocation> missing = missing(q);
        long wait = st == Status.COMPLETED && q.repeatable() ? cooldownLeft(selected, q) : 0;
        String note = null;
        int noteColor = MUTED;
        boolean needs = false;
        if (claim != null && !(claim.mine() && claim.completed())) {
            String who = claim.by().isEmpty() ? I18n.get("justquests.another_player") : claim.by();
            note = claim.mine() ? I18n.get("justquests.book.reserved") : I18n.get(claim.completed() ? "justquests.book.completed_by" : "justquests.book.taken_by", who);
            if (claim.mine()) noteColor = GOOD;
        } else if (st == Status.CLAIM) {
            note = I18n.get("justquests.book.claim_note");
            noteColor = GOOD;
        } else if (st == Status.LOCKED && !missing.isEmpty()) {
            // requires_min: any N of the listed quests; the missing ones follow as links
            note = q.needed() < q.requires().size()
                ? I18n.get("justquests.book.needs_of", q.needed(), q.requires().size(), q.requires().size() - missing.size())
                : I18n.get("justquests.book.needs_list");
            needs = true;
        } else if (wait > 0) {
            note = I18n.get("justquests.book.again_in", duration(wait));
        } else if (st == Status.COMPLETED && !q.rewards().isEmpty()) {
            // rewards are paid out the moment a quest completes (a claim button comes with 0.4.0)
            note = I18n.get("justquests.book.rewards_received");
            noteColor = GOOD;
        }
        if (note != null) dy = lines(g, note, dx, dy, dw, noteColor) + 1;
        if (needs) dy = needsLinks(g, missing, dx, dy, dw, viewTop, viewBottom, mouseX, mouseY) + 1;
        if (q.resetOnDeath()) {
            dy = lines(g, I18n.get(q.team() ? "justquests.book.reset_on_death_team" : "justquests.book.reset_on_death"), dx, dy, dw, BAD) + 1;
        }

        String desc = q.description().get(lang());
        if (!desc.isBlank()) {
            for (var line : this.font.split(Theme.text(desc), dw)) {
                text(g, line, dx, dy, MUTED);
                dy += 9;
            }
        }
        dy += 3;
        text(g, Component.translatable("justquests.book.objectives"), dx, dy, HEAD);
        dy += 11;
        List<QuestObjective> objs = q.objectives();
        boolean finished = st == Status.COMPLETED || st == Status.CLAIM;
        java.util.function.IntPredicate goalDone = j -> finished || prog != null && prog.get(j) >= objs.get(j).requiredCount();
        for (int i : q.shownOrder()) {
            // the objectives of a group are alternatives: one of them is enough
            if (q.orBefore(i)) {
                text(g, I18n.get("justquests.book.or"), dx + 8, dy, MUTED);
                dy += 10;
            }
            int need = objs.get(i).requiredCount();
            int cur = finished ? need : prog != null ? Math.min(prog.get(i), need) : 0;
            boolean done = cur >= need;
            boolean otherWay = !done && q.alternativeDone(i, goalDone);
            // a long goal wraps onto the next line instead of being cut off
            dy = lines(g, (done ? "✓ " : cur + "/" + need + " ") + QuestIcons.label(q, i).getString(), dx, dy, dw,
                done ? GOOD : otherWay ? MUTED : TEXT);
            // progress bar
            theme.progress(g, false, dx, dy, Math.min(theme.barW, dw), theme.barH, need > 0 ? cur / (float) need : 1f);
            dy += theme.barH + 3;
        }
        ChoiceReward choice = ChoiceReward.of(q);
        if (!q.rewards().isEmpty()) {
            dy += 2;
            text(g, Component.translatable("justquests.book.rewards"), dx, dy, HEAD);
            dy += 11;
            for (QuestReward r : q.rewards()) {
                if (r != choice) {
                    dy = lines(g, r.display().getString(), dx, dy, dw, TEXT);
                    continue;
                }
                // a choice: one line per option; while the rewards wait, a click picks one
                text(g, Component.translatable("justquests.book.choose_one"), dx, dy, MUTED);
                dy += 10;
                for (int i = 0; i < choice.options().size(); i++) {
                    boolean open = st == Status.CLAIM, picked = open && i == pick;
                    var optionLines = this.font.split(Theme.text(choice.options().get(i).display().getString()), dw - 11);
                    int h = optionLines.size() * 10;
                    // only an option that is wholly on the page can be picked
                    boolean shown = dy - 1 >= viewTop && dy - 1 + h <= viewBottom;
                    radio(g, dx + 1, dy, picked, open && shown && in(mouseX, mouseY, dx, dy - 1, dw, h));
                    for (int l = 0; l < optionLines.size(); l++) {
                        text(g, optionLines.get(l), dx + 11, dy + l * 10, picked ? GOOD : TEXT);
                    }
                    if (open && shown) optionHits.add(new int[] {dx, dy - 1, dw, h, i});
                    dy += h;
                }
            }
        }
        g.disableScissor();

        // how far the page can scroll, and a scroll bar while it can
        int contentH = dy + detailScroll - viewTop, viewH = viewBottom - viewTop;
        detailMax = Math.max(0, contentH - viewH);
        if (detailMax > 0) {
            // a dark track and a raised light thumb; a click on the track moves the page there
            int barX = dx + detailW() - 4, thumbH = Math.max(10, viewH * viewH / contentH);
            int thumbY = viewTop + (viewH - thumbH) * Math.min(detailScroll, detailMax) / detailMax;
            boolean overBar = in(mouseX, mouseY, barX, viewTop, 4, viewH);
            g.fill(barX, viewTop, barX + 4, viewBottom, 0xFF373737);
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF8B8B8B);
            g.fill(barX, thumbY, barX + 3, thumbY + thumbH - 1, overBar ? WHITE : LIGHT);
            scrollBar = new int[] {barX, viewTop, viewH, thumbH};
        }

        // claim / accept / abandon button
        if (st == Status.ACTIVE) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_abandon_hover" : "button_abandon_normal",
                actionX(), actionY(), 72, 20);
            centered(g, I18n.get("justquests.book.abandon"), actionX() + 36, actionY() + 6, TEXT);
            // pin to the HUD
            boolean pinned = ClientSettings.isPinned(selected);
            blit(g, pinned ? "button_pin_on" : in(mouseX, mouseY, pinX(), pinY(), 20, 20) ? "button_pin_hover" : "button_pin_normal",
                pinX(), pinY(), 20, 20);
            if (pinned) text(g, Component.translatable("justquests.book.pinned"), pinX() + 23, pinY() + 6, GOOD);
        } else if (st == Status.CLAIM && choice != null && pick < 0) {
            blit(g, "button_claim_disabled", actionX(), actionY(), 72, 20);
            centered(g, I18n.get("justquests.book.pick"), actionX() + 36, actionY() + 6, MUTED);
        } else if (st == Status.CLAIM) {
            blit(g, in(mouseX, mouseY, actionX(), actionY(), 72, 20) ? "button_claim_hover" : "button_claim_normal",
                actionX(), actionY(), 72, 20);
            centered(g, I18n.get("justquests.book.claim"), actionX() + 36, actionY() + 6, TEXT);
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

    /** Text wrapped to the width, 10 pixels a line; returns the y below it. */
    private int lines(GuiGraphics g, String text, int x, int y, int w, int color) {
        for (var line : this.font.split(Theme.text(text), w)) {
            text(g, line, x, y, color);
            y += 10;
        }
        return y;
    }

    /**
     * The quests a locked quest still needs, one per line, underlined under the mouse: a click opens
     * that quest. One the book does not have (hidden by a permission) stays plain text. Returns the y below.
     */
    private int needsLinks(GuiGraphics g, List<ResourceLocation> missing, int x, int y, int w,
                           int viewTop, int viewBottom, int mouseX, int mouseY) {
        for (ResourceLocation id : missing) {
            Quest req = ClientQuestData.get(id);
            String name = "› " + (req != null ? req.title().get(lang()) : id.toString());
            int h = this.font.split(Theme.text(name), w - 4).size() * 10;
            // only a line that is wholly on the page can be clicked
            boolean link = req != null && y - 1 >= viewTop && y - 1 + h <= viewBottom;
            boolean hover = link && in(mouseX, mouseY, x, y - 1, w, h);
            var text = Theme.text(name);
            if (hover) text.withStyle(net.minecraft.ChatFormatting.UNDERLINE);
            for (var line : this.font.split(text, w - 4)) {
                text(g, line, x + 4, y, req != null ? LINK : MUTED);
                y += 10;
            }
            if (link) links.add(new Link(x, y - h - 1, w, h, id));
        }
        return y;
    }

    /** The HUD corner from the client settings (an unknown value counts as top left). */
    private static String corner() {
        String c = ClientSettings.hudCorner;
        return c.equals("top_right") || c.equals("bottom_left") || c.equals("bottom_right") ? c : "top_left";
    }

    /** Clockwise: top left, top right, bottom right, bottom left. */
    private static String nextCorner(String c) {
        return switch (c) {
            case "top_left" -> "top_right";
            case "top_right" -> "bottom_right";
            case "bottom_right" -> "bottom_left";
            default -> "top_left";
        };
    }

    /** A small pixel radio box in front of a choice option. */
    private void radio(GuiGraphics g, int x, int y, boolean on, boolean hover) {
        g.fill(x, y, x + 7, y + 7, 0xFF373737);
        g.fill(x + 1, y + 1, x + 6, y + 6, hover ? WHITE : 0xFFE0E0E0);
        if (on) g.fill(x + 2, y + 2, x + 5, y + 5, GOOD);
    }

    /** The stats page: totals, per-category progress (hover an icon for its name), dates and server rank. */
    private void renderStats(GuiGraphics g, int mouseX, int mouseY) {
        int dx = detailX(), dy = detailY(), dw = detailW();
        PlayerQuestData d = data();
        Map<ResourceLocation, Quest> all = ClientQuestData.getQuests();
        int total = all.size();
        int done = (int) all.keySet().stream().filter(d::isCompleted).count();
        int pct = total > 0 ? done * 100 / total : 0;

        text(g, Component.translatable("justquests.book.stats.title"), dx, dy, TITLE_DARK);
        dy += 13;
        text(g, fit(I18n.get("justquests.book.stats.completed", done, total, pct), dw), dx, dy, TEXT);
        dy += 10;
        theme.progress(g, false, dx, dy, Math.min(theme.barW, dw), theme.barH, total > 0 ? done / (float) total : 0f);
        dy += theme.barH + 3;
        ClientQuestData.Rank rank = ClientQuestData.rank();
        String line = I18n.get("justquests.book.stats.active", d.active.size())
            + (rank != null ? "   " + I18n.get("justquests.book.stats.rank", rank.pos(), rank.of()) : "");
        text(g, fit(line, dw), dx, dy, TEXT);
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
                text(g, "+" + (cats.size() - shown), dx + col * cellW + 4, dy + 4, MUTED);
                break;
            }
            int cx = dx + col * cellW;
            blit(g, QuestIcons.categoryIcon(c.getKey()), cx, dy, 16, 16);
            text(g, c.getValue()[0] + "/" + c.getValue()[1], cx + 18, dy + 4, c.getValue()[0] == c.getValue()[1] ? GOOD : TEXT);
            if (in(mouseX, mouseY, cx, dy, cellW, 16)) hovered = QuestIcons.categoryName(c.getKey());
            shown++;
            if (++col == 3) {
                col = 0;
                dy += 17;
            }
        }
        if (col != 0) dy += 17;
        text(g, fit(hovered != null ? I18n.get("justquests.book.stats.by_category_named", hovered) : I18n.get("justquests.book.stats.by_category"), dw), dx, headY, HEAD);

        // first and last completion
        List<Long> times = d.completed.values().stream().filter(t -> t > 0L).sorted().toList();
        if (!times.isEmpty()) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            dy += 2;
            text(g, I18n.get("justquests.book.stats.first", fmt.format(new java.util.Date(times.get(0)))), dx, dy, MUTED);
            dy += 9;
            text(g, I18n.get("justquests.book.stats.last", fmt.format(new java.util.Date(times.get(times.size() - 1)))), dx, dy, MUTED);
        }
    }

    /** A label centred on cx (button texts differ in length between languages). */
    private void centered(GuiGraphics g, String text, int cx, int y, int color) {
        text(g, text, cx - width(text) / 2, y, color);
    }

    private static String duration(long ms) {
        long minutes = (ms + 59_999) / 60_000;
        long h = minutes / 60, m = minutes % 60;
        return h > 0 ? I18n.get("justquests.time.hours_minutes", h, m) : I18n.get("justquests.time.minutes", m);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (placing) {
            // left: the popups go here; right: back to the automatic place
            if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) Popups.keep((int) mx, (int) my, this.width, this.height);
            else if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) Popups.reset();
            else return true;
            placing = false;
            return click();
        }
        if (button == 0) {
            if (in(mx, my, closeX(), closeY(), 11, 11)) { onClose(); return true; }
            if (in(mx, my, statsX(), statsY(), 14, 14)) { showStats = !showStats; return click(); }
            if (in(mx, my, cornerX(), cornerY(), 14, 14)) {
                ClientSettings.hudCorner = nextCorner(corner());
                ClientSettings.hud = true;   // moving the tracker means it should show
                ClientSettings.save();
                cornerPreviewUntil = System.currentTimeMillis() + 2000;
                return click();
            }
            if (in(mx, my, hudX(), hudY(), 14, 14)) {
                ClientSettings.hud = !ClientSettings.hud;
                ClientSettings.save();
                return click();
            }
            if (in(mx, my, popX(), popY(), 14, 14)) {
                placing = true;
                return click();
            }
            if (in(mx, my, sortX(), toolY(), 16, 16)) {
                ClientSettings.byStatus = !ClientSettings.byStatus;
                ClientSettings.save();
                page = 0;
                rebuild();
                return click();
            }
            if (in(mx, my, filterX(), filterY(), 16, 16)) {
                ClientSettings.hideCompleted = !ClientSettings.hideCompleted;
                ClientSettings.save();
                rebuild();
                return click();
            }
            if (search.visible && !query.isEmpty() && in(mx, my, clearX(), searchY() + 2, 10, 10)) {
                search.setValue("");
                return click();
            }
            if (hasPrev() && in(mx, my, prevX(), navY(), 12, 12)) { page--; return turn(); }
            if (hasNext() && in(mx, my, nextX(), nextY(), 12, 12)) { page++; return turn(); }
            Entry header = headerAt(mx, my);
            if (header != null && query.isBlank()) {
                ClientSettings.toggleFold(header.fold());
                rebuild();
                return click();
            }
            int start = page * ROWS;
            for (int i = 0; i < ROWS && start + i < entries.size(); i++) {
                Entry e = entries.get(start + i);
                if (e.isQuest() && in(mx, my, listX(), rowY(i), ROW_W, ROW_H)) {
                    pick = -1;
                    detailScroll = 0;
                    detailMax = 0;
                    // a second click on the selected quest goes back to the overview page
                    selected = e.id().equals(selected) && !showStats ? null : e.id();
                    showStats = false;
                    return click();
                }
            }
            if (!showStats && selected == null) {
                if (popupHit != null && in(mx, my, popupHit[0], popupHit[1], popupHit[2], popupHit[3])) {
                    ClientSettings.popups = !ClientSettings.popups;
                    ClientSettings.save();
                    return click();
                }
                if (autoHit != null && in(mx, my, autoHit[0], autoHit[1], autoHit[2], autoHit[3])) {
                    send("quest autoaccept " + (ClientQuestData.autoAccept() ? "off" : "on"));
                    return click();
                }
                if (acceptable() > 0 && in(mx, my, actionX(), actionY(), 72, 20)) {
                    send("quest accept all");
                    return click();
                }
            }
            if (!showStats && scrollBar != null && in(mx, my, scrollBar[0], scrollBar[1], 4, scrollBar[2])) {
                // the middle of the thumb goes where the track was clicked
                int room = Math.max(1, scrollBar[2] - scrollBar[3]);
                int at = (int) my - scrollBar[1] - scrollBar[3] / 2;
                detailScroll = Math.max(0, Math.min(detailMax, at * detailMax / room));
                return true;
            }
            if (!showStats && selected != null) {
                for (Link l : links) {
                    if (in(mx, my, l.x(), l.y(), l.w(), l.h())) { open(l.id()); return click(); }
                }
                for (int[] h : optionHits) {
                    if (in(mx, my, h[0], h[1], h[2], h[3])) { pick = h[4]; return click(); }
                }
            }
            if (!showStats && selected != null && data().isActive(selected) && in(mx, my, pinX(), pinY(), 20, 20)) {
                ClientSettings.togglePin(selected);
                return click();
            }
            if (!showStats && selected != null && in(mx, my, actionX(), actionY(), 72, 20)) {
                Quest q = ClientQuestData.get(selected);
                if (q != null) {
                    Status st = status(selected, q);
                    if (st == Status.ACTIVE) send("quest abandon " + selected);
                    else if (st == Status.AVAILABLE) send("quest accept " + selected);
                    else if (st == Status.CLAIM && ChoiceReward.of(q) == null) send("quest claim " + selected);
                    else if (st == Status.CLAIM && pick >= 0) send("quest claim " + selected + " " + (pick + 1));
                }
                return click();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** The mouse wheel over the list turns the pages; over a long quest page it scrolls the page. */
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (in(mx, my, listX(), rowY(0), ROW_W, ROWS * ROW_H)) {
            if (scrollY < 0 && hasNext()) { page++; turn(); }
            else if (scrollY > 0 && hasPrev()) { page--; turn(); }
            return true;
        }
        if (!showStats && detailMax > 0 && scrollY != 0 && in(mx, my, detailX(), detailY(), detailW(), pageBottom() - detailY())) {
            detailScroll = Math.max(0, Math.min(detailMax, detailScroll + (scrollY < 0 ? 10 : -10)));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private void send(String cmd) {
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(cmd);
        }
    }

    /** The game's own background behind the book (unless the theme turns it off), then the theme's layer. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        if (placing) return;   // the game shows through while the popups are placed
        if (Theme.get().blur) super.renderBackground(g, mouseX, mouseY, pt);
        backgroundLayer(g);
    }

    /** The theme's layer behind the book: a colour, or background.png stretched or tiled. */
    private void backgroundLayer(GuiGraphics g) {
        Theme t = Theme.get();
        switch (t.bgMode) {
            case "color" -> g.fill(0, 0, this.width, this.height, t.bgColor);
            case "stretch" -> t.stretch(g, "quest_book/background", 0, 0, this.width, this.height, this.width, this.height);
            case "tile" -> t.tile(g, "quest_book/background", 0, 0, this.width, this.height);
            default -> { }
        }
    }

    /**
     * Placing the popups: the game shows through, a sample popup follows the mouse and the tracker is
     * shown, so the popups can be kept clear of it.
     */
    private void renderPlacing(GuiGraphics g, int mouseX, int mouseY) {
        QuestHud.preview(g);
        Popups.preview(g, mouseX, mouseY, this.width, this.height);
        String a = I18n.get("justquests.popup.place_1"), b = I18n.get("justquests.popup.place_2");
        int w = Math.max(width(a), width(b)) + 12, x = (this.width - w) / 2, y = this.height / 2 + 24;
        g.fill(x, y, x + w, y + 26, 0xB0000000);
        g.drawString(this.font, Theme.text(a), this.width / 2 - width(a) / 2, y + 4, 0xFFFFFFFF, true);
        g.drawString(this.font, Theme.text(b), this.width / 2 - width(b) / 2, y + 15, 0xFFC6C6C6, true);
    }

    /** Esc while the popups are placed goes back to the book instead of closing it. */
    @Override
    public void onClose() {
        if (placing) {
            placing = false;
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        QuestEvents.sound("book.close");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
