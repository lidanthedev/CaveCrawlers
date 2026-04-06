package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossManager;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexBossesCategoryMenu extends IndexBaseCategoryMenu {

    public IndexBossesCategoryMenu(Player player, String query) {
        super(player, IndexCategory.BOSSES, query);
    }

    @Override
    public void setupGui() {
        Map<String, BossDrops> dropsMap = BossManager.getInstance().getDropsMap();
        List<Map.Entry<String, BossDrops>> entries = dropsMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> {
                    MythicMob mob = MythicMobsHook.getInstance().getMobByName(entry.getValue().getEntityName());
                    if (mob == null) return 0;
                    return mob.getHealth().get();
                }))
                .toList();
        for (Map.Entry<String, BossDrops> dropsEntry : entries) {
            String name = String.valueOf(dropsEntry.getKey());
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            addItem(name, ItemBuilder.from(itemGenerator.bossDropsToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            // Same query, do nothing
            return;
        }
        new IndexBossesCategoryMenu(player, query).open();
    }
}
