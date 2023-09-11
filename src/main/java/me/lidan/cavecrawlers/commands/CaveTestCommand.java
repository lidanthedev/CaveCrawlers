package me.lidan.cavecrawlers.commands;

import de.tr7zw.nbtapi.NBTItem;
import dev.triumphteam.gui.components.util.ItemNbt;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemExporter;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsLoader;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.JsonMessage;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.io.File;
import java.util.*;

@Command({"cavetest", "ct"})
@CommandPermission("cavecrawlers.test")
public class CaveTestCommand {

    private CustomConfig config = new CustomConfig("test");
    private StatsManager statsManager = StatsManager.getInstance();
    private final CommandHandler handler;
    private final CaveCrawlers plugin;

    public CaveTestCommand(CommandHandler handler) {
        this.handler = handler;
        this.plugin = CaveCrawlers.getInstance();
        handler.getAutoCompleter().registerSuggestion("itemID", (args, sender, command) -> ItemsManager.getInstance().getKeys());
        handler.getAutoCompleter().registerSuggestion("handID", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            if (player != null){
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
            return Collections.singleton("");
        });
    }

    @Subcommand("reload items")
    public void reloadItems(CommandSender sender){
        ItemsLoader.delete();
        ItemsManager.delete();
        plugin.registerItems();
        sender.sendMessage("reloaded Items!");
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
        String ID = ItemsManager.getInstance().getIDofItemStack(hand);
        sender.sendMessage(Objects.requireNonNullElse(ID, "This is not a Custom Item!"));
    }

    @Subcommand("item update")
    public void itemUpdate(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();;
        ItemStack updateItemStack = ItemsManager.getInstance().updateItemStack(hand);
        sender.getEquipment().setItem(EquipmentSlot.HAND, updateItemStack);
    }

    @Subcommand("item updateInv")
    public void itemUpdateInv(Player sender){
        ItemsManager.getInstance().updatePlayerInventory(sender);
    }

    @Subcommand("item give")
    @AutoComplete("@itemID *")
    public void itemGive(Player sender, String ID){
        ItemStack exampleSword = ItemsManager.getInstance().buildItem(ID, 1);
        sender.getInventory().addItem(exampleSword);
    }

    @Subcommand("item export")
    @AutoComplete("@handID *")
    public void itemExport(Player sender, String ID){
        ItemStack hand = sender.getEquipment().getItemInMainHand();

        ItemsManager itemsManager = ItemsManager.getInstance();
        String IDofItem = itemsManager.getIDofItemStack(hand);
        if (IDofItem != null){
            sender.sendMessage("ERROR! Item already has ID! remove with /ct item remove-id");
            return;
        }

        ItemExporter exporter = new ItemExporter(hand);
        ItemInfo itemInfo = exporter.toItemInfo();
        File file = new File(CaveCrawlers.getInstance().ITEMS_DIR_FILE, ID + ".yml");
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
}
