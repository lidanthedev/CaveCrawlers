package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public class BossDropEditorMenu extends DropEditorMenu {

    public BossDropEditorMenu(Player player, BossDrop item, Consumer<BossDrop> onSave, Consumer<BossDrop> onDiscard) {
        super(player, item, drop -> onSave.accept((BossDrop) drop), drop -> onDiscard.accept((BossDrop) drop));
    }

    private BossDrop getBossDrop() {
        return (BossDrop) item;
    }

    @Override
    public void setupGui() {
        super.setupGui();

        BossDrop bossDrop = getBossDrop();

        // Required Points
        gui.setItem(3, 3, ItemBuilder.from(Material.NETHER_STAR)
                .name(MiniMessageUtils.miniMessage("<yellow>Required Points: <points>", Map.of("points", String.valueOf(bossDrop.getRequiredPoints()))))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().promptNumberMin(player, "Enter Required Points", 0).thenAccept(input -> {
                        bossDrop.setRequiredPoints(input);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set required points to %s".formatted(input)));
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        open();
                        return null;
                    });
                }));

        // Track
        String trackText = bossDrop.getTrack() != null ? bossDrop.getTrack() : "None";
        gui.setItem(3, 7, ItemBuilder.from(Material.RAIL)
                .name(MiniMessageUtils.miniMessage("<yellow>Track: <track>", Map.of("track", trackText)))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit", "<yellow>Right-Click to remove"))
                .asGuiItem(event -> {
                    if (event.isRightClick()) {
                        bossDrop.setTrack(null);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Removed track"));
                        setupGui();
                        return;
                    }
                    PromptManager.getInstance().prompt(player, "Enter Track Name").thenAccept(input -> {
                        bossDrop.setTrack(input);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set track to %s".formatted(input)));
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        open();
                        return null;
                    });
                }));

        gui.update();
    }
}

