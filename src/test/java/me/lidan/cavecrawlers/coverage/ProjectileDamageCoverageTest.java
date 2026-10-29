package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageCalculationEvent;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.DamageResolvedEvent;
import me.lidan.cavecrawlers.listeners.DamageEntityListener;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.Holograms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectileDamageCoverageTest {
    private MockCaveCrawlers context;
    private Player player;

    @BeforeEach void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        player = context.server().addPlayer("projectile-events");
    }

    @AfterEach void tearDown() throws Exception {
        context.close();
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void apiProjectilePreservesAbilityProvenanceThroughResolution(boolean ability, boolean withVelocity) {
        DamageCalculation calculation = ability ? mock(AbilityDamage.class) : mock(DamageCalculation.class);
        when(calculation.calculate()).thenReturn(12D);
        when(calculation.isCrit()).thenReturn(true);
        ProjectileSource source = mock(ProjectileSource.class);
        Arrow projectile = mock(Arrow.class);
        when(projectile.getPersistentDataContainer()).thenReturn(player.getPersistentDataContainer());
        when(projectile.getShooter()).thenReturn(player);
        Vector velocity = new Vector(1, 0, 0);
        if (withVelocity) when(source.launchProjectile(Arrow.class, velocity)).thenReturn(projectile);
        else when(source.launchProjectile(Arrow.class)).thenReturn(projectile);
        Mob target = mock(Mob.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        EntityDamageByEntityEvent hit = mock(EntityDamageByEntityEvent.class);
        when(hit.getDamager()).thenReturn(projectile);
        when(hit.getEntity()).thenReturn(target);
        when(hit.getCause()).thenReturn(DamageCause.PROJECTILE);
        when(hit.getFinalDamage()).thenReturn(7D);
        PluginManager events = mock(PluginManager.class);
        DamageEntityListener listener = new DamageEntityListener();

        try (var bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             var holograms = mockStatic(Holograms.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(events);
            Arrow launched = withVelocity
                    ? DamageManager.getInstance().launchProjectile(source, Arrow.class, calculation, velocity)
                    : DamageManager.getInstance().launchProjectile(source, Arrow.class, calculation);
            assertSame(projectile, launched);
            listener.onEntityDamageByEntity(hit);
            listener.onDamageResolved(hit);
            listener.onDamageResolved(hit);
        }

        var prepared = ArgumentCaptor.forClass(DamageCalculationEvent.class);
        verify(events).callEvent(prepared.capture());
        assertNull(prepared.getValue().getCalculation());
        assertEquals(12D, prepared.getValue().getDamage());
        var resolved = ArgumentCaptor.forClass(DamageResolvedEvent.class);
        verify(events).callEvent(resolved.capture());
        assertEquals(ability, resolved.getValue().isAbility());
        assertTrue(resolved.getValue().isCritical());
        assertEquals(7D, resolved.getValue().getDamage());
    }

    @Test void nonProjectileResolutionKeepsTheCalculationBasedAbilityCheck() throws Exception {
        Mob target = mock(Mob.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        EntityDamageByEntityEvent hit = mock(EntityDamageByEntityEvent.class);
        when(hit.getDamager()).thenReturn(player);
        when(hit.getFinalDamage()).thenReturn(7D);
        PluginManager events = mock(PluginManager.class);
        DamageCalculation calculation = mock(AbilityDamage.class);
        Method apply = DamageEntityListener.class.getDeclaredMethod("damageMobAfterCalculation",
                EntityDamageByEntityEvent.class, Player.class, Mob.class, double.class, boolean.class,
                DamageCalculation.class);
        apply.setAccessible(true);
        try (var bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             var holograms = mockStatic(Holograms.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(events);
            apply.invoke(null, hit, player, target, 12D, false, calculation);
            new DamageEntityListener().onDamageResolved(hit);
        }
        var resolved = ArgumentCaptor.forClass(DamageResolvedEvent.class);
        verify(events).callEvent(resolved.capture());
        assertTrue(resolved.getValue().isAbility());
    }
}
