package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.drops.DropRarity;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.Rarity;
import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.skills.CoinSkillReward;
import me.lidan.cavecrawlers.skills.CommandSkillReward;
import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.SkillObjective;
import me.lidan.cavecrawlers.skills.SkillReward;
import me.lidan.cavecrawlers.skills.StatSkillReward;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.Serializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueObjectCoverageTest {
    @Test
    void rangeParsesNegativeAndInclusiveBounds() {
        Range range = new Range("-2-2");

        assertEquals(-2, range.getMin());
        assertEquals(2, range.getMax());
        assertEquals(List.of(-2L, -1L, 0L, 1L, 2L), range.getRangeAsList());
        assertTrue(range.isInRange(-2));
        assertTrue(range.isInRange(2));
        assertFalse(range.isInRange(3));
        assertEquals("-2-2", range.toString());
    }

    @Test
    void rangeIteratorHasStableExhaustion() {
        Range range = new Range(4, 5);
        var iterator = range.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(4L, iterator.next());
        assertEquals(5L, iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
    }

    @Test
    void dropRarityUsesDocumentedBoundaries() {
        assertEquals(DropRarity.INSANE, DropRarity.getRarity(0.01));
        assertEquals(DropRarity.CRAZY_RARE, DropRarity.getRarity(0.2));
        assertEquals(DropRarity.VERY_RARE, DropRarity.getRarity(1));
        assertEquals(DropRarity.RARE, DropRarity.getRarity(1.01));
    }

    @Test
    void statsAddMultiplyAndCloneDoNotAlias() {
        Stats stats = new Stats();
        stats.set(StatType.DAMAGE, 10);
        stats.add(StatType.DAMAGE, 2.5);
        stats.add(StatType.MANA, 7);
        stats.multiply(2);

        Stats clone = stats.clone();
        clone.set(StatType.DAMAGE, 1);

        assertEquals(25, stats.get(StatType.DAMAGE).getValue());
        assertEquals(14, stats.get(StatType.INTELLIGENCE).getValue());
        assertEquals(1, clone.get(StatType.DAMAGE).getValue());
        assertNotSame(stats.get(StatType.DAMAGE), clone.get(StatType.DAMAGE));
    }

    @Test
    void statsDeserializeAcceptsYamlNumberVariants() {
        Stats integerStats = Stats.deserialize(Map.of("DAMAGE", 12));
        Stats longStats = Stats.deserialize(Map.of("DAMAGE", 13L));
        Stats doubleStats = Stats.deserialize(Map.of("DAMAGE", 14.5D));

        assertEquals(12, integerStats.get(StatType.DAMAGE).getValue());
        assertEquals(13, longStats.get(StatType.DAMAGE).getValue());
        assertEquals(14.5, doubleStats.get(StatType.DAMAGE).getValue());
    }

    @Test
    void statsDeserializeSkipsNonNumericValues() {
        Stats stats = Stats.deserialize(Map.of("DAMAGE", "not-a-number", "HEALTH", 7));

        assertEquals(0, stats.get(StatType.DAMAGE).getValue());
        assertEquals(7, stats.get(StatType.HEALTH).getValue());
    }

    @Test
    void statsSerializationRoundTripsGameplayValues() {
        Stats original = new Stats();
        original.set(StatType.HEALTH, 321.5);
        original.set(StatType.SPEED, 42);

        Stats copy = Stats.deserialize(original.serialize());

        assertEquals(original.get(StatType.HEALTH).getValue(), copy.get(StatType.HEALTH).getValue());
        assertEquals(original.get(StatType.SPEED).getValue(), copy.get(StatType.SPEED).getValue());
    }

    @Test
    void perkSerializationPreservesPriorityAndStats() {
        Stats stats = new Stats();
        stats.set(StatType.HEALTH, 50);
        Perk original = new Perk("Elite", "combat", "perk.elite", 7, stats);

        Perk copy = Perk.deserialize(original.serialize());

        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getTrack(), copy.getTrack());
        assertEquals(original.getPermission(), copy.getPermission());
        assertEquals(7, copy.getPriority());
        assertEquals(50, copy.getStats().get(StatType.HEALTH).getValue());
    }

    @Test
    void skillObjectiveRoundTripsActionAmountAndWorlds() {
        SkillObjective objective = SkillObjective.fromString("MINE DIAMOND 2.5 caves,deep");

        assertEquals(SkillAction.MINE, objective.getAction());
        assertEquals("DIAMOND", objective.getObjective());
        assertEquals(2.5, objective.getAmount());
        assertEquals(List.of("caves", "deep"), objective.getWorlds());
        assertEquals(objective, SkillObjective.fromString(objective.toSaveString()));
    }

    @Test
    void skillObjectiveRequiresActionObjectiveAndAmount() {
        assertThrows(IllegalArgumentException.class, () -> SkillObjective.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> SkillObjective.fromString("MINE"));
        assertThrows(IllegalArgumentException.class, () -> SkillObjective.fromString("MINE DIAMOND"));
    }

    @Test
    void skillRewardParsesAllSupportedForms() {
        SkillReward stat = SkillReward.valueOf("STAT DAMAGE 4.5");
        SkillReward coins = SkillReward.valueOf("COINS 12");
        SkillReward item = SkillReward.valueOf("ITEM DIAMOND");
        SkillReward command = SkillReward.valueOf("COMMAND give %player% diamond 1");

        assertEquals(4.5, ((StatSkillReward) stat).getStat().getValue());
        assertEquals(12, ((CoinSkillReward) coins).getAmount());
        assertEquals("DIAMOND", ((me.lidan.cavecrawlers.skills.ItemSkillReward) item).getItem());
        assertEquals("give %player% diamond 1", ((CommandSkillReward) command).getCommand());
    }

    @Test
    void skillInfoAutoRewardsAreIndependentPerLevel() {
        List<Double> curve = new ArrayList<>(List.of(10D, 20D, 30D));
        Map<Integer, List<SkillReward>> rewards = new HashMap<>();
        rewards.put(1, new ArrayList<>(List.of(new CoinSkillReward(5))));
        SkillInfo info = new SkillInfo("Mining", rewards, true, 3, List.of(), curve, SkillInfo.DEFAULT_ICON);

        assertEquals(5, ((CoinSkillReward) info.getRewards(1).getFirst()).getAmount());
        assertEquals(5, ((CoinSkillReward) info.getRewards(2).getFirst()).getAmount());
        assertEquals(5, ((CoinSkillReward) info.getRewards(3).getFirst()).getAmount());
        assertNotSame(info.getRewards(1), info.getRewards(2));
        assertNotSame(info.getStats(1), info.getStats(2));
    }

    @Test
    void skillInfoMissingDefaultCurveDoesNotMutateGlobalList() {
        List<Double> defaults = new ArrayList<>(List.of(10D, 20D));
        var previous = me.lidan.cavecrawlers.skills.Skill.getDefaultXpToLevelList();
        me.lidan.cavecrawlers.skills.Skill.setDefaultXpToLevelList(defaults);
        try {
            SkillInfo.deserialize(Map.of("name", "A", "maxLevel", 5));
            assertEquals(List.of(10D, 20D), defaults);
        } finally {
            me.lidan.cavecrawlers.skills.Skill.setDefaultXpToLevelList(previous);
        }
    }

    @Test
    void serializerRoundTripsSerializableObjects() {
        ArrayList<String> original = new ArrayList<>(List.of("cave", "crawler"));
        String encoded = Serializer.serialize(original);

        ArrayList<String> decoded = Serializer.deserialize(encoded);

        assertEquals(original, decoded);
    }

    @Test
    void itemAndRarityRegistriesRejectUnknownValues() {
        assertEquals(ItemType.SWORD, ItemType.valueOf("sword"));
        assertEquals(Rarity.LEGENDARY, Rarity.getRarity(5));
        assertThrows(IllegalArgumentException.class, () -> ItemType.valueOf("missing"));
        assertEquals(Rarity.NONE, Rarity.getRarity(999));
    }
}
