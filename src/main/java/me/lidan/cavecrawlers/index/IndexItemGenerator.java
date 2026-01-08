package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexItemGenerator {
    public static final Component UNKNOWN_DROP = MiniMessageUtils.miniMessage("<yellow>Unknown Drop Type");
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();

    private static Component resolveCommandDrop(Drop drop) {
        Map<String, Object> placeholders = getPlaceholdersForCommandDrop(drop);
        return MiniMessageUtils.miniMessage("<white><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    private static @NonNull Map<String, Object> getPlaceholdersForCommandDrop(Drop drop) {
        return getPlaceholdersForValue(drop, drop.getValue());
    }

    private static Component resolveCoinsDrop(Drop drop) {
        Map<String, Object> placeholders = getPlaceholdersForCoinsDrop(drop);
        return MiniMessageUtils.miniMessage("<white><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
    }

    private static @NonNull Map<String, Object> getPlaceholdersForCoinsDrop(Drop drop) {
        Map<String, Object> placeholders = new HashMap<>();
        Range range = new Range(drop.getValue());
        if (range.getMin() == range.getMax()) {
            placeholders.put("value", ChatColor.GOLD + StringUtils.getNumberFormat(range.getMin()) + ChatColor.YELLOW + " Coins");
        } else {
            placeholders.put("value", ChatColor.GOLD + StringUtils.getNumberFormat(range.getMin()) + "-" + StringUtils.getNumberFormat(range.getMax()) + ChatColor.YELLOW + " Coins");
        }
        addDropPlaceholders(drop, placeholders);
        return placeholders;
    }

    private static void addDropPlaceholders(Drop drop, Map<String, Object> placeholders) {
        placeholders.put("chance", String.format("%.2f%%", drop.getChance()));
        String chanceModifierIcon = drop.getChanceModifier() != null ? drop.getChanceModifier().getColoredIcon() : ChatColor.RED + "✘";
        String amountModifierIcon = drop.getAmountModifier() != null ? drop.getAmountModifier().getColoredIcon() : ChatColor.RED + "✘";
        placeholders.put("chance_modifier", drop.getChanceModifier() != null ? drop.getChanceModifier().getFormatName() : "None");
        placeholders.put("amount_modifier", drop.getAmountModifier() != null ? drop.getAmountModifier().getFormatName() : "None");
        placeholders.put("chance_modifier_icon", chanceModifierIcon);
        placeholders.put("amount_modifier_icon", amountModifierIcon);
    }

    private static Component resolveItemDrop(Drop drop) {
        Drop.ItemDropInfo result = Drop.getItemDropInfo(drop.getValue());
        if (result == null || result.itemInfo() == null) {
            return MiniMessageUtils.miniMessage("<red>Invalid Item");
        }
        Map<String, Object> placeholders = getPlaceholdersForItemDrop(drop, result);
        return MiniMessageUtils.miniMessage("<white><range> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
    }

    private static @NonNull Map<String, Object> getPlaceholdersForItemDrop(Drop drop, Drop.ItemDropInfo result) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("value", result.itemInfo().getFormattedName());
        placeholders.put("range", result.range().toString());
        addDropPlaceholders(drop, placeholders);
        return placeholders;
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

    private static @NonNull Map<String, Object> getPlaceholdersForValue(Drop drop, String mobName) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("value", mobName);
        addDropPlaceholders(drop, placeholders);
        return placeholders;
    }

    public Map<String, Object> getDropPlaceholders(Drop drop) {
        if (drop.getType() == DropType.ITEM) {
            Drop.ItemDropInfo result = Drop.getItemDropInfo(drop.getValue());
            if (result != null) {
                return getPlaceholdersForItemDrop(drop, result);
            }
        } else if (drop.getType() == DropType.MOB) {
            String mobName = getMobNameByID(drop.getValue());
            if (mobName != null) {
                return getPlaceholdersForValue(drop, mobName);
            }
        } else if (drop.getType() == DropType.COINS) {
            return getPlaceholdersForCoinsDrop(drop);
        } else if (drop.getType() == DropType.COMMAND) {
            return getPlaceholdersForCommandDrop(drop);
        }
        return new HashMap<>();
    }

    private Component resolveMobDrop(Drop drop) {
        String mobName = getMobNameByID(drop.getValue());
        if (mobName == null) {
            return MiniMessageUtils.miniMessage("<red>Invalid Mob");
        }
        Map<String, Object> placeholders = getPlaceholdersForValue(drop, mobName);
        return MiniMessageUtils.miniMessage("<white><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    public Component dropToComponent(Drop drop) {
        return resolveDropValue(drop);
    }

    public List<Component> dropsToComponents(List<Drop> drops) {
        List<Component> components = new ArrayList<>();
        for (Drop drop : drops) {
            components.add(dropToComponent(drop));
        }
        return components;
    }

    public List<Component> dropsToLore(List<Drop> drops) {
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessage("<gray>-- Drops --"));
        if (drops.isEmpty()) {
            lore.add(MiniMessageUtils.miniMessage("<red>No Drops"));
        } else {
            for (Component dropComponent : dropsToComponents(drops)) {
                lore.add(MiniMessageUtils.miniMessage("<gray>- </gray>").append(dropComponent));
            }
        }
        return lore;
    }

    public List<Component> blockInfoToLore(BlockInfo blockInfo) {
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessage("<gray>-- Block Info --"));
        lore.add(MiniMessageUtils.miniMessage("<gray>Power: <green><block_power>", Map.of("block_power", StringUtils.getNumberFormat(blockInfo.getBlockPower()))));
        lore.add(MiniMessageUtils.miniMessage("<gray>Strength: <yellow><strength>", Map.of("strength", StringUtils.getNumberFormat(blockInfo.getBlockStrength()))));
        lore.add(MiniMessageUtils.miniMessage("<gray>Required Tool: <white><tool>", Map.of("tool", blockInfo.getBrokenBy().getName())));
        lore.add(Component.empty());
        lore.addAll(dropsToLore(blockInfo.getDrops()));
        return lore;
    }

    public ItemStack blockInfoToItemStack(BlockInfo blockInfo) {
        List<Component> lore = blockInfoToLore(blockInfo);
        return ItemBuilder.from(blockInfo.getBlock()).lore(lore).build();
    }
}
