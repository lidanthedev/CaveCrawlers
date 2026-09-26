package me.lidan.cavecrawlers.coverage;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.AutoFullShopAbility;
import me.lidan.cavecrawlers.items.abilities.AutoPortableShopAbility;
import me.lidan.cavecrawlers.items.abilities.PortableShopAbility;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.shop.ShopPurchaseEvent;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EconomyShopCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager itemsManager;
    private ItemsManager itemsMock;
    private Economy economy;
    private Player player;
    private ItemInfo result;
    private ItemInfo ingredient;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        setStatic(ShopManager.class, "instance", null);
        itemsManager = ItemsManager.getInstance();
        itemsMock = mock(ItemsManager.class, Answers.RETURNS_DEFAULTS);
        setStatic(ItemsManager.class, "instance", itemsMock);
        economy = mock(Economy.class);
        CaveCrawlers.economy = economy;
        player = context.server().addPlayer("buyer");
        when(economy.getBalance(player)).thenReturn(100D);
        when(economy.withdrawPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(
                new EconomyResponse(0, 100, EconomyResponse.ResponseType.SUCCESS, null));
        result = item("RESULT");
        ingredient = item("INGREDIENT");
        when(itemsMock.hasItems(any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        CaveCrawlers.economy = null;
        setStatic(ItemsManager.class, "instance", null);
        setStatic(ShopManager.class, "instance", null);
        context.close();
    }

    @Test
    void vaultHelpersRoundToOneDecimalBeforeCallingProvider() {
        VaultUtils.giveCoins(player, 1.29);
        VaultUtils.takeCoins(player, 2.99);

        verify(economy).depositPlayer(player, 1.2);
        verify(economy).withdrawPlayer(player, 2.9);
    }

    @Test
    void shopPurchaseMutatesEachResourceExactlyOnceOnSuccess() {
        ShopItem shopItem = new ShopItem(result, 2, 25, Map.of(ingredient, 3));

        assertTrue(shopItem.buy(player, true));

        verify(economy).withdrawPlayer(player, 25D);
        verify(itemsMock).removeItems(player, Map.of(ingredient, 3));
        verify(itemsMock).giveItem(player, result, 2);
    }

    @Test
    void missingIngredientsPreventEveryPurchaseSideEffect() {
        when(itemsMock.hasItems(any(), any())).thenReturn(false);
        ShopItem shopItem = new ShopItem(result, 1, 25, Map.of(ingredient, 3));

        assertFalse(shopItem.buy(player, true));

        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        verify(itemsMock, never()).removeItems(any(Player.class), any(Map.class));
        verify(itemsMock, never()).giveItem(any(Player.class), any(ItemInfo.class), anyInt());
    }

    @Test
    void failedVaultWithdrawalMustNotConsumeIngredientsOrGrantResult() {
        when(economy.withdrawPlayer(player, 25D)).thenReturn(
                new EconomyResponse(0, 100, EconomyResponse.ResponseType.FAILURE, "declined"));
        ShopItem shopItem = new ShopItem(result, 1, 25, Map.of(ingredient, 3));

        assertFalse(shopItem.buy(player, true));

        verify(itemsMock, never()).removeItems(any(Player.class), any(Map.class));
        verify(itemsMock, never()).giveItem(any(Player.class), any(ItemInfo.class), anyInt());
    }

    @Test
    void subTenthPriceCannotNormalizeIntoAFreePurchase() {
        ShopItem shopItem = new ShopItem(result, 1, 0.09, Map.of());

        assertFalse(shopItem.canBuy(player));
        assertFalse(shopItem.buy(player, true));

        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        verify(itemsMock, never()).giveItem(any(Player.class), any(ItemInfo.class), anyInt());
    }

    @Test
    void extraDecimalPriceUsesNormalizedValueEverywhere() {
        ShopItem shopItem = new ShopItem(result, 1, 2.99, Map.of());

        assertTrue(shopItem.canBuy(player));
        assertTrue(shopItem.buy(player, true));

        verify(economy).withdrawPlayer(player, 2.9D);
        assertTrue(shopItem.toList(displayItem()).stream().anyMatch(line -> line.contains("2.9 Coins")));
    }

    @ParameterizedTest
    @MethodSource("invalidPrices")
    void nonFiniteOrNegativePricesCannotBePurchased(double price) {
        ShopItem shopItem = new ShopItem(result, 1, price, Map.of());

        assertFalse(shopItem.canBuy(player));
    }

    @Test
    void zeroPriceWithNoIngredientsIsExplicitlyFree() {
        ShopItem shopItem = new ShopItem(result, 1, 0, Map.of());

        assertTrue(shopItem.canBuy(player));
        assertTrue(shopItem.buy(player, true));
        verify(itemsMock).giveItem(player, result, 1);
    }

    @Test
    void ingredientOnlyPriceDoesNotDisplayZeroCoins() {
        ShopItem shopItem = new ShopItem(result, ingredient, 3);

        assertFalse(shopItem.toList(displayItem()).stream().anyMatch(line -> line.contains("Coins")));
    }

    @Test
    void automaticPortablePurchaseUsesEventAdjustedPrice() throws Exception {
        when(economy.getBalance(player)).thenReturn(50D);
        ShopItem shopItem = new ShopItem(result, 1, 100, Map.of());
        registerShop(shopItem);

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            ShopPurchaseEvent event = invocation.getArgument(0);
            event.setPrice(5);
            return null;
        }).when(pluginManager).callEvent(any(ShopPurchaseEvent.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            new TestAutoPortableShopAbility().buy(player, autoShopItem());
        }

        verify(economy).withdrawPlayer(player, 5D);
        verify(itemsMock).giveItem(player, result, 1);
        verify(pluginManager, times(1)).callEvent(any(ShopPurchaseEvent.class));
    }

    @Test
    void automaticFullPurchaseUsesEventAdjustedIngredients() throws Exception {
        ItemInfo replacementIngredient = item("REPLACEMENT_INGREDIENT");
        ShopItem shopItem = new ShopItem(result, 1, 0, Map.of(ingredient, 2));
        registerShop(shopItem);

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            ShopPurchaseEvent event = invocation.getArgument(0);
            event.setIngredients(Map.of(replacementIngredient, 1));
            return null;
        }).when(pluginManager).callEvent(any(ShopPurchaseEvent.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            new TestAutoFullShopAbility().buy(player, autoShopItem());
        }

        verify(itemsMock).removeItems(player, Map.of(replacementIngredient, 1));
        verify(itemsMock).giveItem(player, result, 1);
        verify(pluginManager, times(1)).callEvent(any(ShopPurchaseEvent.class));
    }

    @Test
    void automaticPurchaseCancellationPreventsSideEffects() throws Exception {
        ShopItem shopItem = new ShopItem(result, 1, 0, Map.of());
        registerShop(shopItem);

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            ShopPurchaseEvent event = invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(ShopPurchaseEvent.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            new TestAutoFullShopAbility().buy(player, autoShopItem());
        }

        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
        verify(itemsMock, never()).removeItems(any(Player.class), any(Map.class));
        verify(itemsMock, never()).giveItem(any(Player.class), any(ItemInfo.class), anyInt());
        verify(pluginManager, times(1)).callEvent(any(ShopPurchaseEvent.class));
    }

    private static Stream<Double> invalidPrices() {
        return Stream.of(-1D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    private ItemInfo item(String id) {
        ItemInfo item = new ItemInfo(id, new me.lidan.cavecrawlers.stats.Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        when(itemsMock.getItemByID(id)).thenReturn(item);
        item.setID(id);
        return item;
    }

    private ItemStack displayItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(result.getFormattedName());
        meta.setLore(List.of("item"));
        item.setItemMeta(meta);
        return item;
    }

    private void registerShop(ShopItem... shopItems) {
        ShopMenu shopMenu = new ShopMenu("test", java.util.Arrays.asList(shopItems));
        ShopManager.getInstance().registerMenu("test", shopMenu);
    }

    private ItemStack autoShopItem() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemNbt.setString(item, PortableShopAbility.PORTABLE_SHOP_ID, "test");
        ItemNbt.setString(item, AutoPortableShopAbility.PORTABLE_SHOP_ITEM, "0");
        return item;
    }

    private static final class TestAutoPortableShopAbility extends AutoPortableShopAbility {
        private void buy(Player player, ItemStack item) {
            buyAutomatically(player, item);
        }
    }

    private static final class TestAutoFullShopAbility extends AutoFullShopAbility {
        private void buy(Player player, ItemStack item) {
            buyAutomatically(player, item);
        }
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
