package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiClickAbilityTest {
    private MockCaveCrawlers context;
    private AbilityManager manager;
    private final List<String> used = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setAbilityManagerInstance(null);
        manager = AbilityManager.getInstance();
        manager.registerAbility("LASER", new RecordingClickAbility("Laser", used));
        manager.registerAbility("MULTI_ABILITY", new MultiClickAbility());
    }

    @AfterEach
    void tearDown() throws Exception {
        setAbilityManagerInstance(null);
        context.close();
    }

    @Test
    void multiAbilityListensToEveryChildActionAndDispatchesByAction() {
        MultiClickAbility multi = (MultiClickAbility) manager.getAbilityByID(
                "MULTI_ABILITY{abilities=[\"LASER\", \"LASER{name:\\\"Left\\\",allowedActions:[\\\"LEFT_CLICK_AIR\\\"]}\"]}");

        List<Action> actions = Arrays.asList(multi.getAllowedActions());
        assertTrue(actions.contains(Action.RIGHT_CLICK_AIR));
        assertTrue(actions.contains(Action.LEFT_CLICK_AIR));

        multi.useAbility(click(Action.LEFT_CLICK_AIR));
        assertEquals(List.of("Left"), used);

        used.clear();
        multi.useAbility(click(Action.RIGHT_CLICK_AIR));
        assertEquals(List.of("Laser"), used);
    }

    @Test
    void childWithMalformedSettingsIsSkipped() {
        // missing closing brace on the second child's settings
        MultiClickAbility multi = (MultiClickAbility) manager.getAbilityByID(
                "MULTI_ABILITY{abilities=[\"LASER\", \"LASER{allowedActions:[\\\"LEFT_CLICK_AIR\\\"]\"]}");

        assertEquals(List.of(Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK), Arrays.asList(multi.getAllowedActions()));
    }

    private PlayerInteractEvent click(Action action) {
        return new PlayerInteractEvent(context.server().addPlayer(), action, null, null, null);
    }

    private static void setAbilityManagerInstance(AbilityManager value) throws Exception {
        Field field = AbilityManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class RecordingClickAbility extends ClickAbility {
        private final List<String> used;

        private RecordingClickAbility(String name, List<String> used) {
            super(name, "description", 0, 0);
            this.used = used;
        }

        @Override
        public void activateAbility(PlayerEvent playerEvent) {
            used.add(getName());
        }

        @Override
        protected boolean useAbility(PlayerEvent playerEvent) {
            return true;
        }
    }
}
