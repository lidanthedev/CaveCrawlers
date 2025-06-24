package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ConfirmGui {
    private final Player player;
    private final Component title;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final Gui gui;

    public ConfirmGui(Player player, Component title, Runnable onConfirm, Runnable onCancel) {
        this.player = player;
        this.title = title;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.gui = Gui.gui().title(title).rows(3).create();
        gui.disableAllInteractions();
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        initGui();
    }

    public ConfirmGui(Player player, Component title, Runnable onConfirm) {
        this(player, title, onConfirm, null);
    }

    public ConfirmGui(Player player, Runnable onConfirm, Runnable onCancel) {
        this(player, Component.text("Confirm Action"), onConfirm, onCancel);
    }

    public void initGui() {
        gui.setItem(2, 5, ItemBuilder.from(Material.PAPER).name(MiniMessageUtils.miniMessage("<yellow>Are you sure?")).asGuiItem());

        gui.setItem(2, 8, ItemBuilder.from(Material.RED_CONCRETE).name(MiniMessageUtils.miniMessage("<red>Cancel")).asGuiItem(event -> {
            player.closeInventory();
            if (onCancel != null) {
                onCancel.run();
            }
        }));

        gui.setItem(2, 2, ItemBuilder.from(Material.GREEN_CONCRETE).name(MiniMessageUtils.miniMessage("<green>Confirm")).asGuiItem(event -> {
            player.closeInventory();
            if (onConfirm != null) {
                onConfirm.run();
            }
        }));
    }

    public void open() {
        gui.open(player);
    }
}
