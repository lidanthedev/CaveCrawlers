package me.lidan.cavecrawlers.commands;

import dev.triumphteam.gui.components.util.ItemNbt;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.core.mobs.MobExecutor;
import io.lumine.mythic.core.skills.SkillExecutor;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.altar.Altar;
import me.lidan.cavecrawlers.altar.AltarDrop;
import me.lidan.cavecrawlers.altar.AltarManager;
import me.lidan.cavecrawlers.drops.DropLoader;
import me.lidan.cavecrawlers.entities.BossEntityData;
import me.lidan.cavecrawlers.entities.EntityManager;
import me.lidan.cavecrawlers.griffin.GriffinDrop;
import me.lidan.cavecrawlers.griffin.GriffinDrops;
import me.lidan.cavecrawlers.griffin.GriffinLoader;
import me.lidan.cavecrawlers.griffin.GriffinManager;
import me.lidan.cavecrawlers.gui.ItemsGui;
import me.lidan.cavecrawlers.gui.PlayerViewer;
import me.lidan.cavecrawlers.items.*;
import me.lidan.cavecrawlers.items.abilities.AbilityManager;
import me.lidan.cavecrawlers.items.abilities.BoomAbility;
import me.lidan.cavecrawlers.items.abilities.ItemAbility;
import me.lidan.cavecrawlers.mining.BlockInfo;
import me.lidan.cavecrawlers.mining.BlockLoader;
import me.lidan.cavecrawlers.mining.MiningManager;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.packets.PacketManager;
import me.lidan.cavecrawlers.perks.Perk;
import me.lidan.cavecrawlers.perks.PerksManager;
import me.lidan.cavecrawlers.shop.ShopItem;
import me.lidan.cavecrawlers.shop.ShopLoader;
import me.lidan.cavecrawlers.shop.ShopManager;
import me.lidan.cavecrawlers.shop.ShopMenu;
import me.lidan.cavecrawlers.stats.StatType;
import me.lidan.cavecrawlers.stats.Stats;
import me.lidan.cavecrawlers.stats.StatsManager;
import me.lidan.cavecrawlers.storage.PlayerData;
import me.lidan.cavecrawlers.storage.PlayerDataManager;
import me.lidan.cavecrawlers.utils.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.lang.reflect.Field;
import java.util.*;

import static org.bukkit.Bukkit.getConsoleSender;

@Command({"cavetest", "ct"})
@CommandPermission("cavecrawlers.test")
public class CaveTestCommand {

