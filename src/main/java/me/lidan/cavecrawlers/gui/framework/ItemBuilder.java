package me.lidan.cavecrawlers.gui.framework;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.lidan.cavecrawlers.items.ItemNbt;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Compatibility builder backed by Bukkit item data and rendered by InvUI.
 */
public final class ItemBuilder {
    private final ItemStack stack;

    private ItemBuilder(ItemStack stack) {
        this.stack = stack;
    }

    public static ItemBuilder from(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder from(ItemStack stack) {
        return new ItemBuilder(stack.clone());
    }

    public static ItemBuilder skull() {
        return from(Material.PLAYER_HEAD);
    }

    public ItemBuilder name(Component name) {
        stack.editMeta(meta -> meta.displayName(name));
        return this;
    }

    public ItemBuilder setName(String name) {
        stack.editMeta(meta -> meta.setDisplayName(name));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        stack.editMeta(meta -> meta.lore(lore));
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder setLore(String... lore) {
        stack.editMeta(meta -> meta.setLore(Arrays.asList(lore)));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        stack.editMeta(meta -> meta.setLore(lore));
        return this;
    }

    public ItemBuilder addLore(String line) {
        stack.editMeta(meta -> {
            var lore = meta.getLore();
            lore = lore == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(lore);
            lore.add(line);
            meta.setLore(lore);
        });
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        stack.editMeta(meta -> meta.addItemFlags(flags));
        return this;
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    public ItemBuilder unbreakable() {
        stack.editMeta(meta -> meta.setUnbreakable(true));
        return this;
    }

    public ItemBuilder setNbt(String key, String value) {
        ItemNbt.setString(stack, key, value);
        return this;
    }

    public ItemBuilder owner(Player player) {
        stack.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(player));
        return this;
    }

    public ItemBuilder texture(String texture) {
        stack.editMeta(SkullMeta.class, meta -> {
            PlayerProfile profile = org.bukkit.Bukkit.createProfile(java.util.UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
        });
        return this;
    }

    public ItemBuilder texture(String texture, java.util.UUID ignored) {
        return texture(texture);
    }

    public ItemStack build() {
        return stack.clone();
    }

    public GuiItem asGuiItem() {
        return new GuiItem(build(), null);
    }

    public GuiItem asGuiItem(Consumer<GuiClick> action) {
        return new GuiItem(build(), action);
    }
}
