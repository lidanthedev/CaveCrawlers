package me.lidan.cavecrawlers.items.abilities;

import lombok.Getter;
import lombok.ToString;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@ToString
public abstract class ItemAbility implements Cloneable {
    private String name;
    private String description;
    private double cost;
    private long cooldown;
    private final Cooldown<UUID> abilityCooldown;

    public ItemAbility(String name, String description, double cost, long cooldown) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        if (cooldown < 50){
            cooldown = 50;
        }
        this.cooldown = cooldown;
        this.abilityCooldown = new Cooldown<>();
    }

    public void activateAbility(PlayerEvent playerEvent){
        Player player = playerEvent.getPlayer();
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
        manaStat.setValue(manaStat.getValue() - getCost());
        String msg = ChatColor.GOLD + name + "!" + ChatColor.AQUA + " (%s Mana)".formatted((int)getCost());
        ActionBarManager.getInstance().actionBar(player, msg);
        useAbility(playerEvent);
    }

    public void abilityFailedNoMana(Player player){
        player.sendMessage(ChatColor.RED + "Not Enough Mana! (%s required!)".formatted((int) getCost()));
    }

    public void abilityFailedCooldown(Player player){
        player.sendMessage(ChatColor.RED + "Still on cooldown!");
    }

    public boolean hasAbility(ItemStack itemStack){
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemInfo itemInfo = itemsManager.getItemFromItemStack(itemStack);
        return itemInfo != null && itemInfo.getAbility() == this;
    }

    protected abstract void useAbility(PlayerEvent playerEvent);

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Item Ability: " + getName());
        list.addAll(StringUtils.loreBuilder(getDescription()));
        list.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.DARK_AQUA + (int)getCost());
        double cooldownDouble = (double) getCooldown() /1000;
        list.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldownDouble);
        return list;
    }

    public ItemAbility buildAbilityWithSettings(Map<String, Object> map){
        ItemAbility ability = clone();
        String name = (String) map.getOrDefault("name", this.name);
        String description = (String) map.getOrDefault("description", this.description);
        double cost = (double) map.getOrDefault("cost", this.cost);
        long cooldown = (long) map.getOrDefault("cooldown", this.cooldown);

        ability.name = name;
        ability.description = description;
        ability.cost = cost;
        ability.cooldown = cooldown;

        return ability;
    }

    public String getID(){
        return AbilityManager.getInstance().getIDbyAbility(this);
    }

    @Override
    public ItemAbility clone() {
        try {
            return (ItemAbility) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
