package me.lidan.cavecrawlers.items.abilities;

import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FullSuperiorAbility extends ItemAbility implements Listener {

    public FullSuperiorAbility() {
        super("Superior Blood", "Most of your stats are increased by 5%",0,0);
    }

    protected boolean hasFullSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return helmet != null && chestplate != null && leggings != null && boots != null &&
                helmet.getItemMeta().getDisplayName().equals("Superior Helmet") &&
                chestplate.getItemMeta().getDisplayName().equals("Superior Chestplate") &&
                leggings.getItemMeta().getDisplayName().equals("Superior Leggings") &&
                boots.getItemMeta().getDisplayName().equals("Superior Boots");
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player p = playerEvent.getPlayer();
        hasFullSet(p);

        Stats stats = StatsManager.getInstance().getStats(p);
        stats.multiply(5);
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
