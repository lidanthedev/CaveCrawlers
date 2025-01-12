package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MenuGui {
    public static final String MENU_TITLE = "<gray>Menu";
    public static final String SKYBLOCK_WORLD = "ASkyBlock";
    private final Player player;
    private final Gui gui;

    public static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static CustomConfig config = new CustomConfig("menu.yml");
    // heads
    public static final String KITS_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTg3MDhkNGI0YWIxMGI5YmE4NWVkMWE5MjQyYmY4MTEwNWM1NTk2ZDc0M2YyY2EyMGEzMzg3ZTI5ZDA2MzM0NSJ9fX0=";
    public static final String WARRPS_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBiZmMyNTc3ZjZlMjZjNmM2ZjczNjVjMmM0MDc2YmNjZWU2NTMxMjQ5ODkzODJjZTkzYmNhNGZjOWUzOWIifX19";
    public static final String DAILY_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY3N2U2NWRmMjk5OWQwMzE5ZmRiY2JhM2MwOTJmMTYwYjk5YjRiNDY3OTgzYWY4MWZjZmExZWI0NWQzOWEzIn19fQ==";
    public static final String MADDOX_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTMzNmQ3Y2M5NWNiZjY2ODlmNWU4Yzk1NDI5NGVjOGQxZWZjNDk0YTQwMzEzMjViYjQyN2JjODFkNTZhNDg0ZCJ9fX0=";
    public static final String ACBAG_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYxYTkxOGMwYzQ5YmE4ZDA1M2U1MjJjYjkxYWJjNzQ2ODkzNjdiNGQ4YWEwNmJmYzFiYTkxNTQ3MzA5ODVmZiJ9fX0=";
    public static final String ISLAND_WARP_SKULL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjE1MWNmZmRhZjMwMzY3MzUzMWE3NjUxYjM2NjM3Y2FkOTEyYmE0ODU2NDMxNThlNTQ4ZDU5YjJlYWQ1MDExIn19fQ==";
    // commands
    public static final String SKILLS_COMMAND = config.getString("skills-command", "skills");
    public static final String KITS_COMMAND = config.getString("kits-command", "kits");
    public static final String SHOP_COMMAND = config.getString("shop-command", "shop");
    public static final String WARP_COMMAND = config.getString("warp-command", "warp");
    public static final String DAILY_COMMAND = config.getString("daily-command", "daily");
    public static final String STORAGE_COMMAND = config.getString("storage-command", "ec");
    public static final String PETS_COMMAND = config.getString("pets-command", "pets");
    public static final String CRAFTING_TABLE_COMMAND = config.getString("crafting-table-command", "craftingtable");
    public static final String MADDOX_COMMNAD = config.getString("maddox-command", "slayer");
    public static final String SELL_COMMAND = config.getString("sell-command", "sell");
    public static final String ACBAG_COMMAND = config.getString("acbag-command", "acbag");
    public static final String ISLAND_COMMAND = config.getString("island-command", "is");
    public static final String HUB_COMMAND = config.getString("hub-command", "hub");
    // settings
    public static final boolean HAS_SKYBLOCK = plugin.getConfig().getBoolean("has-skyblock", false);

    public MenuGui(Player player) {
        this.player = player;
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        this.gui = Gui.gui().rows(6).title(MiniMessageUtils.miniMessageString(MENU_TITLE)).create();

        gui.disableAllInteractions();
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§f%s Stats:".formatted(player.getName())).setLore(statsMessage).asGuiItem());
        gui.setItem(20, ItemBuilder.from(Material.DIAMOND_SWORD).setName("§aSkills").setLore("§7View your Skill progression and\n §7rewards.").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SKILLS_COMMAND);
        })));
        gui.setItem(21, ItemBuilder.skull().texture(KITS_SKULL_TEXTURE).setName("§eKits").setLore("§7Redeem your kits, whenever","§7you want. Some kits are only for §edonator ranks", "§k","§eClick to preview kits").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(KITS_COMMAND);
        })));
        gui.setItem(22, ItemBuilder.from(Material.EMERALD).setName("§aShop").setLore("§7Buy and sell", "§7all types of items.", "§c", "§eClick to open").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SHOP_COMMAND);
        })));
        gui.setItem(23, ItemBuilder.skull().texture(WARRPS_SKULL_TEXTURE).setName("§eWarps").setLore("§7Teleport and explore all", "§7warps on that realm.", "§", "§eClick to preview kits").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(WARP_COMMAND);
        })));
        gui.setItem(24, ItemBuilder.from(Material.ENDER_CHEST).setName("§aStorage").setLore("§7Store Items", "§7At Your Personal Storage.", "", "§eClick to open").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(STORAGE_COMMAND);
        })));
        gui.setItem(31, ItemBuilder.from(Material.GOLD_INGOT).setName("§eSell").setLore("§7", "§7Click to sell items").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            sender.performCommand(SELL_COMMAND);
        })));

        World world = player.getWorld();
        if (HAS_SKYBLOCK && !world.getName().equalsIgnoreCase(SKYBLOCK_WORLD)) {
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP_SKULL_TEXTURE).setName("§bWarp To: §aPrivate Island").setLore("§7Teleports you back to your", "§7private island.", "", "§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                sender.performCommand(ISLAND_COMMAND);
            })));
        } else {
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP_SKULL_TEXTURE).setName("§bWarp To: §aHub").setLore("§7Teleports you to", "§7spawn.", "", "§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                sender.performCommand(HUB_COMMAND);
            })));
        }

        gui.setItem(49, GuiItems.CLOSE_ITEM);
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
    }

    public void open(){
        gui.open(player);
    }
}