    private final ShopManager shopManager = ShopManager.getInstance();
    private final ItemsManager itemsManager = ItemsManager.getInstance();
    private final StatsManager statsManager = StatsManager.getInstance();
    private final MiningManager miningManager = MiningManager.getInstance();
    private final GriffinManager griffinManager = GriffinManager.getInstance();
    private final AbilityManager abilityManager = AbilityManager.getInstance();
    private final PerksManager perksManager = PerksManager.getInstance();
    private final EntityManager entityManager = EntityManager.getInstance();
    private final AltarManager altarManager = AltarManager.getInstance();
    private CustomConfig config = new CustomConfig("test");
    private final CommandHandler handler;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    public CaveTestCommand(CommandHandler handler) {
        this.handler = handler;
        handler.getAutoCompleter().registerSuggestion("itemID", (args, sender, command) -> itemsManager.getKeys());
        handler.getAutoCompleter().registerSuggestion("shopId", (args, sender, command) -> ShopManager.getInstance().getKeys());
        handler.getAutoCompleter().registerSuggestion("handID", (args, sender, command) -> {
            Player player = Bukkit.getPlayer(sender.getName());
            if (player != null){
                return getFillID(player);
            }
            return Collections.singleton("");
        });
        handler.getAutoCompleter().registerSuggestion("abilityID", (args, sender, command) -> abilityManager.getAbilityMap().keySet());
        if (plugin.getMythicBukkit() != null) {
            MobExecutor mobExecutor = plugin.getMythicBukkit().getMobManager();
            handler.getAutoCompleter().registerSuggestion("mobID", (args, sender, command) -> mobExecutor.getMobNames());
            SkillExecutor skillExecutor = plugin.getMythicBukkit().getSkillManager();
            handler.getAutoCompleter().registerSuggestion("skillID", (args, sender, command) -> skillExecutor.getSkillNames());
        }
        handler.getAutoCompleter().registerSuggestion("abilityID", (args, sender, command) -> abilityManager.getAbilityMap().keySet());
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

    @Subcommand("reload plugin")
    public void ReloadPlugin(CommandSender sender){
        Bukkit.dispatchCommand(getConsoleSender(), "plugman reload CaveCrawlers");
        sender.sendMessage(ChatColor.GREEN + "CaveCrawlers reloaded!");
    }


    @Subcommand("config saveStats")
    public void saveStats(Player sender){
        config.set("stat", statsManager.getStats(sender));
        sender.sendMessage("set stat to your stats!");
        config.save();
    }

    @Subcommand("config saveConfMsg")
    public void saveConfigMessage(Player sender){
        config.set("conf-msg", new ConfigMessage("error %player_name%", "Cool %player_name%", Sound.BLOCK_ANVIL_DESTROY));
        sender.sendMessage("Saved Config Message!");
        config.save();
    }

    @Subcommand("config sendConfMsg")
    public void sendConfigMessage(Player sender){
        ConfigMessage message = (ConfigMessage) config.get("conf-msg");
        message.sendMessage(sender);
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
    public void itemGive(CommandSender sender, Player player, @Named("Item ID") String ID, @Default("1") int amount){
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

    @Subcommand("item import")
    @AutoComplete("@handID *")
    public void itemImport(Player sender, String ID){
        ItemStack hand = sender.getEquipment().getItemInMainHand();

        ItemInfo oldInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (oldInfo != null){
            sender.sendMessage("ERROR! Item already has ID! Changing base item instead");
            sender.sendMessage("Don't want that? remove with the ID /ct item remove-id");
            oldInfo.setBaseItem(hand);
            itemsManager.setItem(oldInfo.getID(), oldInfo);
            return;
        }

        if (ID.equals("FILL")){
            ID = getFillID(sender).iterator().next();
            sender.sendMessage("Fill: " + ID);
        }

        ItemInfo itemInfo;
        try {
            ItemExporter exporter = new ItemExporter(hand);
            itemInfo = exporter.toItemInfo();
        }
        catch (Exception error){
            sender.sendMessage("Seems like you didn't use the right format but I'll try to create the item anyways");
            String name = ID.replace("_"," ");
            name = StringUtils.setTitleCase(name);
            Stats stats = new Stats(true);
            itemInfo = new ItemInfo(name, stats, ItemType.MATERIAL, hand, Rarity.COMMON);
        }

        itemsManager.setItem(ID, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        sender.getInventory().addItem(itemStack);

        sender.sendMessage("Exported Item with ID " + ID);
    }

    @Subcommand("item remove-id")
    public void itemRemoveID(Player sender){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemNbt.removeTag(hand, ItemsManager.ITEM_ID);
        sender.sendMessage("Removed ID from Item! it will no longer update or apply stats!");
    }

    @Subcommand("item browse")
    public void itemBrowse(Player sender, @Default("") String query){
        new ItemsGui(sender, query).open();
    }

    // TODO: add these commands
    // item create <id> <material> - create item with Id and material
    //
    //you must hold an item with an already existing id to edit
    //item edit stat <stat> <number> - edit held item's stat
    //item edit ability <ability> - edit held item's  ability
    //item edit name <name> - edit held item's name
    //item edit description <description> - edit held item's description
    //item edit type <type> - edit held item's type
    //item edit rarity <rarity> - edit held item's rarity
    //item edit baseItem <material> - edit held item's base item

    @Subcommand("item create")
    public void itemCreate(Player sender, String ID, Material material){
        if (itemsManager.getItemByID(ID) != null){
            sender.sendMessage("ERROR! ITEM ALREADY EXISTS!");
            return;
        }

        String name = ID.replace("_"," ");
        name = StringUtils.setTitleCase(name);
        Stats stats = new Stats(true);
        ItemInfo itemInfo = new ItemInfo(name, stats, ItemType.MATERIAL, material, Rarity.COMMON);
        itemsManager.setItem(ID, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(itemInfo, 1);
        sender.getInventory().addItem(itemStack);
        sender.sendMessage("Created Item!");
    }

    @Subcommand("item clone")
    @AutoComplete("@itemID *")
    public void itemClone(Player sender, String originId, String Id){
        ItemInfo itemInfo = itemsManager.getItemByID(originId);
        if (itemInfo == null){
            sender.sendMessage("ERROR! ITEM DOESN'T EXIST!");
            return;
        }

        itemsManager.setItem(Id, itemInfo);
        ItemStack itemStack = itemsManager.buildItem(Id, 1);
        sender.getInventory().addItem(itemStack);
        sender.sendMessage("Cloned Item!");
    }

    @Subcommand("item edit stat")
    public void itemEditStat(Player sender, StatType stat, double number){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
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
    public void itemEditAbility(Player sender, String abilityId){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setAbilityID(abilityId);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Ability!");
    }

    @Subcommand("item edit name")
    public void itemEditName(Player sender, String name){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setName(name);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Name!");
    }

    @Subcommand("item edit description")
    public void itemEditDescription(Player sender, String description){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setDescription(description);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Description!");
    }

    @Subcommand("item edit type")
    public void itemEditType(Player sender, ItemType type){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setType(type);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Type!");
    }

    @Subcommand("item edit rarity")
    public void itemEditRarity(Player sender, Rarity rarity){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setRarity(rarity);
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Rarity!");
    }

    @Subcommand("item edit baseItem")
    public void itemEditBaseItem(Player sender, Material material){
        ItemStack hand = sender.getEquipment().getItemInMainHand();
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(hand);
        if (itemInfo == null){
            sender.sendMessage("ERROR! NO ITEM INFO FOUND!");
            return;
        }
        itemInfo.setBaseItem(new ItemStack(material));
        itemsManager.setItem(itemInfo.getID(), itemInfo);
        itemUpdate(sender);
        sender.sendMessage("Updated Base Item!");
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
    @AutoComplete("@shopId *")
    public void shopOpen(Player sender, String ID){
        ShopMenu shopMenu = shopManager.getShop(ID);
        shopMenu.open(sender);
    }

    @Subcommand("playerviewer")
    public void playerViewerOpen(Player sender, @Optional Player arg){
        if(arg == null) {
            arg = sender;
        }
        new PlayerViewer(arg).open(sender);
    }

    @Subcommand("shop add")
    @AutoComplete("@shopId @itemID @itemID *")
    public void shopAdd(CommandSender sender, String shopId, String resultId, String ingredientId, int amount){
        shopManager.addItemToShop(shopId, resultId, ingredientId, amount);
        sender.sendMessage("Added item to shop!");
    }

    @Subcommand("shop create")
    public void shopCreate(CommandSender sender, String shopId){
        shopManager.createShop(shopId);
        sender.sendMessage("Shop Created!");
    }

    @Subcommand("shop update")
    @AutoComplete("@shopId * @itemID *")
    public void shopUpdate(CommandSender sender, String shopId, int slotId, String ingredientId, int amount){
        shopManager.updateShop(shopId, slotId, ingredientId, amount);
        sender.sendMessage("Updated shop!");
    }

    @Subcommand("shop updateCoins")
    @AutoComplete("@shopId *")
    public void shopUpdateCoins(CommandSender sender, String shopId, int slotId, double coins){
        shopManager.updateShopCoins(shopId, slotId, coins);
        sender.sendMessage("Updated shop!");
    }

    @Subcommand("shop remove")
    @AutoComplete("@shopId *")
    public void shopRemove(CommandSender sender, String shopId, int slotId){
        shopManager.removeShop(shopId, slotId);
        sender.sendMessage("Removed slot from shop!");
    }

    @Subcommand("shop delete")
    @AutoComplete("@shopId")
    public void shopDelete(CommandSender sender, String shopId){
        shopManager.deleteShop(shopId);
        sender.sendMessage("Deleted shop!");
    }

    @Subcommand("shop edit")
    @AutoComplete("@shopId *")
    public void shopEdit(Player sender, String shopId, int slotId){
        ShopMenu shopMenu = shopManager.getShop(shopId);
        assert shopMenu != null;
        ShopItem shopItem = shopMenu.getShopItemList().get(slotId);
        shopMenu.shopEditor(sender, shopItem, slotId);
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
            Entity entity = BukkitUtils.getTargetEntity(sender, 20);
            if (entity != null){
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

    @Subcommand("test griffinGrass")
    public void testGriffinGrass(Player sender){
        Block block = griffinManager.generateGriffinLocation(sender);

        sender.teleport(block.getLocation().add(0.5, 2, 0.5));

        sender.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());

        sender.sendMessage("Got: " + block.getLocation() + " With block: " + block.getType());
    }

    @Subcommand("test griffinCrash")
    public void testGriffinCrash(Player sender){
        Location pos1 = new Location(sender.getWorld(), -88,88,148);

        Block block = BukkitUtils.getRandomBlockFilter(pos1,pos1, res -> true);

        sender.teleport(block.getLocation().add(0.5, 2, 0.5));

        sender.sendBlockChange(block.getLocation(), Material.GOLD_BLOCK.createBlockData());

        sender.sendMessage("Got: " + block.getLocation() + " With block: " + block.getType());
    }

    @Subcommand("test line")
    public void testLine(Player sender){
        Location pos1 = sender.getEyeLocation();
        Location pos2 = pos1.clone().add(pos1.getDirection().multiply(50));

        BukkitUtils.getLineBetweenTwoPoints(pos1, pos2, 0.5, loc -> {
            sender.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
        });
    }

    @Subcommand("test ability")
    public void testAbility(Player sender, String data) throws ParseException {
        ItemAbility ability = abilityManager.getAbilityByID(data);
        if (ability == null){
            sender.sendMessage("Ability not found!");
            return;
        }
        plugin.getLogger().info("Ability: " + ability);
    }

    @Subcommand("test griffinConf")
    public void testGriffinConfig(Player sender){
        GriffinDrops drops = new GriffinDrops(List.of(new GriffinDrop("mob", 0.5, "MinosHunter1"), new GriffinDrop("mob", 0.3, "SiameseLynxes1"), new GriffinDrop("coins", 0.2, "1000-5000")));
        CustomConfig customConfig = GriffinLoader.getInstance().getConfig("COMMON");
        customConfig.set("COMMON", drops);
        customConfig.save();
    }

    @Subcommand("test perks")
    public void testPerks(Player sender){
        Map<String, Perk> perks = perksManager.getPerks(sender);
        sender.sendMessage("Perks: " + perks);
    }

    @Subcommand("test bossSpawn")
    public void testBossSpawn(Player sender) throws InvalidMobTypeException {
        Location location = sender.getLocation();
        Entity entity = plugin.getMythicBukkit().getAPIHelper().spawnMythicMob("TestBoss", sender.getLocation());
        if (!(entity instanceof LivingEntity livingEntity)) return;
        entityManager.setEntityData(livingEntity.getUniqueId(), new BossEntityData(livingEntity));
    }

    @Subcommand("mythic skill")
    @AutoComplete("@skillID")
    public void mythicSkill(Player sender, String skill){
        plugin.getMythicBukkit().getAPIHelper().castSkill(sender, skill, sender.getLocation());
    }

    @Subcommand("mythic addSpawner")
    @AutoComplete("@mobID")
    public void mythicAddSpawner(Player sender, String skill){
        plugin.getMythicBukkit().getAPIHelper().castSkill(sender, skill, sender.getLocation());
    }

    @Subcommand("altar create")
    public void altarCreate(Player sender, String altarName){
        ItemInfo itemInfo = itemsManager.getItemFromItemStackSafe(sender.getInventory().getItemInMainHand());
        if (itemInfo == null){
            sender.sendMessage("§cERROR! You must hold the summoning item!");
            return;
        }
        Altar altar = new Altar();
        altar.setItemToSpawn(itemInfo);
        altarManager.updateAltar(altarName, altar);
        sender.sendMessage("Created Altar!");
    }

    @Subcommand("altar setSpawnLocation")
    public void altarSetSpawnLocation(Player sender, Altar altar){
        altar.setSpawnLocation(sender.getLocation());
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage("Set Spawn Location!");
    }

    @Subcommand("altar addSummonBlock")
    public void altarAddSummonBlock(Player sender, Altar altar){
        Block block = sender.getTargetBlock(null, 10);
        sender.spawnParticle(Particle.FLAME, block.getLocation(), 1, 0, 0, 0, 0);
        altar.getAltarLocations().add(block.getLocation());
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage("Added Summon Block!");
    }

    @Subcommand("altar setMaterial")
    public void altarSetMaterial(Player sender, Altar altar, Material material){
        altar.setAltarMaterial(material);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage("Set Material!");
    }

    @Subcommand("altar setUsedMaterial")
    public void altarSetUsedMaterial(Player sender, Altar altar, Material material){
        altar.setAlterUsedMaterial(material);
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage("Set Used Material!");
    }

    @Subcommand("altar addSpawn")
    @AutoComplete("* @mobID *")
    public void altarAddSpawn(Player sender, Altar altar, String mob, double chance){
        altar.getSpawns().add(new AltarDrop(chance, mob));
        altarManager.updateAltar(altar.getId(), altar);
        sender.sendMessage("Added Spawn!");
    }
}
