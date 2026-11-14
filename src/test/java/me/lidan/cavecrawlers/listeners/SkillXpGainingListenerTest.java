package me.lidan.cavecrawlers.listeners;

import me.lidan.cavecrawlers.entities.BossEntityData;
import me.lidan.cavecrawlers.entities.EntityData;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.entities.LootShareEntityData;
import me.lidan.cavecrawlers.skills.SkillAction;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.Mockito.*;

class SkillXpGainingListenerTest {
    @Test
    void bossWithoutKillerStillAwardsOnlineParticipants() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start();
             MockedStatic<SkillsManager> skills = mockStatic(SkillsManager.class)) {
            SkillsManager manager = mock(SkillsManager.class);
            skills.when(SkillsManager::getInstance).thenReturn(manager);
            Player helper = context.server().addPlayer("helper");
            LivingEntity boss = entity(null);
            BossEntityData data = new BossEntityData(boss);
            data.addDamage(helper.getUniqueId(), 1);
            EntityManager.getInstance().setEntityData(boss.getUniqueId(), data);
            try {
                new SkillXpGainingListener().onEntityDeath(death(boss));

                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", helper);
                verifyNoMoreInteractions(manager);
            } finally {
                EntityManager.getInstance().clear();
            }
        }
    }

    @Test
    void ordinaryMobWithoutKillerDoesNotAwardXp() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start();
             MockedStatic<SkillsManager> skills = mockStatic(SkillsManager.class)) {
            SkillsManager manager = mock(SkillsManager.class);
            skills.when(SkillsManager::getInstance).thenReturn(manager);

            new SkillXpGainingListener().onEntityDeath(death(entity(null)));

            verifyNoInteractions(manager);
        }
    }

    @Test
    void bossAwardsEveryOnlineParticipantOnceIncludingSmallContributors() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start();
             MockedStatic<SkillsManager> skills = mockStatic(SkillsManager.class)) {
            SkillsManager manager = mock(SkillsManager.class);
            skills.when(SkillsManager::getInstance).thenReturn(manager);
            Player killer = context.server().addPlayer("killer");
            Player helper = context.server().addPlayer("helper");
            context.server().addPlayer("bystander");
            LivingEntity boss = entity(killer);
            BossEntityData data = new BossEntityData(boss);
            data.addDamage(killer.getUniqueId(), 999);
            data.addDamage(killer.getUniqueId(), 100);
            data.addDamage(helper.getUniqueId(), 1);
            data.addDamage(UUID.randomUUID(), 50);
            EntityManager.getInstance().setEntityData(boss.getUniqueId(), data);
            try {
                new SkillXpGainingListener().onEntityDeath(death(boss));

                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", killer);
                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", helper);
                verifyNoMoreInteractions(manager);
            } finally {
                EntityManager.getInstance().clear();
            }
        }
    }

    @Test
    void ordinaryMobStillAwardsOnlyTheKiller() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start();
             MockedStatic<SkillsManager> skills = mockStatic(SkillsManager.class)) {
            SkillsManager manager = mock(SkillsManager.class);
            skills.when(SkillsManager::getInstance).thenReturn(manager);
            Player killer = context.server().addPlayer("killer");
            Player helper = context.server().addPlayer("helper");
            LivingEntity mob = entity(killer);
            EntityData data = new EntityData(mob);
            data.addDamage(helper.getUniqueId(), 10);
            EntityManager.getInstance().setEntityData(mob.getUniqueId(), data);
            try {
                new SkillXpGainingListener().onEntityDeath(death(mob));

                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", killer);
                verifyNoMoreInteractions(manager);
            } finally {
                EntityManager.getInstance().clear();
            }
        }
    }

    @Test
    void lootShareStillUsesItsConfiguredDamageThreshold() throws Exception {
        try (MockCaveCrawlers context = MockCaveCrawlers.start();
             MockedStatic<SkillsManager> skills = mockStatic(SkillsManager.class)) {
            SkillsManager manager = mock(SkillsManager.class);
            skills.when(SkillsManager::getInstance).thenReturn(manager);
            Player killer = context.server().addPlayer("killer");
            Player helper = context.server().addPlayer("helper");
            Player smallContributor = context.server().addPlayer("small-contributor");
            LivingEntity mob = entity(killer);
            when(mob.getMaxHealth()).thenReturn(100D);
            LootShareEntityData data = new LootShareEntityData(mob, 10, killer.getUniqueId());
            data.addDamage(killer.getUniqueId(), 89);
            data.addDamage(helper.getUniqueId(), 10);
            data.addDamage(smallContributor.getUniqueId(), 1);
            EntityManager.getInstance().setEntityData(mob.getUniqueId(), data);
            try {
                new SkillXpGainingListener().onEntityDeath(death(mob));

                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", killer);
                verify(manager).tryGiveXp(SkillAction.KILL, "ZOMBIE", helper);
                verifyNoMoreInteractions(manager);
            } finally {
                EntityManager.getInstance().clear();
            }
        }
    }

    private static LivingEntity entity(Player killer) {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getKiller()).thenReturn(killer);
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        return entity;
    }

    private static EntityDeathEvent death(LivingEntity entity) {
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
        return event;
    }
}
