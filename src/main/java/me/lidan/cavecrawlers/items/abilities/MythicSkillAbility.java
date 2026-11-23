package me.lidan.cavecrawlers.items.abilities;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public class MythicSkillAbility extends ClickAbility{
    private final MythicBukkit mythicBukkit;
    @Setting
    private String mythicSkill;

    public MythicSkillAbility(String mythicSkill) {
        super("Mythic Skill", "Activate your mythic skill!", 10, 5);
        this.mythicSkill = mythicSkill;
        mythicBukkit = MythicBukkit.inst();
    }

    @Override
    protected boolean useAbility(PlayerEvent playerEvent) {
        Player player = playerEvent.getPlayer();
        return mythicBukkit.getAPIHelper().castSkill(player, mythicSkill, player.getLocation());
    }

}
