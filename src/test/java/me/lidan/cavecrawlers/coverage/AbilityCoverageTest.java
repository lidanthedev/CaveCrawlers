package me.lidan.cavecrawlers.coverage;

import com.google.gson.JsonParser;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class AbilityCoverageTest {
    private MockCaveCrawlers context;
    private AbilityManager manager;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(AbilityManager.class, "instance", null);
        manager = AbilityManager.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(AbilityManager.class, "instance", null);
        context.close();
    }

    @Test
    void abilityIdsResolveBaseAndConfiguredVariants() {
        TestAbility base = new TestAbility("Base", "description", 10, 1000);
        manager.registerAbility("base", base);

        ItemAbility configured = manager.getAbilityByID("base{\"name\":\"Variant\",\"cost\":25,\"cooldown\":1200}");

        assertSame(base, manager.getAbilityByID("base"));
        assertEquals("Variant", configured.getName());
        assertEquals(25, configured.getCost());
        assertEquals(1200, configured.getCooldown());
        assertSame(configured, manager.getAbilityByID("base{\"name\":\"Variant\",\"cost\":25,\"cooldown\":1200}"));
        assertEquals("base", manager.getIDbyAbility(base));
    }

    @Test
    void malformedOrUnknownAbilityIdsReturnNull() {
        manager.registerAbility("base", new TestAbility("Base", "description", 10, 1000));

        assertNull(manager.getAbilityByID("missing"));
        assertNull(manager.getAbilityByID("base{not-json}"));
    }

    @Test
    void cooldownValuesAreClampedToMinimum() {
        TestAbility ability = new TestAbility("Base", "description", 10, 1);

        assertEquals(50, ability.getCooldown());
    }

    @Test
    void configuredAbilityHasIndependentCooldownState() {
        TestAbility base = new TestAbility("Base", "description", 10, 1000);
        manager.registerAbility("base", base);
        ItemAbility configured = manager.getAbilityByID("base{\"cost\":20}");

        assertNotSame(base.getAbilityCooldown(), configured.getAbilityCooldown());
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class TestAbility extends ItemAbility {
        private TestAbility(String name, String description, double cost, long cooldown) {
            super(name, description, cost, cooldown);
        }

        @Override
        protected boolean useAbility(PlayerEvent playerEvent) {
            return true;
        }
    }
}
