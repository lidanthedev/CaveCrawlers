package me.lidan.cavecrawlers.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChatPromptListener implements Listener {

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = PromptManager.getFutureMap().remove(playerUUID);
        if (future != null) {
            String message = MiniMessageUtils.componentToString(event.message());
            Bukkit.getScheduler().runTask(CaveCrawlers.getInstance(), () -> future.complete(message));
            PromptManager.getFutureMap().remove(playerUUID);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        CompletableFuture<String> future = PromptManager.getFutureMap().get(playerUUID);
        if (future != null) {
            Bukkit.getScheduler().runTask(CaveCrawlers.getInstance(), () -> future.completeExceptionally(new RuntimeException("Player rejected the prompt")));
            PromptManager.getFutureMap().remove(playerUUID);
            event.setCancelled(true);
        }
    }
}
