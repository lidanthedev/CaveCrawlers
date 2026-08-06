package me.lidan.cavecrawlers.gui.framework;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PaginatedGui extends Gui {
    private final List<GuiItem> content = new ArrayList<>();
    private final int pageSize;
    private int page;

    PaginatedGui(int rows, Component title, int pageSize) {
        super(rows, title);
        this.pageSize = pageSize;
    }

    public void addItem(GuiItem item) {
        content.add(item);
    }

    public void next() {
        if (getNextPageNum() != page) {
            page++;
            update();
        }
    }

    public void previous() {
        if (getPrevPageNum() != page) {
            page--;
            update();
        }
    }

    public int getCurrentPageNum() {
        return page;
    }

    public int getNextPageNum() {
        return Math.min(page + 1, Math.max(0, pageCount() - 1));
    }

    public int getPrevPageNum() {
        return Math.max(0, page - 1);
    }

    private int effectivePageSize() {
        return pageSize > 0 ? pageSize : Math.max(1, items.length);
    }

    private int pageCount() {
        return Math.max(1, (content.size() + effectivePageSize() - 1) / effectivePageSize());
    }

    @Override
    protected void render() {
        GuiItem[] fixed = items.clone();
        int first = page * effectivePageSize();
        int contentIndex = first;
        for (int i = 0; i < fixed.length && contentIndex < Math.min(content.size(), first + effectivePageSize()); i++)
            if (fixed[i] == null) fixed[i] = content.get(contentIndex++);
        var saved = items.clone();
        System.arraycopy(fixed, 0, items, 0, items.length);
        super.render();
        System.arraycopy(saved, 0, items, 0, items.length);
    }

    @Override
    public void open(Player player) {
        super.open(player);
    }
}
