package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.integration.mythic.MythicItemSupport;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicIntegrationCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager items;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        setStatic(AbilityManager.class, "instance", null);
        setStatic(MythicMobsHook.class, "instance", null);
        items = ItemsManager.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        setStatic(AbilityManager.class, "instance", null);
        setStatic(MythicMobsHook.class, "instance", null);
        context.close();
    }

    @Test
    void mythicItemSupplierExposesIdentityComparison() {
        ItemInfo item = new ItemInfo("Diamond", new Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        items.registerItem("DIAMOND", item);
        MythicItemSupport support = new MythicItemSupport();

        org.bukkit.inventory.ItemStack generated = new org.bukkit.inventory.ItemStack(Material.DIAMOND);
        org.bukkit.inventory.meta.ItemMeta meta = generated.getItemMeta();
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(context.plugin(), ItemsManager.ITEM_ID),
                org.bukkit.persistence.PersistentDataType.STRING, "DIAMOND");
        generated.setItemMeta(meta);

        assertEquals("cavecrawlers", support.getNamespace());
        assertTrue(support.isSimilar("DIAMOND", generated));
        assertNull(support.getItem("missing"));
        assertTrue(support.getAvailableItemNames().contains("DIAMOND"));
    }

    @Test
    void mythicHookWithoutBackendIsSafeForLookupAndSpawn() {
        MythicMobsHook hook = MythicMobsHook.getInstance();

        assertNull(hook.getMobByID("missing"));
        assertNull(hook.getMobByName("Missing"));
        assertNull(hook.getMobID(null));
        assertNull(hook.spawnMythicMob("missing", null));
        assertEquals("Error", hook.getMobNameByMythicMob(null));
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
