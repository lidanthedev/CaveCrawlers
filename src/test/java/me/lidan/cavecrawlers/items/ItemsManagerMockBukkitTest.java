package me.lidan.cavecrawlers.items;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemsManagerMockBukkitTest {
    private ServerMock server;
    private CaveCrawlers plugin;
    private FileConfiguration config;
    private MockedStatic<JavaPlugin> javaPlugin;
    private ItemsManager itemsManager;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = createPlugin(server);
        config = plugin.getConfig();
        javaPlugin = org.mockito.Mockito.mockStatic(JavaPlugin.class);
        javaPlugin.when(() -> JavaPlugin.getPlugin(CaveCrawlers.class)).thenReturn(plugin);

        setStatic(ItemsManager.class, "instance", null);
        itemsManager = ItemsManager.getInstance();
        setStatic(Altar.class, "itemsManager", itemsManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        javaPlugin.close();
        MockBukkit.unmock();
    }

    @Test
    void nullStackIsNotRecognized() {
        assertNull(itemsManager.getItemFromItemStackSafe(null));
    }

    @Test
    void canonicalPdcIdIsRecognizedRegardlessOfDisplayName() {
        ItemInfo item = registerItem("CUSTOM_SWORD", "Custom Sword", ItemType.SWORD);
        ItemStack stack = namedItem(Material.DIAMOND, "A misleading name");
        setItemId(stack, item.getID());

        assertSame(item, itemsManager.getItemFromItemStackSafe(stack));
    }

    @Test
    void displayNameAloneDoesNotIdentifyACustomItem() {
        registerItem("GOLD_INGOT", "Gold Ingot", ItemType.MATERIAL);
        ItemStack stack = namedItem(Material.GOLD_INGOT, "§7Gold Ingot");

        assertNull(itemsManager.getItemFromItemStackSafe(stack));
    }

    @Test
    void unknownCanonicalIdDoesNotFallBackToDisplayName() {
        registerItem("GOLD_INGOT", "Gold Ingot", ItemType.MATERIAL);
        ItemStack stack = namedItem(Material.GOLD_INGOT, "§7Gold Ingot");
        setItemId(stack, "REMOVED_ITEM");

        assertNull(itemsManager.getItemFromItemStackSafe(stack));
    }

    @Test
    void customBuildAndUpdateDispatchOnlyTheirOwnEventsAndReturnReplacements() {
        ItemInfo item = registerItem("CUSTOM_ITEM", "Custom Item", ItemType.MATERIAL);
        ItemStack buildReplacement = new ItemStack(Material.GOLD_INGOT);
        ItemStack updateReplacement = new ItemStack(Material.EMERALD);
        List<String> eventOrder = new ArrayList<>();
        AtomicReference<ItemStack> builtBeforeReplacement = new AtomicReference<>();

        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            if (event instanceof ItemBuildEvent buildEvent) {
                eventOrder.add("build");
                buildEvent.setBuiltItem(buildReplacement);
            }
            if (event instanceof ItemUpdateEvent updateEvent) {
                eventOrder.add("update");
                builtBeforeReplacement.set(updateEvent.getBuiltItem());
                updateEvent.setBuiltItem(updateReplacement);
            }
            return null;
        }).when(pluginManager).callEvent(any(Event.class));

        ItemBuilder itemBuilderMock = mock(ItemBuilder.class, org.mockito.Answers.RETURNS_SELF);
        AtomicReference<ItemStack> builderInput = new AtomicReference<>();
        when(itemBuilderMock.amount(anyInt())).thenAnswer(invocation -> {
            builderInput.get().setAmount(invocation.getArgument(0));
            return itemBuilderMock;
        });
        when(itemBuilderMock.build()).thenAnswer(invocation -> builderInput.get().clone());

        ItemStack original = namedItem(Material.DIAMOND, "old name");
        NamespacedKey addonKey = new NamespacedKey(plugin, "addon-tag");
        ItemMeta originalMeta = original.getItemMeta();
        originalMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, ItemsManager.ITEM_ID), PersistentDataType.STRING, item.getID());
        originalMeta.getPersistentDataContainer().set(addonKey, PersistentDataType.STRING, "kept");
        originalMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
        original.setItemMeta(originalMeta);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             MockedStatic<ItemBuilder> itemBuilder = mockStatic(ItemBuilder.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            itemBuilder.when(() -> ItemBuilder.from(any(ItemStack.class))).thenAnswer(invocation -> {
                builderInput.set(invocation.getArgument(0));
                return itemBuilderMock;
            });

            assertSame(buildReplacement, itemsManager.buildItem(item, 2));
            assertEquals(List.of("build"), eventOrder);

            eventOrder.clear();
            assertSame(updateReplacement, itemsManager.updateItemStack(original));
        }

        assertEquals(List.of("update"), eventOrder);
        assertNotNull(builtBeforeReplacement.get());
        assertEquals(3, builtBeforeReplacement.get().getEnchantmentLevel(Enchantment.UNBREAKING));
        assertEquals("kept", builtBeforeReplacement.get().getItemMeta()
                .getPersistentDataContainer().get(addonKey, PersistentDataType.STRING));
    }

    @Test
    void vanillaBuildDispatchesItemBuildEvent() {
        AtomicReference<ItemBuildEvent> received = new AtomicReference<>();
        PluginManager pluginManager = mock(PluginManager.class);
        doAnswer(invocation -> {
            received.set(invocation.getArgument(0));
            return null;
        }).when(pluginManager).callEvent(any(ItemBuildEvent.class));

        ItemBuilder itemBuilderMock = mock(ItemBuilder.class, org.mockito.Answers.RETURNS_SELF);
        AtomicReference<ItemStack> builderInput = new AtomicReference<>();
        when(itemBuilderMock.amount(anyInt())).thenAnswer(invocation -> {
            builderInput.get().setAmount(invocation.getArgument(0));
            return itemBuilderMock;
        });
        when(itemBuilderMock.build()).thenAnswer(invocation -> builderInput.get().clone());

        ItemStack built;
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
             MockedStatic<ItemBuilder> itemBuilder = mockStatic(ItemBuilder.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            itemBuilder.when(() -> ItemBuilder.from(any(ItemStack.class))).thenAnswer(invocation -> {
                builderInput.set(invocation.getArgument(0));
                return itemBuilderMock;
            });
            built = itemsManager.buildItem(new VanillaItemInfo(Material.DIAMOND), 2);
        }

        assertEquals(Material.DIAMOND, built.getType());
        assertEquals(2, built.getAmount());
        assertNotNull(received.get());
        verify(pluginManager, times(1)).callEvent(any(ItemBuildEvent.class));
    }

    @Test
    void configuredVanillaConversionRemainsExplicit() {
        ItemInfo item = registerItem("VANILLA_DIAMOND", "Diamond", ItemType.MATERIAL);
        config.set("vanilla-convert.DIAMOND", item.getID());

        assertSame(item, itemsManager.getItemFromItemStackSafe(new ItemStack(Material.DIAMOND)));
    }

    @Test
    void displayNameOnlyStackDoesNotProvideStats() {
        ItemInfo item = registerItem("ADMIN_SWORD", "Admin Sword", ItemType.SWORD);
        item.getStats().set(StatType.DAMAGE, 5_000);
        StatsManager statsManager = StatsManager.getInstance();
        ItemStack counterfeit = namedItem(Material.NETHERITE_SWORD, "§cAdmin Sword");

        assertNull(statsManager.getStatsFromItemStack(counterfeit, ItemSlot.HAND));

        ItemStack real = namedItem(Material.NETHERITE_SWORD, "§cAdmin Sword");
        setItemId(real, item.getID());
        assertSame(item.getStats(), statsManager.getStatsFromItemStack(real, ItemSlot.HAND));
    }

    @Test
    void displayNameOnlyStackCannotAdvanceAnAltar() {
        ItemInfo item = registerItem("SUMMONING_ITEM", "Summoning Item", ItemType.MATERIAL);
        Player player = server.addPlayer("tester");
        Location location = new Location(server.addSimpleWorld("altar-world"), 0, 64, 0);
        Block block = location.getBlock();
        block.setType(Material.END_PORTAL_FRAME);
        ItemStack counterfeit = namedItem(Material.DIAMOND, "§dSummoning Item");
        player.getInventory().setItemInMainHand(counterfeit);

        Altar altar = new Altar(
                List.of(location),
                location,
                List.of(),
                item,
                Material.END_PORTAL_FRAME,
                Material.BEDROCK,
                null,
                null,
                100,
                200);
        PlayerInteractEvent event = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                counterfeit,
                block,
                BlockFace.UP);

        altar.onPlayerInteract(event);

        assertEquals(0, altar.getTotalPlaced());
        assertEquals(Material.END_PORTAL_FRAME, block.getType());
        assertEquals(1, player.getInventory().getItemInMainHand().getAmount());
    }

    private ItemInfo registerItem(String id, String name, ItemType type) {
        ItemInfo item = new ItemInfo(name, new Stats(), type, Material.DIAMOND, Rarity.COMMON);
        itemsManager.registerItem(id, item);
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private void setItemId(ItemStack stack, String id) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, ItemsManager.ITEM_ID),
                PersistentDataType.STRING,
                id);
        stack.setItemMeta(meta);
    }

    private static CaveCrawlers createPlugin(ServerMock server) throws Exception {
        // CaveCrawlers is final and Paper's JavaPlugin lookup requires a plugin classloader.
        Unsafe unsafe;
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        CaveCrawlers plugin = (CaveCrawlers) unsafe.allocateInstance(CaveCrawlers.class);
        setField(plugin, JavaPlugin.class, "server", server);
        setField(plugin, JavaPlugin.class, "pluginMeta", new PluginDescriptionFile(
                "CaveCrawlers",
                "test",
                CaveCrawlers.class.getName()));
        setField(plugin, JavaPlugin.class, "newConfig", new org.bukkit.configuration.file.YamlConfiguration());
        return plugin;
    }

    private static void setField(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
