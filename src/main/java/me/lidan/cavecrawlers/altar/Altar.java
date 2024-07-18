package me.lidan.cavecrawlers.altar;

import lombok.Data;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Data
public class Altar {
    private List<Location> altarLocations = new ArrayList<>();
    private Location spawnLocation;
    private List<Drop> spawns = new ArrayList<>();
    private Material altarMaterial;
    private Material alterUsedMaterial;
    private ConfigMessage announce;

    public Altar(List<Location> altarLocations, Location spawnLocation, List<Drop> spawns, Material altarMaterial, Material alterUsedMaterial, ConfigMessage announce) {
        this.altarLocations = altarLocations;
        this.spawnLocation = spawnLocation;
        this.spawns = spawns;
        this.altarMaterial = altarMaterial;
        this.alterUsedMaterial = alterUsedMaterial;
        this.announce = announce;
    }

    public void resetAltar() {
        for (Location location : altarLocations) {
            location.getBlock().setType(altarMaterial);
        }
    }

    public void disableAltar() {
        for (Location location : altarLocations) {
            location.getBlock().setType(alterUsedMaterial);
        }
    }
}
