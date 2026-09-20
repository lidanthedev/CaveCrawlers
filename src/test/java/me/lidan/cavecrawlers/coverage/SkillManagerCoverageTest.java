package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.SkillObjective;
import me.lidan.cavecrawlers.skills.SkillReward;
import me.lidan.cavecrawlers.skills.SkillXpGainEvent;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class SkillManagerCoverageTest {
    private MockCaveCrawlers context;
    private SkillsManager manager;
    private PlayerSkillsManager persistence;
    private Player player;
    private SkillInfo mining;
    private Skills skills;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        setStatic(SkillsManager.class, "instance", null);
        setStatic(PlayerDataManager.class, "instance", null);
        persistence = mock(PlayerSkillsManager.class);
        setStatic(PlayerSkillsManager.class, "instance", persistence);
        player = context.server().addPlayer("xp-player");
        when(persistence.canPersistPlayer(player.getUniqueId())).thenReturn(true);

        manager = new SkillsManager();
        mining = new SkillInfo("Mining", Map.of(), false, 10, List.of(
                new SkillObjective(SkillAction.MINE, "DIAMOND", 2, List.of()),
                new SkillObjective(SkillAction.MINE, "DIAMOND", 5, List.of("world"))),
                new ArrayList<>(List.of(10D, 20D)), Material.PAPER);
        manager.register("mining", mining);
        skills = new Skills(List.of(new me.lidan.cavecrawlers.skills.Skill(mining, 0)));
        skills.setUuid(player.getUniqueId());
        when(persistence.getSkills(player)).thenReturn(skills);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(SkillsManager.class, "instance", null);
        setStatic(PlayerDataManager.class, "instance", null);
        setStatic(PlayerSkillsManager.class, "instance", null);
        context.close();
    }

    @Test
    void objectiveSelectionPrefersMostSpecificWorldMatch() {
        manager.tryGiveXp(mining, SkillAction.MINE, Material.DIAMOND, player);

        assertEquals(5, skills.get(mining).getTotalXp());
    }

    @Test
    void objectiveWithWrongWorldIsIgnored() {
        SkillInfo otherWorld = new SkillInfo("MiningOther", Map.of(), false, 10, List.of(
                new SkillObjective(SkillAction.MINE, "DIAMOND", 7, List.of("nether"))),
                new ArrayList<>(List.of(10D)), Material.PAPER);
        manager.register("other", otherWorld);

        manager.tryGiveXp(otherWorld, SkillAction.MINE, "DIAMOND", player);

        assertEquals(0, skills.get(otherWorld).getTotalXp());
    }

    @Test
    void unsafePersistenceSessionBlocksXpAward() {
        when(persistence.canPersistPlayer(player.getUniqueId())).thenReturn(false);

        manager.tryGiveXp(mining, SkillAction.MINE, "DIAMOND", player);

        assertEquals(0, skills.get(mining).getTotalXp());
    }

    @Test
    void matchingObjectiveLookupReturnsOnlyMatchingSkillDefinitions() {
        SkillInfo other = new SkillInfo("Combat", Map.of(), false, 1,
                List.of(new SkillObjective(SkillAction.KILL, "ZOMBIE", 1, List.of())),
                new ArrayList<>(List.of(10D)), Material.IRON_SWORD);
        manager.register("combat", other);

        Map<SkillInfo, List<SkillObjective>> matches = manager.getObjectivesMatching(SkillAction.MINE, "diamond");

        assertEquals(1, matches.size());
        assertEquals(mining, matches.keySet().iterator().next());
        assertEquals(2, matches.get(mining).size());
    }

    @Test
    void xpEventCanModifyAwardAndCancellationPreventsMutation() {
        PluginManager pluginManager = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            doAnswer(invocation -> {
                SkillXpGainEvent event = invocation.getArgument(0);
                event.setXpGained(7);
                return null;
            }).when(pluginManager).callEvent(org.mockito.ArgumentMatchers.any(SkillXpGainEvent.class));

            manager.giveXp(player, mining, 1, false);
            assertEquals(7, skills.get(mining).getTotalXp());

            org.mockito.Mockito.reset(pluginManager);
            doAnswer(invocation -> {
                ((SkillXpGainEvent) invocation.getArgument(0)).setCancelled(true);
                return null;
            }).when(pluginManager).callEvent(org.mockito.ArgumentMatchers.any(SkillXpGainEvent.class));
            manager.giveXp(player, mining, 1, false);
            assertEquals(7, skills.get(mining).getTotalXp());
        }
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
