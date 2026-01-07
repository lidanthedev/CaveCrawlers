package me.lidan.cavecrawlers.index;

import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class IndexItemGenerator {
    public static final Component UNKNOWN_DROP = MiniMessageUtils.miniMessage("<yellow>Unknown Drop Type");
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();

    private static Component resolveCommandDrop(Drop drop) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("value", drop.getValue());
        addDropPlaceholders(drop, placeholders);
        return MiniMessageUtils.miniMessage("<white> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    private static Component resolveCoinsDrop(Drop drop) {
        Map<String, Object> placeholders = new HashMap<>();
        Range range = new Range(drop.getValue());
        if (range.getMin() == range.getMax()) {
            placeholders.put("value", ChatColor.GOLD + StringUtils.getNumberFormat(range.getMin()) + ChatColor.YELLOW + " Coins");
        } else {
            placeholders.put("value", ChatColor.GOLD + StringUtils.getNumberFormat(range.getMin()) + "-" + StringUtils.getNumberFormat(range.getMax()) + ChatColor.YELLOW + " Coins");
        }
        addDropPlaceholders(drop, placeholders);
        return MiniMessageUtils.miniMessage("<white> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
    }

    private static void addDropPlaceholders(Drop drop, Map<String, Object> placeholders) {
        placeholders.put("chance", String.format("%.2f%%", drop.getChance()));
        String chanceModifierIcon = drop.getChanceModifier() != null ? drop.getChanceModifier().getColoredIcon() : ChatColor.RED + "✘";
        String amountModifierIcon = drop.getAmountModifier() != null ? drop.getAmountModifier().getColoredIcon() : ChatColor.RED + "✘";
        placeholders.put("chance_modifier_icon", chanceModifierIcon);
        placeholders.put("amount_modifier_icon", amountModifierIcon);
    }

    private static Component resolveItemDrop(Drop drop) {
        Drop.ItemDropInfo result = Drop.getItemDropInfo(drop.getValue());
        if (result == null || result.itemInfo() == null) {
            return MiniMessageUtils.miniMessage("<red>Invalid Item");
        }
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("value", result.itemInfo().getFormattedName());
        placeholders.put("range", result.range().toString());
        addDropPlaceholders(drop, placeholders);
        return MiniMessageUtils.miniMessage("<white> <range> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
    }

    public @Nullable String getMobNameByID(String id) {
        MythicMob mob = plugin.getMythicBukkit().getAPIHelper().getMythicMob(id);
        if (mob == null) {
            return null;
        }
        return mob.getDisplayName().get();
    }

    private Component resolveDropValue(Drop drop) {
        if (drop.getType() == DropType.ITEM) {
            return resolveItemDrop(drop);
        } else if (drop.getType() == DropType.MOB) {
            return resolveMobDrop(drop);
        } else if (drop.getType() == DropType.COINS) {
            return resolveCoinsDrop(drop);
        } else if (drop.getType() == DropType.COMMAND) {
            return resolveCommandDrop(drop);
        }
        return UNKNOWN_DROP;
    }

    private Component resolveMobDrop(Drop drop) {
        String mobName = getMobNameByID(drop.getValue());
        if (mobName == null) {
            return MiniMessageUtils.miniMessage("<red>Invalid Mob");
        }
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("value", mobName);
        addDropPlaceholders(drop, placeholders);
        return MiniMessageUtils.miniMessage("<white> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    public Component dropToComponent(Drop drop) {
        return resolveDropValue(drop);
    }
}
