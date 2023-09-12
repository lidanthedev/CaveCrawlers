package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.items.ItemsManager;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemsGui  {
    private final Player player;
    private final PaginatedGui gui;

    public ItemsGui(Player player) {
        this.player = player;
        this.gui = Gui.paginated()
                .title(Component.text(ChatColor.BLUE + "Items Browser!"))
                .rows(6)
                .pageSize(45) // Set the size you want, or leave it to be automatic.
                .create();
        gui.disableAllInteractions();
        // Previous item
        gui.setItem(6, 3, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> gui.previous()));
        // Next item
        gui.setItem(6, 7, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> gui.next()));
        ItemsManager itemsManager = ItemsManager.getInstance();
        for (String ID : itemsManager.getKeys()) {
            ItemStack itemStack = itemsManager.buildItem(ID, 1);
            GuiItem guiItem = ItemBuilder.from(itemStack.clone()).addLore(ChatColor.DARK_GRAY + "ID: " + ID).asGuiItem(event -> {
                HumanEntity clicked = event.getWhoClicked();
                clicked.getInventory().addItem(itemStack);
            });
            gui.addItem(guiItem);
        }
        gui.getFiller().fillBottom(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
    }

    public void open(){
        gui.open(player);
    }
}
