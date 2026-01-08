package me.lidan.cavecrawlers.utils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.settings.Settings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoostedConfiguration extends YamlDocument {
    protected BoostedConfiguration(@NotNull InputStream document, @Nullable InputStream defaults, @NotNull Settings... settings) throws IOException {
        super(document, defaults, settings);
    }

    protected BoostedConfiguration(@NotNull File document, @Nullable InputStream defaults, @NotNull Settings... settings) throws IOException {
        super(document, defaults, settings);
    }


    public @NotNull Set<String> getKeys(boolean deep) {
        return super.getRoutesAsStrings(deep);
    }


    public @NotNull Map<String, Object> getValues(boolean deep) {
        return super.getStringRouteMappedValues(deep);
    }


    public boolean contains(@NotNull String path, boolean ignoreDefault) {
        return ((ignoreDefault) ? get(path, null) : get(path)) != null;
    }


    public boolean isSet(@NotNull String path) {
        return get(path) != null;
    }


    public int getInt(@NotNull String path, int def) {
        return super.getInt(path, def);
    }


    public boolean getBoolean(@NotNull String path, boolean def) {
        return super.getBoolean(path, def);
    }


    public double getDouble(@NotNull String path, double def) {
        return super.getDouble(path, def);
    }


    public long getLong(@NotNull String path, long def) {
        return super.getLong(path, def);
    }


    public @NotNull List<Integer> getIntegerList(@NotNull String path) {
        return super.getIntList(path);
    }


    public @NotNull List<Boolean> getBooleanList(@NotNull String path) {
        return (List<Boolean>) super.getList(path);
    }


    public @NotNull List<Character> getCharacterList(@NotNull String path) {
        return (List<Character>) super.getList(path);
    }


    public @Nullable <T> T getObject(@NotNull String path, @NotNull Class<T> clazz) {
        return super.getAs(path, clazz);
    }


    public @Nullable <T> T getObject(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        return super.getAs(path, clazz, def);
    }


    public @Nullable <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz) {
        return super.getAs(path, clazz);
    }


    public @Nullable <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        return super.getAs(path, clazz, def);
    }


    public @Nullable Vector getVector(@NotNull String path) {
        return super.getAs(path, Vector.class);
    }


    public @Nullable Vector getVector(@NotNull String path, @Nullable Vector def) {
        return super.getAs(path, Vector.class, def);
    }


    public boolean isVector(@NotNull String path) {
        return getVector(path) != null;
    }


    public @Nullable OfflinePlayer getOfflinePlayer(@NotNull String path) {
        return getAs(path, OfflinePlayer.class);
    }


    public @Nullable OfflinePlayer getOfflinePlayer(@NotNull String path, @Nullable OfflinePlayer def) {
        return getAs(path, OfflinePlayer.class, def);
    }


    public boolean isOfflinePlayer(@NotNull String path) {
        return getOfflinePlayer(path) != null;
    }


    public @Nullable ItemStack getItemStack(@NotNull String path) {
        return getAs(path, ItemStack.class);
    }


    public @Nullable ItemStack getItemStack(@NotNull String path, @Nullable ItemStack def) {
        return getAs(path, ItemStack.class, def);
    }


    public boolean isItemStack(@NotNull String path) {
        return getItemStack(path) != null;
    }


    public @Nullable Color getColor(@NotNull String path) {
        return getAs(path, Color.class);
    }


    public @Nullable Color getColor(@NotNull String path, @Nullable Color def) {
        return getAs(path, Color.class, def);
    }


    public boolean isColor(@NotNull String path) {
        return getColor(path) != null;
    }


    public @Nullable Location getLocation(@NotNull String path) {
        return getAs(path, Location.class);
    }


    public @Nullable Location getLocation(@NotNull String path, @Nullable Location def) {
        return getAs(path, Location.class, def);
    }


    public boolean isLocation(@NotNull String path) {
        return getLocation(path) != null;
    }


    public @Nullable Section getConfigurationSection(@NotNull String path) {
        return getSection(path);
    }


    public boolean isConfigurationSection(@NotNull String path) {
        return getSection(path) != null;
    }

    public @Nullable Section getDefaultSection() {
        return super.getDefaults();
    }
}
