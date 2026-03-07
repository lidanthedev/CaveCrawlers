package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.gui.selectors.MythicMobSelector;
import me.lidan.cavecrawlers.index.editor.EntityDropsEditorMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexMobsCategoryMenu extends IndexBaseCategoryMenu {

    public IndexMobsCategoryMenu(Player player, String query) {
        super(player, IndexCategory.MOBS, query);
    }

    @Override
    public void setupGui() {
        Map<String, EntityDrops> dropsMap = DropsManager.getInstance().getEntityDropsMap();
        List<Map.Entry<String, EntityDrops>> entries = dropsMap.entrySet().stream()
                .sorted(Comparator.comparing(entry ->
                        ChatColor.stripColor(entry.getValue().getEntityName())
                ))
                .toList();
        for (Map.Entry<String, EntityDrops> dropsEntry : entries) {
            String mobName = dropsEntry.getKey();
            if (!ChatColor.stripColor(mobName.toLowerCase()).contains(query)) continue;
            addItem(mobName, ItemBuilder.from(itemGenerator.entityDropsToItemStack(dropsEntry.getValue())).asGuiItem(), event -> {
                String originalKey = dropsEntry.getKey();
                new EntityDropsEditorMenu(player, dropsEntry.getValue(), updated -> {
                    DropsManager.getInstance().renameEntityDrops(originalKey, updated.getEntityName(), updated);
                }, onClose -> {
                    search(query, true);
                }).open();
            });
        }

        if (player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION) && plugin.getConfig().getBoolean(EXPERIMENTAL_INDEX_EDITOR)) {
            gui.setItem(1, 9, ItemBuilder.from(Material.EMERALD_BLOCK)
                    .name(MiniMessageUtils.miniMessage("<green>New Mob Drop"))
                    .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to create a new mob drop entry"))
                    .asGuiItem(event -> new MythicMobSelector(player, "",
                            MiniMessageUtils.miniMessage("Select Entity"),
                            (clickEvent, mob) -> {
                                io.lumine.mythic.api.skills.placeholders.PlaceholderString dn = mob.getDisplayName();
                                String displayName = (dn != null && dn.isPresent()) ? dn.get() : mob.getInternalName();
                                if (DropsManager.getInstance().getEntityDrops(displayName) != null) {
                                    player.sendMessage(MiniMessageUtils.miniMessage("<red>A drop table for '%s' already exists".formatted(displayName)));
                                    search(query, true);
                                    return;
                                }
                                EntityDrops newDrops = new EntityDrops(displayName, new ArrayList<>(), 0);
                                new EntityDropsEditorMenu(player, newDrops, updated ->
                                        DropsManager.getInstance().updateEntityDrops(updated.getEntityName(), updated),
                                        onClose -> search(query, true)
                                ).open();
                            }, () -> {
                        search(query, true);
                    }).open()));
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            return;
        }
        new IndexMobsCategoryMenu(player, query).open();
    }
}
