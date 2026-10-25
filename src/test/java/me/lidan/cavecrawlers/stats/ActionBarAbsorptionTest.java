package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionBarAbsorptionTest {
    private MockCaveCrawlers context;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    void readoutShowsTheActualAmountInGoldAndHidesEmptyPools() {
        Component readout = ActionBarManager.absorptionReadout(3200);
        assertEquals(" +3200❤", PlainTextComponentSerializer.plainText().serialize(readout));
        assertEquals(NamedTextColor.GOLD, readout.color());
        assertEquals(Component.empty(), ActionBarManager.absorptionReadout(0));
        assertEquals(Component.empty(), ActionBarManager.absorptionReadout(Double.NaN));
        assertEquals(" +1❤", PlainTextComponentSerializer.plainText().serialize(ActionBarManager.absorptionReadout(0.25)));
    }

    @Test
    void defaultActionBarIncludesRealAbsorptionAlongsideHealthAndAlerts() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getHealth()).thenReturn(32000D);
        when(player.getMaxHealth()).thenReturn(32000D);
        when(player.getAbsorptionAmount()).thenReturn(3200D);
        ActionBarManager manager = new ActionBarManager();

        String normal = PlainTextComponentSerializer.plainText().serialize(manager.actionBarBuildAdventure(player));
        String alert = PlainTextComponentSerializer.plainText().serialize(manager.actionBarBuildAdventure(player, "Healing!"));
        assertTrue(normal.contains("32000/32000❤ +3200❤"), normal);
        assertTrue(alert.contains("+3200❤"), alert);
        assertTrue(alert.contains("Healing!"), alert);
        assertFalse(normal.contains("<absorption-display>"));
    }
}
