package me.lidan.cavecrawlers.drops;

import lombok.Data;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Data
public class Drop implements ConfigurationSerializable {
    private static final Logger log = LoggerFactory.getLogger(Drop.class);
    protected String type;
    protected double chance;
    protected String value;
    protected @Nullable ConfigMessage announce; // config message for announcing the drop

    public Drop(String type, double chance, String value, @Nullable ConfigMessage announce) {
        this.type = type;
        this.chance = chance;
        this.value = value;
        this.announce = announce;
    }

    public Drop(String type, double chance, String value) {
        this(type, chance, value, null);
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "type", type,
                "chance", chance,
                "value", value,
                "announce", announce
        );
    }

    public static Drop deserialize(Map<String, Object> map) {
        if (map.containsKey("itemID")){
            log.warn("ItemID is deprecated, use value instead at {}", map);
            String itemID = (String) map.get("itemID");
            String amountStr = map.get("amount").toString();
            return new Drop("item", (double) map.get("chance"), itemID + " " + amountStr);
        }

        return new Drop(
                (String) map.get("type"),
                (double) map.get("chance"),
                (String) map.get("value"),
                ConfigMessage.getMessage((String) map.get("announce"))
        );
    }

}
