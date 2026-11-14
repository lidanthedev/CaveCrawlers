package me.lidan.cavecrawlers.integration.playervaults;

import com.drtshock.playervaults.PlayerVaults;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlayerVaultsHook {
    private PlayerVaultsHook() {
    }

    public static int getMaxVaults(Plugin vaults, Player player) {
        if (!player.hasPermission("playervaults.commands.use")) {
            return 0;
        }
        int limit = ((PlayerVaults) vaults).getMaxVaultAmountPermTest();
        for (int number = limit; number > 0; number--) {
            if (player.hasPermission("playervaults.amount." + number)) {
                return number;
            }
        }
        return 0;
    }
}
