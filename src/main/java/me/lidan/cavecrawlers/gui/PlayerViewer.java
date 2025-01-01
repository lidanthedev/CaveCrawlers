package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.LuckpermsUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class PlayerViewer {
    private final Player player;
    private final Gui gui;
    private final LevelConfigManager levelconfigManager = LevelConfigManager.getInstance();

    public PlayerViewer(Player player) {
        this.player = player;
        this.gui = Gui.gui()
                .title(Component.text(ChatColor.GRAY + player.getName()+"'s " + "Profile"))
                .rows(6)
                .create();
        // Helmet
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null) {
            gui.setItem(10, ItemBuilder.from(helmet.clone()).asGuiItem());
        }
        // Chestplate
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null) {
            gui.setItem(19, ItemBuilder.from(chestplate.clone()).asGuiItem());
        }
        // Leggings
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null) {
            gui.setItem(28, ItemBuilder.from(leggings.clone()).asGuiItem());
        }
        // Boots
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null) {
            gui.setItem(37, ItemBuilder.from(boots.clone()).asGuiItem());
        }
        // Player Head
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§f%s Stats:".formatted(player.getName())).setLore(statsMessage).asGuiItem());

        // mainHand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            gui.setItem(1, ItemBuilder.from(mainHand.clone()).asGuiItem());
        }
        // Bank
        gui.setItem(15, ItemBuilder.from(Material.GOLD_INGOT).setName(ChatColor.GRAY + "Money: " + ChatColor.GOLD + StringUtils.getNumberFormat(VaultUtils.getCoins(player))).asGuiItem());
        // Level
        String playerId = player.getUniqueId().toString();
        int level = levelconfigManager.getPlayerLevel(playerId);
        String colorName = levelconfigManager.getLevelColor(level);
        ChatColor levelColor = ChatColor.valueOf(colorName);
        gui.setItem(16, ItemBuilder.from(Material.EMERALD).setName(ChatColor.GRAY + "Level: " + ChatColor.DARK_GRAY + "[" + levelColor + level + ChatColor.DARK_GRAY + "]").asGuiItem());
        // Rank
        gui.setItem(24, ItemBuilder.from(Material.GOLD_BLOCK).setName(ChatColor.GRAY + "Rank: " + LuckpermsUtils.getPlayerPrefix(player)).asGuiItem());
        // glass
        gui.getFiller().fill(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        gui.disableAllInteractions();
    }

    public void open(Player viewer) {
        gui.open(viewer);
    }
}


