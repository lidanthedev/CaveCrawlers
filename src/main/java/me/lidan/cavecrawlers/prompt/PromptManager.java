package me.lidan.cavecrawlers.prompt;

import lombok.Getter;
import me.lidan.cavecrawlers.api.PromptAPI;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.TitleBuilder;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PromptManager implements PromptAPI {
    @Getter
    private static final Map<UUID, PromptFuture> futureMap = new ConcurrentHashMap<>();
    public static final int TITLE_FADE_IN_AND_OUT = 500;
    public static final int TITLE_STAY = 10000;
    public static final String PROMPT_SUBTITLE = "Type your response in chat";
    public static final String PROMPT_SUBTITLE_TO_CANCEL = "Left click to cancel";
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

    @Override
    public CompletableFuture<String> prompt(Player player, String promptTitle) {
        return prompt(player, promptTitle, PROMPT_SUBTITLE);
    }

    @Override
    public CompletableFuture<String> prompt(Player player, String promptTitle, String promptSubtitle) {
        PromptFuture future = new PromptFuture(promptTitle);
        futureMap.put(player.getUniqueId(), future);
        player.closeInventory();
        showTitle(player, promptTitle, promptSubtitle);

        future.whenComplete((response, throwable) -> player.resetTitle());

        return future;
    }

    @Override
    public CompletableFuture<Integer> promptNumber(Player player, String promptTitle) {
        return prompt(player, promptTitle)
                .thenApply(response -> {
                    try {
                        return Integer.parseInt(response);
                    } catch (NumberFormatException e) {
                        throw new PromptException("Invalid number format: " + response, e);
                    }
                });
    }

    @Override
    public CompletableFuture<Integer> promptNumberMin(Player player, String promptTitle, int min) {
        return promptNumber(player, promptTitle)
                .thenApply(response -> {
                    if (response < min) {
                        throw new PromptException("Number must be at least " + min + ": " + response);
                    }
                    return response;
                });
    }

    @Override
    public CompletableFuture<Double> promptDouble(Player player, String promptTitle) {
        return prompt(player, promptTitle)
                .thenApply(response -> {
                    try {
                        double parsed = Double.parseDouble(response);
                        if (!Double.isFinite(parsed)) {
                            throw new PromptException("Number must be finite: " + response);
                        }
                        return parsed;
                    } catch (NumberFormatException e) {
                        throw new PromptException("Invalid number format: " + response, e);
                    }
                });
    }

    @Override
    public CompletableFuture<Double> promptDoubleMin(Player player, String promptTitle, double min) {
        return promptDouble(player, promptTitle)
                .thenApply(response -> {
                    if (response < min) {
                        throw new PromptException("Number must be at least " + min + ": " + response);
                    }
                    return response;
                });
    }

    public <T extends Enum<T>> CompletableFuture<T> promptEnum(Player player, String promptTitle, Class<T> enumClass) {
        return prompt(player, promptTitle)
                .thenApply(response -> {
                    try {
                        return Enum.valueOf(enumClass, response.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new PromptException("Invalid input: " + response, e);
                    }
                });
    }

    public CompletableFuture<StatType> promptStatType(Player player, String promptTitle) {
        return promptStatType(player, promptTitle, false);
    }

    public CompletableFuture<StatType> promptStatType(Player player, String promptTitle, boolean showStatTypes) {
        if (showStatTypes) {
            player.sendMessage(MiniMessageUtils.miniMessage("<yellow>Available Stat Types: <gold>Click on a stat type to select it"));
            for (StatType value : StatType.values()) {
                player.sendMessage(MiniMessageUtils.miniMessage("<gold><click:run_command:'/cavecrawlers prompt answer <stat_name_raw>'><stat_name> (<stat_name_raw>)</click>", Map.of("stat_name", value.getFormatNameComponent(), "stat_name_raw", value.name())));
            }
        }
        return prompt(player, promptTitle)
                .thenApply(response -> {
                    try {
                        return StatType.valueOf(response.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new PromptException("Invalid stat type: " + response, e);
                    }
                });
    }

    public static void showTitle(Player player, String promptTitle) {
        showTitle(player, promptTitle, PROMPT_SUBTITLE);
    }

    public static void showTitle(Player player, String promptTitle, String promptSubtitle) {
        new TitleBuilder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setFadeIn(TITLE_FADE_IN_AND_OUT)
                .setStay(TITLE_STAY)
                .setFadeOut(TITLE_FADE_IN_AND_OUT)
                .setPlayers(player)
                .show();
    }
}
