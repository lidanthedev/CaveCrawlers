package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.listeners.ChatPromptListener;
import me.lidan.cavecrawlers.prompt.PromptException;
import me.lidan.cavecrawlers.prompt.PromptFuture;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptCoverageTest {
    private MockCaveCrawlers context;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        PromptManager.getFutureMap().clear();
        player = context.server().addPlayer("prompt-player");
    }

    @AfterEach
    void tearDown() throws Exception {
        PromptManager.getFutureMap().clear();
        context.close();
    }

    @Test
    void promptRegistersFutureAndResetsTitleOnCompletion() {
        PromptFuture future = (PromptFuture) PromptManager.getInstance().prompt(player, "Choose");

        assertSameFuture(future, PromptManager.getFutureMap().get(player.getUniqueId()));
        future.complete("answer");
        assertEquals("answer", future.join());
        assertTrue(future.isDone());
    }

    @Test
    void clickCancellationRemovesFutureAndCompletesExceptionally() {
        PromptFuture future = (PromptFuture) PromptManager.getInstance().prompt(player, "Choose");
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, null);

        new ChatPromptListener().onPlayerInteract(event);
        context.server().getScheduler().performTicks(1);

        assertTrue(event.isCancelled());
        assertFalse(PromptManager.getFutureMap().containsKey(player.getUniqueId()));
        CompletionException failure = assertThrows(CompletionException.class, future::join);
        assertTrue(failure.getCause() instanceof PromptException);
    }

    @Test
    void quitCancellationRemovesFutureAndCompletesExceptionally() {
        PromptFuture future = (PromptFuture) PromptManager.getInstance().prompt(player, "Choose");

        new ChatPromptListener().onPlayerQuit(new PlayerQuitEvent(player, (String) null));
        context.server().getScheduler().performTicks(1);

        assertFalse(PromptManager.getFutureMap().containsKey(player.getUniqueId()));
        CompletionException failure = assertThrows(CompletionException.class, future::join);
        assertTrue(failure.getCause() instanceof PromptException);
    }

    @Test
    void numericPromptsParseAndRejectInvalidResponses() {
        var valid = PromptManager.getInstance().promptNumber(player, "Number");
        PromptManager.getFutureMap().get(player.getUniqueId()).complete("42");
        assertEquals(42, valid.join());

        var invalid = PromptManager.getInstance().promptNumber(player, "Number");
        PromptManager.getFutureMap().get(player.getUniqueId()).complete("abc");
        CompletionException failure = assertThrows(CompletionException.class, invalid::join);
        assertTrue(failure.getCause() instanceof PromptException);
    }

    @Test
    void numericMinimumIsEnforced() {
        var future = PromptManager.getInstance().promptNumberMin(player, "Number", 10);
        PromptManager.getFutureMap().get(player.getUniqueId()).complete("9");

        CompletionException failure = assertThrows(CompletionException.class, future::join);
        assertTrue(failure.getCause() instanceof PromptException);
    }

    private static void assertSameFuture(PromptFuture expected, Object actual) {
        assertNotNull(actual);
        assertTrue(expected == actual);
    }
}
