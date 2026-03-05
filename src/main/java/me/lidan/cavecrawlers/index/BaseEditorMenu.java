package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public abstract class BaseEditorMenu<T> {
    protected final IndexManager indexManager;
    protected Player player;
    protected T item;
    protected Consumer<T> onSave;
    protected Consumer<T> onClose;
    protected BaseGui gui;
    protected boolean closed = false;

    public BaseEditorMenu(Player player, T item, Consumer<T> onSave, Consumer<T> onClose, boolean paginate) {
        this.player = player;
        this.item = item;
        this.onSave = onSave;
        this.onClose = onClose;
        Component title = MiniMessageUtils.miniMessage("Editor - %s".formatted(item.getClass().getSimpleName()));
        this.indexManager = IndexManager.getInstance();
        if (paginate) {
            this.gui = Gui.paginated().title(title)
                    .rows(6)
                    .pageSize(45)
                    .create();
        } else {
            this.gui = Gui.gui()
                    .title(title)
                    .rows(6)
                    .create();
        }
        if (onSave == null) {
            this.onSave = drop -> {
            };
        }
        if (onClose == null) {
            this.onClose = drop -> {
            };
        }
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        gui.setCloseGuiAction(event -> {
            close();
        });
        setupGui();
    }

    public BaseEditorMenu(Player player, T item, Consumer<T> onSave, Consumer<T> onClose) {
        this(player, item, onSave, onClose, false);
    }

    public abstract void setupGui();

    public void open() {
        gui.open(player);
    }

    public void save() {
        onSave.accept(item);
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        player.closeInventory();
        onClose.accept(item);
    }
}
