package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
            addItem(name, ItemBuilder.from(itemGenerator.blockInfoToItemStack(dropsEntry.getValue())).asGuiItem(), event -> {
                new BlockInfoEditorMenu(player, dropsEntry.getValue(), updated -> {
                    MiningManager.getInstance().setBlockInfo(dropsEntry.getKey().name(), updated);
                }, onClose -> {
                    search(query, true);
                }).open();
            });
        }

        if (player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION)) {
            gui.setItem(1, 9, ItemBuilder.from(Material.EMERALD_BLOCK)
                    .name(MiniMessageUtils.miniMessage("<green>New Block"))
                    .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to create a new block drop entry"))
                    .asGuiItem(event -> {
                        PromptManager.getInstance().prompt(player, "Enter Block Material").thenAccept(input -> {
                            try {
                                Material material = Material.valueOf(input.toUpperCase());
                                if (dropsMap.containsKey(material)) {
                                    player.sendMessage(MiniMessageUtils.miniMessage("<red>A block entry for '%s' already exists".formatted(material.name())));
                                    search(query, true);
                                    return;
                                }
                                BlockInfo newBlock = new BlockInfo(1, 1, new ArrayList<>());
                                new BlockInfoEditorMenu(player, newBlock, updated -> {
                                    MiningManager.getInstance().setBlockInfo(material.name(), updated);
                                }, onClose -> {
                                    search(query, true);
                                }).open();
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid material: %s".formatted(input)));
                                search(query, true);
                            }
                        }).exceptionally(throwable -> {
                            search(query, true);
                            return null;
                        });
                    }));
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            return;
        }
        new IndexBlocksCategoryMenu(player, query).open();
    }
}
