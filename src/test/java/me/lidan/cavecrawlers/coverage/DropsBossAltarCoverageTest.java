package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarDrop;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.drops.DropsManager;
import me.lidan.cavecrawlers.drops.SimpleDrop;
import me.lidan.cavecrawlers.entities.BossEntityData;
import me.lidan.cavecrawlers.entities.LootShareEntityData;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.stats.Stats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class DropsBossAltarCoverageTest {
    private MockCaveCrawlers context;
    private ItemsManager itemsManager;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        itemsManager = ItemsManager.getInstance();
        setStatic(Altar.class, "itemsManager", itemsManager);
        setStatic(AltarManager.class, "instance", null);
        setStatic(me.lidan.cavecrawlers.bosses.BossManager.class, "instance", null);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        setStatic(AltarManager.class, "instance", null);
        setStatic(me.lidan.cavecrawlers.bosses.BossManager.class, "instance", null);
        context.close();
    }

    @Test
    void dropItemParserPreservesConfiguredIdAndInclusiveRange() {
        ItemInfo item = item("DIAMOND");

        Drop.ItemDropInfo parsed = Drop.getItemDropInfo("DIAMOND 2-4");

        assertEquals(item.getID(), parsed.itemInfo().getID());
        assertEquals(2, parsed.range().getMin());
        assertEquals(4, parsed.range().getMax());
        assertTrue(parsed.range().isInRange(2));
        assertTrue(parsed.range().isInRange(4));
    }

    @Test
    void dropAndLegacySimpleDropSerializationRetainGameplayFields() {
        item("DIAMOND");
        Drop drop = new Drop(DropType.ITEM, 12.5, "DIAMOND 2-4", null,
                me.lidan.cavecrawlers.stats.StatType.MAGIC_FIND, me.lidan.cavecrawlers.stats.StatType.MINING_FORTUNE);

        Drop copy = Drop.deserialize(drop.serialize());
        SimpleDrop legacy = SimpleDrop.deserialize(Map.of(
                "itemID", "DIAMOND", "amount", "2-4", "chance", 75D, "announce", false));

        assertEquals(drop.getType(), copy.getType());
        assertEquals(drop.getChance(), copy.getChance());
        assertEquals(drop.getValue(), copy.getValue());
        assertTrue(legacy.serialize().containsKey("itemID"));
    }

    @Test
    void entityDropsGiveExperienceAndRollEachDropOnce() {
        Player player = context.server().addPlayer("drop-player");
        Drop first = spy(new Drop(DropType.COINS, 100, "1", null));
        Drop second = spy(new Drop(DropType.COMMAND, 100, "say test", null));
        org.mockito.Mockito.doNothing().when(first).roll(player);
        org.mockito.Mockito.doNothing().when(second).roll(player);

        new EntityDrops("zombie", List.of(first, second), 7).roll(player);

        assertEquals(7, player.getTotalExperience());
        verify(first).roll(player);
        verify(second).roll(player);
    }

    @Test
    void dropsManagerRegistersLooksUpAndRollsConfiguredDrops() {
        me.lidan.cavecrawlers.drops.DropsManager manager =
                me.lidan.cavecrawlers.drops.DropsManager.getInstance();
        manager.clear();
        EntityDrops drops = new EntityDrops("zombie", List.of(), 0);
        manager.register("zombie", drops);

        assertSame(drops, manager.getEntityDrops("zombie"));
        assertNull(manager.getEntityDrops("missing"));

        Player player = context.server().addPlayer("manager-drop-player");
        Drop drop = spy(new Drop(DropType.COINS, 100, "1", null));
        org.mockito.Mockito.doNothing().when(drop).roll(player);
        manager.rollDropsForPlayer(player, List.of(drop));
        verify(drop).roll(player);
        manager.clear();
    }

    @Test
    void lootShareIncludesEligiblePlayersAndForcesTheSummoner() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getMaxHealth()).thenReturn(100D);
        Player summoner = context.server().addPlayer("summoner");
        Player helper = context.server().addPlayer("helper");
        LootShareEntityData data = new LootShareEntityData(entity, 10, summoner.getUniqueId());
        data.addDamage(summoner.getUniqueId(), 1);
        data.addDamage(helper.getUniqueId(), 10);
        List<Player> recipients = new ArrayList<>();

        data.giveDropsToPlayers(recipients::add);

        assertEquals(2, recipients.size());
        assertTrue(recipients.contains(summoner));
        assertTrue(recipients.contains(helper));
        assertEquals(10D, data.getDamage(summoner.getUniqueId()));
    }

    @Test
    void duplicateDeathCallbacksDoNotGrantLootTwice() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getMaxHealth()).thenReturn(100D);
        Player summoner = context.server().addPlayer("loot-once");
        LootShareEntityData data = new LootShareEntityData(entity, 10, summoner.getUniqueId());
        data.addDamage(summoner.getUniqueId(), 10);
        EntityDrops drops = mock(EntityDrops.class);
        DropsManager manager = DropsManager.getInstance();
        manager.clear();
        manager.register(null, drops);
        EntityDeathEvent event = new EntityDeathEvent(entity,
                mock(org.bukkit.damage.DamageSource.class), List.of(), 0);

        data.onDeath(event);
        data.onDeath(event);

        verify(drops, times(1)).roll(summoner);
        manager.clear();
    }

    @Test
    void bossRankingUsesFullDoublePrecision() throws Exception {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        Player lower = context.server().addPlayer("lower");
        Player higher = context.server().addPlayer("higher");
        BossEntityData data = new BossEntityData(entity);
        Map<UUID, Double> damage = new LinkedHashMap<>();
        damage.put(lower.getUniqueId(), 10.1D);
        damage.put(higher.getUniqueId(), 10.9D);
        setField(data, EntityDataHolder.class, "damageMap", damage);
        List<String> ranking = new ArrayList<>();
        BossDrops drops = new BossDrops(List.of(), null, null, List.of(100)) {
            @Override
            public void drop(Player player, int points) {
                ranking.add(player.getName() + ":" + points);
            }
        };
        me.lidan.cavecrawlers.bosses.BossManager manager = me.lidan.cavecrawlers.bosses.BossManager.getInstance();
        manager.clear();
        manager.registerEntityDrops(null, drops);
        invokeDeath(data, entity);

        assertEquals(List.of("higher:100", "lower:0"), ranking);
    }

    @Test
    void bossAnnouncementCapturesEachDropPlaceholderSnapshot() {
        CapturingMessage message = new CapturingMessage();
        BossDrop drop = new BossDrop(DropType.COINS, 100, "1", message, null, null, 0, null);
        Player first = context.server().addPlayer("first");
        Player second = context.server().addPlayer("second");
        Map<String, String> shared = drop.getPlaceholders();

        drop.announceToPlayersInSameWorld(first, shared);
        drop.announceToPlayersInSameWorld(second, shared);
        context.server().getScheduler().performTicks(20);

        assertEquals(4, message.captured.size());
        assertEquals("first", message.captured.get(0).get("player"));
        assertEquals("first", message.captured.get(1).get("player"));
        assertEquals("second", message.captured.get(2).get("player"));
        assertEquals("second", message.captured.get(3).get("player"));
    }

    @Test
    void bossDropsGrantEachNonNullTrackOnceButAllowIndependentNullTracks() {
        Player player = context.server().addPlayer("boss-drop-player");
        List<String> granted = new ArrayList<>();
        BossDrop first = trackingBossDrop("track", granted);
        BossDrop duplicate = trackingBossDrop("track", granted);
        BossDrop untracked = trackingBossDrop(null, granted);

        new BossDrops(List.of(first, duplicate, untracked, untracked), null, null, List.of())
                .drop(player, 100);

        assertEquals(List.of("track", "null", "null"), granted);
    }

    @Test
    void altarAcceptsOnlyCanonicalItemAndConsumesExactlyOne() {
        ItemInfo item = item("ALTAR_ITEM");
        Player player = context.server().addPlayer("altar-player");
        Location location = new Location(context.server().addSimpleWorld("altar"), 0, 64, 0);
        Block block = location.getBlock();
        block.setType(Material.END_PORTAL_FRAME);
        ItemStack stack = tagged(item, 2);
        player.getInventory().setItemInMainHand(stack);
        Altar altar = new Altar(List.of(location), location, List.of(), item,
                Material.END_PORTAL_FRAME, Material.BEDROCK, null, null, 100, 200);

        altar.onPlayerInteract(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, stack, block, BlockFace.UP));

        assertEquals(1, altar.getTotalPlaced());
        assertEquals(Material.BEDROCK, block.getType());
        assertEquals(1, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void altarDoesNotSilentlyConsumeTheFinalItemWhenNoSpawnRollSucceeds() {
        ItemInfo item = item("ALTAR_ITEM");
        Player player = context.server().addPlayer("altar-no-spawn");
        Location location = new Location(context.server().addSimpleWorld("altar-no-spawn-world"), 0, 64, 0);
        Block block = location.getBlock();
        block.setType(Material.END_PORTAL_FRAME);
        ItemStack stack = tagged(item, 2);
        player.getInventory().setItemInMainHand(stack);
        Altar altar = new Altar(List.of(location), location,
                List.of(new NoSpawnDrop()), item, Material.END_PORTAL_FRAME, Material.BEDROCK,
                null, null, 100, 200);

        altar.onPlayerInteract(new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, stack, block, BlockFace.UP));

        assertEquals(0, altar.getTotalPlaced());
        assertEquals(Material.END_PORTAL_FRAME, block.getType());
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void altarFailureRefundsOnlyConsumedNonCreativeContributions() {
        ItemInfo item = item("ALTAR_ITEM");
        Player contributor = context.server().addPlayer("altar-contributor");
        Player creative = context.server().addPlayer("altar-creative");
        creative.setGameMode(org.bukkit.GameMode.CREATIVE);
        Location first = new Location(context.server().addSimpleWorld("altar-refund-world"), 0, 64, 0);
        Location second = first.clone().add(1, 0, 0);
        first.getBlock().setType(Material.END_PORTAL_FRAME);
        second.getBlock().setType(Material.END_PORTAL_FRAME);
        ItemStack contributorStack = tagged(item, 1);
        ItemStack creativeStack = tagged(item, 1);
        contributor.getInventory().setItemInMainHand(contributorStack);
        creative.getInventory().setItemInMainHand(creativeStack);
        Altar altar = new Altar(List.of(first, second), first,
                List.of(new NoSpawnDrop()), item, Material.END_PORTAL_FRAME, Material.BEDROCK,
                null, null, 100, 200);

        altar.onPlayerInteract(new PlayerInteractEvent(contributor, Action.RIGHT_CLICK_BLOCK,
                contributorStack, first.getBlock(), BlockFace.UP));
        altar.onPlayerInteract(new PlayerInteractEvent(creative, Action.RIGHT_CLICK_BLOCK,
                creativeStack, second.getBlock(), BlockFace.UP));

        assertEquals(1, itemsManager.getAllItems(contributor).getOrDefault(item, 0));
        assertEquals(1, creative.getInventory().getItemInMainHand().getAmount());
        assertEquals(Material.END_PORTAL_FRAME, first.getBlock().getType());
        assertEquals(Material.END_PORTAL_FRAME, second.getBlock().getType());
        assertEquals(0, altar.getTotalPlaced());
    }

    @Test
    void altarManagerMatchesLocationAndCanonicalItemIdentity() {
        ItemInfo item = item("ALTAR_ITEM");
        Location location = new Location(context.server().addSimpleWorld("altar-manager"), 1, 64, 1);
        Altar altar = new Altar(List.of(location), location, List.of(), item,
                Material.END_PORTAL_FRAME, Material.BEDROCK, null, null, 100, 200);
        AltarManager manager = AltarManager.getInstance();
        manager.registerAltar("test", altar);

        assertSame(altar, manager.getAltarAtLocation(location, item));
        assertTrue(manager.getAltarNames().contains("test"));
        assertEquals(List.of(), manager.getAltarsWithMob("missing"));
    }

    @Test
    void altarDropSerializationRoundTripsBasicMobDefinition() {
        AltarDrop drop = new AltarDrop(100, "Zombie");

        Drop copy = AltarDrop.deserialize(drop.serialize());

        assertEquals(DropType.MOB, copy.getType());
        assertEquals(100D, copy.getChance());
        assertEquals("Zombie", copy.getValue());
    }

    private ItemInfo item(String id) {
        ItemInfo item = new ItemInfo(id, new Stats(), ItemType.MATERIAL, Material.DIAMOND, Rarity.COMMON);
        item.setID(id);
        itemsManager.registerItem(id, item);
        try {
            Field field = Drop.class.getDeclaredField("itemsManager");
            field.setAccessible(true);
            ((ItemsManager) field.get(null)).registerItem(id, item);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return item;
    }

    private BossDrop trackingBossDrop(String track, List<String> granted) {
        return new BossDrop(DropType.COINS, 100, "1", null, null, null, 0, track) {
            @Override public void drop(Player player, Location location) {
                granted.add(String.valueOf(getTrack()));
            }
        };
    }

    private ItemStack tagged(ItemInfo item, int amount) {
        ItemStack stack = new ItemStack(Material.DIAMOND, amount);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(context.plugin(), ItemsManager.ITEM_ID),
                PersistentDataType.STRING, item.getID());
        stack.setItemMeta(meta);
        return stack;
    }

    private static void invokeDeath(BossEntityData data, LivingEntity entity) throws Exception {
        var method = me.lidan.cavecrawlers.entities.BossEntityData.class.getMethod("onDeath",
                org.bukkit.event.entity.EntityDeathEvent.class);
        method.invoke(data, new org.bukkit.event.entity.EntityDeathEvent(entity,
                mock(org.bukkit.damage.DamageSource.class), List.of(), 0));
    }

    private static void setField(Object target, Class<?> ignored, String name, Object value) throws Exception {
        Field field = me.lidan.cavecrawlers.entities.EntityData.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class EntityDataHolder {
    }

    private static final class CapturingMessage extends ConfigMessage {
        private final List<Map<String, String>> captured = new ArrayList<>();

        private CapturingMessage() {
            super("");
        }

        @Override
        public void sendMessage(Player player, Map<String, String> placeholders) {
            captured.add(new HashMap<>(placeholders));
        }
    }

    private static final class NoSpawnDrop extends AltarDrop {
        private NoSpawnDrop() { super(100, "missing"); }
        @Override public boolean rollChance() { return false; }
    }
}
