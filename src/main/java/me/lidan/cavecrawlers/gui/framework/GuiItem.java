package me.lidan.cavecrawlers.gui.framework;

import xyz.xenondevs.invui.item.Item;

import java.util.function.Consumer;

public final class GuiItem {
    private final org.bukkit.inventory.ItemStack stack;
    private Consumer<GuiClick> action;

    GuiItem(org.bukkit.inventory.ItemStack stack, Consumer<GuiClick> action) {
        this.stack = stack;
        this.action = action;
    }

    public void setAction(Consumer<GuiClick> action) {
        this.action = action;
    }

    public org.bukkit.inventory.ItemStack getItemStack() {
        return stack.clone();
    }

    Item toInvUiItem() {
        var builder = Item.builder().setItemProvider(stack.clone());
        if (action != null)
            builder.addClickHandler(click -> action.accept(new GuiClick(click.player(), click.clickType())));
        return builder.build();
    }
}
