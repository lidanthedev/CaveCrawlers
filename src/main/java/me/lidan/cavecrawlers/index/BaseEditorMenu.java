package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public abstract class BaseEditorMenu<T> {
    protected Player player;
    protected T item;
    protected Consumer<T> onSave;
    protected Consumer<T> onDiscard;
    protected BaseGui gui;

    public BaseEditorMenu(Player player, T item, Consumer<T> onSave, Consumer<T> onDiscard) {
        this.player = player;
        this.item = item;
        this.onSave = onSave;
        this.onDiscard = onDiscard;
        this.gui = Gui.gui()
                .title(MiniMessageUtils.miniMessage("Editor - %s".formatted(item.getClass().getSimpleName())))
                .rows(6)
                .create();
        gui.disableAllInteractions();
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
        setupGui();
    }

    public abstract void setupGui();

    public void save() {
        player.closeInventory();
        onSave.accept(item);
    }

    public void discard() {
        player.closeInventory();
        onDiscard.accept(item);
    }
}
