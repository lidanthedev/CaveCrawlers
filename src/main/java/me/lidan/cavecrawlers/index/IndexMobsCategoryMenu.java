package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexMobsCategoryMenu extends IndexBaseCategoryMenu {

    public IndexMobsCategoryMenu(Player player, String query) {
        super(player, IndexCategory.MOBS, query);
    }

    @Override
    public void setupGui() {
        Map<String, EntityDrops> dropsMap = DropsManager.getInstance().getEntityDropsMap();
        List<Map.Entry<String, EntityDrops>> entries = dropsMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> {
                    MythicMob mob = MythicMobsHook.getInstance().getMobByName(entry.getValue().getEntityName());
                    if (mob == null) return 0;
                    return mob.getHealth().get();
                }))
                .toList();
        for (Map.Entry<String, EntityDrops> dropsEntry : entries) {
            String mobName = dropsEntry.getKey();
            if (!ChatColor.stripColor(mobName.toLowerCase()).contains(query)) continue;
            addItem(mobName, ItemBuilder.from(itemGenerator.entityDropsToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            // Same query, do nothing
            return;
        }
        new IndexMobsCategoryMenu(player, query).open();
    }
}
