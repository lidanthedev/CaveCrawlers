package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.prompt.PromptManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class IndexMobsCategoryMenu extends IndexBaseCategoryMenu {
    public IndexMobsCategoryMenu(Player player) {
        this(player, "");
    }

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
        gui.setItem(1, 5, SEARCH_ITEM.asGuiItem(event -> {
            PromptManager.getInstance().prompt(player, "Search").thenAccept(result -> {
                new IndexMobsCategoryMenu(player, result).open();
            }).exceptionally(throwable -> {
                new IndexMobsCategoryMenu(player, "").open();
                return null;
            });
        }));
    }
}
