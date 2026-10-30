package me.lidan.cavecrawlers.commands;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReloadAllTest {
    @Test void reloadsCoreAndAddonsImmediatelyAfterShowingTitle() {
        try (var bukkit = mockStatic(Bukkit.class)) {
            var plugins = mock(PluginManager.class);
            var scheduler = mock(BukkitScheduler.class);
            var first = mock(Player.class);
            var second = mock(Player.class);
            var console = mock(ConsoleCommandSender.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(first, second));
            bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
            when(plugins.isPluginEnabled("PlugMan")).thenReturn(true);
            var addon = mock(Plugin.class, RETURNS_DEEP_STUBS);
            when(addon.getName()).thenReturn("TestAddon");
            when(addon.getPluginMeta().getPluginDependencies()).thenReturn(List.of("CaveCrawlers"));
            when(addon.getPluginMeta().getPluginSoftDependencies()).thenReturn(List.of());
            when(plugins.getPlugins()).thenReturn(new Plugin[]{addon});
            var reloads = new ArrayList<String>();
            bukkit.when(() -> Bukkit.dispatchCommand(eq(console), anyString())).thenAnswer(invocation -> {
                verify(first).showTitle(any(Title.class));
                verify(second).showTitle(any(Title.class));
                reloads.add(invocation.getArgument(1));
                return true;
            });
            var command = mock(CaveCrawlersMainCommand.class, CALLS_REAL_METHODS);
            command.reloadAll(mock(CommandSender.class));
            var title = ArgumentCaptor.forClass(Title.class);
            verify(first).showTitle(title.capture());
            verify(second).showTitle(title.getValue());
            assertEquals("Game Updating", PlainTextComponentSerializer.plainText().serialize(title.getValue().title()));
            assertEquals(Duration.ofSeconds(5), title.getValue().times().stay());
            assertEquals(List.of("plugman reload CaveCrawlers", "plugman reload TestAddon"), reloads);
            verifyNoInteractions(scheduler);
        }
    }
}
