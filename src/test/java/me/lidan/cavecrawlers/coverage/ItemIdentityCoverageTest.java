package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.items.VanillaItemInfo;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemIdentityCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager manager;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        manager = ItemsManager.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        context.close();
    }

    @Test
    void vanillaIdsResolveOnlyOneLeadingPrefix() {
        assertInstanceOf(VanillaItemInfo.class, manager.getItemByID("VANILLA-DIAMOND"));
        assertEquals(Material.DIAMOND, ((VanillaItemInfo) manager.getItemByID("VANILLA-DIAMOND")).getBaseItem().getType());
        assertNull(manager.getItemByID("VANILLA-VANILLA-DIAMOND"));
        assertNull(manager.getItemByID("VANILLA-"));
    }

    @Test
    void pdcIdentityWinsOverConfiguredVanillaConversion() {
        context.plugin().getConfig().set("vanilla-convert.DIAMOND", "VANILLA_DIAMOND");
        ItemInfo item = item("CUSTOM_DIAMOND");
        ItemStack stack = new ItemStack(Material.DIAMOND);
        setId(stack, item.getID());

        assertEquals(item.getID(), manager.getIDofItemStack(stack));
        assertSame(item, manager.getItemFromItemStackSafe(stack));
    }

    @Test
    void knownItemMapRoundTripsWithoutChangingCounts() {
        ItemInfo item = item("KNOWN");
        Map<ItemInfo, Integer> original = Map.of(item, 4);

        Map<String, Integer> encoded = manager.itemMapToStringMap(original);
        Map<ItemInfo, Integer> decoded = manager.stringMapToItemMap(encoded);

        assertEquals(Map.of(item.getID(), 4), encoded);
        assertEquals(4, decoded.get(item));
    }

    @Test
    void unknownItemMapEntriesAreNotInsertedAsNullKeys() {
        ItemInfo item = item("KNOWN");
        Map<String, Integer> encoded = new LinkedHashMap<>();
        encoded.put(item.getID(), 2);
        encoded.put("REMOVED", 1);

        Map<ItemInfo, Integer> decoded = manager.stringMapToItemMap(encoded);

        assertEquals(1, decoded.size());
        assertEquals(2, decoded.get(item));
        assertNull(decoded.get(null));
    }

    @Test
    void itemInfoCloneCopiesMutableSemantics() {
        ItemInfo original = item("CLONE");
        original.getStats().set(me.lidan.cavecrawlers.stats.StatType.DAMAGE, 9);
        original.getBaseItem().setAmount(3);

        ItemInfo copy = original.clone();
        copy.getStats().set(me.lidan.cavecrawlers.stats.StatType.DAMAGE, 1);
        copy.getBaseItem().setAmount(1);

        assertNotSame(original, copy);
        assertEquals(9, original.getStats().get(me.lidan.cavecrawlers.stats.StatType.DAMAGE).getValue());
        assertEquals(3, original.getBaseItem().getAmount());
    }

    private ItemInfo item(String id) {
        ItemInfo item = new ItemInfo(id, new Stats(), ItemType.MATERIAL, Material.DIAMOND, Rarity.COMMON);
        item.setID(id);
        manager.registerItem(id, item);
        return item;
    }

    private void setId(ItemStack stack, String id) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(context.plugin(), ItemsManager.ITEM_ID),
                PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
