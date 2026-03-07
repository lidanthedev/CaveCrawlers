package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.bosses.BossManager;
import me.lidan.cavecrawlers.gui.selectors.MythicMobSelector;
import me.lidan.cavecrawlers.index.editor.BossDropsEditorMenu;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexBossesCategoryMenu extends IndexBaseCategoryMenu {

    public IndexBossesCategoryMenu(Player player, String query) {
        super(player, IndexCategory.BOSSES, query);
    }

    @Override
    public void setupGui() {
        Map<String, BossDrops> dropsMap = BossManager.getInstance().getDropsMap();
        List<Map.Entry<String, BossDrops>> entries = dropsMap.entrySet().stream()
                .sorted(Comparator.comparing(entry ->
                        ChatColor.stripColor(entry.getValue().getEntityName())
                ))
                .toList();
        for (Map.Entry<String, BossDrops> dropsEntry : entries) {
            String name = String.valueOf(dropsEntry.getKey());
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            addItem(name, ItemBuilder.from(itemGenerator.bossDropsToItemStack(dropsEntry.getValue())).asGuiItem(), event -> {
                new BossDropsEditorMenu(player, dropsEntry.getValue(), updated -> {
                    BossManager.getInstance().updateBossDrops(dropsEntry.getKey(), updated);
                }, onClose -> {
                    search(query, true);
                }).open();
            });
        }

        if (player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION) && plugin.getConfig().getBoolean(EXPERIMENTAL_INDEX_EDITOR)) {
            gui.setItem(1, 9, ItemBuilder.from(Material.EMERALD_BLOCK)
                    .name(MiniMessageUtils.miniMessage("<green>New Boss Drop"))
                    .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to create a new boss drop entry"))
                    .asGuiItem(event -> new MythicMobSelector(player, "",
                            MiniMessageUtils.miniMessage("Select Boss Entity"),
                            (clickEvent, mob) -> {
                                io.lumine.mythic.api.skills.placeholders.PlaceholderString dn = mob.getDisplayName();
                                String displayName = (dn != null && dn.isPresent()) ? dn.get() : mob.getInternalName();
                                if (BossManager.getInstance().getEntityDrops(displayName) != null) {
                                    player.sendMessage(MiniMessageUtils.miniMessage("<red>A boss drop table for '%s' already exists".formatted(displayName)));
                                    search(query, true);
                                    return;
                                }
                                BossDrops newBossDrops = new BossDrops(new ArrayList<>(), displayName, null, List.of(300, 250, 200, 150, 100));
                                new BossDropsEditorMenu(player, newBossDrops, updated ->
                                        BossManager.getInstance().updateBossDrops(updated.getEntityName(), updated),
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
        new IndexBossesCategoryMenu(player, query).open();
    }
}
