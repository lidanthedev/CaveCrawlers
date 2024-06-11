package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillsGui {
    private final Player player;
    private final Gui gui;

    public SkillsGui(Player player) {
        this.player = player;
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        gui = new Gui(3, ChatColor.WHITE + "七七七七七七七七\uD83E\uDE71");
        gui.disableAllInteractions();
        gui.setItem(3, getSkillGuiItem(skills.get(SkillType.COMBAT)));
        gui.setItem(4, getSkillGuiItem(skills.get(SkillType.FARMING)));
        gui.setItem(5, getSkillGuiItem(skills.get(SkillType.FISHING)));
        gui.setItem(6, getSkillGuiItem(skills.get(SkillType.FORAGING)));
        gui.setItem(7, getSkillGuiItem(skills.get(SkillType.MINING)));
        gui.setItem(8, getSkillGuiItem(skills.get(SkillType.ALCHEMY)));
    }

    @NotNull
    private static GuiItem getSkillGuiItem(Skill skill) {
        SkillType skillType = skill.getType();
        int amount = skill.getLevel();
        if (amount == 0){
            amount = 1;
        }
        String skillName = StringUtils.setTitleCase(skillType.name());
        GuiItem guiItem = ItemBuilder.from(Material.PAPER).setName(ChatColor.AQUA + skillName + " " + skill.getLevel() + "/50" + " (" + Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d + "%)").amount(amount).model(110007).asGuiItem();
        return guiItem;
    }

    public void open(){
        gui.open(player);
    }
}
