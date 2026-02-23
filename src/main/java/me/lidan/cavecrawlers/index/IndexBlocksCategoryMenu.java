package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexBlocksCategoryMenu extends IndexBaseCategoryMenu {

    public IndexBlocksCategoryMenu(Player player, String query) {
        super(player, IndexCategory.BLOCKS, query);
    }

    @Override
    public void setupGui() {
        Map<Material, BlockInfo> dropsMap = MiningManager.getInstance().getBlockInfoMap();
        List<Map.Entry<Material, BlockInfo>> sortedDrops = dropsMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getBlockPower()))
                .toList();
        for (Map.Entry<Material, BlockInfo> dropsEntry : sortedDrops) {
            String name = String.valueOf(dropsEntry.getKey());
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            addItem(name, ItemBuilder.from(itemGenerator.blockInfoToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            // Same query, do nothing
            return;
        }
        new IndexBlocksCategoryMenu(player, query).open();
    }
}
