package me.lidan.cavecrawlers.gui.selectors;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import me.lidan.cavecrawlers.gui.PaginatedSelector;
import me.lidan.cavecrawlers.index.EntityHeads;
import me.lidan.cavecrawlers.index.IndexManager;
import me.lidan.cavecrawlers.integration.MythicMobsHook;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;

public class MythicMobSelector extends PaginatedSelector<MythicMob> {

    public MythicMobSelector(Player player, String query, BiConsumer<InventoryClickEvent, MythicMob> callback) {
        this(player, query, MiniMessageUtils.miniMessage("Mythic Mob Selector"), callback);
    }

    public MythicMobSelector(Player player, String query, Component title, BiConsumer<InventoryClickEvent, MythicMob> callback) {
        this(player, query, title, callback, null);
    }

    public MythicMobSelector(Player player, String query, Component title, BiConsumer<InventoryClickEvent, MythicMob> callback, Runnable onBack) {
        super(player, query, title, callback, onBack);
    }

    @Override
    public void setupGui() {
        Collection<MythicMob> mobs = MythicMobsHook.getInstance().getAllMobs();
        if (mobs == null) {
            return;
        }
        mobs = mobs.stream().sorted(Comparator.comparing(MythicMob::getInternalName)).toList();
        for (MythicMob mythicMob : mobs) {
            PlaceholderString displayName = mythicMob.getDisplayName();
            String nameStr = (displayName != null && displayName.isPresent())
                    ? displayName.get()
                    : mythicMob.getInternalName();
            if (!nameStr.toLowerCase().contains(query) && !mythicMob.getInternalName().toLowerCase().contains(query))
                continue;
            ItemStack baseMaterial = EntityHeads.fromEntityType(EntityType.valueOf(mythicMob.getEntityTypeString()));
            List<Component> lore = new ArrayList<>(IndexManager.mobInfoToLore(mythicMob));
            lore.add(MiniMessageUtils.miniMessage("<gray>ID: <id>", Map.of("id", mythicMob.getInternalName())));
            gui.addItem(ItemBuilder.from(baseMaterial).name(MiniMessageUtils.miniMessage("<name>", Map.of("name", nameStr))).lore(lore).asGuiItem(event -> callback.accept(event, mythicMob)));
        }
    }

    @Override
    protected void searchInternal(String query) {
        new MythicMobSelector(player, query, title, callback, onBack).open();
    }
}
