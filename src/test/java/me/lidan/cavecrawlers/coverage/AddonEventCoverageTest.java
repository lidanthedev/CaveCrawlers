package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarUseEvent;
import me.lidan.cavecrawlers.damage.DamageCalculation;
import me.lidan.cavecrawlers.damage.DamageCalculationEvent;
import me.lidan.cavecrawlers.damage.FinalDamageCalculation;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropGiveEvent;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.items.ItemBuildEvent;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemUpdateEvent;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.PlayerItemAbilityUseEvent;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.listeners.DamageEntityListener;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockMineStartEvent;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopPurchaseEvent;
import me.lidan.cavecrawlers.shop.ShopSellEvent;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class AddonEventCoverageTest {
    private MockCaveCrawlers context;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        player = context.server().addPlayer("addon-events");
    }

    @AfterEach
    void tearDown() throws Exception {
        CaveCrawlers.economy = null;
        setStatic(ItemsManager.class, "instance", null);
        setStatic(StatsManager.class, "instance", null);
        context.close();
    }

    @Test
    void abilityUseEventCarriesMutableCostCooldownAndItemContext() {
        TestAbility ability = new TestAbility("Test", "description", 10, 1000);
        ItemStack stack = new ItemStack(Material.STICK);
        ItemInfo itemInfo = item("ABILITY_ITEM");
        PlayerItemAbilityUseEvent event = new PlayerItemAbilityUseEvent(player, ability, stack, itemInfo, 1000, 10);

        event.setCooldown(2500);
        event.setCost(2.5);
        event.setCancelled(true);

        assertSame(ability, event.getItemAbility());
        assertSame(stack, event.getItemStack());
        assertSame(itemInfo, event.getItemInfo());
        assertEquals(2500, event.getCooldown());
        assertEquals(2.5, event.getCost());
        assertTrue(event.isCancelled());
    }

    @Test
    void abilityActivationUsesAddonCostAndCooldownAndStopsWhenCooldownIsActive() throws Exception {
        setStatic(StatsManager.class, "instance", null);
        StatsManager.getInstance().getStats(player).get(StatType.MANA).setValue(10);
        TestAbility ability = new TestAbility("Test", "description", 10, 1000);
        ItemStack stack = new ItemStack(Material.STICK);
        player.getInventory().setItemInMainHand(stack);

        PluginManager pluginManager = mock(PluginManager.class);
        AtomicReference<PlayerItemAbilityUseEvent> received = new AtomicReference<>();
        doAnswer(invocation -> {
            PlayerItemAbilityUseEvent event = invocation.getArgument(0);
            received.set(event);
            event.setCost(3);
            event.setCooldown(5000);
            return null;
        }).when(pluginManager).callEvent(any(PlayerItemAbilityUseEvent.class));

        PlayerInteractEvent input = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, stack, null, null);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            ability.activateAbility(input);
            ability.activateAbility(input);
        }

        assertEquals(1, ability.uses);
        assertEquals(7, StatsManager.getInstance().getStats(player).get(StatType.MANA).getValue());
        assertEquals(Material.STICK, received.get().getItemStack().getType());
        assertEquals(1, received.get().getItemStack().getAmount());
        verify(pluginManager, org.mockito.Mockito.times(2)).callEvent(any(PlayerItemAbilityUseEvent.class));
    }

    @Test
    void damageListenerPublishesCancellableCalculationEvent() throws Exception {
        Mob target = mock(Mob.class);
        EntityDamageByEntityEvent nativeEvent = mock(EntityDamageByEntityEvent.class);
        DamageCalculation calculation = new FinalDamageCalculation(12, true);
        PluginManager pluginManager = mock(PluginManager.class);
        AtomicReference<DamageCalculationEvent> received = new AtomicReference<>();
        doAnswer(invocation -> {
            DamageCalculationEvent event = invocation.getArgument(0);
            received.set(event);
            event.setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(DamageCalculationEvent.class));

        Method applyDamage = DamageEntityListener.class.getDeclaredMethod(
                "damageMobAfterCalculation", EntityDamageByEntityEvent.class, Player.class, Mob.class,
                double.class, boolean.class, DamageCalculation.class);
        applyDamage.setAccessible(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            applyDamage.invoke(null, nativeEvent, player, target, 12D, true, calculation);
        }

        assertSame(calculation, received.get().getCalculation());
        assertEquals(12, received.get().getDamage());
        verify(nativeEvent).setCancelled(true);
    }

    @Test
    void shopPurchaseEventChangesTheTransactionBeforeSideEffects() throws Exception {
        ItemsManager itemsManager = mock(ItemsManager.class, Answers.RETURNS_DEFAULTS);
        setStatic(ItemsManager.class, "instance", itemsManager);
        ItemInfo result = item("RESULT");
        ItemInfo ingredient = item("INGREDIENT");
        when(itemsManager.hasItems(any(), any())).thenReturn(true);

        Economy economy = mock(Economy.class);
        CaveCrawlers.economy = economy;
        when(economy.getBalance(player)).thenReturn(100D);
        when(economy.withdrawPlayer(player, 5D)).thenReturn(
                new EconomyResponse(0, 95, EconomyResponse.ResponseType.SUCCESS, null));

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            ShopPurchaseEvent event = invocation.getArgument(0);
            event.setPrice(5);
            event.setResultAmount(3);
            event.getIngredients().clear();
            return null;
        }).when(pluginManager).callEvent(any(ShopPurchaseEvent.class));

        ShopItem shopItem = new ShopItem(result, 1, 25, Map.of(ingredient, 2));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            assertTrue(shopItem.buy(player, true));
        }

        verify(economy).withdrawPlayer(player, 5D);
        verify(itemsManager).removeItems(player, Map.of());
        verify(itemsManager).giveItem(player, result, 3);
    }

    @Test
    void allOtherAddonEventsExposeTheirMutableOrCancellableState() {
        ItemStack sellable = new ItemStack(Material.DIAMOND);
        ShopSellEvent sell = new ShopSellEvent(player, List.of(sellable), List.of(new ItemStack(Material.DIRT)), 2.5, false);
        sell.setPrice(7.5);
        sell.setTrashUnsellable(true);
        sell.setCancelled(true);

        World world = context.server().addSimpleWorld("addon-events-world");
        Block block = world.getBlockAt(0, 64, 0);
        ItemInfo tool = item("PICKAXE");
        BlockMineStartEvent mine = new BlockMineStartEvent(player, block, new BlockInfo(10, 0, List.of()), tool, 20);
        mine.setRequiredTicks(3);
        mine.setCancelled(true);

        DropGiveEvent drop = new DropGiveEvent(new Drop(DropType.COINS, 100, "1", null), player, new Location(world, 0, 64, 0));
        drop.setCancelled(true);

        AltarUseEvent altar = new AltarUseEvent(player, new Altar(), block, tool, new ItemStack(Material.DIAMOND), 100, true);
        altar.setPoints(250);
        altar.setCancelled(true);

        assertEquals(7.5, sell.getPrice());
        assertTrue(sell.isTrashUnsellable());
        assertTrue(sell.isCancelled());
        assertEquals(3, mine.getRequiredTicks());
        assertTrue(mine.isCancelled());
        assertTrue(drop.isCancelled());
        assertEquals(250, altar.getPoints());
        assertTrue(altar.isFinalPlacement());
        assertTrue(altar.isCancelled());
    }

    @Test
    void itemBuildAndUpdateEventsAllowReplacementStacks() {
        ItemStack original = new ItemStack(Material.STONE);
        ItemStack built = new ItemStack(Material.PAPER);
        ItemStack replacement = new ItemStack(Material.GOLD_INGOT);
        ItemInfo itemInfo = item("REPLACED");

        ItemBuildEvent build = new ItemBuildEvent(original, built, itemInfo);
        ItemUpdateEvent update = new ItemUpdateEvent(original, built, itemInfo);
        build.setBuiltItem(replacement);
        update.setBuiltItem(replacement);

        assertSame(replacement, build.getBuiltItem());
        assertSame(replacement, update.getBuiltItem());
    }

    private ItemInfo item(String id) {
        ItemInfo item = new ItemInfo(id, new Stats(), ItemType.MATERIAL, Material.DIAMOND, Rarity.COMMON);
        item.setID(id);
        return item;
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class TestAbility extends ItemAbility {
        private int uses;

        private TestAbility(String name, String description, double cost, long cooldown) {
            super(name, description, cost, cooldown);
        }

        @Override
        protected boolean useAbility(org.bukkit.event.player.PlayerEvent playerEvent) {
            uses++;
            return true;
        }
    }
}
