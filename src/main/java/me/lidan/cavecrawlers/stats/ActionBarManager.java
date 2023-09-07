package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.utils.Cooldown;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ActionBarManager {
    public static final int ACTION_BAR_COOLDOWN = 1000;
    private static ActionBarManager instance;

    private Cooldown<UUID> cooldown;

    public ActionBarManager() {
        cooldown = new Cooldown<>();
    }

    public String[] actionBarBuild(Player player){
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] arr = new String[3];
        arr[0] = ChatColor.RED.toString() + (int) player.getHealth() + "/" + (int) player.getMaxHealth() + StatType.HEALTH.getIcon();
        arr[1] = ChatColor.GREEN.toString() + (int) stats.get(StatType.DEFENSE).getValue() + StatType.DEFENSE.getIcon();
        arr[2] = ChatColor.AQUA.toString() + (int) stats.get(StatType.MANA).getValue() + "/" + (int) stats.get(StatType.INTELLIGENCE).getValue() + StatType.INTELLIGENCE.getIcon();
        return arr;
    }

    public void actionBar(Player player, String alert){
        String[] args = actionBarBuild(player);
        args[1] = alert;
        String msg = String.join(" ", args);
        sendActionBar(player, msg);
        cooldown.startCooldown(player.getUniqueId());
    }

    public void actionBar(Player player){
        if (cooldown.getCurrentCooldown(player.getUniqueId()) < ACTION_BAR_COOLDOWN){
            return;
        }
        String[] args = actionBarBuild(player);
        String msg = String.join(" ", args);
        sendActionBar(player, msg);
    }

    public static void sendActionBar(Player player, String message){
        TextComponent component = new TextComponent(message);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
    }

    public static ActionBarManager getInstance() {
        if (instance == null){
            instance = new ActionBarManager();
        }
        return instance;
    }
}
