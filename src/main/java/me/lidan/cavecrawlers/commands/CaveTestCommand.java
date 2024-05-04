package me.lidan.cavecrawlers.commands;

import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.drops.Drop;
import me.lidan.cavecrawlers.drops.DropLoader;
import me.lidan.cavecrawlers.drops.EntityDrops;
import me.lidan.cavecrawlers.gui.ItemsGui;
import me.lidan.cavecrawlers.items.ItemExporter;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsLoader;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockLoader;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerData;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.JsonMessage;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.File;
import java.util.*;

@Command({"cavetest", "ct"})
@CommandPermission("cavecrawlers.test")
public class CaveTestCommand {

    private final ShopManager shopManager;
    private final ItemsManager itemsManager;
    private final StatsManager statsManager;
    private final MiningManager miningManager;
    private CustomConfig config = new CustomConfig("test");
    private final CommandHandler handler;
    private final CaveCrawlers plugin;

    public CaveTestCommand(CommandHandler handler) {
        this.handler = handler;
        this.plugin = CaveCrawlers.getInstance();
        this.shopManager = ShopManager.getInstance();
        this.itemsManager = ItemsManager.getInstance();
        this.statsManager = StatsManager.getInstance();
        this.miningManager = MiningManager.getInstance();
        handler.getAutoCompleter().registerSuggestion("itemID", (args, sender, command) -> itemsManager.getKeys());
        handler.getAutoCompleter().registerSuggestion("shopID", (args, sender, command) -> ShopManager.getInstance().getKeys());
        handler.getAutoCompleter().registerSuggestion("handID", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            if (player != null){
                return getFillID(player);
            }
            return Collections.singleton("");
        });
    }

    @NotNull
    private static Set<String> getFillID(Player player) {
        ItemStack hand = player.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (!meta.hasDisplayName()){
            return Collections.singleton("");
        }
        String name = meta.getDisplayName();
        name = ChatColor.stripColor(name);
        name = name.toUpperCase(Locale.ROOT);
        name = name.replaceAll(" ", "_");
        return Collections.singleton(name);
    }

    @Subcommand("reload items")
    public void reloadItems(CommandSender sender){
        ItemsLoader loader = ItemsLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Items!");
    }

    @Subcommand("reload shops")
    public void reloadShops(CommandSender sender){
        ShopLoader loader = ShopLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Shops!");
    }

    @Subcommand("reload blocks")
    public void reloadBlocks(CommandSender sender){
        BlockLoader loader = BlockLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Blocks!");
    }

    @Subcommand("reload drops")
    public void reloadDrops(CommandSender sender){
        DropLoader loader = DropLoader.getInstance();
        loader.clear();
        loader.load();
        sender.sendMessage("reloaded Drops!");
    }

    @Subcommand("config saveStats")
    public void saveStats(Player sender){
        config.set("stat", statsManager.getStats(sender));
        sender.sendMessage("set stat to your stats!");
        config.save();
    }

    @Subcommand("config send")
    public void sendConfig(Player sender, String key){
        sender.sendMessage("" + config.get(key));
    }

    @Subcommand("config save")
    public void saveConfig(CommandSender sender){
        config.save();
    }

    @Subcommand("config reload")
    public void reloadConfig(CommandSender sender){
        config.load();
    }

    @Subcommand("item getID")
    public void itemGetID(Player sender){
        ItemStack hand = sender.getInventory().getItemInMainHand();
        String ID = itemsManager.getIDofItemStack(hand);
        sender.sendMessage(Objects.requireNonNullElse(ID, "This is not a Custom Item!"));
    }

    @Subcommand("item update")
    public void itemUpdate(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();;
        ItemStack updateItemStack = itemsManager.updateItemStack(hand);
        sender.getEquipment().setItem(EquipmentSlot.HAND, updateItemStack);
    }

    @Subcommand("item updateInv")
    public void itemUpdateInv(Player sender){
        itemsManager.updatePlayerInventory(sender);
    }

    @Subcommand("item give")
    @AutoComplete("* @itemID *")
    public void itemGive(Player sender, Player player, @Named("Item ID") String ID, @Default("1") int amount){
        ItemStack exampleSword = itemsManager.buildItem(ID, 1);
        for (int i = 0; i < amount; i++) {
            itemsManager.giveItemStacks(player ,exampleSword);
        }
    }

    @Subcommand("item get")
    @AutoComplete("@itemID *")
    public void itemGet(Player sender, @Named("Item ID") String ID, @Default("1") int amount){
        itemGive(sender, sender, ID, amount);
    }

    @Subcommand("item export")
    @AutoComplete("@handID *")
    public void itemExport(Player sender, String ID){
        ItemStack hand = sender.getEquipment().getItemInMainHand();

        String IDofItem = itemsManager.getIDofItemStack(hand);
        if (IDofItem != null){
            sender.sendMessage("ERROR! Item already has ID! remove with /ct item remove-id");
            return;
        }

        if (ID.equals("FILL")){
            ID = getFillID(sender).iterator().next();
            sender.sendMessage("Fill: " + ID);
        }

        ItemExporter exporter = new ItemExporter(hand);
        ItemInfo itemInfo = exporter.toItemInfo();
        File file = new File(ItemsLoader.getInstance().getFileDir(), ID + ".yml");
        CustomConfig customConfig = new CustomConfig(file);
        customConfig.set(ID, itemInfo);
        customConfig.save();

        itemsManager.registerItem(ID, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        sender.getInventory().addItem(itemStack);

        sender.sendMessage("Exported Item with ID " + ID);
    }

    @Subcommand("item remove-id")
    public void itemRemoveID(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemNbt.removeTag(hand, "ITEM_ID");
        sender.sendMessage("Removed ID from Item! it will no longer update or apply stats!");
    }

    @Subcommand("item browse")
    public void itemBrowse(Player sender){
        new ItemsGui(sender).open();
    }

    @Subcommand("lores")
    public void showLore(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null){
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }

        List<String> lore = meta.getLore();
        String name = meta.getDisplayName();

        JsonMessage message = new JsonMessage();
        message.append(name).setClickAsSuggestCmd("/ie rename %s".formatted(name.replaceAll("§", "&"))).save().send(sender);
        if (!meta.hasLore()) return;
        for (int i = 0; i < lore.size(); i++) {
            message = new JsonMessage();
            String line = lore.get(i);
            message.append(line).setClickAsSuggestCmd("/ie lore set %s %s".formatted(i+1 ,line.replaceAll("§", "&"))).save().send(sender);
        }
    }

    @Command("lores")
    public void loresCommand(Player sender){
        showLore(sender);
    }

    @Subcommand("packet test")
    public void packetTest(Player player, int stage){
        PacketManager packetManager = PacketManager.getInstance();
        packetManager.setBlockDestroyStage(player, player.getTargetBlock(null, 10).getLocation(), stage);
    }

    @Subcommand("nbt set")
    public void nbtSet(Player sender, String key, String value){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemNbt.setString(hand, key, value);
        sender.sendMessage("set NBT!");
    }

    @Subcommand("nbt get")
    public void nbtGet(Player sender, String key){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        String value = ItemNbt.getString(hand, key);
        sender.sendMessage("value: " + value);
    }

    @Subcommand("nbt send")
    public void nbtSend(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null){
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
    public void pixelAuction(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null){
            sender.sendMessage("ERROR! NO META FOUND!");
            return;
        }
        if (!meta.hasLore()) return;
        List<String> lore = meta.getLore();
        List<Integer> linesToDelete = new ArrayList<>();
        int auctionLine = -1;
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains(ChatColor.DARK_GRAY + "[")){
                linesToDelete.add(i);
            }
            if (line.contains("This item can be reforged!")){
                linesToDelete.add(i);
            }
            if (line.contains("-----------")){
                auctionLine = i;
            }
            if (auctionLine != -1){
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
    public void pixelReformat(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null){
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
    public void coinsSet(CommandSender sender, OfflinePlayer player, double amount){
        VaultUtils.setCoins(player, amount);
    }

    @Subcommand("coins give")
    public void coinsGive(CommandSender sender, OfflinePlayer player, double amount){
        VaultUtils.giveCoins(player, amount);
    }

    @Subcommand("coins take")
    public void coinsTake(CommandSender sender, OfflinePlayer player, double amount){
        VaultUtils.takeCoins(player, amount);
    }

    @Subcommand("coins get")
    public void coinsGet(CommandSender sender, OfflinePlayer player){
        double coins = VaultUtils.getCoins(player);
        sender.sendMessage(player.getName() + " has " + coins);
    }

    @Subcommand("shop open")
    @AutoComplete("@shopID *")
    public void shopOpen(Player sender, String ID){
        ShopMenu shopMenu = shopManager.getShop(ID);
        shopMenu.open(sender);
    }

    @Subcommand("shop add")
    @AutoComplete("@shopID @itemID @itemID *")
    public void shopAdd(CommandSender sender, String shopID, String resultID, String ingredientID, int amount){
        shopManager.addItemToShop(shopID, resultID, ingredientID, amount);
        sender.sendMessage("Added item to shop!");
    }

    @Subcommand("shop create")
    public void shopCreate(CommandSender sender, String shopID){
        shopManager.createShop(shopID);
        sender.sendMessage("Shop Created!");
    }

    @Subcommand("mining test")
    public void miningTest(CommandSender sender, double miningSpeed, int blockStrength){
        long ticksToBreak = MiningManager.getTicksToBreak(miningSpeed, blockStrength);
        sender.sendMessage("Ticks to break: " + ticksToBreak);
    }

    @Subcommand("mining getMat")
    public void miningGetMat(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        String mat = hand.getType().name();
        new JsonMessage().append(mat).setClickAsSuggestCmd(mat).save().send(sender);
    }

    @Subcommand("mining getTargetMat")
    public void miningGetTargetMat(Player sender){
        Block targetBlock = sender.getTargetBlock(null, 10);
        String mat = targetBlock.getType().name();
        new JsonMessage().append(mat).setClickAsSuggestCmd(mat).save().send(sender);
    }

    @Subcommand("mining setHardness")
    public void miningSetHardness(Player sender, int strength, int power){
        Block targetBlock = sender.getTargetBlock(null, 10);
        BlockInfo blockInfo = new BlockInfo(strength, power, new HashMap<>());
        Material type = targetBlock.getType();
        if (type == Material.AIR) return;
        miningManager.setBlockInfo(type.name(), blockInfo);
        sender.sendMessage(type + " set strength to " + strength + " and power to " + power);
    }

    @Subcommand("sound play")
    public void soundPlay(Player sender, Sound sound, @Default("1") float volume, @Default("1") float pitch){
        sender.playSound(sender, sound, volume, pitch);
    }

    @Subcommand("drop test")
    public void dropTest(Player sender){
        config.set("testDrop", new EntityDrops("&aSlime", List.of(new Drop("SLIMEBALL", 1, 100, true)), 1));
        config.save();
    }

    @Subcommand("data load")
    public void dataTest(Player sender){
        PlayerDataManager dataManager = PlayerDataManager.getInstance();
        PlayerData playerData = dataManager.loadPlayerData(sender.getUniqueId());
        sender.sendMessage(playerData.toString());
    }

    @Subcommand("data save")
    public void dataSave(Player sender){
        PlayerDataManager dataManager = PlayerDataManager.getInstance();
        dataManager.savePlayerData(sender.getUniqueId());
        sender.sendMessage("Saved Player Data!");
    }

    @Subcommand("kill target")
    public void killTarget(Player sender, @Default("1") int amount){
        // kill the entity the sender is looking at
        for (int i = 0; i < amount; i++) {
            Entity entity = getTargetEntity(sender, 20);
            if (entity != null){
                entity.remove();
            }
        }
    }

    private static Entity getTargetEntity(LivingEntity sender, int range) {
        Location location = sender.getEyeLocation();
        Vector vector = location.getDirection();
        World world = location.getWorld();
        for (int i = 0; i < range; i++) {
            Vector newVector = vector.clone().multiply(i);
            Location newLocation = location.clone().add(newVector);
            for (Entity entity : world.getNearbyEntities(newLocation, 0.5, 1, 0.5)) {
                if (entity != sender){
                    return entity;
                }
            }
        }
        return null;
    }
}
