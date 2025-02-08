package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
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
    public static void giveCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.depositPlayer(player, amount);
    }

    /**
     * Take coins from a player
     * @param player the player to take coins from
     * @param amount the amount of coins to take
     */
    public static void takeCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.withdrawPlayer(player, amount);
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
