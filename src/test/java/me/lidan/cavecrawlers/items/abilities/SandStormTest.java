package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SandStormTest {
    @Test
    void spawnsOneAlternatingBlockPerRingPosition() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start()) {
            World world = mock(World.class);
            Player player = mock(Player.class);
            Location center = new Location(world, 0.5, 64, 0.5);
            when(player.getLocation()).thenReturn(center);
            when(player.getWorld()).thenReturn(world);
            PlayerEvent event = mock(PlayerEvent.class);
            when(event.getPlayer()).thenReturn(player);
            AbilityDamage calculation = mock(AbilityDamage.class);
            SandStorm ability = spy(new SandStorm(10000, 2));
            doReturn(calculation).when(ability).getDamageCalculation(player);
            doNothing().when(ability).summonFallingBlock(any(), any(), eq(player), eq(calculation));

            assertTrue(ability.useAbility(event));
            context.server().getScheduler().performTicks(20);

            ArgumentCaptor<Location> locations = ArgumentCaptor.forClass(Location.class);
            ArgumentCaptor<Material> materials = ArgumentCaptor.forClass(Material.class);
            verify(ability, times(40)).summonFallingBlock(
                    locations.capture(), materials.capture(), eq(player), eq(calculation));
            assertEquals(40, new HashSet<>(locations.getAllValues()).size());
            for (int i = 0; i < 40; i++) {
                assertEquals(i % 2 == 0 ? Material.SAND : Material.RED_SAND, materials.getAllValues().get(i));
                assertEquals(center.getY(), locations.getAllValues().get(i).getY());
            }
        }
    }
}
