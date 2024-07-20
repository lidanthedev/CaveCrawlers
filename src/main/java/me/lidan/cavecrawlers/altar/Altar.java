package me.lidan.cavecrawlers.altar;

import lombok.Data;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.entities.BossEntityData;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Data
public class Altar implements ConfigurationSerializable {
    public static final int POINTS_PER_ITEM = 100;
    public static final int ALTAR_RECHARGE = 200;
    private static ItemsManager itemsManager = ItemsManager.getInstance();
    private static EntityManager entityManager = EntityManager.getInstance();

    private String Id;

    private List<Location> altarLocations = new ArrayList<>();
    private Location spawnLocation;
    private List<AltarDrop> spawns = new ArrayList<>();
    private ItemInfo itemToSpawn;
    private Material altarMaterial;
    private Material alterUsedMaterial;
    private ConfigMessage placeAnnounce;
    private ConfigMessage spawnAnnounce;

    private Map<UUID, Integer> playerPlacedMap = new HashMap<>();
    private LivingEntity spawnedEntity;

    public Altar(List<Location> altarLocations, Location spawnLocation, List<AltarDrop> spawns, ItemInfo itemToSpawn, Material altarMaterial, Material alterUsedMaterial, ConfigMessage placeAnnounce, ConfigMessage spawnAnnounce) {
        this.altarLocations = altarLocations;
        this.spawnLocation = spawnLocation;
        this.spawns = spawns;
        this.itemToSpawn = itemToSpawn;
        this.altarMaterial = altarMaterial;
        this.alterUsedMaterial = alterUsedMaterial;
        this.placeAnnounce = placeAnnounce;
        this.spawnAnnounce = spawnAnnounce;
    }

    public Altar(){
        this(new ArrayList<>(), null, new ArrayList<>(), null, Material.END_PORTAL_FRAME, Material.BEDROCK, null, null);
    }

    public void resetAltarBlocks() {
        for (Location location : altarLocations) {
            location.getBlock().setType(altarMaterial);
        }
    }

    public boolean isAltar(Location location) {
        return altarLocations.contains(location);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (!isAltar(clickedBlock.getLocation())) return;
        if (!itemsManager.hasItem(player, itemToSpawn, 1)) return;
        if (itemsManager.getItemFromItemStackSafe(player.getInventory().getItemInMainHand()) != itemToSpawn) return;
        if (clickedBlock.getType() != altarMaterial) return;
        if (player.getGameMode() != GameMode.CREATIVE)
            itemsManager.removeItems(player, itemToSpawn, 1);
        int afterPlace = playerPlacedMap.getOrDefault(player.getUniqueId(), 0) + 1;
        playerPlacedMap.put(player.getUniqueId(), afterPlace);
        clickedBlock.setType(alterUsedMaterial);
        if (placeAnnounce != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getDisplayName());
            placeholders.put("item", itemToSpawn.getName());
            placeholders.put("amount", String.valueOf(afterPlace));
            placeholders.put("max_amount", String.valueOf(altarLocations.size()));
            sendAnnounce(placeAnnounce, placeholders, player.getWorld());
        }
        if (afterPlace == altarLocations.size()) {
            roll();
        }
    }

    public void sendAnnounce(ConfigMessage message, Map<String, String> placeholders, World world){
        if (message == null) return;
        for (Player player : world.getPlayers()) {
            message.sendMessage(player, placeholders);
        }
    }

    private void roll() {
        for (AltarDrop spawn : spawns) {
            if (spawn.rollChance()){
                Entity entity = spawn.giveMob(spawnLocation);
                if (!(entity instanceof LivingEntity livingEntity)) return;
                spawnedEntity = livingEntity;
                onSpawn(livingEntity);
                return;
            }
        }
    }

    public void onSpawn(LivingEntity livingEntity) {
        BossEntityData entityData = new BossEntityData(livingEntity);
        for (Map.Entry<UUID, Integer> uuidIntegerEntry : playerPlacedMap.entrySet()) {
            entityData.addPoints(uuidIntegerEntry.getKey(), uuidIntegerEntry.getValue() * POINTS_PER_ITEM);
        }
        entityData.addOnDeathRunnable(() -> {
            Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), this::resetAltar, ALTAR_RECHARGE);
        });
        entityManager.setEntityData(livingEntity.getUniqueId(), entityData);
        playerPlacedMap.clear();
        if (spawnAnnounce != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("entity", livingEntity.getName());
            sendAnnounce(spawnAnnounce, placeholders, livingEntity.getWorld());
        }
    }

    public void resetAltar(){
        refundAltar();
        resetAltarBlocks();
    }

    public void refundAltar() {
        for (Map.Entry<UUID, Integer> uuidIntegerEntry : playerPlacedMap.entrySet()) {
            Player player = Bukkit.getPlayer(uuidIntegerEntry.getKey());
            if (player == null) continue;
            itemsManager.giveItem(player, itemToSpawn, uuidIntegerEntry.getValue());
        }
        playerPlacedMap.clear();
    }

    public void disableAltar() {
        for (Location location : altarLocations) {
            location.getBlock().setType(alterUsedMaterial);
        }
    }


    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("altarLocations", altarLocations);
        map.put("spawnLocation", spawnLocation);
        map.put("spawns", spawns);
        map.put("itemToSpawn", itemToSpawn.getID());
        map.put("altarMaterial", altarMaterial.toString());
        map.put("alterUsedMaterial", alterUsedMaterial.toString());
        map.put("placeAnnounce", ConfigMessage.getIdOfMessage(placeAnnounce));
        map.put("spawnAnnounce", ConfigMessage.getIdOfMessage(spawnAnnounce));
        return map;
    }

    public static Altar deserialize(Map<String, Object> map) {
        List<Location> altarLocations = (List<Location>) map.get("altarLocations");
        Location spawnLocation = (Location) map.get("spawnLocation");
        List<AltarDrop> spawns = (List<AltarDrop>) map.get("spawns");
        ItemInfo itemToSpawn = itemsManager.getItemByID(map.get("itemToSpawn").toString());
        Material altarMaterial = Material.valueOf(map.get("altarMaterial").toString());
        Material alterUsedMaterial = Material.valueOf(map.get("alterUsedMaterial").toString());
        ConfigMessage placeAnnounce = ConfigMessage.getMessage((String) map.get("placeAnnounce"));
        ConfigMessage spawnAnnounce = ConfigMessage.getMessage((String) map.get("spawnAnnounce"));
        return new Altar(altarLocations, spawnLocation, spawns, itemToSpawn, altarMaterial, alterUsedMaterial, placeAnnounce, spawnAnnounce);
    }
}
