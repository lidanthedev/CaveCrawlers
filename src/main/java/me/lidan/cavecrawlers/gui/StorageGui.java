package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.integration.playervaults.PlayerVaultsHook;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StorageGui {
    private final Player player;
    private final PaginatedGui gui;

    public StorageGui(Player player) {
        this.player = player;
        gui = Gui.paginated().rows(6).pageSize(28)
                .title(MiniMessageUtils.miniMessageString("<dark_aqua><bold>Storage")).create();
        gui.disableAllInteractions();
        gui.getFiller().fillBorder(GuiItems.GLASS_ITEM);
        gui.setItem(4, ItemBuilder.from(Material.ENDER_CHEST).setName("§dEnder Chest")
                .setLore("§7Open your personal Ender Chest.", "", "§eClick to open")
                .asGuiItem(event -> MenuGui.runCommand(player, MenuGui.getCommand("ender-chest", MenuGui.STORAGE_COMMAND))));
        gui.setItem(48, GuiItems.BACK_ITEM.asGuiItem(event ->
                Bukkit.getScheduler().runTask(MenuGui.plugin, () -> new MenuGui(player).open())));
        gui.setItem(49, GuiItems.CLOSE_ITEM);

        Plugin vaults = Bukkit.getPluginManager().getPlugin("PlayerVaults");
        if (vaults != null && vaults.isEnabled()) {
            int maxVaults = PlayerVaultsHook.getMaxVaults(vaults, player);
            for (int number = 1; number <= maxVaults; number++) {
                int vault = number;
                gui.addItem(ItemBuilder.from(Material.CHEST).setName("§aPlayer Vault #" + vault)
                        .setLore("§7Store items in your personal vault.", "", "§eClick to open")
                        .asGuiItem(event -> MenuGui.runCommand(player,
                                MenuGui.getCommand("player-vault", "playervaults:pv {vault}")
                                        .replace("{vault}", Integer.toString(vault)))));
            }
            if (maxVaults == 0) {
                gui.setItem(22, ItemBuilder.from(Material.BARRIER).setName("§cPlayer Vaults Locked")
                        .setLore("§7You don't have access", "§7to any player vaults.").asGuiItem());
            }
        }
        if (gui.getPageItems().isEmpty()) {
            gui.getFiller().fill(GuiItems.GLASS_ITEM);
        }
    }

    public void open() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerVaults")) {
            MenuGui.runCommand(player, MenuGui.getCommand("ender-chest", MenuGui.STORAGE_COMMAND));
            return;
        }
        gui.open(player);
        GuiItems.setupNextPreviousItems(gui, 6, 2, 8);
    }
}
