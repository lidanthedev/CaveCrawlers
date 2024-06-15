package me.lidan.cavecrawlers.griffin;

import lombok.*;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.drops.SimpleDrop;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"griffinManager"})
public class GriffinDrop extends Drop implements ConfigurationSerializable {
    private static final Logger log = LoggerFactory.getLogger(GriffinDrop.class);
    private final ConfigMessage COINS_MESSAGE = ConfigMessage.getMessageOrDefault("griffin_coins_message", "&e&lGRIFFIN! You got %amount% coins!");
    private final ConfigMessage MOB_MESSAGE = ConfigMessage.getMessageOrDefault("griffin_mobs_message", "&c&lGRIFFIN! &cYou found %name%!");
    private final GriffinManager griffinManager = GriffinManager.getInstance();

    public GriffinDrop(String type, double chance, String value, ConfigMessage announce) {
        super(type, chance, value, announce);
        if (announce == null) {
            if (this.type == DropType.COINS) {
                this.announce = COINS_MESSAGE;
            }
            else if (this.type == DropType.MOB) {
                this.announce = MOB_MESSAGE;
            }
            else if (this.type == DropType.ITEM) {
                this.announce = Drop.RARE_DROP_MESSAGE;
            }
        }
    }

    public GriffinDrop(String type, double chance, String value) {
        this(type, chance, value, null);
    }


    @Override
    protected Entity giveMob(Player player, Location location) {
        Entity entity = super.giveMob(player, location);
        if (entity != null) {
            griffinManager.protectMobForPlayer(player, entity);
        }
        return entity;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", type, "chance", chance, "value", value);
    }

    public static GriffinDrop deserialize(Map<String, Object> map) {
        return new GriffinDrop((String) map.get("type"), (double) map.get("chance"), (String) map.get("value"), ConfigMessage.getMessage((String) map.get("announce")));
    }
}
