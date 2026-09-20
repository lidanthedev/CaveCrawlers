package me.lidan.cavecrawlers.items;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.List;

import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
