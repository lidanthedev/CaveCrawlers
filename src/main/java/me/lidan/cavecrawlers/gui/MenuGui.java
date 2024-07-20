package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.levels.LevelConfigLoader;
import me.lidan.cavecrawlers.levels.LevelInfo;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MenuGui {
    private final Player player;
    private final Gui gui;
    // heads
    public final String KITS_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTg3MDhkNGI0YWIxMGI5YmE4NWVkMWE5MjQyYmY4MTEwNWM1NTk2ZDc0M2YyY2EyMGEzMzg3ZTI5ZDA2MzM0NSJ9fX0=";
    public final String WARRPS_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBiZmMyNTc3ZjZlMjZjNmM2ZjczNjVjMmM0MDc2YmNjZWU2NTMxMjQ5ODkzODJjZTkzYmNhNGZjOWUzOWIifX19";
    public final String DAILY_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY3N2U2NWRmMjk5OWQwMzE5ZmRiY2JhM2MwOTJmMTYwYjk5YjRiNDY3OTgzYWY4MWZjZmExZWI0NWQzOWEzIn19fQ==";
    public final String MADDOX_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTMzNmQ3Y2M5NWNiZjY2ODlmNWU4Yzk1NDI5NGVjOGQxZWZjNDk0YTQwMzEzMjViYjQyN2JjODFkNTZhNDg0ZCJ9fX0=";
    public final String ACBAG_SKULL_TEXTRUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYxYTkxOGMwYzQ5YmE4ZDA1M2U1MjJjYjkxYWJjNzQ2ODkzNjdiNGQ4YWEwNmJmYzFiYTkxNTQ3MzA5ODVmZiJ9fX0=";
    public final String ISLAND_WARP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjE1MWNmZmRhZjMwMzY3MzUzMWE3NjUxYjM2NjM3Y2FkOTEyYmE0ODU2NDMxNThlNTQ4ZDU5YjJlYWQ1MDExIn19fQ==";
    // commands
    public final String SKILLS_COMMAND = "skills";
    public final String KITS_COMMAND = "kits";
    public final String SHOP_COMMAND = "shop";
    public final String WARP_COMMAND = "warp";
    public final String DAILY_COMMAND = "daily";
    public final String ENDER_CHEST_COMMAND = "ec";
    public final String PETS_COMMAND = "pets";
    public final String CRAFTING_TABLE_COMMAND = "craft";
    public final String MADDOX_COMMNAD = "slayermenu";
    public final String SELL_COMMAND = "sell";
    public final String ACBAG_COMMAND = "acbag";
    public final String ISLAND_COMMAND = "is";
    public final String HUB_COMMAND = "spawn";

    public MenuGui(Player player) {
        this.player = player;
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        int currentLevel = LevelConfigLoader.getInstance(JavaPlugin.getPlugin(CaveCrawlers.class)).getPlayerLevel(player.getUniqueId().toString());
        String currentColor = LevelConfigLoader.getInstance(JavaPlugin.getPlugin(CaveCrawlers.class)).getLevelColor(5);
        this.gui = new Gui(6, "§rMenu");

        gui.disableAllInteractions();
        gui.setItem(1, ItemBuilder.skull().texture(MADDOX_SKULL_TEXTURE).setName("Skyblock Level").setLore(ChatColor.GRAY + "SkyBlock Level "+ currentColor + currentLevel).asGuiItem());
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§f%s Stats:".formatted(player.getName())).setLore(statsMessage).asGuiItem());
        gui.setItem(20, ItemBuilder.from(Material.DIAMOND_SWORD).setName("§aSkills").setLore("§7View your Skill progression and\n §7rewards.").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SKILLS_COMMAND);
        })));
        gui.setItem(21, ItemBuilder.skull().texture(KITS_SKULL_TEXTURE).setName("§eKits").setLore("§7Redeem your kits, whenever","§7you want. Some kits are only for §edonator ranks", "§k","§eClick to preview kits").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(KITS_COMMAND);
        })));
        gui.setItem(22, ItemBuilder.from(Material.EMERALD).setName("§aShop").setLore("§c","§7you will be able to buy and sell","§7all type of items.","§c","§eClick to open").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SHOP_COMMAND);
        })));
            gui.setItem(23, ItemBuilder.skull().texture(WARRPS_SKULL_TEXTURE).setName("§eWarps").setLore("§7Teleport and explore all", "§fwarps on that realm.", "§", "§eClick to preview kits").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(WARP_COMMAND);
        })));
        gui.setItem(24, ItemBuilder.skull().texture(DAILY_SKULL_TEXTURE).setName("§eDaily").setLore("§7Redeem your dailies, whenever", "§7you want. Some dailies are only", "§7for §edonator ranks§7.", "§7","§eClick to preview kits").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(DAILY_COMMAND);
        })));
        gui.setItem(25, ItemBuilder.from(Material.ENDER_CHEST).setName("§aEnder Chest").setLore("§7Store Items","§7At Your Ender Chest.").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(ENDER_CHEST_COMMAND);
        })));
        gui.setItem(30, ItemBuilder.from(Material.BONE).setName("§aPets").setLore("§7Pets Gives Stat boosts","§7And level up from skills.","§7","§eClick to open").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(PETS_COMMAND);
        })));
        gui.setItem(31, ItemBuilder.from(Material.CRAFTING_TABLE).setName("§eCrafting Table").setLore("§7Craft Items and vanilla items", "§eClick to open").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(CRAFTING_TABLE_COMMAND);
        })));
        gui.setItem(32, ItemBuilder.skull().texture(MADDOX_SKULL_TEXTURE).setName("§cSlayers").setLore("§7Click to start §cHARD Bosses").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(MADDOX_COMMNAD);
        })));
        gui.setItem(40, ItemBuilder.from(Material.GOLD_INGOT).setName("§eSell").setLore("§7","§7Click to sell items").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SELL_COMMAND);
        })));
        World world = player.getWorld();
        if (world.getName().equalsIgnoreCase("ASkyBlock")) { /// did here skull texture island warp too cuz it's the same head
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP).setName("§bWarp To: §aHub").setLore("§7Teleports you to", "§7spawn.", "","§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                sender.performCommand(HUB_COMMAND);
            })));
        } else {
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP).setName("§bWarp To: §aPrivate Island").setLore("§7Teleports you back to your", "§7private island.", "","§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                sender.performCommand(ISLAND_COMMAND);
            })));
        }

        gui.setItem(53, ItemBuilder.skull().texture(ACBAG_SKULL_TEXTRUE).setName("§aAccessory Bag").setLore("§7Store all of your accessories.").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(ACBAG_COMMAND);
        })));
        gui.setItem(49, ItemBuilder.from(Material.BARRIER).setName(ChatColor.RED + "Close Menu").setLore("","§eClick to Close Menu").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.closeInventory();
        })));
        gui.getFiller().fill(ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(Component.text("")).asGuiItem());
    }

    public void open(){
        gui.open(player);
    }
}
