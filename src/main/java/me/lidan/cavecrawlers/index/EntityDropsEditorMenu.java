package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.selectors.MythicMobSelector;
import me.lidan.cavecrawlers.prompt.PromptException;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EntityDropsEditorMenu extends BaseEditorMenu<EntityDrops> {
    public EntityDropsEditorMenu(Player player, EntityDrops item, Consumer<EntityDrops> onSave, Consumer<EntityDrops> onClose) {
        super(player, item.deepCopy(), onSave, onClose);
    }

    @Override
    public void setupGui() {
        gui.getFiller().fill(GuiItems.GLASS_ITEM);

        // Entity Name
        gui.setItem(2, 5, ItemBuilder.from(Material.NAME_TAG)
                .name(MiniMessageUtils.miniMessage("<yellow>Entity Name: <name>", Map.of("name", item.getEntityName())))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to select mob"))
                .asGuiItem(event -> new MythicMobSelector(player, "",
                        MiniMessageUtils.miniMessage("Select Entity"),
                        (clickEvent, mob) -> {
                            String internalName = mob.getInternalName();
                            PlaceholderString dn = mob.getDisplayName();
                            String label = (dn != null && dn.isPresent()) ? dn.get() : internalName;
                            item.setEntityName(internalName);
                            player.sendMessage(MiniMessageUtils.miniMessage("<green>Set entity to %s (%s)".formatted(label, internalName)));
                            setupGui();
                            open();
                        }, () -> {
                    setupGui();
                    open();
                }).open()));

        // Drop List
        List<Drop> dropList = item.getDropList();
        List<Component> lore = indexManager.dropsToComponents(dropList);
        lore.addAll(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit drops"));
        gui.setItem(3, 5, ItemBuilder.from(Material.CHEST)
                .name(MiniMessageUtils.miniMessage("<yellow>Drops (<count>)", Map.of("count", String.valueOf(dropList.size()))))
                .lore(lore)
                .asGuiItem(event -> {
                    new DropListEditorMenu(player, new ArrayList<>(dropList), updatedDrops -> {
                        item.setDropList(updatedDrops);
                    }, discarded -> {
                        setupGui();
                        open();
                    }).open();
                }));

        // XP
        gui.setItem(4, 5, ItemBuilder.from(Material.EXPERIENCE_BOTTLE)
                .name(MiniMessageUtils.miniMessage("<yellow>XP: <xp>", Map.of("xp", String.valueOf(item.getXp()))))
                .lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit"))
                .asGuiItem(event -> {
                    PromptManager.getInstance().promptNumberMin(player, "Enter XP amount", 0).thenAccept(input -> {
                        item.setXp(input);
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Set XP to %s".formatted(input)));
                        setupGui();
                        open();
                    }).exceptionally(throwable -> {
                        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                        if (cause instanceof PromptException) {
                            player.sendMessage(MiniMessageUtils.miniMessage("<red>%s".formatted(cause.getMessage())));
                        } else {
                            player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                        }
                        open();
                        return null;
                    });
                }));

        // Back / Save button
        gui.setItem(6, 1, createBackItem());
        gui.update();
    }
}
