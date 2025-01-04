package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
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
    private final Gui gui;
    private final List<GuiItem> items = new ArrayList<>();
    private int currentPage = 0;

    public SkillsGui(Player player) {
        this.player = player;
        Skills skills = PlayerDataManager.getInstance().getSkills(player);
        gui = Gui.gui().rows(5).title(MiniMessageUtils.miniMessageString("Your Skills")).create();
        gui.disableAllInteractions();

        for (Skill skill : skills) {
            GuiItem skillGuiItem = getSkillGuiItem(skill);
            skillGuiItem.setAction(event -> {
                new SkillsRewardsGui(player, skill).open();
            });
            items.add(skillGuiItem);
        }

        gui.getFiller().fillBetweenPoints(0, 0, 2, 9, GuiItems.GLASS_ITEM);
        gui.getFiller().fillBetweenPoints(4, 0, 5, 9, GuiItems.GLASS_ITEM);
        gui.getFiller().fillBetweenPoints(3, 0, 3, 9, GuiItems.GLASS_ITEM);

        gui.setItem(1, 5, ItemBuilder.from(Material.DIAMOND_SWORD).name(MiniMessageUtils.miniMessageString("<italic:false><green>Your skills")).flags(ItemFlag.values()).asGuiItem());

        gui.setItem(39, GuiItems.BACK_ITEM);
        gui.setItem(40, GuiItems.CLOSE_ITEM);

        updateItems();
    }

    private void updateItems() {
        List<GuiItem> guiItems = items.subList(currentPage * 7, Math.min(7 * (currentPage + 1), items.size()));
        int size = guiItems.size();
        List<Integer> layoutForItems = getLayoutForItems(size);
        getLayoutForItems(7).forEach(i -> gui.setItem(3, i, GuiItems.GLASS_ITEM));
        for (int i = 0; i < size; i++) {
            gui.setItem(3, layoutForItems.get(i), guiItems.get(i));
        }


        if (items.size() > 7) {
            gui.setItem(3, 1, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> previous()));
            gui.setItem(3, 9, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> next()));
        }
        gui.update();
    }

    public void next() {
        if (currentPage * 7 + 7 >= items.size()) {
            return;
        }
        currentPage++;
        updateItems();
    }

    public void previous() {
        if (currentPage == 0) {
            return;
        }
        currentPage--;
        updateItems();
    }

    public static @NotNull GuiItem getSkillRewardGuiItem(SkillInfo skillInfo, int level, boolean unlocked) {
        Material material = unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        List<Component> lore = new ArrayList<>(getRewardsLoreForLevel(skillInfo, level));
        if (unlocked) {
            lore.add(MiniMessageUtils.miniMessageString("<italic:false><green>Unlocked!"));
        } else {
            lore.add(MiniMessageUtils.miniMessageString("<italic:false><red>Locked!"));
        }

        return ItemBuilder.from(material).name(MiniMessageUtils.miniMessageString("<italic:false><green><name> Level <level>", Map.of("name", StringUtils.setTitleCase(skillInfo.getName()), "level", StringUtils.valueOf(level)))).lore(lore).asGuiItem();
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

    public static GuiItem getSkillGuiItem(Skill skill) {
        return getSkillGuiItem(skill, skill.getType().getIcon());
    }

    public static GuiItem getSkillGuiItem(Skill skill, Material material) {
        return getSkillGuiItem(skill, new ItemStack(material));
    }

    @NotNull
    public static GuiItem getSkillGuiItem(Skill skill, ItemStack baseItem) {
        SkillInfo skillType = skill.getType();
        String skillName = StringUtils.setTitleCase(skillType.getName());
        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessageUtils.miniMessageString(""));
        lore.add(MiniMessageUtils.miniMessageString("<italic:false><gray>Progress to Level <next-level>: <yellow><progress>%", Map.of("next-level", String.valueOf(skill.getLevel() + 1),
                "progress", StringUtils.getNumberFormat(skill.getXp() / skill.getXpToLevel() * 100))));
        lore.add(MiniMessageUtils.miniMessageComponent("<italic:false><bar> <yellow><xp><gold>/<yellow><max>", Map.of(
                "bar", MiniMessageUtils.progressBar(skill.getXp(), skill.getXpToLevel(), 20),
                "xp", MiniMessageUtils.miniMessageString(StringUtils.getNumberFormat(skill.getXp())),
                "max", MiniMessageUtils.miniMessageString(StringUtils.getShortNumber(skill.getXpToLevel())))));
        if (skill.getLevel() < skill.getType().getMaxLevel()) {
            lore.addAll(getRewardsLoreForLevel(skillType, skill.getLevel() + 1));
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

    public static List<Component> getRewardsLoreForLevel(SkillInfo skillType, int level) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.space());
        lore.add(MiniMessageUtils.miniMessageString("<italic:false><gray>Level <level> Rewards:", Map.of(
                "level", String.valueOf(level)
        )));
        lore.addAll(getOnlyRewardsLoreForLevel(skillType.getRewards(level)));
        return lore;
    }

    public static List<Component> getOnlyRewardsLoreForLevel(List<SkillReward> rewards) {
        List<Component> lore = new ArrayList<>();
        if (rewards == null) {
            return lore;
        }
        for (SkillReward skillReward : rewards) {
            lore.add(MiniMessageUtils.miniMessageComponent("  <italic:false><msg>", Map.of("msg", skillReward.getRewardMessage())));
        }
        return lore;
    }

    public void open() {
        gui.open(player);
    }
}
