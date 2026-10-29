package me.lidan.cavecrawlers.commands;

import me.lidan.cavecrawlers.CaveCrawlers;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReloadAllTest {
    @Test void titleIsSentToEveryPlayerBeforeAnyReload() throws Exception {
        try (var bukkit = mockStatic(Bukkit.class)) {
            var plugins = mock(PluginManager.class);
            var scheduler = mock(BukkitScheduler.class);
            CaveCrawlers plugin = null;
            var first = mock(Player.class);
            var second = mock(Player.class);
            var console = mock(ConsoleCommandSender.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(first, second));
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            when(plugins.isPluginEnabled("PlugMan")).thenReturn(true);
            when(plugins.getPlugins()).thenReturn(new Plugin[0]);
            var command = mock(CaveCrawlersMainCommand.class, CALLS_REAL_METHODS);
            command.reloadAll(mock(CommandSender.class));
            var title = ArgumentCaptor.forClass(Title.class);
            verify(first).showTitle(title.capture());
            verify(second).showTitle(title.getValue());
            assertEquals("Game Updating", PlainTextComponentSerializer.plainText().serialize(title.getValue().title()));
            bukkit.verify(() -> Bukkit.dispatchCommand(console, "plugman reload CaveCrawlers"), never());
            var work = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTask(eq(plugin), work.capture());
            work.getValue().run();
            bukkit.verify(() -> Bukkit.dispatchCommand(console, "plugman reload CaveCrawlers"));
        }
    }
}
