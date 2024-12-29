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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
        String skillName = StringUtils.setTitleCase(skillType.name());
        return ItemBuilder.from(baseItem).name(MiniMessageUtils.miniMessageString("<italic:false><green><name> <level>", Map.of(
                        "name", skillName,
                        "level", String.valueOf(skill.getLevel())
                )))
                .lore(MiniMessageUtils.miniMessageString(""), MiniMessageUtils.miniMessageString("<italic:false><gray>Progress to Level <next-level>: <yellow><progress>%", Map.of("next-level", String.valueOf(skill.getLevel() + 1),
                                "progress", String.valueOf(skill.getXp() / skill.getXpToLevel() * 100))),
                        MiniMessageUtils.miniMessageComponent("<italic:false><bar> <yellow><xp><gold>/<yellow><max>", Map.of(
                                "bar", MiniMessageUtils.progressBar(skill.getXp(), skill.getXpToLevel(), 20),
                                "xp", MiniMessageUtils.miniMessageString(StringUtils.getNumberFormat(skill.getXp())),
                                "max", MiniMessageUtils.miniMessageString(StringUtils.getShortNumber(skill.getXpToLevel())))),
                        MiniMessageUtils.miniMessageString(""),
                        MiniMessageUtils.miniMessageString("<italic:false><yellow>Click to view!"))
                .flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
                .asGuiItem();
    }

    public void open(){
        gui.open(player);
    }
}
