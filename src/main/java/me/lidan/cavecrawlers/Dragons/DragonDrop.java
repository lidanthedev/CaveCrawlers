package me.lidan.cavecrawlers.Dragons;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.entities.LootShareEntityData;
import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DragonDrop extends Drop implements ConfigurationSerializable {
    private static final Logger log = LoggerFactory.getLogger(DragonDrop.class);
    private static final GriffinManager dragonManager = GriffinManager.getInstance();
    private static final EntityManager entityManager = EntityManager.getInstance();
    public static final int DAMAGE_THRESHOLD_PERCENT = 10;
    private final ConfigMessage MOB_MESSAGE = ConfigMessage.getMessageOrDefault("dragon_mobs_message", "&8[&5Dragon System&8]! &cYou Summoned %name%!");

    public DragonDrop(String type, double chance, String value, ConfigMessage announce) {
        super(type, chance, value, announce);
        if (announce == null) {
            if (this.type == DropType.MOB) {
                this.announce = MOB_MESSAGE;
            }
            else if (this.type == DropType.ITEM) {
                this.announce = Drop.RARE_DROP_MESSAGE;
            }
        }
    }

    public DragonDrop(String type, double chance, String value) {
        this(type, chance, value, null);
    }


    @Override
    protected Entity giveMob(Player player, Location location) {
        Entity entity = super.giveMob(player, location);
        if (entity instanceof LivingEntity livingEntity) {
            entityManager.setEntityData(livingEntity.getUniqueId(), new LootShareEntityData(livingEntity, DAMAGE_THRESHOLD_PERCENT, player.getUniqueId()));
        }
        return entity;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of("type", type, "chance", chance, "value", value);
    }

    public static DragonDrop deserialize(Map<String, Object> map) {
        return new DragonDrop((String) map.get("type"), (double) map.get("chance"), (String) map.get("value"), ConfigMessage.getMessage((String) map.get("announce")));
    }
}