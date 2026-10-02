package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.commands.SellCommand;
import me.lidan.cavecrawlers.commands.StatCommand;
import me.lidan.cavecrawlers.gui.SellMenu;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommandBoundaryCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager itemsManager;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        setStatic(StatsManager.class, "instance", null);
        itemsManager = ItemsManager.getInstance();
        player = context.server().addPlayer("command-player");
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        setStatic(StatsManager.class, "instance", null);
        context.close();
    }

    @Test
    void setPriceUsesCanonicalItemIdAndPersistsValue() {
        ItemInfo item = new ItemInfo("Diamond", new Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        itemsManager.registerItem("DIAMOND", item);
        SellCommand command = new SellCommand();

        command.setPrice(player, 3.5, "DIAMOND");

        assertEquals(3.5, SellMenu.config.getDouble("prices.DIAMOND"));
    }

    @Test
    void unknownItemPriceDoesNotCreateConfigurationEntry() {
        SellCommand command = new SellCommand();
        SellMenu.config.set("prices.UNKNOWN", null);

        command.setPrice(player, 3.5, "UNKNOWN");

        assertEquals(0, SellMenu.config.getDouble("prices.UNKNOWN", 0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidPriceValuesAreRejectedBeforeConfigurationMutation(double price) {
        ItemInfo item = new ItemInfo("Diamond", new Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        itemsManager.registerItem("DIAMOND", item);
        SellMenu.config.set("prices.DIAMOND", null);

        new SellCommand().setPrice(player, price, "DIAMOND");

        assertFalse(SellMenu.config.contains("prices.DIAMOND"));
    }

    @Test
    void statCommandsMutateOnlyTheTargetAdder() throws Exception {
        StatsManager manager = StatsManager.getInstance();
        Stats targetStats = new Stats();
        Field adders = StatsManager.class.getDeclaredField("statsAdder");
        adders.setAccessible(true);
        @SuppressWarnings("unchecked")
        var map = (java.util.Map<java.util.UUID, Stats>) adders.get(manager);
        map.put(player.getUniqueId(), targetStats);

        StatCommand command = new StatCommand();
        command.add(player, StatType.DAMAGE, 8, player);
        command.set(player, StatType.STRENGTH, 11, player);

        assertEquals(8, targetStats.get(StatType.DAMAGE).getValue());
        assertEquals(11, targetStats.get(StatType.STRENGTH).getValue());
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
