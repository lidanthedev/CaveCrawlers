package me.lidan.cavecrawlers.utils;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.OfflinePlayer;

public class VaultUtils {
    public static void giveCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.depositPlayer(player, amount);
    }

    public static void takeCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.withdrawPlayer(player, amount);
    }

    public static void setCoins(OfflinePlayer player, double amount) {
        amount = Math.floor(amount * 10d)/ 10d;
        CaveCrawlers.economy.withdrawPlayer(player, CaveCrawlers.economy.getBalance(player));
        CaveCrawlers.economy.depositPlayer(player, amount);
    }

    public static double getCoins(OfflinePlayer player) {
        return CaveCrawlers.economy.getBalance(player);
    }
}
