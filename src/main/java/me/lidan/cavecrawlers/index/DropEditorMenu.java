package me.lidan.cavecrawlers.index;

import com.cryptomorin.xseries.XEnchantment;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.gui.GuiItems;
import me.lidan.cavecrawlers.gui.selectors.StatTypeSelector;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DropEditorMenu extends BaseEditorMenu<Drop> {
    public DropEditorMenu(Player player, Drop item, Consumer<Drop> onSave, Consumer<Drop> onDiscard) {
        super(player, item.clone(), onSave, onDiscard);
    }

    public static ItemBuilder createDropItem(Drop drop) {
        Component name = indexManager.dropToComponent(drop);
        return ItemBuilder.from(drop.getType().getMaterial()).name(name).lore(Component.empty(), MiniMessageUtils.miniMessage("<yellow>Click to edit drop"), MiniMessageUtils.miniMessage("<yellow>Shift-Left click to move down"), MiniMessageUtils.miniMessage("<yellow>Shift-Right click to move up"), MiniMessageUtils.miniMessage("<yellow>Drop item to delete"));
    }

    public void validate() throws IllegalStateException {
        if (item.getChance() < 0) {
            throw new IllegalStateException("Chance must be greater than or equal to 0");
        }
        if (item.getValue() == null || item.getValue().isEmpty()) {
            throw new IllegalStateException("Value cannot be empty");
        }
        Component name = indexManager.dropToComponent(item);
        String uncoloredName = MiniMessageUtils.componentToString(name);
        if (uncoloredName.contains("Invalid")) {
            throw new IllegalStateException("Invalid drop value");
        }
    }

    @Override
    public void setupGui() {
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
        DropType[] dropTypes = DropType.values();
        List<Integer> layoutForItems = GuiItems.getLayoutForItems(dropTypes.length);
        DropType selectedType = item.getType();
        Enchantment enchantment = XEnchantment.UNBREAKING.get();
        gui.setItem(1, 5, createDropItem(item).asGuiItem());
        for (int i = 0; i < dropTypes.length; i++) {
            DropType dropType = dropTypes[i];
            Material material = dropType.getMaterial();
            ItemBuilder itemBuilder = ItemBuilder.from(material)
                    .name(MiniMessageUtils.miniMessage("<yellow>%s".formatted(dropType.name())))
                    .lore(MiniMessageUtils.miniMessageList(
                            "<gray>Click to select %s drop type".formatted(dropType.name()),
                            dropType == selectedType ? "<green>Selected" : "<red>Not Selected"
                    ));
            if (dropType == selectedType && enchantment != null) {
                itemBuilder.enchant(enchantment, 1).flags(ItemFlag.HIDE_ENCHANTS);
            }
            gui.setItem(2, layoutForItems.get(i), itemBuilder.asGuiItem(event -> {
                item.setType(dropType);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Set drop type to %s".formatted(dropType.name())));
                setupGui();
            }));
        }
        gui.setItem(4, 5, ItemBuilder.from(Material.PAPER).name(MiniMessageUtils.miniMessage("<yellow>Value: <value>", Map.of("value", item.getValue()))).asGuiItem(event -> {
            PromptManager.getInstance().prompt(player, "Enter new value").thenAccept(input -> {
                item.setValue(input);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Set drop value to %s".formatted(input)));
                setupGui();
                open();
            }).exceptionally(throwable -> {
                player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                open();
                return null;
            });
        }));
        gui.setItem(5, 3, ItemBuilder.from(Material.EMERALD).name(MiniMessageUtils.miniMessage("<green>Chance: <chance>", Map.of("chance", item.getChance()))).asGuiItem(event -> {
            PromptManager.getInstance().promptDoubleMin(player, "Enter new chance", 0).thenAccept(input -> {
                item.setChance(input);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Set drop chance to %s".formatted(input)));
                setupGui();
                open();
            }).exceptionally(throwable -> {
                player.sendMessage(MiniMessageUtils.miniMessage("<red>Input cancelled"));
                open();
                return null;
            });
        }));
        String chanceModifierText = item.getChanceModifier() != null ? item.getChanceModifier().name() : "None";
        gui.setItem(5, 5, ItemBuilder.from(Material.STICK).name(MiniMessageUtils.miniMessage("<green>Chance Modifier: <modifier>", Map.of("modifier", chanceModifierText))).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit", "<yellow>Right-Click to remove")).asGuiItem(event -> {
            if (event.isRightClick()) {
                item.setChanceModifier(null);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Removed drop chance modifier"));
                setupGui();
                open();
                return;
            }
            new StatTypeSelector(player, "", (inventoryClickEvent, statType) -> {
                item.setChanceModifier(statType);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Set drop chance modifier to %s".formatted(statType.name())));
                setupGui();
                open();
            }).open();
        }));
        String amountModifierText = item.getAmountModifier() != null ? item.getAmountModifier().name() : "None";
        gui.setItem(5, 7, ItemBuilder.from(Material.QUARTZ).name(MiniMessageUtils.miniMessage("<green>Amount Modifier: <modifier>", Map.of("modifier", amountModifierText))).lore(MiniMessageUtils.miniMessageList("", "<yellow>Click to edit", "<yellow>Right-Click to remove")).asGuiItem(event -> {
            if (event.isRightClick()) {
                item.setAmountModifier(null);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Removed drop amount modifier"));
                setupGui();
                open();
                return;
            }
            new StatTypeSelector(player, "", (inventoryClickEvent, statType) -> {
                item.setAmountModifier(statType);
                player.sendMessage(MiniMessageUtils.miniMessage("<green>Set drop amount modifier to %s".formatted(statType.name())));
                setupGui();
                open();
            }).open();
        }));
        gui.setItem(6, 1, createBackItem());
        gui.update();
    }

    @Override
    public boolean save() {
        try {
            validate();
            super.save();
            player.sendMessage(MiniMessageUtils.miniMessage("<green>Drop saved"));
            return true;
        } catch (IllegalStateException e) {
            player.sendMessage(MiniMessageUtils.miniMessage("<red>Failed to save drop: %s".formatted(e.getMessage())));
            return false;
        }
    }
}
