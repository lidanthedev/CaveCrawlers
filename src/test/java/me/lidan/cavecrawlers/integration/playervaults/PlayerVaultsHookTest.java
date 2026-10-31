package me.lidan.cavecrawlers.integration.playervaults;

import com.drtshock.playervaults.PlayerVaults;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlayerVaultsHookTest {
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
    void vaultAmountPermissionIncludesAllLowerNumberedVaults() {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.commands.use")).thenReturn(true);
        when(player.hasPermission("playervaults.amount.30")).thenReturn(true);
        assertEquals(30, PlayerVaultsHook.getMaxVaults(vaultPlugin(), player));
    }

    @Test
    void commandPermissionAloneDoesNotGrantVaults() {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.commands.use")).thenReturn(true);
        assertEquals(0, PlayerVaultsHook.getMaxVaults(vaultPlugin(), player));
    }

    @Test
    void negatedCommandPermissionBlocksEvenPlayersWithVaultPermissions() {
        Player player = mock(Player.class);
        when(player.hasPermission("playervaults.amount.30")).thenReturn(true);
        PlayerVaults vaults = vaultPlugin();
        assertEquals(0, PlayerVaultsHook.getMaxVaults(vaults, player));
        verifyNoInteractions(vaults);
    }

    @Test
    void wildcardPermissionsUseThePluginsConfiguredLimit() {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        assertEquals(60, PlayerVaultsHook.getMaxVaults(vaultPlugin(), player));
    }

    private static PlayerVaults vaultPlugin() {
        PlayerVaults vaults = mock(PlayerVaults.class);
        when(vaults.getMaxVaultAmountPermTest()).thenReturn(60);
        clearInvocations(vaults);
        return vaults;
    }
}
