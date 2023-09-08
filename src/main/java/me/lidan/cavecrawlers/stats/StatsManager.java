package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemSlot;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final Map<UUID, Stats> statsMap;
    private final Map<UUID, Boolean> statsAutoMap;
    private static StatsManager instance;

    public static StatsManager getInstance() {
        if (instance == null){
            instance = new StatsManager();
        }
        return instance;
    }

    public StatsManager() {
        this.statsMap = new HashMap<>();
        this.statsAutoMap = new HashMap<>();
    }

    public Stats getStats(UUID uuid){
        if (!statsMap.containsKey(uuid)){
            statsMap.put(uuid, new Stats());
        }
        return statsMap.get(uuid);
    }

    public Stats getStats(Player player){
        return getStats(player.getUniqueId());
    }

    public boolean isStatsAuto(Player player){
        return statsAutoMap.getOrDefault(player.getUniqueId(), true);
    }

    public void setStatsAuto(Player player, boolean b){
        statsAutoMap.put(player.getUniqueId(), b);
    }

    public void loadPlayer(Player player){
        applyStats(player);
        Stats stats = getStats(player);
        player.setHealth(player.getMaxHealth());
        double value = stats.get(StatType.INTELLIGENCE).getValue();
        stats.get(StatType.MANA).setValue(value);
    }

    public void applyStats(Player player){
        Stats stats = calculateStats(player);

        if (player.isDead()){
            player.spigot().respawn();
        }

        player.setHealthScale(40);
        double maxHealth = stats.get(StatType.HEALTH).getValue();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);

        // speed
        double speed = stats.get(StatType.SPEED).getValue();
        player.setWalkSpeed((float) (speed/500));

        // health regen
        double healthRegen = ((maxHealth * 0.01) + 1.5);
        double health = player.getHealth();
        player.setHealth(Math.min(health + healthRegen, maxHealth));
        player.setFoodLevel(200);

        // mana regen
        double intel = stats.get(StatType.INTELLIGENCE).getValue();
        Stat manaStat = stats.get(StatType.MANA);
        double mana = manaStat.getValue();
        double manaRegen = intel * 0.02;
        manaStat.setValue(Math.min(mana + manaRegen, intel));

        ActionBarManager.getInstance().actionBar(player);
    }

    public Stats calculateStats(Player player) {
        Stats stats = getStats(player);
        if (isStatsAuto(player)){
            Stats statsFromEquipment = getStatsFromPlayerEquipment(player);
            double manaAmount = stats.get(StatType.MANA).getValue();
            statsFromEquipment.set(StatType.MANA, manaAmount);
            stats = statsFromEquipment;
        }
        statsMap.put(player.getUniqueId(), stats);
        return stats;
    }

    public Stats getStatsFromPlayerEquipment(Player player){
        Stats stats = new Stats();
        EntityEquipment equipment = player.getEquipment();
        ItemStack[] armor = equipment.getArmorContents();
        for (ItemStack itemStack : armor) {
            Stats itemStats = getStatsFromItemStack(itemStack, ItemSlot.ARMOR);
            if (itemStats != null) {
                stats.add(itemStats);
            }
        }

        ItemStack hand = equipment.getItemInMainHand();
        Stats statsFromHand = getStatsFromItemStack(hand, ItemSlot.HAND);
        if (statsFromHand != null)
            stats.add(statsFromHand);

        return stats;

    }

    public @Nullable Stats getStatsFromItemStack(ItemStack itemStack, ItemSlot slot){
        ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStack(itemStack);
        if (itemInfo != null) {
            ItemType type = itemInfo.getType();
            if (type.getSlot() != slot){
                return null;
            }
            return itemInfo.getStats();
        }
        return null;
    }

    public void loadAllPlayers(){
        Bukkit.getOnlinePlayers().forEach(this::loadPlayer);
    }

    public void statLoop(){
        Bukkit.getOnlinePlayers().forEach(this::applyStats);
    }
}
