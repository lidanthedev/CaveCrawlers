package me.lidan.cavecrawlers.shop;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class ShopMenu {
    private final Player player;
    private final Gui gui;

    public ShopMenu(Player player,String title, List<ShopItem> shopItemList) {
        this.player = player;
        this.gui = Gui.gui().title(Component.text(title)).rows(6).disableAllInteractions().create();
        gui.getFiller().fillBorder(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
        for (ShopItem shopItem : shopItemList) {
            GuiItem guiItem = ItemBuilder.from(shopItem.toItem()).asGuiItem(event -> {
                boolean buy = shopItem.buy(player);
                if (!buy) {
                    player.sendMessage(ChatColor.RED + "You don't have the items!");
                }
            });
            gui.addItem(guiItem);
        }
    }

    public void open(){
        gui.open(player);
    }
}
