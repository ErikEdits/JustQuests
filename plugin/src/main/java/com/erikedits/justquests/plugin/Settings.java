package com.erikedits.justquests.plugin;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;

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
