package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ItemSkillReward extends SkillReward {
    public static final int CHANCE = 100;
    private final String item;

    public ItemSkillReward(String item) {
        this.item = item;
    }

    @Override
    public void applyReward(Player player) {
        Drop drop = new Drop(DropType.ITEM, CHANCE, item, null);
        drop.drop(player);
    }

    @Override
    public Component getRewardMessage() {
        Drop.ItemDropInfo dropInfo = Drop.getItemDropInfo(item);
        String item = dropInfo.itemInfo().getFormattedNameWithAmount(dropInfo.range().getMin());
        Component itemComponent = LegacyComponentSerializer.legacySection().deserialize(item);
        return MiniMessageUtils.miniMessageComponent("<green>+<item></green>", Map.of("item", itemComponent));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        return map;
    }

    public static ItemSkillReward deserialize(Map<String, Object> map) {
        return new ItemSkillReward((String) map.get("item"));
    }
}
