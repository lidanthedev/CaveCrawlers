package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.gui.SellMenu;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SellMenuCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager itemsManager;
    private Player player;
    private Economy economy;
    private SellMenu menu;
    private Inventory inventory;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        itemsManager = mock(ItemsManager.class, Answers.RETURNS_DEFAULTS);
        player = context.server().addPlayer("seller");
        economy = mock(Economy.class);
        me.lidan.cavecrawlers.CaveCrawlers.economy = economy;

        YamlConfiguration pricesConfig = new YamlConfiguration();
        pricesConfig.set("prices.DIAMOND", 2.5D);
        pricesConfig.set("prices.GOLD", 0D);
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
        me.lidan.cavecrawlers.CaveCrawlers.economy = null;
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

        verify(economy, org.mockito.Mockito.times(1)).depositPlayer(player, 2.5D);
    }

    @Test
    void failedDepositReturnsSellableStackInsteadOfDestroyingIt() throws Exception {
        ItemStack sellable = new ItemStack(Material.DIAMOND, 3);
        when(itemsManager.getIDofItemStackSafe(sellable)).thenReturn("DIAMOND");
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{sellable});
        when(economy.depositPlayer(player, 7.5D)).thenReturn(
                new net.milkbowl.vault.economy.EconomyResponse(0, 0,
                        net.milkbowl.vault.economy.EconomyResponse.ResponseType.FAILURE, "declined"));

        invokeSell();

        verify(itemsManager).giveItemStacks(player, sellable);
    }

    private void invokeSell() throws Exception {
        Method sell = SellMenu.class.getDeclaredMethod("sell");
        sell.setAccessible(true);
        sell.invoke(menu);
    }

    private Object mockGui(Inventory inventory) {
        Object gui = mock(loadClass("dev.triumphteam.gui.guis.Gui"));
        when(((dev.triumphteam.gui.guis.Gui) gui).getInventory()).thenReturn(inventory);
        return gui;
    }

    private ItemInfo item(String id) {
        ItemInfo info = new ItemInfo(id, new me.lidan.cavecrawlers.stats.Stats(), ItemType.MATERIAL,
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
