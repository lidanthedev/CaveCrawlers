package me.lidan.cavecrawlers.index;

import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class IndexCategory {
    private static final Map<String, IndexCategory> categories = new LinkedHashMap<>();
    public static final IndexCategory MOBS = register("MOBS", new IndexCategory("Mobs"));
    public static final IndexCategory BLOCKS = register("BLOCKS", new IndexCategory("Blocks"));
    public static final IndexCategory BOSSES = register("BOSSES", new IndexCategory("Bosses"));
    public static final IndexCategory ALTARS = register("ALTARS", new IndexCategory("Altars"));

    private String id;
    private final String displayName;

    public IndexCategory(String displayName) {
        this.displayName = displayName;
    }

    public static IndexCategory valueOf(String key) {
        IndexCategory category = categories.get(key.toUpperCase());
        if (category == null) {
            throw new IllegalArgumentException("Index category " + key + " does not exist!");
        }
        return category;
    }

    public static IndexCategory register(String id, IndexCategory category) {
        id = id.toUpperCase();
        category.id = id;
        categories.put(id, category);
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
