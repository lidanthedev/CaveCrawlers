package me.lidan.cavecrawlers.gui;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MenuGuiTest {
    private static MockCaveCrawlers context;

    @BeforeAll
    static void setUp() throws Exception {
        context = MockCaveCrawlers.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        context.close();
    }

    @ParameterizedTest
    @CsvSource({"daily, /daily", "/daily, /daily", "spawn, /spawn", "playervaults:pv 30, /playervaults:pv 30"})
    void commandsUseThePlayerCommandPathAfterTheClickFinishes(String command, String expected) {
        Player player = mock(Player.class);
        MenuGui.runCommand(player, command);
        verifyNoInteractions(player);
        context.server().getScheduler().performOneTick();
        var order = inOrder(player);
        order.verify(player).closeInventory();
        order.verify(player).chat(expected);
        verify(player, never()).performCommand(anyString());
    }

    @Test
    void dailyAndHubDefaultToTheRequestedCommands() {
        assertEquals("daily", MenuGui.DAILY_COMMAND);
        assertEquals("spawn", MenuGui.HUB_COMMAND);
    }

    @Test
    void commandConfigurationIsReadAgainAfterConfigChanges() {
        var config = context.plugin().getConfig();
        try {
            assertEquals("daily", MenuGui.getCommand("daily", "daily"));
            config.set("menu.commands.daily", "rewards");
            assertEquals("rewards", MenuGui.getCommand("daily", "daily"));
            config.set("menu.commands.daily", "/daily");
            assertEquals("/daily", MenuGui.getCommand("daily", "daily"));
        } finally {
            config.set("menu.commands.daily", null);
        }
    }

    @Test
    void storageWithoutPlayerVaultsRunsConfiguredEnderChestCommand() throws Exception {
        Player player = mock(Player.class);
        StorageGui storage = mock(StorageGui.class, CALLS_REAL_METHODS);
        var playerField = StorageGui.class.getDeclaredField("player");
        playerField.setAccessible(true);
        playerField.set(storage, player);
        var config = context.plugin().getConfig();
        try {
            config.set("menu.commands.ender-chest", "enderchest");
            storage.open();
            verifyNoInteractions(player);
            context.server().getScheduler().performOneTick();
            verify(player).chat("/enderchest");
        } finally {
            config.set("menu.commands.ender-chest", null);
        }
    }

    @Test
    void packagedConfigDocumentsEveryMenuCommand() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            var config = YamlConfiguration.loadConfiguration(reader);
            assertEquals(9, config.getConfigurationSection("menu.commands").getKeys(false).size());
            assertEquals("daily", config.getString("menu.commands.daily"));
            assertEquals("spawn", config.getString("menu.commands.hub"));
            assertEquals("playervaults:pv {vault}", config.getString("menu.commands.player-vault"));
        }
    }

    // triumph-gui's ItemBuilder needs CraftBukkit classes, so only buttons rejected before building can run under MockBukkit.
    @ParameterizedTest
    @CsvSource(value = {"-1, BONE, pets", "54, BONE, pets", "30, NOT_A_MATERIAL, pets", "30, BONE, NULL"}, nullValues = "NULL")
    void invalidMenuYmlButtonsAreSkipped(int slot, String material, String command) {
        try {
            MenuGui.config.set("buttons.pets.slot", slot);
            MenuGui.config.set("buttons.pets.material", material);
            MenuGui.config.set("buttons.pets.command", command);
            Gui gui = Gui.gui().rows(6).title(net.kyori.adventure.text.Component.empty()).create();
            MenuGui.addCustomButtons(gui);
            assertTrue(gui.getGuiItems().isEmpty());
        } finally {
            MenuGui.config.set("buttons", null);
        }
    }

    @Test
    void menuWithoutButtonsSectionAddsNothing() {
        Gui gui = Gui.gui().rows(6).title(net.kyori.adventure.text.Component.empty()).create();
        MenuGui.addCustomButtons(gui);
        assertNull(gui.getGuiItem(30));
    }

}
