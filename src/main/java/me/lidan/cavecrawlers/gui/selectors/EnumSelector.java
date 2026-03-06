package me.lidan.cavecrawlers.gui.selectors;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.gui.PaginatedSelector;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;

public class EnumSelector<T extends Enum<T>> extends PaginatedSelector<T> {
    private Class<T> enumClass;

    public EnumSelector(Player player, Class<T> enumClass, String query, BiConsumer<InventoryClickEvent, T> callback) {
        this(player, enumClass, query, callback, null);
    }

    public EnumSelector(Player player, Class<T> enumClass, String query, BiConsumer<InventoryClickEvent, T> callback, Runnable onBack) {
        super(player, query, MiniMessageUtils.miniMessage("%s Selector".formatted(StringUtils.setTitleCase(enumClass.getSimpleName()))), callback, onBack);
        this.enumClass = enumClass;
        setupGui();
    }

    @Override
    public void setupGui() {
        if (enumClass == null) return;
        for (T value : enumClass.getEnumConstants()) {
            String name = value.name();
            if (!name.toLowerCase().contains(query)) continue;
            String displayName = StringUtils.setTitleCase(name.replace("_", " "));
            gui.addItem(ItemBuilder.from(Material.PAPER)
                    .name(MiniMessageUtils.miniMessage("<yellow>%s".formatted(displayName)))
                    .lore(MiniMessageUtils.miniMessageList("<gray>Click to select"))
                    .asGuiItem(event -> callback.accept(event, value)));
        }
    }

    @Override
    protected void searchInternal(String query) {
        new EnumSelector<>(player, enumClass, query, callback, onBack).open();
    }
}
