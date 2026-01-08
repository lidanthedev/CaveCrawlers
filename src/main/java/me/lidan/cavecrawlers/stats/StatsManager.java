package me.lidan.cavecrawlers.stats;

import com.cryptomorin.xseries.XAttribute;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.api.StatsAPI;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemSlot;
import me.lidan.cavecrawlers.items.ItemType;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager implements StatsAPI {
    public static final int SPEED_LIMIT = 500;
    public static final int ATTACK_SPEED_LIMIT = 100;
    private final Map<UUID, Stats> statsMap;
    private final Map<UUID, Stats> statsAdder;
    private static StatsManager instance;
    private static CaveCrawlers plugin = CaveCrawlers.getInstance();

    private StatsManager() {
        this.statsMap = new HashMap<>();
        this.statsAdder = new HashMap<>();
    }

    public static StatsManager getInstance() {
        if (instance == null){
            instance = new StatsManager();
        }
        return instance;
    }

    @Override
    public Stats getStats(UUID uuid){
        if (!statsMap.containsKey(uuid)){
            statsMap.put(uuid, new Stats());
        }
        return statsMap.get(uuid);
    }

    @Override
    public Stats getStats(Player player){
        return getStats(player.getUniqueId());
    }

    public Stats getStatsAdder(Player player){
        return statsAdder.get(player.getUniqueId());
    }

    public void loadPlayer(Player player){
        applyStats(player);
        Stats stats = getStats(player);
        player.setHealth(player.getMaxHealth());
        double value = stats.get(StatType.INTELLIGENCE).getValue();
        stats.get(StatType.MANA).setValue(value);
    }

    public static void healPlayerPercent(Player player, double percent) {
        double maxHealth = player.getAttribute(XAttribute.MAX_HEALTH.get()).getValue();
        healPlayer(player, maxHealth / 100 * percent);
    }

    public static void healPlayer(Player player, double healthRegen) {
        double maxHealth = player.getAttribute(XAttribute.MAX_HEALTH.get()).getValue();
        double health = player.getHealth();
        player.setHealth(Math.min(health + healthRegen, maxHealth));
    }

    public void applyStats(Player player){
        Stats stats = calculateStats(player);

        if (player.isDead()){
            player.spigot().respawn();
        }

        player.setHealthScale(40);

        // speed
        double speed = stats.get(StatType.SPEED).getValue();
        player.setWalkSpeed((float) (speed/ SPEED_LIMIT));

        // health regen
        double maxHealth = stats.get(StatType.HEALTH).getValue();
        player.getAttribute(XAttribute.MAX_HEALTH.get()).setBaseValue(maxHealth);
        double healthRegen = ((maxHealth * 0.01) + 1.5);
        healPlayer(player, healthRegen);
        // hunger disable
        if (plugin.getConfig().getBoolean("hunger-disabled")) {
            player.setFoodLevel(200);
        }
        // mana regen
        double intel = stats.get(StatType.INTELLIGENCE).getValue();
        Stat manaStat = stats.get(StatType.MANA);
        double mana = manaStat.getValue();
        double manaRegen = intel * 0.02;
        manaStat.setValue(Math.min(mana + manaRegen, intel));

        ActionBarManager.getInstance().showActionBar(player);
    }

    public Stats calculateBaseStats() {
        Stats stats = new Stats();
        for (StatType type : StatType.getStats()) {
            stats.set(type, type.getBase());
        }
        return stats;
    }

    public static Stats getStatsFromInventory(Player player) {
        Stats stats = new Stats();
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack itemStack : contents) {
            Stats itemStats = StatsManager.getInstance().getStatsFromItemStack(itemStack, ItemSlot.INVENTORY);
            if (itemStats != null) {
                stats.add(itemStats);
            }
        }
        return stats;
    }

    @Override
    public void register(String id, StatType statType) {
        StatType.register(id, statType);
    }

    private static Stats getStatsFromSkills(Player player) {
        return PlayerDataManager.getInstance().getStatsFromSkills(player);
    }

    public static Stats getStatsFromHotBar(Player player) {
        Stats stats = new Stats();
        ItemStack[] hotbar = new ItemStack[9];
        System.arraycopy(player.getInventory().getContents(), 0, hotbar, 0, 9);
        for (ItemStack itemStack : hotbar) {
            Stats itemStats = StatsManager.getInstance().getStatsFromItemStack(itemStack, ItemSlot.HOTBAR);
            if (itemStats != null) {
                stats.add(itemStats);
            }
        }
        return stats;
    }

    @Override
    public Stats calculateStats(Player player) {
        Stats oldStats = getStats(player);
        Stats stats = calculateBaseStats();

        Stats statsFromEquipment = getStatsFromPlayerEquipment(player);
        Stats statsFromSkills = getStatsFromSkills(player);
        double manaAmount = oldStats.get(StatType.MANA).getValue();
        stats.set(StatType.MANA, manaAmount);
        if (!statsAdder.containsKey(player.getUniqueId())){
            statsAdder.put(player.getUniqueId(), new Stats());
        }
        stats.add(statsFromEquipment);
        stats.add(statsFromSkills);
        stats.add(getStatsAdder(player));
        stats.add(getStatsFromInventory(player));
        stats.add(getStatsFromHotBar(player));

        // stat limits
        Stat speedStat = stats.get(StatType.SPEED);
        Stat attackSpeedStat = stats.get(StatType.ATTACK_SPEED);
        if (speedStat.getValue() > SPEED_LIMIT){
            speedStat.setValue(SPEED_LIMIT);
        }
        if (attackSpeedStat.getValue() > ATTACK_SPEED_LIMIT){
            attackSpeedStat.setValue(ATTACK_SPEED_LIMIT);
        }

        StatsCalculateEvent event = new StatsCalculateEvent(player, stats);
        Bukkit.getPluginManager().callEvent(event);
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

        ItemStack offhand = equipment.getItemInOffHand();
        Stats statsFromOffHand = getStatsFromItemStack(offhand, ItemSlot.OFF_HAND);
        if (statsFromOffHand != null)
            stats.add(statsFromOffHand);

        return stats;

    }

    public @Nullable Stats getStatsFromItemStack(ItemStack itemStack, ItemSlot slot){
        try {
            ItemInfo itemInfo = ItemsManager.getInstance().getItemFromItemStackSafe(itemStack);
            if (itemInfo != null) {
                ItemType type = itemInfo.getType();
                if (type.getSlot() != slot){
                    return null;
                }
                return itemInfo.getStats();
            }
        }
        catch (IllegalArgumentException ignored){}
        catch (Exception e) {
            e.printStackTrace();
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
