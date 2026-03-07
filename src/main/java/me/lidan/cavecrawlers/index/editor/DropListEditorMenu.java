package me.lidan.cavecrawlers.index.editor;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DropListEditorMenu extends BaseEditorMenu<List<Drop>> {
    public DropListEditorMenu(Player player, List<Drop> item, Consumer<List<Drop>> onSave, Consumer<List<Drop>> onDiscard) {
        super(player, item, onSave, onDiscard, true);
    }

    @Override
    public void setupGui() {
        // iterate by index so we can safely move items based on their position
        for (int i = 0; i < item.size(); i++) {
            final int index = i;
            Drop drop = item.get(index);
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
                    if (index < item.size()) {
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
            Drop drop = new Drop(DropType.COINS, 100.0, "100", null, StatType.MAGIC_FIND, null);
            openDropEditor(drop, true);
        }));
        GuiItems.setupNextPreviousItems(getPaginatedGui(), 5);
        gui.setItem(6, 1, createBackItem());
        gui.update();
    }

    private void openDropEditor(Drop drop) {
        openDropEditor(drop, false);
    }

    private void openDropEditor(Drop drop, boolean isNew) {
        new DropEditorMenu(player, drop, updatedDrop -> {
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
        return MiniMessageUtils.miniMessage("Editor - Drop List");
    }

    private PaginatedGui getPaginatedGui() {
        return (PaginatedGui) gui;
    }

    public void reopen() {
        new DropListEditorMenu(player, item, onSave, onClose).open();
    }
}
