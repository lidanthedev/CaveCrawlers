package me.lidan.cavecrawlers.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.prompt.PromptException;
import me.lidan.cavecrawlers.prompt.PromptFuture;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChatPromptListener implements Listener {

    Cooldown<UUID> cooldown = new Cooldown<>(TimeUnit.SECONDS.toMillis(5));

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = PromptManager.getFutureMap().remove(playerUUID);
        if (future != null) {
            String message = MiniMessageUtils.componentToString(event.message());
            Bukkit.getScheduler().runTask(CaveCrawlers.getInstance(), () -> future.complete(message));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = PromptManager.getFutureMap().remove(playerUUID);
        if (future != null) {
            Bukkit.getScheduler().runTask(CaveCrawlers.getInstance(), () -> future.completeExceptionally(new PromptException("Player rejected the prompt")));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = PromptManager.getFutureMap().remove(playerUUID);
        if (future != null) {
            Bukkit.getScheduler().runTask(CaveCrawlers.getInstance(), () -> future.completeExceptionally(new PromptException("Player disconnected while waiting for prompt response")));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        PromptFuture future = PromptManager.getFutureMap().get(playerUUID);
        if (future != null) {
            if (!cooldown.isCooldownFinished(playerUUID)) {
                return;
            }
            cooldown.startCooldown(playerUUID);
            PromptManager.showTitle(event.getPlayer(), future.getPromptTitle(), PromptManager.PROMPT_SUBTITLE_TO_CANCEL);
        }
    }
}
