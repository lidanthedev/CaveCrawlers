package me.lidan.cavecrawlers.stats;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionBarManager {
    public static final int ACTION_BAR_COOLDOWN = 1000;
    private static ActionBarManager instance;
    private static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private static final String format = plugin.getConfig().getString("actionbar.format", "<red><health>/<max-health>❤ [<green><defense>❈] <aqua><mana>/<max-mana>✎");
    private static final boolean enabled = plugin.getConfig().getBoolean("actionbar.enabled", true);

    private final Cooldown<UUID> cooldown;

    public ActionBarManager() {
        cooldown = new Cooldown<>();
    }

    public Component actionBarBuildAdventure(Player player) {
        return actionBarBuildAdventure(player, (Component) null);
    }

    public Component actionBarBuildAdventure(Player player, String message) {
        Component alert = message != null ? LegacyComponentSerializer.legacySection().deserialize(message) : Component.empty();
        return actionBarBuildAdventure(player, alert);
    }

    public Component actionBarBuildAdventure(Player player, Component alert) {
        String formatCopy = format;
        assert formatCopy != null : "Action bar format cannot be null";
        if (alert == null) {
            alert = Component.empty();
            formatCopy = formatCopy.replace("[", "");
            formatCopy = formatCopy.replace("]", "");
        } else {
            // Replace text in [] with <alert>
            formatCopy = formatCopy.replaceFirst("\\[.*]", "<alert>");
        }

        Map<String, Object> placeholders = new HashMap<>(Map.of(
                "health", String.valueOf((int) player.getHealth()),
                "max-health", String.valueOf((int) player.getMaxHealth()),
                "max-mana", String.valueOf((int) StatsManager.getInstance().getStats(player).get(StatType.INTELLIGENCE).getValue()),
                "alert", alert
        ));
        for (StatType statType : StatType.values()) {
            placeholders.putIfAbsent(statType.name().toLowerCase(), String.valueOf((int) StatsManager.getInstance().getStats(player).get(statType).getValue()));
            placeholders.putIfAbsent(statType.name().toLowerCase() + "-icon", statType.getIcon());
        }
        return MiniMessageUtils.miniMessage(formatCopy, placeholders);
    }

    public void actionBar(Player player, String alert){
        Component args = actionBarBuildAdventure(player, alert);
        sendActionBar(player, args);
        cooldown.startCooldown(player.getUniqueId());
    }

    public void actionBar(Player player, Component alert) {
        Component args = actionBarBuildAdventure(player, alert);
        sendActionBar(player, args);
        cooldown.startCooldown(player.getUniqueId());
    }

    public void actionBar(Player player){
        if (cooldown.getCurrentCooldown(player.getUniqueId()) < ACTION_BAR_COOLDOWN){
            return;
        }
        Component args = actionBarBuildAdventure(player);
        sendActionBar(player, args);
    }

    public static void sendActionBar(Player player, Component message) {
        if (enabled) {
            player.sendActionBar(message);
        }
    }

    public static ActionBarManager getInstance() {
        if (instance == null){
            instance = new ActionBarManager();
        }
        return instance;
    }
}
