package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class IndexBossesCategoryMenu extends IndexBaseCategoryMenu {

    public IndexBossesCategoryMenu(Player player, String query) {
        super(player, IndexCategory.BOSSES, query);
    }

    @Override
    public void setupGui() {
        Map<String, BossDrops> dropsMap = BossManager.getInstance().getDropsMap();
        for (Map.Entry<String, BossDrops> dropsEntry : dropsMap.entrySet()) {
            String name = String.valueOf(dropsEntry.getKey());
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            gui.addItem(ItemBuilder.from(itemGenerator.bossDropsToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query) {
        if (this.query.equals(query)) {
            // Same query, do nothing
            return;
        }
        new IndexBossesCategoryMenu(player, query).open();
    }
}
