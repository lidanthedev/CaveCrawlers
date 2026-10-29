package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.gui.SellMenu;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.shop.ShopSellEvent;
import me.lidan.cavecrawlers.shop.ShopTransactionCompletedEvent;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class SellMenuCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager itemsManager;
    private PlayerMock player;
    private Economy economy;
    private SellMenu menu;
    private Inventory inventory;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        itemsManager = mock(ItemsManager.class, Answers.RETURNS_DEFAULTS);
        player = context.server().addPlayer("seller");
        economy = mock(Economy.class);
        CaveCrawlers.economy = economy;
        when(economy.depositPlayer(any(OfflinePlayer.class), anyDouble())).thenReturn(
                new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, null));

        YamlConfiguration pricesConfig = new YamlConfiguration();
        pricesConfig.set("prices.DIAMOND", 2.5D);
        pricesConfig.set("prices.GOLD", 0D);
        pricesConfig.set("prices.FRACTIONAL", 2.59D);
        pricesConfig.set("prices.SUB_TENTH", 0.09D);
        ConfigurationSection prices = pricesConfig.getConfigurationSection("prices");
        inventory = mock(Inventory.class);
        menu = allocateMenu();
        setField(menu, SellMenu.class, "itemsManager", itemsManager);
        setField(menu, SellMenu.class, "prices", prices);
        setField(menu, SellMenu.class, "player", player);
        setField(menu, SellMenu.class, "gui", mockGui(inventory));
    }

    @AfterEach
    void tearDown() throws Exception {
        CaveCrawlers.economy = null;
        context.close();
    }

    @Test
    void priceIsReadFromCanonicalItemIdAndScaledByStackAmount() {
        when(itemsManager.getIDofItemStackSafe(any(ItemStack.class))).thenReturn("DIAMOND");

        assertEquals(7.5D, menu.getPrice(new ItemStack(Material.DIAMOND, 3)));
        assertEquals(2.5D, menu.getPrice("DIAMOND"));
        assertEquals(0D, menu.getPrice("UNKNOWN"));
    }

    @Test
    void previewAggregatesOnlySellableStacks() {
        ItemInfo diamond = item("DIAMOND");
        ItemInfo gold = item("GOLD");
        when(itemsManager.getAllItems(inventory)).thenReturn(Map.of(diamond, 3, gold, 2));

        assertEquals(7.5D, menu.getTotalPrice());
        assertEquals(1, menu.getPrices().size());
        assertEquals(diamond, menu.getPrices().getFirst().itemInfo());
        assertEquals(3, menu.getPrices().getFirst().amount());
    }

    @Test
    void sellingReturnsUnsellableStacksAndPaysSellableStacksOnce() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND, 3);
        ItemStack unsellable = new ItemStack(Material.DIRT, 2);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("DIAMOND");
        when(itemsManager.getIDofItemStackSafe(unsellable)).thenReturn("DIRT");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable, unsellable});

        invokeSell();

        verify(economy).depositPlayer(player, 7.5D);
        verify(itemsManager).giveItemStacks(player, unsellable);
        verify(inventory).clear();
    }

    @Test
    void repeatedSellAfterInventoryClearDoesNotReplayPayout() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND, 1);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("DIAMOND");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable}, new ItemStack[0]);

        invokeSell();
        invokeSell();

        verify(economy, times(1)).depositPlayer(player, 2.5D);
    }

    @Test
    void failedDepositReturnsSellableStackInsteadOfDestroyingIt() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND, 3);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("DIAMOND");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable});
        when(economy.depositPlayer(player, 7.5D)).thenReturn(
                new EconomyResponse(0, 0,
                        EconomyResponse.ResponseType.FAILURE, "declined"));

        invokeSell();

        verify(itemsManager).giveItemStacks(player, sellable);
    }

    @Test
    void saleUsesNormalizedTotalForDepositAndMessage() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("FRACTIONAL");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable});

        invokeSell();

        verify(economy).depositPlayer(player, 2.5D);
        assertEquals("Sold items for 2.5 coins", PlainTextComponentSerializer.plainText()
                .serialize(player.nextComponentMessage()));
    }

    @ParameterizedTest
    @CsvSource({"2.59, 2.5", "0.09, 0"})
    void listenerAdjustedSaleReportsTheNormalizedPayout(double acceptedPrice, double payout) throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("DIAMOND");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable});
        PluginManager manager = mock(PluginManager.class);
        doAnswer(invocation -> {
            ((ShopSellEvent) invocation.getArgument(0)).setPrice(acceptedPrice);
            return null;
        }).when(manager).callEvent(any(ShopSellEvent.class));

        try (var bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            invokeSell();
        }

        verify(economy).depositPlayer(player, payout);
        var completed = ArgumentCaptor.forClass(ShopTransactionCompletedEvent.class);
        verify(manager).callEvent(completed.capture());
        assertEquals(payout, completed.getValue().getPrice());
        assertEquals(Map.of("DIAMOND", 1), completed.getValue().getItems());
        verify(itemsManager, never()).giveItemStacks(player, sellable);
        assertEquals("Sold items for " + (payout == 0 ? "0" : "2.5") + " coins",
                PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage()));
    }

    @Test
    void subTenthSaleIsRefundedWithoutDepositingZero() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("SUB_TENTH");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable});

        invokeSell();

        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(itemsManager).giveItemStacks(player, sellable);
    }

    private void invokeSell() throws Exception {
        Method sell = SellMenu.class.getDeclaredMethod("sell", Boolean.TYPE);
        sell.setAccessible(true);
        sell.invoke(menu, false);
    }

    private Object mockGui(Inventory inventory) {
        Object gui = mock(loadClass("dev.triumphteam.gui.guis.Gui"));
        when(((dev.triumphteam.gui.guis.Gui) gui).getInventory()).thenReturn(inventory);
        return gui;
    }

    private ItemInfo item(String id) {
        ItemInfo info = new ItemInfo(id, new Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        info.setID(id);
        return info;
    }

    private static SellMenu allocateMenu() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        return (SellMenu) unsafe.allocateInstance(SellMenu.class);
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static void setField(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
