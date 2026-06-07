package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.Getter;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class IndexCategory {
    private static final Map<String, IndexCategory> categories = new LinkedHashMap<>();
    public static final IndexCategory MOBS = register("MOBS", new IndexCategory("Mobs", ItemBuilder.from(Material.ZOMBIE_HEAD).name(MiniMessageUtils.miniMessage("<green>Mobs")).asGuiItem(event -> new IndexMobsCategoryMenu((Player) event.getWhoClicked(), "").open())));
    public static final IndexCategory BLOCKS = register("BLOCKS", new IndexCategory("Blocks", ItemBuilder.from(Material.DIAMOND_ORE).name(MiniMessageUtils.miniMessage("<gold>Blocks")).asGuiItem(event -> new IndexBlocksCategoryMenu((Player) event.getWhoClicked(), "").open())));
    public static final IndexCategory BOSSES = register("BOSSES", new IndexCategory("Bosses", ItemBuilder.from(Material.DRAGON_HEAD).name(MiniMessageUtils.miniMessage("<red>Bosses")).asGuiItem(event -> new IndexBossesCategoryMenu((Player) event.getWhoClicked(), "").open())));
    public static final IndexCategory ALTARS = register("ALTARS", new IndexCategory("Altars", ItemBuilder.from(Material.END_PORTAL_FRAME).name(MiniMessageUtils.miniMessage("<aqua>Altars")).asGuiItem(event -> new IndexAltarsCategoryMenu((Player) event.getWhoClicked(), "").open())));

    private String id;
    private final String displayName;
    private final GuiItem guiItem;

    public IndexCategory(String displayName, GuiItem guiItem) {
        this.displayName = displayName;
        this.guiItem = guiItem;
    }

    public static IndexCategory valueOf(String key) {
        IndexCategory category = categories.get(key.toUpperCase());
        if (category == null) {
            throw new IllegalArgumentException("Index category " + key + " does not exist!");
        }
        return category;
    }

    public static IndexCategory register(String id, IndexCategory category) {
        String normalizedId = id.toUpperCase();
        if (category.id != null) {
            throw new IllegalArgumentException("Index category " + category.id + " is already registered");
        }
        if (categories.putIfAbsent(normalizedId, category) != null) {
            throw new IllegalArgumentException("Index category " + normalizedId + " is already registered");
        }
        category.id = normalizedId;
        return category;
    }

    public static IndexCategory[] values() {
        return categories.values().toArray(new IndexCategory[0]);
    }

    public String name() {
        return id;
    }

    public Component getTitle() {
        return Component.text(displayName + " Index");
    }
}
