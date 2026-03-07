package me.lidan.cavecrawlers.index.editor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BossDropListEditorMenu extends BaseEditorMenu<List<BossDrop>> {
    public BossDropListEditorMenu(Player player, List<BossDrop> item, Consumer<List<BossDrop>> onSave, Consumer<List<BossDrop>> onDiscard) {
        super(player, item, onSave, onDiscard, true);
    }

    @Override
    public void setupGui() {
        for (int i = 0; i < item.size(); i++) {
            final int index = i;
            BossDrop drop = item.get(index);
            gui.addItem(DropEditorMenu.createDropItem(drop).asGuiItem(event -> {
                ClickType clickType = event.getClick();

                if (clickType == ClickType.SHIFT_LEFT) {
                    if (index < item.size() - 1) {
                        Collections.swap(item, index, index + 1);
                        save();
                        reopen();
                    }
                    return;
                }

                if (clickType == ClickType.SHIFT_RIGHT) {
                    if (index > 0) {
                        Collections.swap(item, index, index - 1);
                        save();
                        reopen();
                    }
                    return;
                }

                if (clickType == ClickType.DROP) {
                    if (index >= 0 && index < item.size()) {
                        item.remove(index);
                        save();
                        reopen();
                    }
                    return;
                }

                openDropEditor(drop);
            }));
        }
        gui.setItem(6, 5, ItemBuilder.from(Material.EMERALD_BLOCK).name(MiniMessageUtils.miniMessage("<green>Add")).asGuiItem(event -> {
            BossDrop drop = new BossDrop(DropType.COINS.name(), 100.0, "100", 0, null, null);
            openDropEditor(drop, true);
        }));
        GuiItems.setupNextPreviousItems(getPaginatedGui(), 5);
        gui.setItem(6, 1, createBackItem());
        gui.update();
    }

    private void openDropEditor(BossDrop drop) {
        openDropEditor(drop, false);
    }

    private void openDropEditor(BossDrop drop, boolean isNew) {
        new BossDropEditorMenu(player, drop, updatedDrop -> {
            if (isNew) {
                item.add(updatedDrop);
            } else {
                int currentIndex = item.indexOf(drop);
                if (currentIndex != -1) {
                    item.set(currentIndex, updatedDrop);
                }
            }
            save();
        }, updatedDrop -> reopen()).open();
    }

    @Override
    public Component getTitle() {
        return MiniMessageUtils.miniMessage("Editor - Boss Drop List");
    }

    private PaginatedGui getPaginatedGui() {
        return (PaginatedGui) gui;
    }

    public void reopen() {
        new BossDropListEditorMenu(player, item, onSave, onClose).open();
    }
}


