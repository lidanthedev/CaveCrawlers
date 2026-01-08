package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.index.*;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"index", "drops", "bestiary"})
@CommandPermission("cavecrawlers.index.view")
public class IndexCommand {
    @Subcommand("menu")
    @DefaultFor({"index", "drops", "bestiary"})
    public void indexMenu(Player player) {
        new IndexMainMenu(player).open();
    }

    @Subcommand("mobs")
    public void indexMobs(Player sender, @Default("") String query) {
        new IndexMobsCategoryMenu(sender, query).open();
    }

    @Subcommand("blocks")
    public void indexBlocks(Player sender, @Default("") String query) {
        new IndexBlocksCategoryMenu(sender, query).open();
    }

    @Subcommand("bosses")
    public void indexBosses(Player sender, @Default("") String query) {
        new IndexBossesCategoryMenu(sender, query).open();
    }

    @Subcommand("altars")
    public void indexAltars(Player sender, @Default("") String query) {
        new IndexAltarsCategoryMenu(sender, query).open();
    }
}
