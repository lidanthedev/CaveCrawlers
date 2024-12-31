package me.lidan.cavecrawlers.skills;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropType;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class ItemSkillReward extends SkillReward {
    private final String item;

    public ItemSkillReward(String item) {
        this.item = item;
    }

    @Override
    public void applyReward(Player player) {
        Drop drop = new Drop(DropType.ITEM, 100, item, null);
        drop.drop(player);
    }
}
