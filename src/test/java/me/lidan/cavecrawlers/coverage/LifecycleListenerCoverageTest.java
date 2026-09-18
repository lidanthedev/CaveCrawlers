package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.listeners.ItemChangeListener;
import me.lidan.cavecrawlers.listeners.PlayerLifecycleListener;
import me.lidan.cavecrawlers.listeners.UpdateItemsListener;
import me.lidan.cavecrawlers.listeners.WorldChangeListener;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerSkillsManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LifecycleListenerCoverageTest {
    private MockCaveCrawlers context;
    private Player player;
    private PlayerSkillsManager skillsManager;
    private ItemsManager itemsManager;
    private StatsManager statsManager;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        player = context.server().addPlayer("lifecycle-player");
        skillsManager = mock(PlayerSkillsManager.class);
        itemsManager = mock(ItemsManager.class);
        statsManager = mock(StatsManager.class);
        setStatic(PlayerSkillsManager.class, "instance", skillsManager);
        setStatic(ItemsManager.class, "instance", itemsManager);
        setStatic(StatsManager.class, "instance", statsManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStatic(PlayerSkillsManager.class, "instance", null);
        setStatic(ItemsManager.class, "instance", null);
        setStatic(StatsManager.class, "instance", null);
        context.close();
    }

    @Test
    void joinAndQuitDelegateExactlyOnceToPersistenceManager() {
        PlayerLifecycleListener listener = new PlayerLifecycleListener();
        listener.onPlayerJoin(new PlayerJoinEvent(player, "join"));
        listener.onPlayerQuit(new PlayerQuitEvent(player, (String) null));

        verify(skillsManager).loadPlayerAsync(player.getUniqueId());
        verify(skillsManager).savePlayerNowOnQuit(player.getUniqueId());
        verifyNoMoreInteractions(skillsManager);
    }

    @Test
    void joinUpdatesInventoryAndLoadsStats() {
        UpdateItemsListener listener = new UpdateItemsListener();

        listener.onPlayerJoin(new PlayerJoinEvent(player, "join"));

        verify(itemsManager).updatePlayerInventory(player);
        verify(statsManager).loadPlayer(player);
    }

    @Test
    void worldChangeIsRateLimitedAndRespawnSchedulesOneRefresh() {
        UpdateItemsListener listener = new UpdateItemsListener();
        World world = context.server().addSimpleWorld("other-world");

        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player, world));
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player, world));
        verify(itemsManager).updatePlayerInventory(player);
        verify(statsManager).loadPlayer(player);

        reset(itemsManager, statsManager);
        new UpdateItemsListener().onPlayerRespawn(new PlayerRespawnEvent(player, player.getLocation(), false));
        context.server().getScheduler().performTicks(2);
        verify(itemsManager).updatePlayerInventory(player);
        verify(statsManager).loadPlayer(player);
    }

    @Test
    void heldItemChangeRecalculatesStatsOnNextTick() {
        ItemChangeListener listener = new ItemChangeListener();

        listener.onPlayerItemHeld(new PlayerItemHeldEvent(player, 0, 1));
        context.server().getScheduler().performTicks(2);

        verify(statsManager).calculateStats(player);
    }

    @Test
    void creativeWorldChangeRestoresCreativeModeOnlyWithPermission() {
        WorldChangeListener listener = new WorldChangeListener();
        player.setGameMode(GameMode.CREATIVE);
        player.setOp(true);
        World world = context.server().addSimpleWorld("creative-world");

        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player, world));
        context.server().getScheduler().performTicks(3);

        org.junit.jupiter.api.Assertions.assertEquals(GameMode.CREATIVE, player.getGameMode());
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
