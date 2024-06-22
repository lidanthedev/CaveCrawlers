package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FullYoungAbility extends ItemAbility implements Listener {

    public FullYoungAbility() {
        super("Young Blood", "Gain +70 Walk Speed per piece",0,0);
    }

    protected boolean hasFullSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return helmet != null && chestplate != null && leggings != null && boots != null &&
                helmet.getItemMeta().getDisplayName().equals("Young Helmet") &&
                chestplate.getItemMeta().getDisplayName().equals("Young Chestplate") &&
                leggings.getItemMeta().getDisplayName().equals("Young Leggings") &&
                boots.getItemMeta().getDisplayName().equals("Young Boots");
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player p = playerEvent.getPlayer();
        hasFullSet(p);

        Stats stats = StatsManager.getInstance().getStats(p);
        stats.add(StatType.SPEED, 280);
        return true;
    }
    @Override
    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Full Set Ability: " + getName());
        list.addAll(StringUtils.loreBuilder(getDescription()));
        return list;
    }
}