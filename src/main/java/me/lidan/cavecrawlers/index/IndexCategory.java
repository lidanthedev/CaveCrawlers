package me.lidan.cavecrawlers.index;

import net.kyori.adventure.text.Component;

public enum IndexCategory {
    MOBS("Mobs"),
    BLOCKS("Blocks"),
    BOSSES("Bosses"),
    ALTARS("Altars"),
    ;

    private final String displayName;

    IndexCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Component getTitle() {
        return Component.text(displayName + " Index");
    }
}
