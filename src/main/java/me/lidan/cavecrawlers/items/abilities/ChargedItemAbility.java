package me.lidan.cavecrawlers.items.abilities;

import com.google.gson.JsonObject;
import lombok.Getter;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.*;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class ChargedItemAbility extends ClickAbility {
    private int maxCharges;
    private long chargeTime;
    private final Map<UUID, Integer> playerCharges = new HashMap<>();
    private final Cooldown<UUID> chargeCooldown = new Cooldown<>();

    public ChargedItemAbility(String name, String description, double cost, int maxCharges, long chargeTime) {
        super(name, description, cost, 100);
        this.maxCharges = maxCharges;
        this.chargeTime = chargeTime;
        recharge();
    }

    private void recharge(){
        Bukkit.getScheduler().runTaskTimer(CaveCrawlers.getInstance(), bukkitTask -> {
            for (UUID uuid : playerCharges.keySet()) {
                int charges = playerCharges.get(uuid);
                if (charges < maxCharges && chargeCooldown.getCurrentCooldown(uuid) >= chargeTime){
                    chargeCooldown.startCooldown(uuid);
                    playerCharges.put(uuid, charges+1);
                }
            }
        }, 0, 20L);
    }

    public int getPlayerCharges(Player player){
        if (!playerCharges.containsKey(player.getUniqueId())){
            setPlayerCharges(player, maxCharges);
        }
        return playerCharges.get(player.getUniqueId());
    }

    public void setPlayerCharges(Player player, int charges){
        playerCharges.put(player.getUniqueId(), charges);
    }

    public void activateAbility(PlayerEvent playerEvent){
        Player player = playerEvent.getPlayer();

        if (getAbilityCooldown().getCurrentCooldown(player.getUniqueId()) < getCooldown()){
            return;
        }
        getAbilityCooldown().startCooldown(player.getUniqueId());

        int charges = getPlayerCharges(player);
        if (charges <= 0){
            abilityFailedCooldown(player);
            return;
        }

        Stats stats = StatsManager.getInstance().getStats(player);
        Stat manaStat = stats.get(StatType.MANA);
        if (manaStat.getValue() < getCost()){
            abilityFailedNoMana(player);
            return;
        }
        if (charges == maxCharges){
            chargeCooldown.startCooldown(player.getUniqueId());
        }

        charges--;
        setPlayerCharges(player, charges);
        manaStat.setValue(manaStat.getValue() - getCost());
        String msg = ChatColor.GOLD + getName() + "!" + ChatColor.AQUA + " (%s Mana) %s".formatted((int)getCost(), StringUtils.progressBar(charges, maxCharges, maxCharges, "O "));
        ActionBarManager.getInstance().showActionBar(player, msg);
        useAbility(playerEvent);
    }

    @Override
    public ItemAbility buildAbilityWithSettings(JsonObject map) {
        ChargedItemAbility ability = (ChargedItemAbility) super.buildAbilityWithSettings(map);
        if (map.has("maxCharges")) {
            ability.maxCharges = map.get("maxCharges").getAsInt();
        }
        if (map.has("chargeTime")) {
            ability.chargeTime = map.get("chargeTime").getAsLong();
        }
        return ability;
    }

    public void abilityFailedCooldown(Player player){
        player.sendMessage(ChatColor.RED + "No More Charges!");
    }
}
