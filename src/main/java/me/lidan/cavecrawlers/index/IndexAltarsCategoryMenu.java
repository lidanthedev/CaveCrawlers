package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class IndexAltarsCategoryMenu extends IndexBaseCategoryMenu {

    public IndexAltarsCategoryMenu(Player player, String query) {
        super(player, IndexCategory.ALTARS, query);
    }

    @Override
    public void setupGui() {
        List<String> names = AltarManager.getInstance().getAltarNames();
        for (String name : names) {
            Altar altar = AltarManager.getInstance().getAltar(name);
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            gui.addItem(ItemBuilder.from(itemGenerator.altarToItemStack(altar)).asGuiItem());
        }
    }

    @Override
    public void search(String query) {
        if (this.query.equals(query)) {
            // Same query, do nothing
            return;
        }
        new IndexAltarsCategoryMenu(player, query).open();
    }
}
