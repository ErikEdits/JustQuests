package com.erikedits.justquests.plugin.quest;

/**
 * Money rewards ({@code justquests:money}) pay through whatever economy plugin the server has, with
 * the command set in config.yml ({@code generator.rewards.money.command}); without one they pay nothing.
 */
public final class Economy {
    private static String command = "";
    private static String name = "Coins";

    private Economy() {}

    public static void configure(String moneyCommand, String currencyName) {
        command = moneyCommand == null ? "" : moneyCommand.trim();
        name = currencyName == null || currencyName.isBlank() ? "Coins" : currencyName;
    }

    public static boolean enabled() {
        return !command.isEmpty();
    }

    /** The console command for the payment, or null without an economy command. */
    static String command(String player, int amount) {
        if (command.isEmpty()) return null;
        String cmd = command.replace("{player}", player).replace("{amount}", String.valueOf(amount));
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }

    public static String name() {
        return name;
    }
}
