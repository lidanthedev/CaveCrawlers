package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SandStormTest {
    @ParameterizedTest
    @CsvSource({"0.5,64,0.5", "-3.2,64.7,-2.8"})
    void preservesStormSpreadWithOneBlockPerColumn(double x, double y, double z) throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start()) {
            World world = mock(World.class);
            Player player = mock(Player.class);
            Location center = new Location(world, x, y, z);
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
            verify(ability, atLeastOnce()).summonFallingBlock(
                    locations.capture(), materials.capture(), eq(player), eq(calculation));
            var spawned = locations.getAllValues();
            assertTrue(spawned.size() > 40, "Keep the inner ring and raised blocks");
            assertEquals(spawned.size(), new HashSet<>(spawned).size());
            assertEquals(spawned.size(), spawned.stream()
                    .map(location -> new Vector(location.getBlockX(), 0, location.getBlockZ()))
                    .distinct().count(), "Only one block may spawn in each horizontal column");
            assertEquals(1, spawned.stream().filter(location -> location.getBlockX() == center.getBlockX() + 2
                    && location.getBlockZ() == center.getBlockZ()).count(), "East must not spawn twice");
            assertEquals(2, spawned.stream().map(Location::getBlockY).distinct().count());
            assertTrue(spawned.stream().anyMatch(location -> location.getX() < x - 9));
            assertTrue(spawned.stream().anyMatch(location -> location.getX() > x + 9));
            assertTrue(spawned.stream().anyMatch(location -> location.getZ() < z - 9));
            assertTrue(spawned.stream().anyMatch(location -> location.getZ() > z + 9));
            for (int i = 0; i < spawned.size(); i++) {
                assertEquals(i % 2 == 0 ? Material.SAND : Material.RED_SAND, materials.getAllValues().get(i));
                Location first = spawned.get(i);
                for (int j = i + 1; j < spawned.size(); j++) {
                    Location second = spawned.get(j);
                    assertTrue(Math.abs(first.getX() - second.getX()) >= 1
                            || Math.abs(first.getY() - second.getY()) >= 1
                            || Math.abs(first.getZ() - second.getZ()) >= 1,
                            "Block volumes must not overlap");
                }
            }
        }
    }
}
