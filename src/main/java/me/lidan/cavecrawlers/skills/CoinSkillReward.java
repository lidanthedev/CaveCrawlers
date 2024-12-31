package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import org.bukkit.entity.Player;

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
}
