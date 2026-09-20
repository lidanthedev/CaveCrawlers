package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningCoverageTest {
    private MockCaveCrawlers context;
    private MiningManager mining;
    private File pendingFile;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(MiningManager.class, "instance", null);
        setStatic(ItemsManager.class, "instance", null);
        mining = MiningManager.getInstance();
        pendingFile = new File(context.plugin().getDataFolder(), "pending-blocks.yml");
        pendingFile.delete();
        new File(context.plugin().getDataFolder(), "pending-blocks.yml.tmp").delete();
    }

    @AfterEach
    void tearDown() throws Exception {
        pendingFile.delete();
        new File(context.plugin().getDataFolder(), "pending-blocks.yml.tmp").delete();
        setStatic(MiningManager.class, "instance", null);
        setStatic(ItemsManager.class, "instance", null);
        context.close();
    }

    @Test
    void breakTimeUsesStrengthAndTreatsZeroSpeedAsOne() {
        assertEquals(60, MiningManager.getTicksToBreak(30, 60));
        assertEquals(90, MiningManager.getTicksToBreak(0, 3));
    }

    @Test
    void invalidBlockDefinitionsAreRemovedAndValidDefinitionsRegistered() {
        BlockInfo invalidStrength = new BlockInfo(-1, 1, List.of());
        BlockInfo invalidPower = new BlockInfo(1, -1, List.of());
        BlockInfo valid = new BlockInfo(10, 2, List.of());

        mining.registerBlock(Material.STONE, invalidStrength);
        mining.registerBlock(Material.DIRT, invalidPower);
        mining.registerBlock(Material.DIAMOND_BLOCK, valid);

        assertSame(valid, mining.getBlockInfo(Material.DIAMOND_BLOCK));
        assertEquals(100000000, mining.getBlockInfo(Material.STONE).getBlockStrength());
        assertEquals(100000000, mining.getBlockInfo(Material.DIRT).getBlockStrength());
    }

    @Test
    void pendingBlockRestoreAppliesOriginalDataAndDeletesSuccessfulFile() throws Exception {
        World world = context.server().addSimpleWorld("restore-world");
        Block block = world.getBlockAt(2, 64, 3);
        block.setType(Material.AIR);
        writePending("restore-world", 2, 64, 3, "minecraft:stone");

        mining.loadBrokenBlocks();

        assertEquals(Material.STONE, block.getType());
        assertFalse(pendingFile.exists());
    }

    @Test
    void invalidPendingEntriesRemainForRetry() throws Exception {
        World world = context.server().addSimpleWorld("restore-world");
        writePending("restore-world", 1, world.getMaxHeight() + 1, 1, "minecraft:stone");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(pendingFile);
        config.set("blocks.1.world", "missing-world");
        config.set("blocks.1.x", 0);
        config.set("blocks.1.y", 64);
        config.set("blocks.1.z", 0);
        config.set("blocks.1.data", "minecraft:stone");
        config.save(pendingFile);

        mining.loadBrokenBlocks();

        assertTrue(pendingFile.exists());
        YamlConfiguration retained = YamlConfiguration.loadConfiguration(pendingFile);
        assertEquals(2, retained.getConfigurationSection("blocks").getKeys(false).size());
    }

    @Test
    void restoreBlockReturnsOriginalMaterialAfterReplacement() throws Exception {
        World world = context.server().addSimpleWorld("regen-world");
        Block block = world.getBlockAt(4, 64, 4);
        block.setType(Material.STONE);
        BlockInfo replacement = new BlockInfo(10, 0, List.of(),
                me.lidan.cavecrawlers.items.ItemType.PICKAXE, Material.BLACK_WOOL.createBlockData());
        var method = MiningManager.class.getDeclaredMethod("handleBlockRegen", Block.class,
                org.bukkit.block.data.BlockData.class, BlockInfo.class);
        method.setAccessible(true);
        method.invoke(mining, block, block.getBlockData(), replacement);
        assertEquals(Material.BLACK_WOOL, block.getType());

        mining.restoreBlock(block);

        assertEquals(Material.STONE, block.getType());
    }

    private void writePending(String world, int x, int y, int z, String data) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("blocks.0.world", world);
        config.set("blocks.0.x", x);
        config.set("blocks.0.y", y);
        config.set("blocks.0.z", z);
        config.set("blocks.0.data", data);
        config.save(pendingFile);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
