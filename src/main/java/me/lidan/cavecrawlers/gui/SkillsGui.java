package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillType;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SkillsGui {
    private final Player player;
    private final Gui gui;

    public SkillsGui(Player player) {
        this.player = player;
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        gui = Gui.gui().rows(5).title(MiniMessageUtils.miniMessageString("Your Skills")).create();
        gui.disableAllInteractions();
        gui.setItem(20, getSkillGuiItem(skills.get(SkillType.COMBAT), Material.STONE_SWORD));
        gui.setItem(21, getSkillGuiItem(skills.get(SkillType.FARMING), Material.GOLDEN_HOE));
        gui.setItem(22, getSkillGuiItem(skills.get(SkillType.FISHING), Material.FISHING_ROD));
        gui.setItem(23, getSkillGuiItem(skills.get(SkillType.FORAGING), Material.SPRUCE_SAPLING));
        gui.setItem(24, getSkillGuiItem(skills.get(SkillType.MINING), Material.STONE_PICKAXE));

        gui.setItem(39, GuiItems.BACK_ITEM);
        gui.setItem(40, GuiItems.CLOSE_ITEM);
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
    }

    private static GuiItem getSkillGuiItem(Skill skill) {
        return getSkillGuiItem(skill, Material.PAPER);
    }

    private static GuiItem getSkillGuiItem(Skill skill, Material material) {
        return getSkillGuiItem(skill, new ItemStack(material));
    }

    @NotNull
    private static GuiItem getSkillGuiItem(Skill skill, ItemStack baseItem) {
        SkillType skillType = skill.getType();
        int amount = skill.getLevel();
        if (amount == 0){
            amount = 1;
        }
        String skillName = StringUtils.setTitleCase(skillType.name());
        GuiItem guiItem = ItemBuilder.from(baseItem).setName(ChatColor.AQUA + skillName + " " + skill.getLevel() + "/50" + " (" + Math.floor(skill.getXp() / skill.getXpToLevel() * 1000d) / 10d + "%)").amount(amount).model(110007).asGuiItem();
        return guiItem;
    }

    public void open(){
        gui.open(player);
    }
}
