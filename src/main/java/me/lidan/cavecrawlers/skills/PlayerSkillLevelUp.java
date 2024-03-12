package me.lidan.cavecrawlers.skills;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerSkillLevelUp extends PlayerEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();
    private final SkillType skillType;

    public PlayerSkillLevelUp(@NotNull Player who, SkillType skillType) {
        super(who);
        this.skillType = skillType;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
