package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

public class IndexBlocksCategoryMenu extends IndexBaseCategoryMenu {

    public IndexBlocksCategoryMenu(Player player, String query) {
        super(player, IndexCategory.BLOCKS, query);
    }

    @Override
    public void setupGui() {
        Map<Material, BlockInfo> dropsMap = MiningManager.getInstance().getBlockInfoMap();
        for (Map.Entry<Material, BlockInfo> dropsEntry : dropsMap.entrySet()) {
            String name = String.valueOf(dropsEntry.getKey());
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            gui.addItem(ItemBuilder.from(itemGenerator.blockInfoToItemStack(dropsEntry.getValue())).asGuiItem());
        }
    }

    @Override
    public void search(String query) {
        if (this.query.equals(query)) {
            // Same query, do nothing
            return;
        }
        new IndexBlocksCategoryMenu(player, query).open();
    }
}
