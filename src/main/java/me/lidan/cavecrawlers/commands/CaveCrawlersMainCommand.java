package me.lidan.cavecrawlers.commands;

import com.cryptomorin.xseries.XMaterial;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarDrop;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.bosses.BossDrop;
import me.lidan.cavecrawlers.bosses.BossDrops;
import me.lidan.cavecrawlers.drops.DropLoader;
import me.lidan.cavecrawlers.entities.BossEntityData;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.gui.ItemsGui;
import me.lidan.cavecrawlers.gui.PlayerViewer;
import me.lidan.cavecrawlers.index.IndexBlocksCategoryMenu;
import me.lidan.cavecrawlers.index.IndexMobsCategoryMenu;
import me.lidan.cavecrawlers.integration.MythicMobsHook;
import me.lidan.cavecrawlers.items.*;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.BoomAbility;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.levels.LevelConfigManager;
import me.lidan.cavecrawlers.levels.LevelInfo;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockLoader;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksManager;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.shop.editor.ShopEditor;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerData;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.*;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.*;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.bukkit.Bukkit.getConsoleSender;

@Command({"ct", "cc", "cavecrawlers"})
@CommandPermission("cavecrawlers.test")
public class CaveCrawlersMainCommand {
    private static final Logger log = LoggerFactory.getLogger(CaveCrawlersMainCommand.class);
    private final ShopManager shopManager = ShopManager.getInstance();
    private final ItemsManager itemsManager = ItemsManager.getInstance();
    private final StatsManager statsManager = StatsManager.getInstance();
    private final MiningManager miningManager = MiningManager.getInstance();
    private final AbilityManager abilityManager = AbilityManager.getInstance();
    private final PerksManager perksManager = PerksManager.getInstance();
    private final EntityManager entityManager = EntityManager.getInstance();
    private final AltarManager altarManager = AltarManager.getInstance();
    private final LevelConfigManager levelconfigManager = LevelConfigManager.getInstance();
    private final CommandHandler handler;
    private final Map<UUID, LevelInfo> playerLevelInfo = new HashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private CustomConfig config = new CustomConfig("test");
    public CaveCrawlersMainCommand(CommandHandler handler) {
        this.handler = handler;
        handler.getAutoCompleter().registerSuggestion("itemID", (args, sender, command) -> itemsManager.getKeys());
        handler.getAutoCompleter().registerSuggestion("shopId", (args, sender, command) -> ShopManager.getInstance().getKeys());
        handler.getAutoCompleter().registerSuggestion("handID", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            if (player != null) {
                return getFillID(player);
            }
            return Collections.singleton("");
        });
        handler.getAutoCompleter().registerSuggestion("abilityID", (args, sender, command) -> abilityManager.getAbilityMap().keySet());
        if (plugin.getMythicBukkit() != null) {
            handler.getAutoCompleter().registerSuggestion("mobID", (args, sender, command) -> plugin.getMythicBukkit().getMobManager().getMobNames());
            handler.getAutoCompleter().registerSuggestion("skillID", (args, sender, command) -> plugin.getMythicBukkit().getSkillManager().getSkillNames());
        } else {
            handler.getAutoCompleter().registerSuggestion("mobID", (args, sender, command) -> Collections.emptySet());
            handler.getAutoCompleter().registerSuggestion("skillID", (args, sender, command) -> Collections.emptySet());
        }
        handler.getAutoCompleter().registerSuggestion("abilityID", (args, sender, command) -> abilityManager.getAbilityMap().keySet());
    }

    @NotNull
    private static Set<String> getFillID(Player player) {
        ItemStack hand = player.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return Collections.singleton("");
        }
        String name = meta.getDisplayName();
        name = ChatColor.stripColor(name);
        name = name.toUpperCase(Locale.ROOT);
        name = name.replaceAll(" ", "_");
        return Collections.singleton(name);
    }

    private Component getHelpMessage(HelpCommandType type, String command, String description) {
        String onlyCommand = command.split("<")[0];
        if (onlyCommand.contains("["))
            onlyCommand = onlyCommand.split("\\[")[0];
        if (type == HelpCommandType.TITLE)
            return MiniMessageUtils.miniMessageString("<color:#D3495B><b><title></b></color>", Map.of("title", command));
        if (type == HelpCommandType.COMMAND) {
            onlyCommand = onlyCommand.toLowerCase();
            return MiniMessageUtils.miniMessageString("<click:suggest_command:'<only_command>'><color:#E9724C><u><all_command></u></color></click> <color:#E0AF79><i>- <description></i></color>\n", Map.of("only_command", onlyCommand, "all_command", command, "description", description));
        }
        if (type == HelpCommandType.LINE)
            return MiniMessageUtils.miniMessageString("<color:#c04253>-------------------------------------</color>");
        return MiniMessageUtils.miniMessageString("<red>ERROR</red>");
    }

    @Subcommand("help")
    @DefaultFor({"ct", "cc", "cavecrawlers"})
    public void mainHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.TITLE, "CaveCrawlers Help", ""));
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc help", "show this message"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item", "item commands"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc shop", "shop commands"));
    }

    @Subcommand("help item")
    @DefaultFor({"ct item", "cc item", "cavecrawlers item"})
    public void itemHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.TITLE, "CaveCrawlers Item Help", ""));
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item give <player> <Item id> [amount]", "give a player an item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item get <Item ID> [amount]", "give yourself an item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item browse", "open the item browser"));
        sender.sendMessage(getHelpMessage(HelpCommandType.TITLE, "CaveCrawlers Item Editor Help", ""));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item create <id> <material>", "create an item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item clone <originId> <id>", "clone an item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit stat <stat> <number>", "edit an item's stat"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit ability <ability>", "edit an item's ability"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit name <name>", "edit an item's name"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit description <description>", "edit an item's description"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit type <type>", "edit an item's type"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit rarity <rarity>", "edit an item's rarity"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit baseItem <material>", "edit an item's base item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item edit baseItemToHand <id>", "edit an item's base item to the item in your hand"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc item import <id>", "import the item in your hand (advanced)"));
    }

    @Subcommand("help shop")
    @DefaultFor({"ct shop", "cc shop", "cavecrawlers shop"})
    public void shopHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.TITLE, "CaveCrawlers Shop Help", ""));
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc shop create <name>", "create a shop item"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc shop open <shop-name>", "open the shop"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc shop editor <shop-name>", "open the shop editor"));
    }

    @Subcommand("help altar")
    @DefaultFor({"ct altar", "cc altar", "cavecrawlers altar"})
    public void altarHelp(CommandSender sender) {
        /*
        Altar commands:
        /cc altar create <name> - create an altar
        /cc altar addspawn <altar-name> <mob-name> <chance> - add a mob spawn to an altar
        /cc altar addsummonblock <altar-name> - adds the block you are looking at as a summon block
        /cc altar setspawnlocation <altar-name> - sets the spawn location for the altar
        /cc altar info <altar-name> - get info about an altar
         */
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.TITLE, "CaveCrawlers Altar Help", ""));
        sender.sendMessage("");
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc altar create <name>", "create an altar"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc altar addspawn <altar-name> <mob-name> <chance>", "add a mob spawn to an altar"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc altar addsummonblock <altar-name>", "adds the block you are looking at as a summon block"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc altar setspawnlocation <altar-name>", "sets the spawn location for the altar"));
        sender.sendMessage(getHelpMessage(HelpCommandType.COMMAND, "/cc altar info <altar-name>", "get info about an altar"));
    }

    @Subcommand("reload items")
    public void reloadItems(CommandSender sender) {
        ItemsLoader loader = ItemsLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Items!");
    }

    @Subcommand("reload shops")
    public void reloadShops(CommandSender sender) {
        ShopLoader loader = ShopLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Shops!");
    }

    @Subcommand("reload blocks")
    public void reloadBlocks(CommandSender sender) {
        BlockLoader loader = BlockLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Blocks!");
    }

    @Subcommand("reload drops")
    public void reloadDrops(CommandSender sender) {
        DropLoader loader = DropLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Drops!");
    }

    @Subcommand("reload plugin")
    public void reloadPlugin(CommandSender sender) {
        if (Bukkit.getPluginManager().getPlugin("PlugMan") == null) {
            sender.sendMessage(ChatColor.RED + "PlugMan is required for this command!");
            return;
        }
        Bukkit.dispatchCommand(getConsoleSender(), "plugman reload CaveCrawlers");
        sender.sendMessage(ChatColor.GREEN + "CaveCrawlers reloaded!");
    }

    @Subcommand("reload addons")
    public void reloadAddons(CommandSender sender) {
        if (Bukkit.getPluginManager().getPlugin("PlugMan") == null) {
            sender.sendMessage(ChatColor.RED + "PlugMan is required for this command!");
            return;
        }
        @NotNull Plugin[] plugins = CaveCrawlers.getInstance().getServer().getPluginManager().getPlugins();
        for (Plugin plugin : plugins) {
            if (plugin.getPluginMeta().getPluginDependencies().contains("CaveCrawlers")) {
                Bukkit.dispatchCommand(getConsoleSender(), "plugman reload " + plugin.getName());
                sender.sendMessage(ChatColor.GREEN + plugin.getName() + " reloaded!");
            }
        }
    }

    @Subcommand("reload all")
    public void reloadAll(CommandSender sender) {
        if (Bukkit.getPluginManager().getPlugin("PlugMan") == null) {
            sender.sendMessage(ChatColor.RED + "PlugMan is required for this command!");
            return;
        }
        reloadPlugin(sender);
        reloadAddons(sender);
    }

    @Subcommand("config saveStats")
    public void saveStats(Player sender) {
        config.set("stat", statsManager.getStats(sender));
        sender.sendMessage("set stat to your stats!");
        config.save();
    }

    @Subcommand("config saveConfMsg")
    public void saveConfigMessage(Player sender) {
        config.set("conf-msg", new ConfigMessage("error %player_name%", "Cool %player_name%", Sound.BLOCK_ANVIL_DESTROY));
        sender.sendMessage("Saved Config Message!");
        config.save();
    }

    @Subcommand("config sendConfMsg")
    public void sendConfigMessage(Player sender) {
        ConfigMessage message = (ConfigMessage) config.get("conf-msg");
        message.sendMessage(sender);
    }

    @Subcommand("config send")
    public void sendConfig(Player sender, String key) {
        sender.sendMessage("" + config.get(key));
    }

    @Subcommand("config save")
    public void saveConfig(CommandSender sender) {
        config.save();
    }

    @Subcommand("config reload")
    public void reloadConfig(CommandSender sender) {
        config.load();
    }

    @Subcommand("item getID")
    public void itemGetID(Player sender) {
        ItemStack hand = sender.getInventory().getItemInMainHand();
        String ID = itemsManager.getIDofItemStack(hand);
        sender.sendMessage(Objects.requireNonNullElse(ID, "This is not a Custom Item!"));
    }

    @Subcommand("item update")
    public void itemUpdate(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemStack updateItemStack = itemsManager.updateItemStack(hand);
        sender.getEquipment().setItem(EquipmentSlot.HAND, updateItemStack);
    }

    @Subcommand("item updateInv")
    public void itemUpdateInv(Player sender) {
        itemsManager.updatePlayerInventory(sender);
    }

    @Subcommand("item give")
    @AutoComplete("* @itemID *")
    public void itemGive(CommandSender sender, Player player, @Named("Item id") String id, @Default("1") int amount) {
        ItemStack exampleSword = itemsManager.buildItem(id, 1);
        for (int i = 0; i < amount; i++) {
            itemsManager.giveItemStacks(player, exampleSword);
        }
    }

    @Subcommand("item get")
    @AutoComplete("@itemID *")
    public void itemGet(Player sender, @Named("Item ID") String ID, @Default("1") int amount) {
        itemGive(sender, sender, ID, amount);
    }

    @Subcommand("item import")
    @AutoComplete("@handID *")
    public void itemImport(Player sender, String id) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();

        ItemInfo oldInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (oldInfo != null) {
            sender.sendMessage("ERROR! Item already has id!");
            sender.sendMessage("Don't want that? remove with the id /cc item remove-id");
            return;
        }

        if (id.equals("FILL")) {
            id = getFillID(sender).iterator().next();
            sender.sendMessage("Fill: " + id);
        }

        ItemInfo itemInfo;
        try {
            ItemExporter exporter = new ItemExporter(hand);
            itemInfo = exporter.toItemInfo();
        } catch (Exception error) {
            sender.sendMessage("Seems like you didn't use the right format but I'll try to create the item anyways");
            String name = id.replace("_", " ");
            name = StringUtils.setTitleCase(name);
            Stats stats = new Stats();
            itemInfo = new ItemInfo(name, stats, ItemType.MATERIAL, hand, Rarity.COMMON);
        }

        itemsManager.setItem(id, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        sender.getInventory().addItem(itemStack);

        sender.sendMessage("Exported Item with id " + id);
    }

    @Subcommand("item remove-id")
    public void itemRemoveID(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemNbt.removeTag(hand, ItemsManager.ITEM_ID);
        sender.sendMessage("Removed ID from Item! it will no longer update or apply stats!");
    }

    @Subcommand("item browse")
    public void itemBrowse(Player sender, @Default("") String query) {
        new ItemsGui(sender, query).open();
    }

    // item creating and editing commands:
    // item create <id> <material> - create item with id and material
    // you must hold an item with an already existing id to edit
    // item edit stat <stat> <number> - edit held item's stat
    // item edit ability <ability> - edit held item's  ability
    // item edit name <name> - edit held item's name
    // item edit description <description> - edit held item's description
    // item edit type <type> - edit held item's type
    // item edit rarity <rarity> - edit held item's rarity
    // item edit baseItem <material> - edit held item's base item
    @Subcommand("item create")
    public void itemCreate(Player sender, String id, Material material) {
        if (itemsManager.getItemByID(id) != null) {
            sender.sendMessage("ERROR! ITEM ALREADY EXISTS!");
            return;
        }
        id = id.toUpperCase();
        String name = id.replace("_", " ");
        name = StringUtils.setTitleCase(name);
        Stats stats = new Stats();
        ItemInfo itemInfo = new ItemInfo(name, stats, ItemType.MATERIAL, material, Rarity.COMMON);
        itemsManager.setItem(id, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        sender.getInventory().addItem(itemStack);
        sender.sendMessage("Created Item!");
    }

    @Subcommand("item clone")
    @AutoComplete("@itemID *")
    public void itemClone(Player sender, String originId, String id) {
        ItemInfo itemInfo = itemsManager.getItemByID(originId);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! ITEM DOESN'T EXIST!");
            return;
        }

        itemsManager.setItem(id, itemInfo.clone());
        ItemStack itemStack = itemsManager.buildItem(id, 1);
        sender.getInventory().addItem(itemStack);
        sender.sendMessage("Cloned Item!");
    }

    @Subcommand("item edit stat")
    public void itemEditStat(Player sender, StatType stat, double number) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.getStats().set(stat, number);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Stat!");
    }

    @Subcommand("item edit ability")
    @AutoComplete("@abilityID")
    public void itemEditAbility(Player sender, String abilityId) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setAbilityID(abilityId);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Ability!");
    }

    @Subcommand("item edit name")
    public void itemEditName(Player sender, String name) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setName(name);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Name!");
    }

    @Subcommand("item edit description")
    public void itemEditDescription(Player sender, @Default("reset") String description) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        if (description.equals("reset")) {
            description = null;
        }
        itemInfo.setDescription(description);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Description!");
    }

    @Subcommand("item edit type")
    public void itemEditType(Player sender, ItemType type) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setType(type);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Type!");
    }

    @Subcommand("item edit rarity")
    public void itemEditRarity(Player sender, Rarity rarity) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setRarity(rarity);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Rarity!");
    }

    @Subcommand("item edit baseItem")
    public void itemEditBaseItem(Player sender, Material material) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setBaseItem(new ItemStack(material));
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Base Item!");
    }

    @Subcommand("item edit baseItemToHand")
    @AutoComplete("@itemID")
    public void itemEditBaseItemToHand(Player sender, String id) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfoHand = itemsManager.getItemFromItemStack(hand);
        if (itemInfoHand != null) {
            sender.sendMessage("ERROR! HELD ITEM ALREADY HAS ID!");
            return;
        }
        ItemInfo itemInfo = itemsManager.getItemByID(id);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! NO ITEM INFO FOUND BY THE PROVIDED ID!");
            return;
        }
        itemInfo.setBaseItem(new ItemStack(hand));
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Base Item!");
    }

    @Subcommand("lores")
    public void showLore(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }

        List<String> lore = meta.getLore();
        String name = meta.getDisplayName();

        Component nameMessage = MiniMessageUtils.miniMessage(
                "<hover:show_text:'Click to rename'><click:suggest_command:'/ie rename <name_click>'><name></click></hover>",
                Map.of("name", name, "name_click", name.replaceAll("§", "&"))
        );
        sender.sendMessage(nameMessage);

        if (!meta.hasLore()) return;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            Component lineMessage = MiniMessageUtils.miniMessage(
                    "<hover:show_text:'Click to edit'><click:suggest_command:'/ie lore set <index> <line_click>'><line></click></hover>",
                    Map.of("index", String.valueOf(i + 1), "line", line, "line_click", line.replaceAll("§", "&"))
            );
            sender.sendMessage(lineMessage);
        }
    }

    @Command("lores")
    public void loresCommand(Player sender) {
        showLore(sender);
    }

    @Subcommand("packet test")
    public void packetTest(Player player, int stage) {
        PacketManager packetManager = PacketManager.getInstance();
        packetManager.setBlockDestroyStage(player, player.getTargetBlock(null, 10).getLocation(), stage);
    }

    @Subcommand("nbt set")
    public void nbtSet(Player sender, String key, String value) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemNbt.setString(hand, key, value);
        sender.sendMessage("set NBT!");
    }

    @Subcommand("nbt get")
    public void nbtGet(Player sender, String key) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        String value = ItemNbt.getString(hand, key);
        sender.sendMessage("value: " + value);
    }

    @Subcommand("nbt send")
    public void nbtSend(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        for (NamespacedKey key : dataContainer.getKeys()) {
            String value = dataContainer.get(key, PersistentDataType.STRING);
            sender.sendMessage(key + ": " + value);
        }
    }

    @Subcommand("pixel auction")
    public void pixelAuction(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }
        if (!meta.hasLore()) return;
        List<String> lore = meta.getLore();
        List<Integer> linesToDelete = new ArrayList<>();
        int auctionLine = -1;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains(ChatColor.DARK_GRAY + "[")) {
                linesToDelete.add(i);
            }
            if (line.contains("This item can be reforged!")) {
                linesToDelete.add(i);
            }
            if (line.contains("-----------")) {
                auctionLine = i;
            }
            if (auctionLine != -1) {
                linesToDelete.add(i);
            }
        }

        for (int i = linesToDelete.size() - 1; i >= 0; i--) {
            lore.remove((int) linesToDelete.get(i));
        }

        lore.forEach(sender::sendMessage);

        meta.setLore(lore);
        hand.setItemMeta(meta);
        sender.sendMessage("Item DEPIXEL AUCTION");
        pixelReformat(sender);
    }

    @Subcommand("pixel reformat")
    public void pixelReformat(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }
        if (!meta.hasLore()) return;
        List<String> lore = meta.getLore();
        String lastLine = lore.get(lore.size() - 1);
        String[] splitLastLine = lastLine.split(" ");
        lore.add(0, ChatColor.DARK_GRAY + splitLastLine[1]);
        lore.add(1, "");

        lore.set(lore.size() - 1, splitLastLine[0]);

        meta.setLore(lore);
        hand.setItemMeta(meta);
    }

    @Subcommand("coins set")
    public void coinsSet(CommandSender sender, OfflinePlayer player, double amount) {
        VaultUtils.setCoins(player, amount);
    }

    @Subcommand("coins give")
    public void coinsGive(CommandSender sender, OfflinePlayer player, double amount) {
        VaultUtils.giveCoins(player, amount);
    }

    @Subcommand("coins take")
    public void coinsTake(CommandSender sender, OfflinePlayer player, double amount) {
        VaultUtils.takeCoins(player, amount);
    }

    @Subcommand("coins get")
    public void coinsGet(CommandSender sender, OfflinePlayer player) {
        double coins = VaultUtils.getCoins(player);
        sender.sendMessage(player.getName() + " has " + coins);
    }

    @Subcommand("shop open")
    @AutoComplete("@shopId *")
    public void shopOpen(Player sender, String ID) {
        ShopMenu shopMenu = shopManager.getShop(ID);
        shopMenu.open(sender);
    }

    @Subcommand("playerviewer")
    public void playerViewerOpen(Player sender, @Optional Player arg) {
        if (arg == null) {
            arg = sender;
        }
        new PlayerViewer(arg).open(sender);
    }

    @Subcommand("shop add")
    @AutoComplete("@shopId @itemID @itemID *")
    public void shopAdd(CommandSender sender, String shopId, String resultId, String ingredientId, int amount) {
        shopManager.addItemToShop(shopId, resultId, ingredientId, amount);
        sender.sendMessage("Added item to shop!");
    }

    @Subcommand("shop create")
    public void shopCreate(CommandSender sender, String shopId) {
        if (shopManager.getShop(shopId) != null) {
            sender.sendMessage(MiniMessageUtils.miniMessage("<red>ERROR! SHOP ALREADY EXISTS! <gold>you can edit it with /cc shop editor <shop-name>"));
            return;
        }
        if (shopId.contains(" ")) {
            sender.sendMessage(MiniMessageUtils.miniMessage("<red>ERROR! SHOP ID CANNOT CONTAIN SPACES!"));
            return;
        }
        ShopMenu shop = shopManager.createShop(shopId);
        if (sender instanceof Player player) {
            new ShopEditor(player, shop).open();
        }
        sender.sendMessage(MiniMessageUtils.miniMessage("<green>Created shop!"));
    }

    @Subcommand("shop update")
    @AutoComplete("@shopId * @itemID *")
    public void shopUpdate(CommandSender sender, String shopId, int slotId, String ingredientId, int amount) {
        shopManager.updateShop(shopId, slotId, ingredientId, amount);
        sender.sendMessage(MiniMessageUtils.miniMessage("<green>Updated shop!"));
    }

    @Subcommand({"shop editor", "shop edit"})
    @AutoComplete("@shopId *")
    public void shopEditor(Player sender, String shopId) {
        ShopMenu shopMenu = shopManager.getShop(shopId);
        if (shopMenu == null) {
            sender.sendMessage(MiniMessageUtils.miniMessage("<red>ERROR! SHOP NOT FOUND!"));
            return;
        }
        new ShopEditor(sender, shopMenu).open();
    }

    @Subcommand("shop updateCoins")
    @AutoComplete("@shopId *")
    public void shopUpdateCoins(CommandSender sender, String shopId, int slotId, double coins) {
        shopManager.updateShopCoins(shopId, slotId, coins);
        sender.sendMessage("Updated shop!");
    }

    @Subcommand("shop remove-item")
    @AutoComplete("@shopId *")
    public void shopRemoveItem(CommandSender sender, String shopId, int slotId) {
        shopManager.removeShopItem(shopId, slotId);
        sender.sendMessage(MiniMessageUtils.miniMessage("<green>Removed slot from shop!"));
    }

    @Subcommand("shop remove")
    @AutoComplete("@shopId")
    public void shopRemove(CommandSender sender, String shopId) {
        shopManager.removeShop(shopId);
        sender.sendMessage(MiniMessageUtils.miniMessage("<green>Removed shop successfully!"));
    }

    @Subcommand("mining test")
    public void miningTest(CommandSender sender, double miningSpeed, int blockStrength) {
        long ticksToBreak = MiningManager.getTicksToBreak(miningSpeed, blockStrength);
        sender.sendMessage("Ticks to break: " + ticksToBreak);
    }

    @Subcommand("mining getMat")
    public void miningGetMat(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        String mat = hand.getType().name();
        Component message = MiniMessageUtils.miniMessageString(
                "<hover:show_text:'Click to copy'><click:suggest_command:'<material>'><material></click></hover>",
                Map.of("material", mat)
        );
        sender.sendMessage(message);
    }

    @Subcommand("mining getTargetMat")
    public void miningGetTargetMat(Player sender) {
        Block targetBlock = sender.getTargetBlock(null, 10);
        String mat = targetBlock.getType().name();
        Component message = MiniMessageUtils.miniMessageString(
                "<hover:show_text:'Click to copy'><click:suggest_command:'<material>'><material></click></hover>",
                Map.of("material", mat)
        );
        sender.sendMessage(message);
    }

    @Subcommand("mining setHardness")
    public void miningSetHardness(Player sender, int strength, int power) {
        Block targetBlock = sender.getTargetBlock(null, 10);
        BlockInfo blockInfo = new BlockInfo(strength, power, new ArrayList<>());
        Material type = targetBlock.getType();
        if (type == Material.AIR) return;
        miningManager.setBlockInfo(type.name(), blockInfo);
        sender.sendMessage(type + " set strength to " + strength + " and power to " + power);
    }

    @Subcommand("sound play")
    public void soundPlay(Player sender, Sound sound, @Default("1") float volume, @Default("1") float pitch) {
        sender.playSound(sender, sound, volume, pitch);
    }

    @Subcommand("data load")
    public void dataTest(Player sender) {
        PlayerDataManager dataManager = PlayerDataManager.getInstance();
        PlayerData playerData = dataManager.loadPlayerData(sender.getUniqueId());
        sender.sendMessage(playerData.toString());
    }

    @Subcommand("data save")
    public void dataSave(Player sender) {
        PlayerDataManager dataManager = PlayerDataManager.getInstance();
        dataManager.savePlayerData(sender.getUniqueId());
        sender.sendMessage("Saved Player Data!");
    }

    @Subcommand("data reset")
    public void dataReset(Player sender) {
        PlayerDataManager dataManager = PlayerDataManager.getInstance();
        dataManager.resetPlayerData(sender.getUniqueId());
        sender.sendMessage("Reset Player Data!");
    }

    @Subcommand("kill target")
    public void killTarget(Player sender, @Default("1") int amount) {
        // kill the entity the sender is looking at
        for (int i = 0; i < amount; i++) {
            Entity entity = BukkitUtils.getTargetEntity(sender, 20);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    @Subcommand("test refAbility")
    public void testRefAbility(Player sender) throws NoSuchFieldException, IllegalAccessException {
        BoomAbility boomAbility = new BoomAbility(100, 100);
        Field baseAbilityDamage = boomAbility.getClass().getDeclaredField("cost");
        baseAbilityDamage.setAccessible(true);
        baseAbilityDamage.set(boomAbility, 20.0);
        plugin.getLogger().info("Ability reflected: " + boomAbility);
    }

    @Subcommand("test griffinCrash")
    public void testGriffinCrash(Player sender) {
        Location pos1 = new Location(sender.getWorld(), -88, 88, 148);

        Block block = BukkitUtils.getRandomBlockFilter(pos1, pos1, res -> true);

        sender.teleport(block.getLocation().add(0.5, 2, 0.5));

        sender.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());

        sender.sendMessage("Got: " + block.getLocation() + " With block: " + block.getType());
    }

    @Subcommand("test line")
    public void testLine(Player sender) {
        Location pos1 = sender.getEyeLocation();
        Location pos2 = pos1.clone().add(pos1.getDirection().multiply(50));

        BukkitUtils.runCallbackBetweenTwoPoints(pos1, pos2, 0.5, loc -> {
            sender.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        });
    }

    @Subcommand("test ability")
    public void testAbility(Player sender, String data) throws ParseException {
        ItemAbility ability = abilityManager.getAbilityByID(data);
        if (ability == null) {
            sender.sendMessage("Ability not found!");
            return;
        }
        plugin.getLogger().info("Ability: " + ability);
    }

    @Subcommand("test perks")
    public void testPerks(Player sender) {
        Map<String, Perk> perks = perksManager.getPerks(sender);
        sender.sendMessage("Perks: " + perks);
    }

    @Subcommand("test bossSpawn")
    public void testBossSpawn(Player sender) {
        if (plugin.getMythicBukkit() == null) {
            sender.sendMessage("MythicBukkit not found!");
            return;
        }
        Entity entity = null;
        try {
            entity = MythicMobsHook.getInstance().spawnMythicMob("TestBoss", sender.getLocation());
        } catch (Exception ignored) {
        }
        if (!(entity instanceof LivingEntity livingEntity)) return;
        entityManager.setEntityData(livingEntity.getUniqueId(), new BossEntityData(livingEntity));
    }

    @Subcommand("test bossConfig")
    public void testBossConfig(Player sender) {
        List<BossDrop> drops = List.of(new BossDrop("item", 10, "GOLD_INGOT", 100));
        List<Integer> bonusPoints = List.of(1, 2, 3, 4, 5);
        config.set("bossDrops", new BossDrops(drops, "&4&lUnstable Dragon", null, bonusPoints));
        config.save();
    }

    @Subcommand("test mini-messages")
    public void testMiniMessages(Player sender, @Default("<green>Testing Message") String message) {
        Component component = MiniMessageUtils.miniMessage(message);
        sender.sendMessage(component);
    }

    @Subcommand("test prompt")
    public void testPrompt(Player sender) {
        CompletableFuture<String> future = PromptManager.getInstance().prompt(sender, "Search");
        future.thenAccept(s -> {
            if (s.isEmpty()) {
                sender.sendMessage("You didn't enter anything!");
            } else {
                sender.sendMessage("You entered: " + s);
            }
        });
        // if exception occurs, it will be handled by the exceptionally block
        future.exceptionally(throwable -> {
            sender.sendMessage("An error occurred: " + throwable.getMessage());
            return null;
        });
    }

    @Subcommand("test armorset")
    @AutoComplete("@itemID LEATHER|IRON|GOLD|DIAMOND|NETHERITE|CHAINMAIL *")
    public void testArmorSet(Player sender, String originId, String materialType) {
        ItemInfo itemInfo = itemsManager.getItemByID(originId);
        if (itemInfo == null) {
            sender.sendMessage("ERROR! ITEM DOESN'T EXIST!");
            return;
        }
        XMaterial material = XMaterial.matchXMaterial(materialType + "_HELMET").orElseThrow();
        if (!material.isSupported()) {
            sender.sendMessage("ERROR! MATERIAL TYPE NOT SUPPORTED!");
            return;
        }
        String[] parts = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
        String setId = originId;
        for (String part : parts) {
            setId = setId.replace(part, "");
        }
        ItemStack originBaseItem = itemInfo.getBaseItem();
        for (String part : parts) {
            String id = setId + part;
            ItemInfo clonedInfo = itemInfo.clone();
            XMaterial materialOpt = XMaterial.matchXMaterial(materialType + "_" + part).orElseThrow();
            ItemStack baseItem = new ItemStack(materialOpt.get());
            try {
                baseItem.setItemMeta(originBaseItem.getItemMeta());
            } catch (IllegalArgumentException error) {
                log.warn("Could not set item meta for {}, using default meta.", id);
            }
            clonedInfo.setBaseItem(baseItem);
            String name = StringUtils.setTitleCase(id.replace("_", " "));
            clonedInfo.setName(name);
            itemsManager.setItem(id, clonedInfo);
            ItemStack itemStack = itemsManager.buildItem(id, 1);
            sender.getInventory().addItem(itemStack);
        }
    }

    @Subcommand("test cooldown")
    public void testCooldown(Player sender) {
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sender.sendMessage("Hold an item in your hand!");
            return;
        }
        PacketManager.getInstance().setCooldown(sender, hand.getType(), 100);
    }

    @Subcommand("test blockdata")
    public void testBlockData(Player sender) {
        try {
            BoostedCustomConfig document = new BoostedCustomConfig(
                    new File(plugin.getDataFolder(), "boosted.yml"));
            Block targetBlock = sender.getTargetBlock(null, 10);
            BlockData blockData = targetBlock.getBlockData();
            sender.sendMessage("Block data: " + blockData.getAsString());
            document.set("testBlockData", blockData.getAsString());
            document.save();
            BlockData loadedBlockData = Bukkit.createBlockData(document.getString("testBlockData"));
            sender.sendMessage("Loaded block data: " + loadedBlockData.getAsString());
        } catch (IOException e) {
            log.error("Error handling boosted.yml", e);
        }
    }

    @Subcommand("test createBlockData")
    public void testCreateBlockData(Player sender, String blockDataString) {
        try {
            BlockData blockData = Bukkit.createBlockData(blockDataString);
            sender.sendMessage("Created block data: " + blockData.getAsString());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Error creating block data: " + e.getMessage());
        }
    }

    @Subcommand("mythic skill")
    @AutoComplete("@skillID")
    public void mythicSkill(Player sender, String skill) {
        if (plugin.getMythicBukkit() == null) {
            sender.sendMessage("MythicBukkit not found!");
            return;
        }
        plugin.getMythicBukkit().getAPIHelper().castSkill(sender, skill, sender.getLocation());
    }

    @Subcommand("mythic addSpawner")
    @AutoComplete("@mobID")
    public void mythicAddSpawner(Player sender, String skill) {
        plugin.getMythicBukkit().getAPIHelper().castSkill(sender, skill, sender.getLocation());
    }

    @Subcommand("altar create")
    public void altarCreate(Player sender, String altarName) {
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(sender.getInventory().getItemInMainHand());
        if (itemInfo == null) {
            sender.sendMessage("§cERROR! You must hold the summoning item!");
            return;
        }
        Altar altar = new Altar();
        altar.setItemToSpawn(itemInfo);
        altar.setSpawnLocation(sender.getLocation());
        altarManager.updateAltar(altarName, altar);
        sender.sendMessage(ChatColor.GREEN + "Created Alter named %s".formatted(altarName));
    }

    @Subcommand("altar setSpawnLocation")
    public void altarSetSpawnLocation(Player sender, Altar altar) {
        altar.setSpawnLocation(sender.getLocation());
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set alter spawn for %s".formatted(altar.getId()));
    }

    @Subcommand("altar addSummonBlock")
    public void altarAddSummonBlock(Player sender, Altar altar) {
        Block block = sender.getTargetBlock(null, 10);
        sender.spawnParticle(Particle.FLAME, block.getLocation(), 100, 1, 1, 1, 0);
        altar.getAltarLocations().add(block.getLocation());
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success add summon block for %s".formatted(altar.getId()));
    }

    @Subcommand("altar setMaterial")
    public void altarSetMaterial(Player sender, Altar altar, Material material) {
        altar.setAltarMaterial(material);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set alter material for %s".formatted(altar.getId()));
    }

    @Subcommand("altar setUsedMaterial")
    public void altarSetUsedMaterial(Player sender, Altar altar, Material material) {
        altar.setAlterUsedMaterial(material);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set alter used material for %s".formatted(altar.getId()));
    }

    @Subcommand("altar setSpawnItem")
    @AutoComplete("* @itemID")
    public void altarSetSpawnItem(Player sender, Altar altar, String itemId) {
        ItemInfo itemInfo = itemsManager.getItemByID(itemId);
        altar.setItemToSpawn(itemInfo);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set spawn item for %s".formatted(altar.getId()));
    }

    @Subcommand("altar setPointsPerItem")
    public void altarSetPointsPerItem(Player sender, Altar altar, int points) {
        altar.setPointsPerItem(points);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set points per item for %s".formatted(altar.getId()));
    }

    @Subcommand("altar setAltarRechargeTime")
    public void altarSetAltarRechargeTime(Player sender, Altar altar, int time) {
        altar.setAltarRechargeTime(time);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success set altar recharge time for %s".formatted(altar.getId()));
    }

    @Subcommand("altar addSpawn")
    @AutoComplete("* @mobID *")
    public void altarAddSpawn(Player sender, Altar altar, String mob, double chance) {
        altar.getSpawns().add(new AltarDrop(chance, mob));
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage(ChatColor.GREEN + "Success add spawn for %s to %s with chance %s".formatted(altar.getId(), mob, chance));
    }

    @Subcommand("altar info")
    public void altarInfo(Player sender, Altar altar) {
        // show the info in a pretty way
        sender.sendMessage("Altar Info:");
        sender.sendMessage("ID: " + altar.getId());
        sender.sendMessage("Item to Spawn: " + altar.getItemToSpawn().getName());
        sender.sendMessage("Altar Material: " + altar.getAltarMaterial());
        sender.sendMessage("Used Material: " + altar.getAlterUsedMaterial());
        sender.sendMessage("Spawns: ");
        for (AltarDrop spawn : altar.getSpawns()) {
            sender.sendMessage("  - " + spawn.getValue() + " with chance " + spawn.getChance());
        }

        sender.sendMessage("Locations are shown visually with fake blocks");
        for (Location altarLocation : altar.getAltarLocations()) {
            sender.sendBlockChange(altarLocation, Material.PINK_CONCRETE.createBlockData());
        }
        sender.sendBlockChange(altar.getSpawnLocation(), Material.YELLOW_CONCRETE.createBlockData());
    }

    @Subcommand("altar reset")
    public void altarReset(Player sender, Altar altar) {
        altar.resetAltar();
        sender.sendMessage("Reset Altar!");
    }

    @Subcommand("altar disable")
    public void altarDisable(Player sender, Altar altar) {
        altar.refundAltar();
        altar.disableAltar();
        sender.sendMessage("Disabled Altar!");
    }

    @Subcommand("level send")
    public void sendLevel(Player sender) {
        String playerId = sender.getUniqueId().toString();
        int level = levelconfigManager.getPlayerLevel(playerId);
        if (level > 0) {
            String colorName = levelconfigManager.getLevelColor(level);
            if (colorName != null) {
                try {
                    ChatColor levelColor = ChatColor.valueOf(colorName);
                    String levelMessage = ChatColor.GREEN + "Your current level is " + levelColor + level;
                    sender.sendMessage(levelMessage);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid color name found in configuration.");
                    e.printStackTrace();
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Color name for level " + level + " is null.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Your level is not set.");
        }
    }

    @Subcommand("level set lvl")
    public void levelSetLevel(Player sender, int level) {
        String playerId = sender.getUniqueId().toString();
        String colorName = levelconfigManager.getLevelColor(level);
        if (colorName == null) {
            colorName = ChatColor.GRAY.name();
            levelconfigManager.setLevelColor(level, ChatColor.GRAY);
        }
        ChatColor levelColor = ChatColor.valueOf(colorName);
        levelconfigManager.setPlayerLevel(playerId, level);
        levelconfigManager.setLevelColor(level, levelColor);
        sender.sendMessage(ChatColor.GREEN + "Your level has been set to " + levelColor + level);
    }

    @Subcommand("level set color")
    public void levelSetColor(Player sender, int level, ChatColor color) {
        levelconfigManager.setLevelColor(level, color);
        sender.sendMessage(ChatColor.GREEN + "Level color for level " + level + " has been set to " + color);
    }

    @Subcommand("index mobs")
    public void indexMobs(Player sender, @Default("") String query) {
        IndexMobsCategoryMenu menu = new IndexMobsCategoryMenu(sender, query);
        menu.open();
    }

    @Subcommand("index blocks")
    public void indexBlocks(Player sender, @Default("") String query) {
        IndexBlocksCategoryMenu menu = new IndexBlocksCategoryMenu(sender, query);
        menu.open();
    }

    enum HelpCommandType {
        LINE,
        COMMAND,
        TITLE
    }
}
