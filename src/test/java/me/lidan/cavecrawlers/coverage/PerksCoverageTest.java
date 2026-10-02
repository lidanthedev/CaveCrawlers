package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksManager;
import me.lidan.cavecrawlers.listeners.PerksListener;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsCalculateEvent;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerksCoverageTest {
    private MockCaveCrawlers context;
    private PerksManager manager;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(PerksManager.class, "instance", null);
        manager = PerksManager.getInstance();
        player = context.server().addPlayer("perk-player");
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(PerksManager.class, "instance", null);
        context.close();
    }

    @Test
    void highestPermittedPerkWinsPerTrack() {
        Perk low = perk("low", "combat", "perk.low", 1, 2);
        Perk high = perk("high", "combat", "perk.high", 5, 7);
        Perk mining = perk("mining", "mining", "perk.mining", 1, 11);
        manager.register("low", low);
        manager.register("high", high);
        manager.register("mining", mining);
        player.addAttachment(context.plugin(), "perk.low", true);
        player.addAttachment(context.plugin(), "perk.high", true);
        player.addAttachment(context.plugin(), "perk.mining", false);

        Map<String, Perk> selected = manager.getPerks(player);

        assertEquals(Map.of("combat", high), selected);
    }

    @Test
    void perkListenerAddsSelectedStatsExactlyOnce() {
        Perk perk = perk("combat", "combat", "perk.combat", 1, 9);
        manager.register("combat", perk);
        player.addAttachment(context.plugin(), "perk.combat", true);
        Stats stats = new Stats();

        new PerksListener().onStatsCalculate(new StatsCalculateEvent(player, stats));

        assertEquals(9, stats.get(StatType.DAMAGE).getValue());
    }

    private Perk perk(String name, String track, String permission, int priority, double damage) {
        Stats stats = new Stats();
        stats.set(StatType.DAMAGE, damage);
        return new Perk(name, track, permission, priority, stats);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
