package me.lidan.cavecrawlers.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LuckPermsUtils {
    /**
     * Get the player prefix
     *
     * @param player the player
     * @return the player prefix
     */
    public static String getPlayerPrefix(Player player) {
        LuckPerms luckPerms = null;
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException | NoClassDefFoundError e) {
            return "";
        }
        // Get the LuckPerms User object for the player
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return ""; // Return empty string if user is not found
        }
        CachedMetaData metaData = user.getCachedData().getMetaData();
        String prefix = metaData.getPrefix();
        if (prefix == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', prefix);
    }
}
