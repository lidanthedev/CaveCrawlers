package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.SkillObjective;
import me.lidan.cavecrawlers.skills.SkillReward;
import me.lidan.cavecrawlers.skills.SkillXpGainEvent;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.stats.Stat;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SkillProgressionCoverageTest {
    private MockCaveCrawlers context;
    private SkillsManager skillsManager;
    private SkillInfo mining;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(SkillsManager.class, "instance", null);
        skillsManager = new SkillsManager();
        player = context.server().addPlayer("skill-player");

        Map<Integer, List<SkillReward>> rewards = new HashMap<>();
        rewards.put(1, List.of(reward()));
        rewards.put(2, List.of(reward()));
        rewards.put(3, List.of(reward()));
        mining = new SkillInfo("Mining", rewards, false, 3, new ArrayList<>(),
                new ArrayList<>(List.of(10D, 20D, 30D)), Material.PAPER);
        skillsManager.register("mining", mining);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(SkillsManager.class, "instance", null);
        context.close();
    }

    @Test
    void levelUpConsumesMultipleThresholdsInOrderAndStopsAtMax() {
        SkillReward first = mock(SkillReward.class);
        SkillReward second = mock(SkillReward.class);
        mining.getRewards().put(1, List.of(first));
        mining.getRewards().put(2, List.of(second));
        Skill skill = new Skill(mining, 0, 35, 10, 35);
        skill.setUuid(player.getUniqueId());

        assertEquals(2, skill.levelUp(true));
        assertEquals(2, skill.getLevel());
        assertEquals(5, skill.getXp());
        assertEquals(30, skill.getXpToLevel());
        verify(first).applyReward(player);
        verify(second).applyReward(player);
    }

    @Test
    void replayingLevelUpWithoutNewXpDoesNotReplayPersistentRewards() {
        SkillReward reward = mock(SkillReward.class);
        SkillInfo info = new SkillInfo("Single", Map.of(1, List.of(reward)), false, 1,
                List.of(), new ArrayList<>(List.of(1D)), Material.PAPER);
        Skill skill = new Skill(info, 0, 0, 1, 0);
        skill.setUuid(player.getUniqueId());
        skill.addXp(1);

        assertEquals(1, skill.levelUp(true));
        assertEquals(0, skill.levelUp(true));

        verify(reward).applyReward(player);
    }

    @Test
    void levelUpAtMaxDoesNotConsumeRemainingXp() {
        Skill skill = new Skill(mining, 3, 99, 30, 99);
        skill.setUuid(player.getUniqueId());

        assertEquals(0, skill.levelUp(true));
        assertEquals(99, skill.getXp());
        assertEquals(3, skill.getLevel());
    }

    @Test
    void skillsDeserializeRebuildsFromTotalXpAfterCurveChangeAndSkipsUnknownTypes() {
        Skill saved = new Skill(mining, 2, 1, 20, 21);
        mining.setXpToLevelList(new ArrayList<>(List.of(5D, 5D, 5D)));
        Skills loaded = Skills.deserialize(Map.of(
                "mining", saved,
                "removed", saved,
                "==", "me.lidan.cavecrawlers.skills.Skills"));

        Skill result = loaded.get(mining);
        assertEquals(3, result.getLevel());
        assertEquals(6, result.getXp());
        assertEquals(21, result.getTotalXp());
    }

    @Test
    void mutationGuardBindsEverySkillInCollection() {
        Skills skills = new Skills(List.of(new Skill(mining, 0)));
        AtomicBoolean allowed = new AtomicBoolean(true);
        skills.bindMutationGuard(allowed::get);
        skills.get(mining).addXp(2);
        allowed.set(false);

        assertThrows(IllegalStateException.class, () -> skills.addXp(mining, 1));
        assertThrows(IllegalStateException.class, () -> skills.get(mining).setLevel(1));
    }

    @Test
    void statsRewardsAccumulateByLevelWithoutAliasing() {
        Map<Integer, List<SkillReward>> rewards = new HashMap<>();
        rewards.put(1, List.of(new me.lidan.cavecrawlers.skills.StatSkillReward(
                new Stat(StatType.STRENGTH, 4))));
        rewards.put(2, List.of(new me.lidan.cavecrawlers.skills.StatSkillReward(
                new Stat(StatType.DAMAGE, 7))));
        SkillInfo info = new SkillInfo("Combat", rewards, false, 2, List.of(),
                new ArrayList<>(List.of(10D, 10D)), Material.IRON_SWORD);

        Stats levelOne = info.getStats(1);
        Stats levelTwo = info.getStats(2);
        levelOne.set(StatType.STRENGTH, 999);

        assertEquals(4, levelTwo.get(StatType.STRENGTH).getValue());
        assertEquals(7, levelTwo.get(StatType.DAMAGE).getValue());
        assertEquals(999, levelOne.get(StatType.STRENGTH).getValue());
    }

    @Test
    void actionObjectivesAreGroupedByAction() {
        SkillObjective mine = new SkillObjective(SkillAction.MINE, "DIAMOND", 2, List.of("world"));
        SkillObjective kill = new SkillObjective(SkillAction.KILL, "ZOMBIE", 3, List.of());
        SkillInfo info = new SkillInfo("Objectives", Map.of(), false, 1,
                List.of(mine, kill), new ArrayList<>(List.of(10D)), Material.PAPER);

        assertEquals(List.of(mine), info.getActionObjectives().get(SkillAction.MINE));
        assertEquals(List.of(kill), info.getActionObjectives().get(SkillAction.KILL));
    }

    @Test
    void xpGainEventExposesPlayerSkillAndCancellation() {
        Skill skill = new Skill(mining, 0);
        SkillXpGainEvent event = new SkillXpGainEvent(player, skill, 4.5);

        assertSame(player, event.getPlayer());
        assertSame(skill, event.getSkill());
        assertEquals(4.5, event.getXpGained());
        assertFalse(event.isCancelled());
        event.setXpGained(7.25);
        event.setCancelled(true);
        assertEquals(7.25, event.getXpGained());
        assertTrue(event.isCancelled());
    }

    @Test
    void skillResetRestoresLevelAndFirstThreshold() {
        Skill skill = new Skill(mining, 2, 8, 20, 28);
        skill.resetSkill();

        assertEquals(0, skill.getLevel());
        assertEquals(0, skill.getXp());
        assertEquals(10, skill.getXpToLevel());
        assertEquals(0, skill.getTotalXp());
    }

    private SkillReward reward() {
        return new SkillReward() {
            @Override public void applyReward(Player player) { }
            @Override public Component getRewardMessage() { return Component.empty(); }
            @Override public Map<String, Object> serialize() { return Map.of(); }
        };
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
