package me.lidan.cavecrawlers.prompt;

import lombok.Getter;
import me.lidan.cavecrawlers.utils.TitleBuilder;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PromptManager {
    @Getter
    private static final Map<UUID, CompletableFuture<String>> futureMap = new ConcurrentHashMap<>();
    public static final int TITLE_FADE_IN_AND_OUT = 500;
    public static final int TITLE_STAY = 10000;
    private static PromptManager instance;

    public static PromptManager getInstance() {
        if (instance == null) {
            instance = new PromptManager();
        }
        return instance;
    }

    private PromptManager() {
        // Private constructor to prevent instantiation
    }

    public CompletableFuture<String> prompt(Player player, String promptTitle) {
        return prompt(player, promptTitle, "Type your response in chat");
    }

    public CompletableFuture<String> prompt(Player player, String promptTitle, String promptSubtitle) {
        CompletableFuture<String> future = new CompletableFuture<>();
        futureMap.put(player.getUniqueId(), future);
        player.closeInventory();
        new TitleBuilder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setFadeIn(TITLE_FADE_IN_AND_OUT)
                .setStay(TITLE_STAY)
                .setFadeOut(TITLE_FADE_IN_AND_OUT)
                .setPlayers(player)
                .show();

        future.whenComplete((response, throwable) -> player.resetTitle());

        return future;
    }

}
