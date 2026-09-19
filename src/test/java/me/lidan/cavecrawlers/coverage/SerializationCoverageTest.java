package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.objects.SoundOptions;
import me.lidan.cavecrawlers.objects.TitleOptions;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.storage.PlayerData;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.Serializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationCoverageTest {
    private MockCaveCrawlers context;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(ItemsManager.class, "instance", null);
        setStatic(SkillsManager.class, "instance", null);
        CaveCrawlers.usePlaceholderAPI = false;
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(ItemsManager.class, "instance", null);
        setStatic(SkillsManager.class, "instance", null);
        context.close();
    }

    @Test
    void itemInfoRoundTripPreservesIdentityStatsAndBaseItem() {
        ItemStack base = new ItemStack(Material.DIAMOND);
        ItemInfo original = new ItemInfo("Sword", "description",
                new Stats(), ItemType.SWORD, base, Rarity.RARE, "ability");
        original.setID("SWORD");
        original.getStats().set(StatType.DAMAGE, 12.5);

        ItemInfo copy = ItemInfo.deserialize(original.serialize());
        copy.setID(original.getID());

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getType(), copy.getType());
        assertEquals(original.getRarity(), copy.getRarity());
        assertEquals(original.getAbilityID(), copy.getAbilityID());
        assertEquals(original.getStats().get(StatType.DAMAGE).getValue(),
                copy.getStats().get(StatType.DAMAGE).getValue());
        assertEquals(Material.DIAMOND, copy.getBaseItem().getType());
    }

    @Test
    void blockInfoRoundTripPreservesReplacementAndDrops() {
        ItemsManager.getInstance().registerItem("DIAMOND", new ItemInfo(
                "Diamond", new Stats(), ItemType.MATERIAL, Material.DIAMOND, Rarity.COMMON));
        Drop drop = new Drop(DropType.ITEM, 100, "DIAMOND 2", null);
        BlockData replacement = Material.STONE.createBlockData();
        BlockInfo original = new BlockInfo(25, 3, List.of(drop), ItemType.PICKAXE, replacement);

        BlockInfo copy = BlockInfo.deserialize(original.serialize());

        assertEquals(25, copy.getBlockStrength());
        assertEquals(3, copy.getBlockPower());
        assertEquals(ItemType.PICKAXE, copy.getBrokenBy());
        assertEquals("minecraft:stone", copy.getReplacementBlockData().getAsString());
        assertEquals(1, copy.getDrops().size());
        assertEquals("DIAMOND 2", copy.getDrops().getFirst().getValue());
    }

    @Test
    void messageSoundAndTitleRoundTripPreservesFormatting() {
        TitleOptions title = new TitleOptions("&aTitle", "&bSubtitle", 1, 2, 3);
        SoundOptions sound = new SoundOptions(Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.25f);
        ConfigMessage original = new ConfigMessage("&cMessage", title, "&dAction", sound);

        ConfigMessage copy = ConfigMessage.deserialize(original.serialize());

        assertEquals("§cMessage", copy.getMessage());
        assertEquals("§dAction", copy.getActionbar());
        assertEquals(title, copy.getTitleOptions());
        assertEquals(sound, copy.getSound());
        assertEquals(original.serialize().get("sound"), copy.serialize().get("sound"));
    }

    @Test
    void skillsAndSkillRoundTripRebuildsFromTotalXp() {
        SkillsManager manager = new SkillsManager();
        SkillInfo info = new SkillInfo("Mining", Map.of(), false, 2, List.of(),
                new ArrayList<>(List.of(10D, 20D)), Material.PAPER);
        manager.register("mining", info);
        Skills original = new Skills(List.of(new Skill(info, 1, 5, 20, 15)));

        Skills copy = Skills.deserialize(original.serialize());
        Skill skill = copy.get(info);

        assertEquals(1, skill.getLevel());
        assertEquals(5, skill.getXp());
        assertEquals(15, skill.getTotalXp());
    }

    @Test
    void playerDataRoundTripPreservesTheLogicalSkillsObject() {
        SkillsManager manager = new SkillsManager();
        SkillInfo info = new SkillInfo("Mining", Map.of(), false, 1, List.of(),
                new ArrayList<>(List.of(10D)), Material.PAPER);
        manager.register("mining", info);
        Skills skills = new Skills(List.of(new Skill(info, 0, 4, 10, 4)));
        PlayerData original = new PlayerData(skills);
        original.setLoaded(true);

        PlayerData copy = PlayerData.deserialize(original.serialize());

        assertEquals(4, copy.getSkills().get(info).getTotalXp());
        assertFalse(copy.isLoaded(), "loaded is runtime state, not persisted data");
    }

    @Test
    void shopItemRoundTripPreservesResultPriceAndIngredientCounts() {
        ItemInfo result = new ItemInfo("Result", new Stats(), ItemType.MATERIAL,
                Material.DIAMOND, Rarity.COMMON);
        ItemInfo ingredient = new ItemInfo("Ingredient", new Stats(), ItemType.MATERIAL,
                Material.IRON_INGOT, Rarity.COMMON);
        ItemsManager.getInstance().registerItem("RESULT", result);
        ItemsManager.getInstance().registerItem("INGREDIENT", ingredient);
        ShopItem original = new ShopItem(result, 2, 17.5, Map.of(ingredient, 3));

        ShopItem copy = ShopItem.deserialize(original.serialize());

        assertEquals("RESULT", copy.getResult().getID());
        assertEquals(2, copy.getResultAmount());
        assertEquals(17.5, copy.getPrice());
        assertEquals(3, copy.getIngredientsMap().get(ingredient));
    }

    @Test
    void playerDataYamlSaveAndLoadPreservesSkillProgress() throws Exception {
        SkillsManager manager = new SkillsManager();
        SkillInfo info = new SkillInfo("Mining", Map.of(), false, 1, List.of(),
                new ArrayList<>(List.of(10D)), Material.PAPER);
        manager.register("mining", info);
        ConfigurationSerialization.registerClass(Skills.class);
        ConfigurationSerialization.registerClass(Skill.class);
        UUID uuid = UUID.randomUUID();
        java.io.File file = new java.io.File(context.plugin().getDataFolder(), "players/" + uuid + ".yml");
        file.delete();
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        try {
            Skills skills = new Skills(List.of(new Skill(info, 0, 6, 10, 6)));
            PlayerData source = new PlayerData(skills);
            source.setLoaded(true);
            source.savePlayer(uuid);

            PlayerData loaded = new PlayerData();
            loaded.loadPlayer(uuid);

            assertEquals(6, loaded.getSkills().get(info).getTotalXp());
            assertTrue(loaded.isLoaded());
        } finally {
            file.delete();
        }
    }

    @Test
    void serializerRoundTripSupportsBukkitSerializableCollections() {
        List<String> original = List.of("alpha", "beta", "gamma");

        String encoded = Serializer.serialize(original);
        List<?> copy = Serializer.deserialize(encoded);

        assertEquals(original, copy);
    }

    @Test
    void serializerRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> Serializer.deserialize("not-base64"));
    }

    @Test
    void itemCloneCopiesMutableBaseItemAndStats() {
        ItemInfo original = new ItemInfo("Sword", new Stats(), ItemType.SWORD,
                Material.DIAMOND, Rarity.COMMON);
        original.getStats().set(StatType.DAMAGE, 5);
        ItemInfo copy = original.clone();

        copy.getStats().set(StatType.DAMAGE, 100);
        copy.getBaseItem().setAmount(4);

        assertEquals(5, original.getStats().get(StatType.DAMAGE).getValue());
        assertEquals(1, original.getBaseItem().getAmount());
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
