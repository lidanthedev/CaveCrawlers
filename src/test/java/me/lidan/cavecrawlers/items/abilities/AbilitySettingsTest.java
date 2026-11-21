package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Particle;
import org.bukkit.event.block.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilitySettingsTest {
    private MockCaveCrawlers context;
    private AbilityManager manager;
    private LaserAbility base;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setAbilityManagerInstance(null);
        manager = AbilityManager.getInstance();
        base = new LaserAbility("Laser", "Shoot Laser", 10, 100, Particle.END_ROD, 100, 0.1, 16);
        manager.registerAbility("BASIC_LASER", base);
    }

    @AfterEach
    void tearDown() throws Exception {
        setAbilityManagerInstance(null);
        context.close();
    }

    @Test
    void bindsEveryLevelOfTheHierarchy() throws Exception {
        LaserAbility laser = (LaserAbility) manager.getAbilityByID("BASIC_LASER{"
                + "name:\"Big Laser\",description:d,cost:5.5,cooldown:20,"
                + "allowedActions:[LEFT_CLICK_AIR],displayAction:\"SHIFT\","
                + "statToScale:strength,baseAbilityDamage:200,abilityScaling:0.5,crit:true,"
                + "particle:FLAME,range:32}");

        assertEquals("Big Laser", laser.getName());
        assertEquals("d", laser.getDescription());
        assertEquals(5.5, laser.getCost());
        assertEquals(20, laser.getCooldown());
        assertArrayEquals(new Action[]{Action.LEFT_CLICK_AIR}, laser.getAllowedActions());
        assertEquals("SHIFT", laser.getDisplayAction());
        assertSame(StatType.STRENGTH, laser.statToScale);
        assertEquals(200, laser.baseAbilityDamage);
        assertEquals(0.5, laser.abilityScaling);
        assertTrue(laser.crit);
        assertEquals(Particle.FLAME, setting(laser, "particle"));
        assertEquals(32, setting(laser, "range"));

        // base ability untouched
        assertEquals("Laser", base.getName());
        assertEquals(Particle.END_ROD, setting(base, "particle"));
    }

    @Test
    void keepsLegacyNumberCoercion() {
        // getAsLong truncates, like the old hand-written parser
        assertEquals(20, manager.getAbilityByID("BASIC_LASER{cooldown:20.9}").getCooldown());
    }

    @Test
    void ignoresUnknownKeys() {
        assertEquals("X", manager.getAbilityByID("BASIC_LASER{name:X,notAKey:1}").getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BASIC_LASER{cost:-1}", "BASIC_LASER{cooldown:-1}", "BASIC_LASER{particle:NOPE}", "BASIC_LASER{range:abc}"})
    void rejectsInvalidSettings(String id) {
        assertNull(manager.getAbilityByID(id));
    }

    @Test
    void exposesKeysInHierarchyOrder() {
        assertEquals(List.of("name", "description", "cost", "cooldown", "allowedActions", "displayAction",
                        "statToScale", "baseAbilityDamage", "abilityScaling", "crit", "particle", "range"),
                List.copyOf(AbilitySettings.fields(LaserAbility.class).keySet()));
    }

    private static Object setting(ItemAbility ability, String key) throws IllegalAccessException {
        Field field = AbilitySettings.fields(ability.getClass()).get(key);
        return field.get(ability);
    }

    private static void setAbilityManagerInstance(AbilityManager value) throws Exception {
        Field field = AbilityManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }
}
