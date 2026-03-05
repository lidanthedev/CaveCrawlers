package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.gui.GuiItems;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class DropListEditorMenu extends BaseEditorMenu<List<Drop>> {
    public DropListEditorMenu(Player player, List<Drop> item, Consumer<List<Drop>> onSave, Consumer<List<Drop>> onDiscard) {
        super(player, item, onSave, onDiscard, true);
    }

    @Override
    public void setupGui() {
        for (Drop drop : item) {
            gui.addItem(DropEditorMenu.createDropItem(drop).asGuiItem(event -> {
                new DropEditorMenu(player, drop, updatedDrop -> {
                    int index = item.indexOf(drop);
                    item.set(index, updatedDrop);
                    onSave.accept(item);
                }, updatedDrop -> {
                    reopen();
                }).open();
            }));
        }

        GuiItems.setupNextPreviousItems((PaginatedGui) gui, 6);
    }

    public void reopen() {
        new DropListEditorMenu(player, item, onSave, onClose).open();
    }
}
