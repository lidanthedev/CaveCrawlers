package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class IndexMainMenu {
    private Player player;
    private Gui gui;

    public IndexMainMenu(Player player) {
        this.player = player;
        this.gui = Gui.gui().rows(6).title(MiniMessageUtils.miniMessage("Index Main Menu")).create();
        gui.disableAllInteractions();
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
        gui.setItem(3, 2, ItemBuilder.from(Material.ZOMBIE_HEAD).name(MiniMessageUtils.miniMessage("<green>Mobs")).asGuiItem(event -> new IndexMobsCategoryMenu(player, "").open()));
        gui.setItem(3, 4, ItemBuilder.from(Material.DIAMOND_ORE).name(MiniMessageUtils.miniMessage("<gold>Blocks")).asGuiItem(event -> new IndexBlocksCategoryMenu(player, "").open()));
        gui.setItem(3, 6, ItemBuilder.from(Material.DRAGON_HEAD).name(MiniMessageUtils.miniMessage("<red>Bosses")).asGuiItem(event -> new IndexBossesCategoryMenu(player, "").open()));
        gui.setItem(3, 8, ItemBuilder.from(Material.END_PORTAL_FRAME).name(MiniMessageUtils.miniMessage("<aqua>Altars")).asGuiItem(event -> new IndexAltarsCategoryMenu(player, "").open()));
        gui.setItem(6, 5, GuiItems.CLOSE_ITEM);
    }

    public void open() {
        gui.open(player);
    }
}
