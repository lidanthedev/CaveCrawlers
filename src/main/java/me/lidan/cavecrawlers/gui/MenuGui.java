package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MenuGui {
    private final Player player;
    private final Gui gui;
    public final String AFK_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWM0YTQxNTU0MzQyYjJjYmZiZDg5NWU5ZWM3MDg5YWU4NjFmZWM4ZjUxNjkzODIyZWMxZWIzY2EzZjE4In19fQ==";
    public final String SPAWN_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjE1MWNmZmRhZjMwMzY3MzUzMWE3NjUxYjM2NjM3Y2FkOTEyYmE0ODU2NDMxNThlNTQ4ZDU5YjJlYWQ1MDExIn19fQ==";
    public final String AFK_COMMAND = "warp afk";
    public final String SPAWN_COMMAND = "spawn";

    public MenuGui(Player player) {
        this.player = player;
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        this.gui = new Gui(3, "§7Menu");

        gui.disableAllInteractions();
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§f%s Stats:".formatted(player.getName())).setLore(statsMessage).asGuiItem());
        gui.setItem(11, ItemBuilder.skull().texture(AFK_SKULL_TEXTURE).setName("§fWarp AFK").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(AFK_COMMAND);
        })));
        gui.setItem(15, ItemBuilder.skull().texture(SPAWN_SKULL_TEXTURE).setName("§fWarp Spawn").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SPAWN_COMMAND);
        })));
        gui.getFiller().fill(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
    }

    public void open(){
        gui.open(player);
    }
}
