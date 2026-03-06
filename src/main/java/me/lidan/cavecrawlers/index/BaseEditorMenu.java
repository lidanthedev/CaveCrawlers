package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public abstract class BaseEditorMenu<T> {
    public static final ItemBuilder BACK_ITEM = ItemBuilder.from(Material.ARROW).name(MiniMessageUtils.miniMessage("<green>Save and go back")).lore(Component.empty(), MiniMessageUtils.miniMessage("<yellow>Click to To Save and Back"), MiniMessageUtils.miniMessage("<red>Right click to force go back"));
    protected static final IndexManager indexManager = IndexManager.getInstance();
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
        Component title = getTitle();
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
        setupGui();
    }

    public BaseEditorMenu(Player player, T item, Consumer<T> onSave, Consumer<T> onClose) {
        this(player, item, onSave, onClose, false);
    }


    public Component getTitle() {
        return MiniMessageUtils.miniMessage("Editor - %s".formatted(item.getClass().getSimpleName()));
    }

    public abstract void setupGui();

    public void open() {
        gui.open(player);
    }

    public boolean save() {
        onSave.accept(item);
        return true;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        player.closeInventory();
        onClose.accept(item);
    }

    public GuiItem createBackItem() {
        return BACK_ITEM.asGuiItem(event -> {
            if (event.isRightClick()) {
                player.sendMessage(MiniMessageUtils.miniMessage("<yellow>Forced go back (changes might not be saved)"));
                close();
                return;
            }
            if (save()) {
                close();
            }
        });
    }
}
