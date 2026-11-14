package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MenuGui {
    public static final String MENU_TITLE = "<dark_aqua><bold>Menu";
    public static final String SKYBLOCK_WORLD = "ASkyBlock";
    private final Player player;
    private final Gui gui;

    public static final CaveCrawlers plugin = CaveCrawlers.getInstance();
    public static CustomConfig config = new CustomConfig("menu.yml");

    public MenuGui(Player player) {
        this.player = player;
        Stats stats = StatsManager.getInstance().getStats(player);
        String[] statsMessage = stats.toFormatString().split("\n");
        this.gui = Gui.gui().rows(6).title(MiniMessageUtils.miniMessageString(MENU_TITLE)).create();

        gui.disableAllInteractions();
        gui.setItem(13, ItemBuilder.skull().owner(player).setName("§b" + player.getName() + "'s Stats").setLore(statsMessage).asGuiItem());
        gui.setItem(20, ItemBuilder.from(Material.DIAMOND_SWORD).setName("§aSkills").setLore("§7View your skill progression", "§7and level rewards.", "", "§eClick to view skills").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            runCommand(sender, getCommand("skills", SKILLS_COMMAND));
        })));
        gui.setItem(21, ItemBuilder.skull().texture(DAILY_SKULL_TEXTURE).setName("§eDaily Rewards").setLore("§7Come back each day", "§7to claim your rewards.", "", "§eClick to view daily rewards").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            runCommand(sender, getCommand("daily", DAILY_COMMAND));
        })));
        gui.setItem(22, ItemBuilder.from(Material.BOOK).setName("§bIndex").setLore("§7Discover mobs, drops,", "§7bosses, blocks and altars.", "", "§eClick to browse the index").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            runCommand(sender, getCommand("index", INDEX_COMMAND));
        })));
        gui.setItem(23, ItemBuilder.skull().texture(WARRPS_SKULL_TEXTURE).setName("§bWarps").setLore("§7Explore the server and", "§7choose your next destination.", "", "§eClick to view warps").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            runCommand(sender, getCommand("warps", WARP_COMMAND));
        })));
        gui.setItem(24, ItemBuilder.from(Material.ENDER_CHEST).setName("§dStorage").setLore("§7Access your personal", "§7item storage.", "", "§eClick to choose storage").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTask(plugin, () -> new StorageGui(sender).open());
        })));
        gui.setItem(31, ItemBuilder.from(Material.GOLD_INGOT).setName("§6Sell Items").setLore("§7Turn your spare items", "§7into coins.", "", "§eClick to open the sell menu").asGuiItem((event -> {
            Player sender = (Player) event.getWhoClicked();
            runCommand(sender, getCommand("sell", SELL_COMMAND));
        })));

        World world = player.getWorld();
        if (hasSkyblock() && !world.getName().equalsIgnoreCase(SKYBLOCK_WORLD)) {
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP_SKULL_TEXTURE).setName("§bWarp To: §aPrivate Island").setLore("§7Teleports you back to your", "§7private island.", "", "§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                runCommand(sender, getCommand("island", ISLAND_COMMAND));
            })));
        } else {
            gui.setItem(47, ItemBuilder.skull().texture(ISLAND_WARP_SKULL_TEXTURE).setName("§bWarp To: §aHub").setLore("§7Teleports you to", "§7spawn.", "", "§eClick to Warp").asGuiItem((event -> {
                Player sender = (Player) event.getWhoClicked();
                runCommand(sender, getCommand("hub", HUB_COMMAND));
            })));
        }

        gui.setItem(49, GuiItems.CLOSE_ITEM);
        gui.getFiller().fill(GuiItems.GLASS_ITEM);
    }
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
    public static final String INDEX_COMMAND = config.getString("index-command", "index");
    public static final String WARP_COMMAND = config.getString("warp-command", "warps");
    public static final String DAILY_COMMAND = config.getString("daily-command", "daily");
    public static final String STORAGE_COMMAND = config.getString("storage-command", "ec");
    public static final String PETS_COMMAND = config.getString("pets-command", "pets");
    public static final String CRAFTING_TABLE_COMMAND = config.getString("crafting-table-command", "craftingtable");
    public static final String MADDOX_COMMNAD = config.getString("maddox-command", "slayer");
    public static final String SELL_COMMAND = config.getString("sell-command", "sell");
    public static final String ACBAG_COMMAND = config.getString("acbag-command", "acbag");
    public static final String ISLAND_COMMAND = config.getString("island-command", "is");
    public static final String HUB_COMMAND = config.getString("hub-command", "spawn");
    // settings

    private static boolean hasSkyblock() {
        return plugin.getConfig().getBoolean("has-skyblock", false);
    }

    static String getCommand(String key, String fallback) {
        return plugin.getConfig().getString("menu.commands." + key, fallback);
    }

    static void runCommand(Player player, String command) {
        // Inventory changes must wait until the click event has finished.
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            // Menu aliases such as /daily also need the player command preprocess event.
            player.chat(command.startsWith("/") ? command : "/" + command);
        });
    }

    public void open(){
        gui.open(player);
    }
}
