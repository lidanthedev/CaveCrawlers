package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.damage.AbilityDamage;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageManager;
import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import me.lidan.cavecrawlers.damage.PlayerDamageCalculation;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsCalculateEvent;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsDamageCoverageTest {
    private MockCaveCrawlers context;
    private StatsManager statsManager;
    private ItemsManager itemsManager;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(StatsManager.class, "instance", null);
        setStatic(ItemsManager.class, "instance", null);
        statsManager = StatsManager.getInstance();
        itemsManager = ItemsManager.getInstance();
        player = context.server().addPlayer("fighter");
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(StatsManager.class, "instance", null);
        setStatic(ItemsManager.class, "instance", null);
        context.close();
    }

    @Test
    void baseStatsUseEveryRegisteredBaseValue() {
        Stats base = statsManager.calculateBaseStats();

        for (StatType type : StatType.values()) {
            assertEquals(type.getBase(), base.get(type).getValue(), type.name());
        }
    }

    @Test
    void statCalculationCapsSpeedAndAttackSpeedAndDoesNotAccumulateEquipment() throws Exception {
        ItemInfo sword = item("FAST_SWORD", ItemType.SWORD);
        sword.getStats().set(StatType.SPEED, 900);
        sword.getStats().set(StatType.ATTACK_SPEED, 900);
        player.getInventory().setItemInMainHand(tagged(sword));

        try (MockedStatic<PlayerDataManager> data = org.mockito.Mockito.mockStatic(PlayerDataManager.class)) {
            PlayerDataManager playerData = mock(PlayerDataManager.class);
            data.when(PlayerDataManager::getInstance).thenReturn(playerData);
            when(playerData.getStatsFromSkills(player)).thenReturn(new Stats());

            Stats first = statsManager.calculateStats(player);
            player.getInventory().setItemInMainHand(null);
            Stats second = statsManager.calculateStats(player);

            assertEquals(StatsManager.SPEED_LIMIT, first.get(StatType.SPEED).getValue());
            assertEquals(StatsManager.ATTACK_SPEED_LIMIT, first.get(StatType.ATTACK_SPEED).getValue());
            assertEquals(100, second.get(StatType.SPEED).getValue());
            assertEquals(0, second.get(StatType.ATTACK_SPEED).getValue());
        }
    }

    @Test
    void statsCalculateEventCarriesMutableResult() {
        Stats stats = new Stats();
        StatsCalculateEvent event = new StatsCalculateEvent(player, stats);
        event.getStats().set(StatType.DAMAGE, 42);

        assertSame(player, event.getPlayer());
        assertEquals(42, event.getStats().get(StatType.DAMAGE).getValue());
        assertTrue(event.getHandlers() != null);
    }

    @Test
    void statsCalculationPublishesOneMutableEventResult() {
        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            StatsCalculateEvent event = invocation.getArgument(0);
            event.getStats().set(StatType.DAMAGE, 99);
            return null;
        }).when(pluginManager).callEvent(any(StatsCalculateEvent.class));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS);
             MockedStatic<PlayerDataManager> data = org.mockito.Mockito.mockStatic(PlayerDataManager.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            PlayerDataManager playerData = mock(PlayerDataManager.class);
            data.when(PlayerDataManager::getInstance).thenReturn(playerData);
            when(playerData.getStatsFromSkills(player)).thenReturn(new Stats());

            Stats result = statsManager.calculateStats(player);

            assertEquals(99, result.get(StatType.DAMAGE).getValue());
            verify(pluginManager, times(1)).callEvent(any(StatsCalculateEvent.class));
        }
    }

    @Test
    void playerDamageFormulaUsesStrengthAndCritDamage() throws Exception {
        Stats stats = statsManager.getStats(player);
        stats.set(StatType.DAMAGE, 10);
        stats.set(StatType.STRENGTH, 50);
        stats.set(StatType.CRIT_CHANCE, 100);
        stats.set(StatType.CRIT_DAMAGE, 100);

        PlayerDamageCalculation calculation = new PlayerDamageCalculation(player);

        assertTrue(calculation.isCrit());
        assertEquals(45D, calculation.calculate(), 1e-9);
    }

    @Test
    void zeroCritChanceIsDeterministicallyNonCritical() {
        statsManager.getStats(player).set(StatType.CRIT_CHANCE, 0);

        PlayerDamageCalculation calculation = new PlayerDamageCalculation(player);

        assertFalse(calculation.isCrit());
    }

    @Test
    void abilityDamageUsesConfiguredScalingAndOptionalCrit() {
        Stats stats = statsManager.getStats(player);
        stats.set(StatType.INTELLIGENCE, 100);
        stats.set(StatType.ABILITY_DAMAGE, 25);
        stats.set(StatType.CRIT_DAMAGE, 50);

        AbilityDamage normal = new AbilityDamage(player, 100, 0.25);
        AbilityDamage critical = new AbilityDamage(player, 100, 0.25, StatType.INTELLIGENCE, true);

        assertEquals(62.5, normal.calculate(), 1e-9);
        assertEquals(93.75, critical.calculate(), 1e-9);
    }

    @Test
    void attackSpeedFormulaRespectsConfiguredCaps() {
        assertEquals(500, DamageManager.calculateAttackSpeed(0));
        assertEquals(250, DamageManager.calculateAttackSpeed(100));
        assertEquals(0, DamageManager.calculateAttackSpeed(200));
    }

    @Test
    void damageCalculationOverrideIsConsumedOnce() throws Exception {
        setStatic(DamageManager.class, "instance", null);
        DamageManager manager = DamageManager.getInstance();
        DamageCalculation override = new FinalDamageCalculation(12.5, true);
        manager.setDamageCalculation(player, override);

        assertSame(override, manager.getDamageCalculation(player));
        assertTrue(manager.getDamageCalculation(player) instanceof PlayerDamageCalculation);
    }

    private ItemInfo item(String id, ItemType type) {
        ItemInfo item = new ItemInfo(id, new Stats(), type, Material.DIAMOND, Rarity.COMMON);
        item.setID(id);
        itemsManager.registerItem(id, item);
        return item;
    }

    private ItemStack tagged(ItemInfo item) {
        ItemStack stack = new ItemStack(Material.DIAMOND);
        var meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(context.plugin(), ItemsManager.ITEM_ID),
                org.bukkit.persistence.PersistentDataType.STRING, item.getID());
        stack.setItemMeta(meta);
        return stack;
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
