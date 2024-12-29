package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuiItems {
    public static final @NotNull GuiItem CLOSE_ITEM = ItemBuilder.from(Material.BARRIER).setName(ChatColor.RED + "Close Menu").setLore("", "§eClick to Close Menu").asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        sender.closeInventory();
    }));

    public static final @NotNull GuiItem BACK_ITEM = ItemBuilder.from(Material.ARROW).setName(ChatColor.GRAY + "Go Back").setLore("", "§eClick to To Back").asGuiItem((event -> {
        Player sender = (Player) event.getWhoClicked();
        MenuGui menuGui = new MenuGui(sender);
        menuGui.open();
    }));
    public static final @NotNull GuiItem GLASS_ITEM = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem();
}
