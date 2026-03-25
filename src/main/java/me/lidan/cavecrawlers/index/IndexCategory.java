package me.lidan.cavecrawlers.index;

import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class IndexCategory {
    public static final IndexCategory MOBS = new IndexCategory("Mobs");
    public static final IndexCategory BLOCKS = new IndexCategory("Blocks");
    public static final IndexCategory BOSSES = new IndexCategory("Bosses");
    public static final IndexCategory ALTARS = new IndexCategory("Altars");
    private static final Map<String, IndexCategory> categories = new LinkedHashMap<>();

    static {
        register("MOBS", MOBS);
        register("BLOCKS", BLOCKS);
        register("BOSSES", BOSSES);
        register("ALTARS", ALTARS);
    }

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

    public static void register(String id, IndexCategory category) {
        id = id.toUpperCase();
        category.id = id;
        categories.put(id, category);
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
