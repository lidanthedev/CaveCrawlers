package me.lidan.cavecrawlers.altar;

import lombok.Data;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Data
public class Altar implements ConfigurationSerializable {
    private static ItemsManager itemsManager = ItemsManager.getInstance();

    private List<Location> altarLocations = new ArrayList<>();
    private Location spawnLocation;
    private List<AltarDrop> spawns = new ArrayList<>();
    private ItemInfo itemToSpawn;
    private Material altarMaterial;
    private Material alterUsedMaterial;
    private ConfigMessage announce;

    private Map<UUID, Integer> playerPlacedMap = new HashMap<>();

    public Altar(List<Location> altarLocations, Location spawnLocation, List<AltarDrop> spawns, ItemInfo itemToSpawn, Material altarMaterial, Material alterUsedMaterial, ConfigMessage announce) {
        this.altarLocations = altarLocations;
        this.spawnLocation = spawnLocation;
        this.spawns = spawns;
        this.itemToSpawn = itemToSpawn;
        this.altarMaterial = altarMaterial;
        this.alterUsedMaterial = alterUsedMaterial;
        this.announce = announce;
    }

    public void resetAltar() {
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
        if (clickedBlock.getType() != altarMaterial) return;
        itemsManager.removeItems(player, itemToSpawn, 1);
        int afterPlace = playerPlacedMap.getOrDefault(player.getUniqueId(), 0) + 1;
        playerPlacedMap.put(player.getUniqueId(), afterPlace);
        clickedBlock.setType(alterUsedMaterial);
        if (afterPlace == altarLocations.size()) {
            roll();
        }
    }

    private void roll() {
        for (AltarDrop spawn : spawns) {
            spawn.roll(spawnLocation);
        }
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
        map.put("announce", announce);
        return map;
    }

    public static Altar deserialize(Map<String, Object> map) {
        List<Location> altarLocations = (List<Location>) map.get("altarLocations");
        Location spawnLocation = (Location) map.get("spawnLocation");
        List<AltarDrop> spawns = (List<AltarDrop>) map.get("spawns");
        ItemInfo itemToSpawn = itemsManager.getItemByID(map.get("itemToSpawn").toString());
        Material altarMaterial = Material.valueOf(map.get("altarMaterial").toString());
        Material alterUsedMaterial = Material.valueOf(map.get("alterUsedMaterial").toString());
        ConfigMessage announce = ConfigMessage.getMessage((String) map.get("announce"));
        return new Altar(altarLocations, spawnLocation, spawns, itemToSpawn, altarMaterial, alterUsedMaterial, announce);
    }
}
