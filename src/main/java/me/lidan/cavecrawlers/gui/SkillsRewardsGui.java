package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;


public class SkillsRewardsGui {
    public static final List<Integer> LAYOUT_FOR_ITEMS = List.of(9, 18, 27, 28, 29, 20, 11, 2, 3, 4, 13, 22, 31, 32, 33, 24, 15, 6, 7, 8, 17, 26, 35, 44, 53);

    private final Gui gui;
    private final Player player;
    private final Skill skill;
    private int currentPage = 0;

    public SkillsRewardsGui(Player player, Skill skill) {
        this.player = player;
        this.gui = Gui.gui().title(MiniMessageUtils.miniMessageString("<name> Skill", Map.of("name", StringUtils.setTitleCase(skill.getType().getName())))).rows(6).create();
        this.skill = skill;
        gui.disableAllInteractions();
        gui.setItem(0, SkillsGui.getSkillGuiItem(skill));
        gui.setItem(49, GuiItems.CLOSE_ITEM);
        gui.setItem(48, GuiItems.PREVIOUS_ARROW_ITEM.asGuiItem(event -> {
            if (currentPage > 0) {
                currentPage--;
                updateItems();
            } else {
                new SkillsGui(player).open();
            }
        }));
        gui.setItem(50, GuiItems.NEXT_ARROW_ITEM.asGuiItem(event -> {
            int maxLevel = skill.getType().getMaxLevel();
            int max = (maxLevel / 25);
            if (maxLevel % 25 == 0) {
                max--;
            }
            if (currentPage < max) {
                currentPage++;
                updateItems();
            }
        }));
        updateItems();

        gui.getFiller().fill(GuiItems.GLASS_ITEM);
    }

    private void updateItems() {
        int maxLevel = skill.getType().getMaxLevel();
        for (Integer i : LAYOUT_FOR_ITEMS) {
            gui.setItem(i, GuiItems.GLASS_ITEM);
        }
        for (int i = (25 * currentPage) + 1; i <= Math.min(25 * (currentPage + 1), maxLevel); i++) {
            gui.setItem(LAYOUT_FOR_ITEMS.get(i - (25 * currentPage) - 1), SkillsGui.getSkillRewardGuiItem(skill.getType(), i, skill.getLevel() >= i));
        }
        gui.update();
    }

    public void open() {
        gui.open(player);
    }
}
