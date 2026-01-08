package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class IndexMobsCategoryMenu extends IndexBaseCategoryMenu {

    public IndexMobsCategoryMenu(Player player, String query) {
        super(player, IndexCategory.MOBS, query);
    }

    @Override
    public void setupGui() {
        Map<String, EntityDrops> dropsMap = DropsManager.getInstance().getEntityDropsMap();
        for (Map.Entry<String, EntityDrops> dropsEntry : dropsMap.entrySet()) {
            String mobName = dropsEntry.getKey();
            if (!ChatColor.stripColor(mobName.toLowerCase()).contains(query)) continue;
            gui.addItem(ItemBuilder.from(itemGenerator.entityDropsToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query) {
        if (this.query.equals(query)) {
            // Same query, do nothing
            return;
        }
        new IndexMobsCategoryMenu(player, query).open();
    }
}
