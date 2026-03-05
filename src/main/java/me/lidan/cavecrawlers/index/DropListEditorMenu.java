package me.lidan.cavecrawlers.index;

import me.lidan.cavecrawlers.drops.Drop;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class DropListEditorMenu extends BaseEditorMenu<List<Drop>> {
    public DropListEditorMenu(Player player, List<Drop> item, Consumer<List<Drop>> onSave, Consumer<List<Drop>> onDiscard) {
        super(player, item, onSave, onDiscard);
    }

    @Override
    public void setupGui() {

    }
}
