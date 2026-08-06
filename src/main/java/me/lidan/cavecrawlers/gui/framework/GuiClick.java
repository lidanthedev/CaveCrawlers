package me.lidan.cavecrawlers.gui.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

/**
 * InvUI click data exposed to the migrated menus.
 */
public record GuiClick(Player player, ClickType clickType) {
    public Player getWhoClicked() {
        return player;
    }

    public ClickType getClick() {
        return clickType;
    }

    public InventoryAction getAction() {
        return clickType == ClickType.MIDDLE ? InventoryAction.CLONE_STACK : InventoryAction.NOTHING;
    }

    public void setCancelled(boolean cancelled) { /* InvUI consumes GUI clicks by design. */ }

    public boolean isRightClick() {
        return clickType.isRightClick();
    }

    public boolean isLeftClick() {
        return clickType.isLeftClick();
    }
}
