package me.lidan.cavecrawlers.stats;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Install only in a disposable localhost test server. It changes the test player's absorption. */
public final class AbsorptionCompatibilityProbe extends JavaPlugin implements Listener {
    private AbsorptionDisplay display;
    private final AtomicBoolean failNextPacket = new AtomicBoolean();

    @Override
    public void onEnable() {
        AbsorptionPackets packets = new AbsorptionPackets(this);
        display = new AbsorptionDisplay(this, new AbsorptionDisplay.Packets() {
            @Override
            public void start(AbsorptionDisplay.Scaler scaler, Consumer<Throwable> onFailure) {
                packets.start((player, entityId, amount) -> {
                    if (failNextPacket.compareAndSet(true, false)) {
                        throw new NoSuchMethodError("Deliberate compatibility probe");
                    }
                    return scaler.scale(player, entityId, amount);
                }, onFailure);
            }

            @Override
            public void stop() {
                packets.stop();
            }

            @Override
            public void send(Player player, float amount) {
                packets.send(player, amount);
            }
        });
        getServer().getPluginManager().registerEvents(display, this);
        getServer().getPluginManager().registerEvents(this, this);
        display.reload();
        int index = AbsorptionPackets.resolveAbsorptionAccessor().getIndex();
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                this, ListenerPriority.MONITOR, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (event.getPacket().getIntegers().read(0) != player.getEntityId()) {
                    return;
                }
                for (var value : event.getPacket().getDataValueCollectionModifier().read(0)) {
                    if (value.getIndex() == index) {
                        getLogger().info("ABS_PROBE_META real=" + player.getAbsorptionAmount() + " shown=" + value.getValue());
                    }
                }
            }
        });
        getLogger().info("ABS_PROBE_READY index=" + index + " server=" + getServer().getVersion());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);
        getServer().getScheduler().runTaskLater(this, () -> {
            player.setHealthScale(40);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 99, false, false));
            getLogger().info("ABS_PROBE_GRANTED real=" + player.getAbsorptionAmount());
        }, 20);
        getServer().getScheduler().runTaskLater(this, () -> {
            player.setAbsorptionAmount(200);
            getLogger().info("ABS_PROBE_PARTIAL real=" + player.getAbsorptionAmount());
        }, 60);
        getServer().getScheduler().runTaskLater(this, () -> {
            failNextPacket.set(true);
            player.setAbsorptionAmount(150);
            getLogger().info("ABS_PROBE_FAILURE_INJECTED real=" + player.getAbsorptionAmount());
        }, 100);
        getServer().getScheduler().runTaskLater(this, () -> {
            display.reload();
            getLogger().info("ABS_PROBE_REENABLED real=" + player.getAbsorptionAmount());
        }, 120);
        getServer().getScheduler().runTaskLater(this, () -> {
            getConfig().set("absorption-display.enabled", false);
            display.reload();
            getLogger().info("ABS_PROBE_DISABLED real=" + player.getAbsorptionAmount());
        }, 140);
    }

    @Override
    public void onDisable() {
        if (display != null) {
            display.close();
        }
    }
}
