package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CoinSkillReward extends SkillReward {
    private final int amount;

    public CoinSkillReward(int amount) {
        this.amount = amount;
    }

    @Override
    public void applyReward(Player player) {
        Drop drop = new Drop(DropType.COINS, 100, String.valueOf(amount), null);
        drop.drop(player);
    }

    @Override
    public Component getRewardMessage() {
        return MiniMessageUtils.miniMessageString("<green>+<coins> coins</green>", Map.of("coins", String.valueOf(amount)));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("amount", amount);
        return map;
    }

    public static CoinSkillReward deserialize(Map<String, Object> map) {
        return new CoinSkillReward((int) map.get("amount"));
    }
}
