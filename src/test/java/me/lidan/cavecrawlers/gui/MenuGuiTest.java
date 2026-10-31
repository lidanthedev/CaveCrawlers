package me.lidan.cavecrawlers.gui;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void vaultAmountPermissionIncludesAllLowerNumberedVaults() throws Exception {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.commands.use")).thenReturn(true);
        when(player.hasPermission("playervaults.amount.30")).thenReturn(true);
        assertEquals(30, maxVaults(vaultPlugin(), player));
    }

    @Test
    void commandPermissionAloneDoesNotGrantVaults() throws Exception {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.commands.use")).thenReturn(true);
        assertEquals(0, maxVaults(vaultPlugin(), player));
    }

    @Test
    void negatedCommandPermissionBlocksEvenPlayersWithVaultPermissions() throws Exception {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.amount.30")).thenReturn(true);
        Plugin vaults = vaultPlugin();
        assertEquals(0, maxVaults(vaults, player));
        verifyNoInteractions(vaults);
    }

    @Test
    void wildcardPermissionsUseThePluginsConfiguredLimit() throws Exception {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        assertEquals(60, maxVaults(vaultPlugin(), player));
    }

    @Test
    void unsupportedPlayerVaultsApiFailsClosed() throws Exception {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.commands.use")).thenReturn(true);
        assertEquals(0, maxVaults(mock(Plugin.class), player));
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "spawn", "playervaults:pv 30"})
    void commandsUseThePlayerCommandPathAfterTheClickFinishes(String command) {
        Player player = mock(Player.class);
        MenuGui.runCommand(player, command);
        verifyNoInteractions(player);
        context.server().getScheduler().performOneTick();
        var order = inOrder(player);
        order.verify(player).closeInventory();
        order.verify(player).chat("/" + command);
        verify(player, never()).performCommand(anyString());
    }

    @Test
    void dailyAndHubDefaultToTheRequestedCommands() {
        assertEquals("daily", MenuGui.DAILY_COMMAND);
        assertEquals("spawn", MenuGui.HUB_COMMAND);
    }

    public interface VaultLimits {
        int getMaxVaultAmountPermTest();
    }

    private static Plugin vaultPlugin() {
        Plugin vaults = mock(Plugin.class, withSettings().extraInterfaces(VaultLimits.class));
        when(((VaultLimits) vaults).getMaxVaultAmountPermTest()).thenReturn(60);
        clearInvocations(vaults);
        return vaults;
    }

    private static int maxVaults(Plugin vaults, Player player) throws Exception {
        Method method = StorageGui.class.getDeclaredMethod("getMaxVaults", Plugin.class, Player.class);
        method.setAccessible(true);
        return (int) method.invoke(null, vaults, player);
    }
}
