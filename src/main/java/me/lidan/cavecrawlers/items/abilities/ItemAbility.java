package me.lidan.cavecrawlers.items.abilities;

import lombok.Getter;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public abstract class ItemAbility {
    private final String name;
    private final String description;
    private final double cost;
    private final long cooldown;
    private final Cooldown<UUID> abilityCooldown;

    public ItemAbility(String name, String description, double cost, long cooldown) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.cooldown = cooldown;
        if (cooldown < 50){
            throw new IllegalArgumentException("cooldown must be at least 50ms");
        }
        this.abilityCooldown = new Cooldown<>();
    }

    public void activateAbility(Player player){
        if (abilityCooldown.getCurrentCooldown(player.getUniqueId()) < cooldown){
            abilityFailedCooldown(player);
            return;
        }
        Stats stats = StatsManager.getInstance().getStats(player);
        Stat manaStat = stats.get(StatType.MANA);
        if (manaStat.getValue() < cost){
            abilityFailedNoMana(player);
            return;
        }

        abilityCooldown.startCooldown(player.getUniqueId());
        manaStat.setValue(manaStat.getValue() - cost);
        String msg = ChatColor.GOLD + name + "!" + ChatColor.AQUA + " (%s Mana)".formatted((int)cost);
        ActionBarManager.getInstance().actionBar(player, msg);
        useAbility(player);
    }

    public void abilityFailedNoMana(Player player){
        player.sendMessage(ChatColor.RED + "Not Enough Mana! (%s required!)".formatted((int) cost));
    }

    public void abilityFailedCooldown(Player player){
        player.sendMessage(ChatColor.RED + "Still on cooldown!");
    }

    public boolean hasAbility(ItemStack itemStack){
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemInfo itemInfo = itemsManager.getItemFromItemStack(itemStack);
        return itemInfo != null && itemInfo.getAbility() == this;
    }

    protected abstract void useAbility(Player player);

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Item Ability: " + name);
        list.addAll(StringUtils.loreBuilder(description));
        list.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.DARK_AQUA + (int)cost);
        double cooldownDouble = (double) cooldown /1000;
        list.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldownDouble);
        return list;
    }

    public String getID(){
        return AbilityManager.getInstance().getIDbyAbility(this);
    }
}
