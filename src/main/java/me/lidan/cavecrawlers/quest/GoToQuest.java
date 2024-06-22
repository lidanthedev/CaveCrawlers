package me.lidan.cavecrawlers.quest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class GoToQuest extends Quest {
    Location location;
    double radius; //how far from the location is ok
    Map<UUID,Location> locationsMap;

    public GoToQuest(Location location, double radius) {
        this.location = location;
        this.radius = radius;
    }
    public GoToQuest(Location location) {
        this(location, 1);
    }

    @Override
    public void startQuest(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {return;}
        //TODO: make the message from config
        player.sendMessage(STR."Go to (\{location.getBlockX()},\{location.getBlockY()},\{location.getBlockZ()}");
        locationsMap.put(uuid, location);
        //TODO: make register for get to the location
    }

    @Override
    public void finishQuest(UUID uuid) {
        locationsMap.remove(uuid);
    }
}
