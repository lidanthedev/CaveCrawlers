package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;

class PluginLifecycleCoverageTest {
    private MockCaveCrawlers context;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    void disableCancelsWorldWorkFlushesPersistenceBeforeClosingDatabaseAndCleansManagers() {
        MiningManager mining = mock(MiningManager.class);
        PlayerSkillsManager skills = mock(PlayerSkillsManager.class);
        Database database = mock(Database.class);
        AltarManager altars = mock(AltarManager.class);
        EntityManager entities = mock(EntityManager.class);

        try (MockedStatic<MiningManager> miningStatic = mockStatic(MiningManager.class);
             MockedStatic<PlayerSkillsManager> skillsStatic = mockStatic(PlayerSkillsManager.class);
             MockedStatic<Database> databaseStatic = mockStatic(Database.class);
             MockedStatic<AltarManager> altarStatic = mockStatic(AltarManager.class);
             MockedStatic<EntityManager> entityStatic = mockStatic(EntityManager.class)) {
            miningStatic.when(MiningManager::getInstance).thenReturn(mining);
            skillsStatic.when(PlayerSkillsManager::getInstance).thenReturn(skills);
            databaseStatic.when(Database::getInstance).thenReturn(database);
            altarStatic.when(AltarManager::getInstance).thenReturn(altars);
            entityStatic.when(EntityManager::getInstance).thenReturn(entities);

            context.plugin().onDisable();

            InOrder order = inOrder(mining, skills, database);
            order.verify(mining).regenBlocks();
            order.verify(skills).shutdown();
            order.verify(database).shutdown();
            verify(altars).reset();
            verify(entities).clear();
        }
    }
}
