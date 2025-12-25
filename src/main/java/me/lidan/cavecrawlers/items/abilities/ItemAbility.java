package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
public abstract class ItemAbility implements Cloneable {
    private static boolean EXPERIMENTAL_ENABLE_COOLDOWN_ANIMATION = CaveCrawlers.getInstance().getConfig().getBoolean("experimental.enable-cooldown-animation", false);
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

        boolean success = useAbility(playerEvent);
        if (success) {
            abilityCooldown.startCooldown(player.getUniqueId());
            if (EXPERIMENTAL_ENABLE_COOLDOWN_ANIMATION) {
                PacketManager.getInstance().setCooldown(player, player.getEquipment().getItemInMainHand().getType(), (int) (cooldown / 1000 * 20));
            }
            manaStat.setValue(manaStat.getValue() - getCost());
            String msg = ChatColor.GOLD + name + "!" + ChatColor.AQUA + " (%s Mana)".formatted((int) getCost());
            ActionBarManager.getInstance().showActionBar(player, msg);
        }
    }

    public void abilityFailedNoMana(Player player){
        String msg = ChatColor.RED + "Not Enough Mana! (%s required!)".formatted((int) getCost());
        ActionBarManager.getInstance().showActionBar(player, msg);
    }

    public void abilityFailedCooldown(Player player){
        double diff = (cooldown - abilityCooldown.getCurrentCooldown(player.getUniqueId()))/1000.0;
        String msg = ChatColor.RED + "Still on cooldown! (%ss Left)".formatted(diff);
        ActionBarManager.getInstance().showActionBar(player, msg);
    }

    public boolean hasAbility(ItemStack itemStack){
        ItemsManager itemsManager = ItemsManager.getInstance();
        ItemInfo itemInfo = itemsManager.getItemFromItemStack(itemStack);
        return itemInfo != null && itemInfo.getAbility() == this;
    }

    protected abstract boolean useAbility(PlayerEvent playerEvent);

    public List<String> toList(){
        List<String> list = new ArrayList<>();
        list.add(ChatColor.GOLD + "Item Ability: " + getName());
        list.addAll(StringUtils.loreBuilder(getDescription()));
        list.add(ChatColor.DARK_GRAY + "Mana Cost: " + ChatColor.DARK_AQUA + (int)getCost());
        double cooldownDouble = (double) getCooldown() /1000;
        list.add(ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GREEN + cooldownDouble);
        return list;
    }

    public ItemAbility buildAbilityWithSettings(JsonObject map){
        ItemAbility ability = clone();
        if (map.has("name")){
            ability.name = map.get("name").getAsString();
        }
        if (map.has("description")){
            ability.description = map.get("description").getAsString();
        }
        if (map.has("cost")){
            ability.cost = map.get("cost").getAsDouble();
        }
        if (map.has("cooldown")){
            ability.cooldown = map.get("cooldown").getAsLong();
        }

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
