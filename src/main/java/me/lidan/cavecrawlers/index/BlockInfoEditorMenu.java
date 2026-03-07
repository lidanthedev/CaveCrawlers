package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.selectors.EnumSelector;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BlockInfoEditorMenu extends BaseEditorMenu<BlockInfo> {
    public BlockInfoEditorMenu(Player player, BlockInfo item, Consumer<BlockInfo> onSave, Consumer<BlockInfo> onClose) {
        super(player, item.clone(), onSave, onClose);
    }

    @Override
    public void setupGui() {
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        // Block Strength
        gui.setItem(2, 3, ItemBuilder.from(Material.IRON_PICKAXE)
                .name(MiniMessageUtils.miniMessage("<yellow>Block Strength: <strength>", Map.of("strength", String.valueOf(item.getBlockStrength()))))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().promptNumberMin(player, "Enter Block Strength", 0).thenAccept(input -> {
                        item.setBlockStrength(input);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set block strength to %s".formatted(input)));
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        open();
                        return null;
                    });
                }));

        // Block Power
        gui.setItem(2, 5, ItemBuilder.from(Material.DIAMOND_PICKAXE)
                .name(MiniMessageUtils.miniMessage("<yellow>Block Power: <power>", Map.of("power", String.valueOf(item.getBlockPower()))))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().promptNumberMin(player, "Enter Block Power", 0).thenAccept(input -> {
                        item.setBlockPower(input);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set block power to %s".formatted(input)));
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        open();
                        return null;
                    });
                }));

        // Broken By (ItemType)
        gui.setItem(2, 7, ItemBuilder.from(Material.WOODEN_PICKAXE)
                .name(MiniMessageUtils.miniMessage("<yellow>Required Tool: <tool>", Map.of("tool", item.getBrokenBy().getName())))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    new EnumSelector<>(player, ItemType.class, "", (clickEvent, selected) -> {
                        item.setBrokenBy(selected);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set required tool to %s".formatted(selected.getName())));
                        setupGui();
                        open();
                    }, () -> {
                        setupGui();
                        open();
                    }).open();
                }));

        // Drop List
        List<Drop> dropList = item.getDrops();
        List<Component> lore = indexManager.dropsToComponents(dropList);
        lore.addAll(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit drops"));
        gui.setItem(3, 5, ItemBuilder.from(Material.CHEST)
                .name(MiniMessageUtils.miniMessage("<yellow>Drops (<count>)", Map.of("count", String.valueOf(dropList.size()))))
                .lore(lore)
                .asGuiItem(event -> {
                    new DropListEditorMenu(player, new ArrayList<>(dropList), updatedDrops -> {
                        item.setDrops(updatedDrops);
                    }, discarded -> {
                        setupGui();
                        open();
                    }).open();
                }));

        // Replacement Block
        Material replacementMaterial = item.getReplacementBlockData().getMaterial();
        gui.setItem(4, 5, ItemBuilder.from(replacementMaterial)
                .name(MiniMessageUtils.miniMessage("<yellow>Replacement Block: <block>", Map.of("block", replacementMaterial.name())))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().prompt(player, "Enter Replacement Block").thenAccept(input -> {
                        try {
                            Material material = Material.valueOf(input.toUpperCase());
                            item.setReplacementBlockData(material.createBlockData());
                            player.sendMessage(MiniMessageUtils.miniMessage("<green>Set replacement block to %s".formatted(material.name())));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid material: %s".formatted(input)));
                        }
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        open();
                        return null;
                    });
                }));

        // Back / Save button
        gui.setItem(6, 1, createBackItem());
        gui.update();
    }
}

