package me.lidan.cavecrawlers.index;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexItemGenerator {
    public static final Component UNKNOWN_DROP = MiniMessageUtils.miniMessage("<yellow>Unknown Drop Type");
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final Logger log = LoggerFactory.getLogger(IndexItemGenerator.class);
    private final Map<String, MythicMob> reverseMobNameCache = new HashMap<>();

    private static Component resolveCommandDrop(Drop drop) {
        Map<String, Object> placeholders = getPlaceholdersForCommandDrop(drop);
        return MiniMessageUtils.miniMessage("<gray><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    private static Component resolveCoinsDrop(Drop drop) {
        Map<String, Object> placeholders = getPlaceholdersForCoinsDrop(drop);
        return MiniMessageUtils.miniMessage("<gray><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
    }

    private static @NonNull Map<String, Object> getPlaceholdersForCommandDrop(Drop drop) {
        return getPlaceholdersForValue(drop, drop.getValue());
    }

    private static Component resolveItemDrop(Drop drop) {
        Drop.ItemDropInfo result = Drop.getItemDropInfo(drop.getValue());
        if (result == null || result.itemInfo() == null) {
            return MiniMessageUtils.miniMessage("<red>Invalid Item");
        }
        Map<String, Object> placeholders = getPlaceholdersForItemDrop(drop, result);
        return MiniMessageUtils.miniMessage("<gray><range> <value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon> <amount_modifier_icon>", placeholders);
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

    private MythicMob getMobByName(String name) {
        name = ChatColor.translateAlternateColorCodes('&', name);
        return reverseMobNameCache.computeIfAbsent(name, mobName -> {
            for (MythicMob mob : plugin.getMythicBukkit().getMobManager().getMobTypes()) {
                if (mob.getDisplayName() != null && mob.getDisplayName().isPresent()) {
                    if (mob.getDisplayName().get().equalsIgnoreCase(mobName))
                        return mob;
                }
            }
            return null;
        });
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
        return MiniMessageUtils.miniMessage("<gray><value> <gray>(<green><chance><gray>)<reset> <chance_modifier_icon>", placeholders);
    }

    /**
     * Generates a consistent, deterministic icon gradient based on the input string.
     * Example output: <gradient:#ff0000:#ffaa00>■</gradient>
     */
    public static String getTrackIcon(String trackId) {
        // 1. Generate a seed from the string
        int hash = trackId.hashCode();

        // 2. Calculate the base Hue (0.0 to 1.0)
        // We use absolute value to handle negative hashes
        float hue = (Math.abs(hash) % 360) / 360f;

        // 3. Define Saturation and Brightness (Keep these high for "Cool" neon look)
        float saturation = 0.85f; // 85% Saturation (Vibrant)
        float brightness = 1.0f;  // 100% Brightness (Readable)

        // 4. Create the two colors for the gradient
        // Color 1: The base color
        Color c1 = Color.getHSBColor(hue, saturation, brightness);

        // Color 2: Shift the hue slightly (e.g., +45 degrees) for a nice analog gradient
        // The % 1.0f wraps it around if it goes over 360 degrees
        Color c2 = Color.getHSBColor((hue + 0.125f) % 1.0f, saturation, brightness);

        // 5. Convert to Hex
        String hex1 = String.format("#%06x", c1.getRGB() & 0x00FFFFFF);
        String hex2 = String.format("#%06x", c2.getRGB() & 0x00FFFFFF);

        // 6. Return the MiniMessage string
        // You can change "●" to whatever 1-char icon you prefer (e.g. ✦, ■, or the first letter)
        return "<gradient:" + hex1 + ":" + hex2 + ">●</gradient>";
    }


    public Component dropToComponent(Drop drop) {
        Component component = resolveDropValue(drop);
        if (drop instanceof BossDrop bossDrop && bossDrop.getTrack() != null) {
            component = MiniMessageUtils.miniMessage(getTrackIcon(bossDrop.getTrack())).append(component).append(MiniMessageUtils.miniMessage("<gold> ><points>", Map.of("points", StringUtils.getNumberFormat(bossDrop.getRequiredPoints()))));
        }
        return component;
    }

    public <T extends Drop> List<Component> dropsToComponents(List<T> drops) {
        List<Component> components = new ArrayList<>();
        for (Drop drop : drops) {
            components.add(dropToComponent(drop));
        }
        return components;
    }

    public <T extends Drop> List<Component> dropsToLore(List<T> drops) {
        return dropsToLore(drops, "Drops");
    }

    public <T extends Drop> List<Component> dropsToLore(List<T> drops, String header) {
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessage("<gray>-- <header> --", Map.of("header", header)));
        if (drops.isEmpty()) {
            lore.add(MiniMessageUtils.miniMessage("<red>No <header>", Map.of("header", header)));
            return lore;
        }

        for (Component dropComponent : dropsToComponents(drops)) {
            lore.add(MiniMessageUtils.miniMessage("<gray>- </gray>").append(dropComponent));
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

    private List<Component> mobInfoToLore(String bossDrops) {
        List<Component> lore = new ArrayList<>();
        MythicMob mob = getMobByName(bossDrops);
        if (mob != null) {
            lore.add(MiniMessageUtils.miniMessage("<gray>-- Mob Info --"));
            lore.add(MiniMessageUtils.miniMessage("<gray>Health: <red><health>", Map.of("health", StringUtils.getNumberFormat(mob.getHealth().get()))));
            lore.add(MiniMessageUtils.miniMessage("<gray>Damage: <red><damage>", Map.of("damage", StringUtils.getNumberFormat(mob.getDamage().get()))));
        }
        return lore;
    }

    public List<Component> entityDropsToLore(EntityDrops entityDrops) {
        List<Component> lore = new ArrayList<>(mobInfoToLore(entityDrops.getEntityName()));
        lore.add(Component.empty());
        lore.addAll(dropsToLore(entityDrops.getDropList()));
        return lore;
    }

    public ItemStack entityDropsToItemStack(EntityDrops entityDrops) {
        List<Component> lore = entityDropsToLore(entityDrops);
        ItemStack baseMaterial = new ItemStack(Material.SKELETON_SKULL);
        try {
            MythicMob mob = getMobByName(entityDrops.getEntityName());
            if (mob != null) {
                baseMaterial = EntityHeads.fromEntityType(EntityType.valueOf(mob.getEntityTypeString()));
            }
        } catch (Exception ignored) {
        }

        return ItemBuilder.from(baseMaterial).name(MiniMessageUtils.miniMessage("<mob_name>", Map.of("mob_name", ChatColor.translateAlternateColorCodes('&', entityDrops.getEntityName())))).lore(lore).build();
    }

    public List<Component> altarToLore(Altar altar) {
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessage("<gray>-- Altar Info --"));
        lore.add(MiniMessageUtils.miniMessage("<gray>Points per Item: <yellow><points_per_item>", Map.of("points_per_item", StringUtils.getNumberFormat(altar.getPointsPerItem()))));
        lore.add(MiniMessageUtils.miniMessage("<gray>Item To Spawn: <white><item_name>", Map.of("item_name", altar.getItemToSpawn() != null ? altar.getItemToSpawn().getFormattedName() : "None")));
        lore.add(MiniMessageUtils.miniMessage("<gray>Recharge Time: <yellow><recharge_time> seconds", Map.of("recharge_time", StringUtils.getNumberFormat(altar.getAltarRechargeTime() / CaveCrawlers.TICKS_TO_SECOND))));
        lore.add(MiniMessageUtils.miniMessage("<gray>Required Altars: <yellow><altar_count>", Map.of("altar_count", StringUtils.getNumberFormat(altar.getAltarLocations().size()))));
        lore.add(Component.empty());
        lore.addAll(dropsToLore(altar.getSpawns(), "Spawns"));
        return lore;
    }

    public ItemStack altarToItemStack(Altar altar) {
        List<Component> lore = altarToLore(altar);
        return ItemBuilder.from(altar.getAltarMaterial()).name(MiniMessageUtils.miniMessage("<gray><name>", Map.of("name", StringUtils.setTitleCase(altar.getId().replace("_", " "))))).lore(lore).build();
    }

    public List<Component> bossDropsToLore(BossDrops bossDrops) {
        List<Component> lore = new ArrayList<>(mobInfoToLore(bossDrops.getEntityName()));
        lore.add(Component.empty());
        lore.add(MiniMessageUtils.miniMessage("<gray>-- Bonus Points --"));
        int pos = 1;
        for (Integer bonusPoint : bossDrops.getBonusPoints()) {
            lore.add(MiniMessageUtils.miniMessage("<yellow>#<position> - <gold><points> Points", Map.of("position", StringUtils.getNumberFormat(pos), "points", StringUtils.getNumberFormat(bonusPoint))));
            pos++;
        }
        lore.add(Component.empty());
        lore.addAll(dropsToLore(bossDrops.getDrops()));
        return lore;
    }

    public ItemStack bossDropsToItemStack(BossDrops bossDrops) {
        List<Component> lore = bossDropsToLore(bossDrops);
        ItemStack baseMaterial = new ItemStack(Material.DRAGON_HEAD);
        try {
            MythicMob mob = getMobByName(bossDrops.getEntityName());
            if (mob != null) {
                baseMaterial = EntityHeads.fromEntityType(EntityType.valueOf(mob.getEntityTypeString()));
            }
        } catch (Exception ignored) {
        }
        return ItemBuilder.from(baseMaterial).name(MiniMessageUtils.miniMessage("<mob_name>", Map.of("mob_name", ChatColor.translateAlternateColorCodes('&', bossDrops.getEntityName())))).lore(lore).build();
    }
}
