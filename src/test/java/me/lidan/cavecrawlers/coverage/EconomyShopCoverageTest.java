package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        assertTrue(shopItem.toList().stream().anyMatch(line -> line.contains("2.9 Coins")));
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

        assertFalse(shopItem.toList().stream().anyMatch(line -> line.contains("Coins")));
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

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
