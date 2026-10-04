package com.erikedits.justquests.plugin;

import com.erikedits.justquests.plugin.book.QuestBook;
import com.erikedits.justquests.plugin.command.QuestCommand;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.data.PlayerStore;
import com.erikedits.justquests.plugin.gui.Menu;
import com.erikedits.justquests.plugin.gui.MenuListener;
import com.erikedits.justquests.plugin.progress.GeneratedQuests;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.progress.QuestListener;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.QuestRegistry;
import com.erikedits.justquests.plugin.text.Items;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.erikedits.justquests.plugin.track.Tracker;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JustQuests as a server plugin: the mod's quests, quest book and rewards for Spigot, Paper and
 * Purpur, played with a plain vanilla client. The quest book is a chest menu, the quest HUD a boss bar.
 */
public final class JustQuestsPlugin extends JavaPlugin implements Listener {
    private final Settings settings = new Settings();
    private QuestRegistry quests;
    private PlayerStore store;
    private ProgressService progress;
    private QuestListener listener;
    private Tracker tracker;
    private QuestBook book;
    private Community community;
    private GeneratedQuests generated = GeneratedQuests.NONE;
    private final Set<UUID> hideCompleted = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings.read(getConfig());
        Lang.load();
        Items.init(getLogger());
        Path data = getDataFolder().toPath();
        Path modWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("justquests");

        writeCustomQuests(data, modWorld);
        quests = new QuestRegistry(getLogger(), getFile(), data, () -> settings.mainQuests);
        quests.loadAll();
        store = new PlayerStore(data.resolve("players"), getLogger());
        store.load(modWorld == null ? null : modWorld.resolve("progress.json"));

        progress = new ProgressService(this);
        listener = new QuestListener(this);
        tracker = new Tracker(this);
        book = new QuestBook(this);
        community = new Community(this, data.resolve("seen-players.json"));
        registerPermissions();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(listener, this);
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(book, this);
        pm.registerEvents(this, this);
        PluginCommand cmd = getCommand("quest");
        if (cmd != null) {
            QuestCommand executor = new QuestCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) listener.tick(p);
        }, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (quests.checkCustom()) {
                getLogger().info("Custom quests changed: " + quests.all().size() + " quest(s) now");
                tracker.updateAll();
            }
        }, 100L, 100L);
        getServer().getScheduler().runTaskTimer(this, () -> store.saveDirty(), 1200L, 1200L);
        for (Player p : Bukkit.getOnlinePlayers()) tracker.update(p);   // after /reload
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        }
        if (tracker != null) tracker.clear();
        if (store != null) store.saveDirty();
    }

    /** custom-quests.json: the mod's file from a world played with the mod, else the template. */
    private void writeCustomQuests(Path data, Path modWorld) {
        try {
            Files.createDirectories(data.resolve("quests"));
            Path file = data.resolve("custom-quests.json");
            if (Files.exists(file)) return;
            Path fromMod = modWorld == null ? null : modWorld.resolve("custom-quests.json");
            if (fromMod != null && Files.exists(fromMod)) {
                Files.copy(fromMod, file);
                getLogger().info("Took over the custom quests of the mod's " + fromMod);
                return;
            }
            try (InputStream in = getResource("custom-quests.json")) {
                if (in != null) Files.copy(in, file);
            }
        } catch (Exception e) {
            getLogger().warning("Could not write custom-quests.json: " + e.getMessage());
        }
    }

    /** Registers the command and quest nodes, so permission plugins list them. */
    private void registerPermissions() {
        PluginManager pm = getServer().getPluginManager();
        for (String sub : QuestCommand.PLAYER) add(pm, "justquests.command." + sub, PermissionDefault.TRUE);
        for (String sub : QuestCommand.ADMIN) add(pm, "justquests.admin." + sub, PermissionDefault.OP);
        for (String node : quests.permissions()) add(pm, node, PermissionDefault.OP);
    }

    private static void add(PluginManager pm, String node, PermissionDefault def) {
        if (pm.getPermission(node) == null) pm.addPermission(new Permission(node, def));
    }

    /** /quest reload: config.yml and the custom quests again. */
    public int reload() {
        reloadConfig();
        settings.read(getConfig());
        quests.reloadCustom();
        registerPermissions();
        tracker.updateAll();
        return quests.all().size();
    }

    public void setMainQuests(boolean on) {
        settings.mainQuests = on;
        getConfig().set("mainQuests", on);
        saveConfig();
        tracker.updateAll();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        community.onJoin(e.getPlayer());
        tracker.update(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        tracker.remove(e.getPlayer());
        hideCompleted.remove(id);
        store.save(id);
    }

    // --- shared helpers -------------------------------------------------------------------------

    /**
     * The permission check: a permission plugin decides when it knows the node; otherwise players
     * may use player commands and operators the rest. The console may do everything.
     */
    public boolean allowed(CommandSender sender, String node, boolean opOnly) {
        if (!(sender instanceof Player p)) return true;
        if (p.isPermissionSet(node) || getServer().getPluginManager().getPermission(node) != null) return p.hasPermission(node);
        return !opOnly || p.isOp();
    }

    /** Whether the player may see and take the quest; quests without a permission are open to all. */
    public boolean canSee(Player player, Quest quest) {
        return quest.permission() == null || allowed(player, quest.permission(), true);
    }

    public Collection<Quest> visibleQuests(Player player) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : quests.all().values()) {
            if (canSee(player, q)) out.add(q);
        }
        return out;
    }

    /** A category's name: translated for the bundled ones, the id itself for pack-defined ones. */
    public String categoryName(String lang, String category) {
        String key = "justquests.category." + category.toLowerCase(Locale.ROOT);
        return Lang.has(key) ? Text.legacy(lang, key) : category;
    }

    /** {rank, players} by completed quests, or null without any. */
    public int[] rank(UUID player) {
        PlayerData mine = store.peek(player);
        int count = 0, better = 0;
        for (Map.Entry<UUID, PlayerData> e : store.all().entrySet()) {
            if (e.getValue().completed.isEmpty()) continue;
            count++;
            if (mine != null && e.getValue().completed.size() > mine.completed.size()) better++;
        }
        if (mine == null || mine.completed.isEmpty()) return null;
        return new int[]{better + 1, count};
    }

    public boolean hidesCompleted(UUID player) {
        return hideCompleted.contains(player);
    }

    public void setHidesCompleted(UUID player, boolean hide) {
        if (hide) hideCompleted.add(player);
        else hideCompleted.remove(player);
    }

    public List<String> selfTest() { return SelfTest.run(this); }

    public Settings settings() { return settings; }

    public QuestRegistry quests() { return quests; }

    public PlayerStore store() { return store; }

    public ProgressService progress() { return progress; }

    public Tracker tracker() { return tracker; }

    public QuestBook book() { return book; }

    public GeneratedQuests generated() { return generated; }

    public void setGenerated(GeneratedQuests g) { generated = g == null ? GeneratedQuests.NONE : g; }
}
