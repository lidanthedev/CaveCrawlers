package me.lidan.cavecrawlers.gui;

import me.lidan.cavecrawlers.gui.framework.GuiClick;
import me.lidan.cavecrawlers.items.ItemInfo;

@FunctionalInterface
public interface ItemsGuiCallback {
    void callback(GuiClick event, ItemInfo clickedItemInfo);
}
