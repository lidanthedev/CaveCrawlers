package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class IndexAltarsCategoryMenu extends IndexBaseCategoryMenu {

    public IndexAltarsCategoryMenu(Player player, String query) {
        super(player, IndexCategory.ALTARS, query);
    }

    @Override
    public void setupGui() {
        List<String> names = AltarManager.getInstance().getAltarNames().stream().sorted().toList();
        for (String name : names) {
            Altar altar = AltarManager.getInstance().getAltar(name);
            if (!ChatColor.stripColor(name.toLowerCase()).contains(query)) continue;
            addItem(name, ItemBuilder.from(itemGenerator.altarToItemStack(altar)).asGuiItem(), event -> {
                player.closeInventory();
                player.performCommand("cavecrawlers altar info " + name);
            });
        }
        if (player.hasPermission(CAVECRAWLERS_INDEX_ADMIN_PERMISSION) && plugin.getConfig().getBoolean(EXPERIMENTAL_INDEX_EDITOR)) {
            gui.setItem(1, 9, ItemBuilder.from(Material.EMERALD_BLOCK)
                    .name(MiniMessageUtils.miniMessage("<green>New Altar"))
                    .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to create a new altar"))
                    .asGuiItem(event -> {
                        player.closeInventory();
                        player.sendMessage(MiniMessageUtils.miniMessage("<click:suggest_command:'/cavecrawlers altar create '><green>To create new altar use the command: <yellow>/cavecrawlers altar create <name> <b>CLICK</b></click>"));
                    }));
        }
    }

    @Override
    public void search(String query, boolean force) {
        if (this.query.equals(query) && !force) {
            // Same query, do nothing
            return;
        }
        new IndexAltarsCategoryMenu(player, query).open();
    }
}
