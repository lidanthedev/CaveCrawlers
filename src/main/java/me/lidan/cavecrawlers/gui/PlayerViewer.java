package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class PlayerViewer {
    private final Player player;
    private final Gui gui;

    public PlayerViewer(Player player) {
        this.player = player;
        this.gui = Gui.gui()
                .title(Component.text(ChatColor.GRAY + "Player Viewer"))
                .rows(6)
                .create();
        // Helmet
        ItemStack helmet = player.getInventory().getHelmet().clone();
        if (helmet != null) {
            GuiItem guiHelmet = new GuiItem(helmet);
            gui.setItem(10, guiHelmet);
        }
        // Chestplate
        ItemStack chestplate = player.getInventory().getChestplate().clone();
        if (chestplate != null) {
            GuiItem guiChestplate = new GuiItem(chestplate);
            gui.setItem(19, guiChestplate);
        }
        // Leggings
        ItemStack leggings = player.getInventory().getLeggings().clone();
        if (leggings != null) {
            GuiItem guiLeggings = new GuiItem(leggings);
            gui.setItem(28, guiLeggings);
        }
        // Boots
        ItemStack boots = player.getInventory().getBoots().clone();
        if (boots != null) {
            GuiItem guiBoots = new GuiItem(boots);
            gui.setItem(37, guiBoots);
        }
        // Player Head
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§f%s Stats:".formatted(player.getName())).setLore(statsMessage).asGuiItem());

        // mainHand
        ItemStack mainHand = player.getInventory().getItemInMainHand().clone();
        if (mainHand != null) {
            GuiItem guiMainHand = new GuiItem(mainHand);
            gui.setItem(11, guiMainHand);
        }
        // Bank
        gui.setItem(15, ItemBuilder.from(Material.GOLD_BLOCK).setName("Money: " + VaultUtils.getCoins(player)).asGuiItem());
        // glass
        gui.getFiller().fill(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        gui.disableAllInteractions();
    }

    public void open() {
        gui.open(player);
    }
}


