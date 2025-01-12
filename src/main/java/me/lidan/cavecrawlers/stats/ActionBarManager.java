package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ActionBarManager {
    public static final int ACTION_BAR_COOLDOWN = 1000;
    private static ActionBarManager instance;

    private Cooldown<UUID> cooldown;

    public ActionBarManager() {
        cooldown = new Cooldown<>();
    }

    public Component[] actionBarBuildAdventure(Player player) {
        Stats stats = StatsManager.getInstance().getStats(player);
        Component[] arr = new Component[3];
        arr[0] = MiniMessageUtils.miniMessageString("<red><health>/<max-health><icon>", Map.of("health", String.valueOf((int) player.getHealth()), "max-health", String.valueOf((int) player.getMaxHealth()), "icon", StatType.HEALTH.getIcon()));
        arr[1] = MiniMessageUtils.miniMessageString("<green><defense><icon>", Map.of("defense", String.valueOf((int) stats.get(StatType.DEFENSE).getValue()), "icon", StatType.DEFENSE.getIcon()));
        arr[2] = MiniMessageUtils.miniMessageString("<aqua><mana>/<max-mana><icon>", Map.of("mana", String.valueOf((int) stats.get(StatType.MANA).getValue()), "max-mana", String.valueOf((int) stats.get(StatType.INTELLIGENCE).getValue()), "icon", StatType.INTELLIGENCE.getIcon()));
        return arr;
    }

    public void actionBar(Player player, String alert){
        Component[] args = actionBarBuildAdventure(player);
        args[1] = LegacyComponentSerializer.legacySection().deserialize(alert);
        sendActionBar(player, args);
        cooldown.startCooldown(player.getUniqueId());
    }

    public void actionBar(Player player, Component alert) {
        Component[] args = actionBarBuildAdventure(player);
        args[1] = alert;
        sendActionBar(player, args);
        cooldown.startCooldown(player.getUniqueId());
    }

    public void actionBar(Player player){
        if (cooldown.getCurrentCooldown(player.getUniqueId()) < ACTION_BAR_COOLDOWN){
            return;
        }
        Component[] args = actionBarBuildAdventure(player);
        sendActionBar(player, args);
    }

    private static void sendActionBar(Player player, Component[] args) {
        Component msg = Component.join(JoinConfiguration.separator(Component.text(" ")), args);
        sendActionBar(player, msg);
    }

    public static void sendActionBar(Player player, String message){
        TextComponent component = new TextComponent(message);
        player.sendActionBar(component);
    }

    public static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    public static ActionBarManager getInstance() {
        if (instance == null){
            instance = new ActionBarManager();
        }
        return instance;
    }
}
