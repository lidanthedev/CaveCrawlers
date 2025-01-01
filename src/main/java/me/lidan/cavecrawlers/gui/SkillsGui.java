package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.SkillReward;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SkillsGui {
    private final Player player;
    private final PaginatedGui gui;

    public SkillsGui(Player player) {
        this.player = player;
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        gui = Gui.paginated().rows(5).pageSize(7).title(MiniMessageUtils.miniMessageString("Your Skills")).create();
        gui.disableAllInteractions();

        for (Skill skill : skills) {
            gui.addItem(getSkillGuiItem(skill));
        }
        int size = gui.getPageItems().size();

        if (size > 7) {
            gui.setItem(3, 1, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> gui.previous()));
            gui.setItem(3, 9, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> gui.next()));
        }

        gui.getFiller().fillBetweenPoints(0, 0, 2, 9, GuiItems.GLASS_ITEM);
        gui.getFiller().fillBetweenPoints(4, 0, 5, 9, GuiItems.GLASS_ITEM);
        gui.getFiller().fillBetweenPoints(3, 0, 3, 9, GuiItems.GLASS_ITEM);

        gui.setItem(39, GuiItems.BACK_ITEM);
        gui.setItem(40, GuiItems.CLOSE_ITEM);

        List<Integer> layoutForItems = getLayoutForItems(size);
        layoutForItems.forEach(i -> gui.removeItem(3, i));
//        for (int i = 0; i < 20; i++) {
//            gui.addItem(ItemBuilder.from(Material.DIAMOND).name(Component.text("Test" + i)).asGuiItem());
//        }
//        gui.getFiller().fill(GuiItems.GLASS_ITEM);
    }

    public List<Integer> getLayoutForItems(int n) {
        switch (n) {
            case 1:
                return List.of(5);
            case 2:
                return List.of(4, 6);
            case 3:
                return List.of(4, 5, 6);
            case 4:
                return List.of(3, 4, 6, 7);
            case 5:
                return List.of(3, 4, 5, 6, 7);
            case 6:
                return List.of(2, 3, 4, 6, 7, 8);
            default: // 7 or more
                return List.of(2, 3, 4, 5, 6, 7, 8);
        }
    }

    private static GuiItem getSkillGuiItem(Skill skill) {
        return getSkillGuiItem(skill, skill.getType().getIcon());
    }

    private static GuiItem getSkillGuiItem(Skill skill, Material material) {
        return getSkillGuiItem(skill, new ItemStack(material));
    }

    @NotNull
    private static GuiItem getSkillGuiItem(Skill skill, ItemStack baseItem) {
        SkillInfo skillType = skill.getType();
        String skillName = StringUtils.setTitleCase(skillType.getName());
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessageString(""));
        lore.add(MiniMessageUtils.miniMessageString("<italic:false><gray>Progress to Level <next-level>: <yellow><progress>%", Map.of("next-level", String.valueOf(skill.getLevel() + 1),
                "progress", String.valueOf(skill.getXp() / skill.getXpToLevel() * 100))));
        lore.add(MiniMessageUtils.miniMessageComponent("<italic:false><bar> <yellow><xp><gold>/<yellow><max>", Map.of(
                "bar", MiniMessageUtils.progressBar(skill.getXp(), skill.getXpToLevel(), 20),
                "xp", MiniMessageUtils.miniMessageString(StringUtils.getNumberFormat(skill.getXp())),
                "max", MiniMessageUtils.miniMessageString(StringUtils.getShortNumber(skill.getXpToLevel())))));
        if (skill.getLevel() < skill.getType().getMaxLevel()) {
            lore.add(Component.space());
            lore.add(MiniMessageUtils.miniMessageString("<italic:false><gray>Level <level> Rewards:", Map.of(
                    "level", String.valueOf(skill.getLevel() + 1)
            )));
            for (SkillReward skillReward : skillType.getRewards().get(skill.getLevel() + 1)) {
                lore.add(MiniMessageUtils.miniMessageComponent("  <italic:false><msg>", Map.of("msg", skillReward.getRewardMessage())));
            }
        }
        lore.add(Component.space());
        lore.add(MiniMessageUtils.miniMessageString("<italic:false><yellow>Click to view!"));
        return ItemBuilder.from(baseItem).name(MiniMessageUtils.miniMessageString("<italic:false><green><name> <level>", Map.of(
                        "name", skillName,
                        "level", String.valueOf(skill.getLevel())
                )))
                .lore(lore)
                .flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
                .asGuiItem();
    }

    public void open() {
        gui.open(player);
        log.info("page: {} with size: {}", gui.getCurrentPageNum(), gui.getCurrentPageItems().size());
    }
}
