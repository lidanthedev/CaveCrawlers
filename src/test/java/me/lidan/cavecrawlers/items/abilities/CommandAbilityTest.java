package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandAbilityTest {
    private MockCaveCrawlers context;
    private AbilityManager manager;
    private Player player;
    private final List<CommandSender> senders = new ArrayList<>();
    private final List<String> args = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setAbilityManagerInstance(null);
        manager = AbilityManager.getInstance();
        manager.registerAbility("COMMAND", new CommandAbility());
        player = context.server().addPlayer();
        context.server().getCommandMap().register("test", new Command("record") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] arguments) {
                senders.add(sender);
                args.add(String.join(" ", arguments));
                return true;
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        setAbilityManagerInstance(null);
        context.close();
    }

    @Test
    void runsAsPlayerByDefaultWithPlayerPlaceholder() {
        assertTrue(use("COMMAND{command:\"/record hi %player%\"}"));
        assertSame(player, senders.getFirst());
        assertEquals("hi " + player.getName(), args.getFirst());
    }

    @Test
    void runsAsConsoleWhenConfigured() {
        assertTrue(use("COMMAND{command:\"record x\",console:true}"));
        assertInstanceOf(ConsoleCommandSender.class, senders.getFirst());
    }

    @Test
    void failsWithoutCommandOrForUnknownCommand() {
        assertFalse(use("COMMAND"));
        assertFalse(use("COMMAND{command:\"definitelyNotACommand\"}"));
        assertTrue(senders.isEmpty());
    }

    private boolean use(String id) {
        CommandAbility ability = (CommandAbility) manager.getAbilityByID(id);
        return ability.useAbility(new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, null, null, null));
    }

    private static void setAbilityManagerInstance(AbilityManager value) throws Exception {
        Field field = AbilityManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }
}
