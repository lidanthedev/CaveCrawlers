package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.selectors.MythicMobSelector;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BossDropsEditorMenu extends BaseEditorMenu<BossDrops> {
    public BossDropsEditorMenu(Player player, BossDrops item, Consumer<BossDrops> onSave, Consumer<BossDrops> onClose) {
        super(player, item, onSave, onClose);
    }

    @Override
    public void setupGui() {
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        // Entity Name
        gui.setItem(2, 5, ItemBuilder.from(Material.NAME_TAG)
                .name(MiniMessageUtils.miniMessage("<yellow>Entity Name: <name>", Map.of("name", item.getEntityName())))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to select mob"))
                .asGuiItem(event -> new MythicMobSelector(player, "",
                        MiniMessageUtils.miniMessage("Select Entity"),
                        (clickEvent, mob) -> {
                            String displayName = mob.getDisplayName().isPresent()
                                    ? mob.getDisplayName().get()
                                    : mob.getInternalName();
                            item.setEntityName(displayName);
                            player.sendMessage(MiniMessageUtils.miniMessage("<green>Set entity name to %s".formatted(displayName)));
                            setupGui();
                            open();
                        }, () -> {
                    setupGui();
                    open();
                }).open()));

        // Drop List
        List<BossDrop> dropList = item.getDrops();
        List<Component> dropsLore = indexManager.dropsToComponents(dropList);
        dropsLore.addAll(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit drops"));
        gui.setItem(3, 5, ItemBuilder.from(Material.CHEST)
                .name(MiniMessageUtils.miniMessage("<yellow>Drops (<count>)", Map.of("count", String.valueOf(dropList.size()))))
                .lore(dropsLore)
                .asGuiItem(event -> {
                    new BossDropListEditorMenu(player, new ArrayList<>(dropList), updatedDrops -> {
                        item.setDrops(updatedDrops);
                    }, discarded -> {
                        setupGui();
                        open();
                    }).open();
                }));

        // Bonus Points
        List<Integer> bonusPoints = item.getBonusPoints();
        String bonusPointsText = bonusPoints.toString();
        gui.setItem(4, 5, ItemBuilder.from(Material.GOLD_INGOT)
                .name(MiniMessageUtils.miniMessage("<yellow>Bonus Points"))
                .lore(MiniMessageUtils.miniMessageList("<gray>" + bonusPointsText, "", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().prompt(player, "Enter Bonus Points (comma separated)").thenAccept(input -> {
                        try {
                            List<Integer> parsed = new ArrayList<>();
                            for (String part : input.split(",")) {
                                parsed.add(Integer.parseInt(part.trim()));
                            }
                            item.setBonusPoints(parsed);
                            player.sendMessage(MiniMessageUtils.miniMessage("<green>Set bonus points to %s".formatted(parsed)));
                        } catch (NumberFormatException e) {
                            player.sendMessage(MiniMessageUtils.miniMessage("<red>Invalid format. Use comma separated numbers (e.g. 300,250,200,150,100)"));
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

