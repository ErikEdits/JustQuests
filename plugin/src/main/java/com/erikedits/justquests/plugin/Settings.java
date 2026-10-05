package com.erikedits.justquests.plugin;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The options of config.yml (the mod's settings.json names), read on start and on /quest reload. */
public final class Settings {
    public boolean discordWelcome = true;
    public boolean announceCompletions = true;
    public boolean completionSound = true;
    public boolean completionToast = true;
    public boolean claimRewards = true;
    public boolean mainQuests = true;
    public boolean giveBookOnFirstJoin = true;
    public boolean bossbar = true;
    public BarColor bossbarColor = BarColor.YELLOW;
    public BarStyle bossbarStyle = BarStyle.SEGMENTED_10;

    // --- generator
    public boolean generator = true;
    public String difficulty = "normal";
    public int boardBase = 5;
    public int boardPerPlayers = 3;
    public int boardMax = 20;
    public int cycleHours = 12;
    public int cycleAnchorHour = 0;
    public boolean exclusiveClaims = true;
    public boolean oneActivePerPlayer = true;
    public boolean releaseOnAbandon = true;
    public int claimExpiryHours = 0;
    public boolean personal = true;
    public int personalQuests = 3;
    public int personalResetHour = 0;
    public boolean weekly = true;
    public int weeklyQuests = 4;
    public DayOfWeek weeklyDay = DayOfWeek.MONDAY;
    public int weeklyHour = 0;
    public boolean serverGoal = true;
    public int goalMinutesPerPlayer = 60;
    public boolean rewardChoice = true;
    public double rewardMultiplier = 1.0;
    public double luckyChance = 0.08;
    public List<String> lucky = new ArrayList<>();
    public double streakBonus = 0.1;
    public double streakMax = 0.5;
    public String moneyCommand = "";
    public double moneyPerValue = 1.0;
    public String moneyName = "Coins";
    public boolean generatorStats = true;
    public boolean adaptiveBalancing = false;

    public void read(FileConfiguration c) {
        discordWelcome = c.getBoolean("discordWelcome", true);
        announceCompletions = c.getBoolean("announceCompletions", true);
        completionSound = c.getBoolean("completionSound", true);
        completionToast = c.getBoolean("completionToast", true);
        claimRewards = c.getBoolean("claimRewards", true);
        mainQuests = c.getBoolean("mainQuests", true);
        giveBookOnFirstJoin = c.getBoolean("questBook.giveOnFirstJoin", true);
        bossbar = c.getBoolean("bossbar.enabled", true);
        bossbarColor = parse(BarColor.class, c.getString("bossbar.color"), BarColor.YELLOW);
        bossbarStyle = parse(BarStyle.class, c.getString("bossbar.style"), BarStyle.SEGMENTED_10);

        generator = c.getBoolean("generator.enabled", true);
        difficulty = c.getString("generator.difficulty", "normal").toLowerCase(Locale.ROOT);
        boardBase = clamp(c.getInt("generator.board.baseQuests", 5), 1, 50);
        boardPerPlayers = clamp(c.getInt("generator.board.perPlayers", 3), 0, 1000);
        boardMax = clamp(c.getInt("generator.board.maxQuests", 20), 1, 50);
        cycleHours = clamp(c.getInt("generator.board.cycleHours", 12), 1, 168);
        cycleAnchorHour = clamp(c.getInt("generator.board.cycleAnchorHour", 0), 0, 23);
        exclusiveClaims = c.getBoolean("generator.board.exclusiveClaims", true);
        oneActivePerPlayer = c.getBoolean("generator.board.oneActivePerPlayer", true);
        releaseOnAbandon = c.getBoolean("generator.board.releaseOnAbandon", true);
        claimExpiryHours = clamp(c.getInt("generator.board.claimExpiryHours", 0), 0, 24 * 365);
        // generator.enabled switches the whole generator: board, personal and weekly quests, server goal
        personal = generator && c.getBoolean("generator.personal.enabled", true);
        personalQuests = clamp(c.getInt("generator.personal.quests", 3), 1, 10);
        personalResetHour = clamp(c.getInt("generator.personal.resetHour", 0), 0, 23);
        weekly = generator && c.getBoolean("generator.weekly.enabled", true);
        weeklyQuests = clamp(c.getInt("generator.weekly.quests", 4), 1, 10);
        weeklyDay = parse(DayOfWeek.class, c.getString("generator.weekly.day"), DayOfWeek.MONDAY);
        weeklyHour = clamp(c.getInt("generator.weekly.hour", 0), 0, 23);
        serverGoal = generator && c.getBoolean("generator.serverGoal.enabled", true);
        goalMinutesPerPlayer = clamp(c.getInt("generator.serverGoal.minutesPerPlayer", 60), 5, 1440);
        rewardChoice = c.getBoolean("generator.rewards.choice", true);
        rewardMultiplier = Math.max(0.1, Math.min(10.0, c.getDouble("generator.rewards.multiplier", 1.0)));
        luckyChance = Math.max(0.0, Math.min(1.0, c.getDouble("generator.rewards.luckyChance", 0.08)));
        lucky = new ArrayList<>(c.getStringList("generator.rewards.lucky"));
        streakBonus = Math.max(0.0, Math.min(1.0, c.getDouble("generator.rewards.streakBonus", 0.1)));
        streakMax = Math.max(0.0, Math.min(5.0, c.getDouble("generator.rewards.streakMax", 0.5)));
        moneyCommand = c.getString("generator.rewards.money.command", "").trim();
        moneyPerValue = Math.max(0.0, c.getDouble("generator.rewards.money.perValue", 1.0));
        moneyName = c.getString("generator.rewards.money.name", "Coins");
        generatorStats = c.getBoolean("generator.stats", true);
        adaptiveBalancing = c.getBoolean("generator.adaptiveBalancing", false);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
