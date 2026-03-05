package me.lidan.cavecrawlers.gui.selectors;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.gui.PaginatedSelector;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.function.BiConsumer;

public class StatTypeSelector extends PaginatedSelector<StatType> {
    public StatTypeSelector(Player player, String query, BiConsumer<InventoryClickEvent, StatType> callback) {
        super(player, query, MiniMessageUtils.miniMessage("Stat Type Selector"), callback);
    }

    @Override
    public void setupGui() {
        for (StatType statType : StatType.values()) {
            if (!statType.name().toLowerCase().contains(query)) continue;
            gui.addItem(ItemBuilder.from(Material.PAPER).name(MiniMessageUtils.miniMessage("<name>", Map.of("name", statType.getFormatNameComponent()))).asGuiItem(event -> callback.accept(event, statType)));
        }
    }

    @Override
    protected void searchInternal(String query) {
        new StatTypeSelector(player, query, callback).open();
    }
}
