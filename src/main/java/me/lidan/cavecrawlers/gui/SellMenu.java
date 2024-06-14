package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.ItemInfo;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.StringUtils;
import me.lidan.cavecrawlers.utils.VaultUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellMenu {
    public static final int SELL_BUTTON_SLOT = 31;
    public static final CustomConfig config = new CustomConfig("sell.yml");
    private final ItemsManager itemsManager = ItemsManager.getInstance();
    private final ConfigurationSection prices;
    private Player player;
    private Gui gui;

    public SellMenu(Player player) {
        this.player = player;
        this.gui = Gui.gui()
                .title(Component.text("Sell Menu"))
                .rows(4)
                .create();
        gui.setItem(SELL_BUTTON_SLOT, ItemBuilder.from(Material.EMERALD).setName("§aSell").asGuiItem(event -> {
            event.setCancelled(true);
            sell();
        }));
        gui.setDefaultClickAction(event -> {
            Bukkit.getScheduler().runTaskLater(CaveCrawlers.getInstance(), bukkitTask -> {
                update();
            }, 1L);
        });
        gui.setCloseGuiAction(event -> {
            ItemStack[] storageContents = gui.getInventory().getStorageContents();
            for (int i = 0; i < storageContents.length; i++) {
                if (storageContents[i] != null && i != SELL_BUTTON_SLOT) {
                    itemsManager.giveItemStacks(player, storageContents[i]);
                }
            }
        });
        prices = config.getConfigurationSection("prices");
    }

    public void update(){
        gui.updateItem(SELL_BUTTON_SLOT, ItemBuilder.from(Material.EMERALD).setName("§aSell").setLore(toLore()).build());
    }

    public List<String> toLore(){
        List<String> lore = new ArrayList<>();
        getPrices().forEach((itemInfo, price) -> lore.add(itemInfo.getFormattedName() + " §6" + StringUtils.getNumberFormat(price)));
        return lore;
    }

    private void sell() {
        double total = 0;
        ItemStack[] storageContents = gui.getInventory().getStorageContents();
        for (int i = 0; i < storageContents.length; i++) {
            if (storageContents[i] != null && i != SELL_BUTTON_SLOT) {
                double price = getPrice(storageContents[i]);
                if (price <= 0) {
                    itemsManager.giveItemStacks(player, storageContents[i]);
                }
                else{
                    total += price;
                    VaultUtils.giveCoins(player, price);
                }
            }
        }
        player.sendMessage("§aSold items for §6" + StringUtils.getNumberFormat(total) + "§a coins");
        gui.getInventory().clear();
        gui.close(player);
    }

    public void open() {
        gui.open(player);
    }

    public HashMap<ItemInfo, Double> getPrices(){
        Map<ItemInfo, Integer> items = itemsManager.getAllItems(gui.getInventory());
        HashMap<ItemInfo, Double> prices = new HashMap<>();
        for (ItemInfo itemInfo : items.keySet()) {
            Integer amount = items.getOrDefault(itemInfo, 0);
            double price = getPrice(itemInfo.getID());
            if (price > 0) {
                prices.put(itemInfo, price * amount);
            }
        }
        return prices;
    }

    public double getPrice(ItemStack itemStack){
        String Id = itemsManager.getIDofItemStackSafe(itemStack);
        return getPrice(Id) * itemStack.getAmount();
    }

    public double getPrice(String Id){
        return prices.getDouble(Id, 0);
    }
}
