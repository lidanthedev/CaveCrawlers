package me.lidan.cavecrawlers.index;

import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class IndexItemGenerator {
    private final ItemsManager itemsManager = ItemsManager.getInstance();

    public Component resolveDropValue(Drop drop) {
        if (drop.getType() == DropType.ITEM) {
            Drop.ItemDropInfo result = Drop.getItemDropInfo(drop.getValue());
            if (result == null || result.itemInfo() == null) {
                return MiniMessageUtils.miniMessage("<red>Invalid Item");
            }
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("value", result.itemInfo().getFormattedName());
            placeholders.put("range", result.range().toString());
            placeholders.put("chance", String.format("%.2f%%", drop.getChance()));
            String chanceModifierIcon = drop.getChanceModifier() != null ? drop.getChanceModifier().getColoredIcon() : ChatColor.RED + "✘";
            String amountModifierIcon = drop.getAmountModifier() != null ? drop.getAmountModifier().getColoredIcon() : ChatColor.RED + "✘";
            placeholders.put("chance_modifier_icon", chanceModifierIcon);
            placeholders.put("amount_modifier_icon", amountModifierIcon);
            return MiniMessageUtils.miniMessage("<white> <range> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
        }
        return MiniMessageUtils.miniMessage("<yellow>Unknown Drop Type");
    }

    public Component dropToComponent(Drop drop) {
        return MiniMessageUtils.miniMessageString("");
    }
}
