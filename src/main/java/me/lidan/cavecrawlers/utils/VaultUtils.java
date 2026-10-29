package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * VaultUtils class to manage economy
 * Making it easier to use VaultAPI
 */
public class VaultUtils {
    /**
     * Give a player coins
     *
     * @param player the player to give coins to
     * @param amount the amount of coins to give
     */
    public static boolean giveCoins(OfflinePlayer player, double amount) {
        return giveCoins(player, amount, "unspecified", "external");
    }

    public static boolean giveCoins(OfflinePlayer player, double amount, String reason, String source) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        amount = Math.floor(amount * 10d)/ 10d;
        if (!Double.isFinite(amount)) return false;
        if (!prepare(player, amount, true, reason, source)) return false;
        var response = CaveCrawlers.economy.depositPlayer(player, amount);
        boolean success = response != null && response.transactionSuccess();
        if (success && amount > 0) Bukkit.getPluginManager().callEvent(
                new CurrencyTransactionEvent(player.getUniqueId(), amount, true, reason, source));
        return success;
    }

    /**
     * Take coins from a player
     * @param player the player to take coins from
     * @param amount the amount of coins to take
     */
    public static boolean takeCoins(OfflinePlayer player, double amount) {
        return takeCoins(player, amount, "unspecified", "external");
    }

    public static boolean takeCoins(OfflinePlayer player, double amount, String reason, String source) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        amount = Math.floor(amount * 10d)/ 10d;
        if (!Double.isFinite(amount)) return false;
        if (!prepare(player, amount, false, reason, source)) return false;
        var response = CaveCrawlers.economy.withdrawPlayer(player, amount);
        boolean success = response != null && response.transactionSuccess();
        if (success && amount > 0) Bukkit.getPluginManager().callEvent(
                new CurrencyTransactionEvent(player.getUniqueId(), amount, false, reason, source));
        return success;
    }

    private static boolean prepare(OfflinePlayer player, double amount, boolean created, String reason, String source) {
        return amount <= 0 || new CurrencyTransactionPrepareEvent(player.getUniqueId(), amount, created, reason, source).callEvent();
    }

    /**
     * Set the amount of coins a player has
     * @param player the player to set the coins for
     * @param amount the amount of coins to set
     */
    public static void setCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.withdrawPlayer(player, CaveCrawlers.economy.getBalance(player));
        CaveCrawlers.economy.depositPlayer(player, amount);
    }

    /**
     * Get the amount of coins a player has
     * @param player the player to get the coins for
     * @return the amount of coins the player has
     */
    public static double getCoins(OfflinePlayer player) {
        return CaveCrawlers.economy.getBalance(player);
    }
}
