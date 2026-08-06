package me.lidan.cavecrawlers.gui.framework;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Project menu facade whose display and interaction implementation is InvUI.
 */
public class Gui {
    private static final Set<Player> OPEN_VIEWERS = ConcurrentHashMap.newKeySet();
    protected final int rows;
    protected final Component title;
    protected final GuiItem[] items;
    protected xyz.xenondevs.invui.gui.Gui invUi;
    protected Window window;
    protected Consumer<GuiClick> defaultClickAction;

    protected Gui(int rows, Component title) {
        this.rows = rows;
        this.title = title;
        this.items = new GuiItem[rows * 9];
    }

    public static Builder gui() {
        return new Builder(false);
    }

    public static Builder paginated() {
        return new Builder(true);
    }

    public static void closeOpenGui(Player player) {
        if (OPEN_VIEWERS.remove(player)) player.closeInventory();
    }

    public int getRows() {
        return rows;
    }

    public Gui disableAllInteractions() {
        return this;
    }

    public void setItem(int row, int column, GuiItem item) {
        setItem((row - 1) * 9 + column - 1, item);
    }

    public void setItem(int slot, GuiItem item) {
        if (slot >= 0 && slot < items.length) items[slot] = item;
    }

    public Filler getFiller() {
        return new Filler(this);
    }

    public void addItem(GuiItem item) {
        for (int i = 0; i < items.length; i++)
            if (items[i] == null) {
                items[i] = item;
                return;
            }
    }

    public void setDefaultClickAction(Consumer<GuiClick> action) {
        this.defaultClickAction = action;
    }

    public void setCloseGuiAction(Consumer<Object> action) {
    }

    public org.bukkit.inventory.Inventory getInventory() {
        return window == null ? org.bukkit.Bukkit.createInventory(null, rows * 9) : window.getViewer().getOpenInventory().getTopInventory();
    }

    public void updateItem(int slot, org.bukkit.inventory.ItemStack item) {
        setItem(slot, ItemBuilder.from(item).asGuiItem());
        update();
    }

    public void close(Player player) {
        if (window != null) window.close();
        else player.closeInventory();
        OPEN_VIEWERS.remove(player);
    }

    public void update() {
        if (window != null && window.isOpen()) open(window.getViewer());
    }

    protected void render() {
        invUi = xyz.xenondevs.invui.gui.Gui.empty(9, rows);
        for (int i = 0; i < items.length; i++) if (items[i] != null) invUi.setItem(i, items[i].toInvUiItem());
    }

    public void open(Player player) {
        OPEN_VIEWERS.add(player);
        render();
        window = Window.builder().setTitle(title).setUpperGui(invUi).setViewer(player).build();
        window.open();
    }

    public static final class Builder {
        private final boolean paginated;
        private Component title = Component.empty();
        private int rows = 1;
        private int pageSize;

        private Builder(boolean paginated) {
            this.paginated = paginated;
        }

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder disableAllInteractions() {
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T extends Gui> T create() {
            return (T) (paginated ? new PaginatedGui(rows, title, pageSize) : new Gui(rows, title));
        }
    }

    public static final class Filler {
        private final Gui gui;

        private Filler(Gui gui) {
            this.gui = gui;
        }

        public void fill(GuiItem item) {
            Arrays.fill(gui.items, item);
        }

        public void fillBottom(GuiItem item) {
            for (int i = (gui.rows - 1) * 9; i < gui.items.length; i++) gui.items[i] = item;
        }

        public void fillBorder(GuiItem item) {
            for (int row = 0; row < gui.rows; row++)
                for (int col = 0; col < 9; col++)
                    if (row == 0 || row == gui.rows - 1 || col == 0 || col == 8) gui.items[row * 9 + col] = item;
        }

        public void fillBetweenPoints(int startRow, int startCol, int endRow, int endCol, GuiItem item) {
            for (int row = startRow; row <= endRow; row++)
                for (int col = startCol; col <= endCol; col++)
                    if (row >= 0 && row < gui.rows && col >= 0 && col < 9) gui.items[row * 9 + col] = item;
        }
    }
}
