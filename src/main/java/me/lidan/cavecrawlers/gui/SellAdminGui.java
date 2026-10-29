package me.lidan.cavecrawlers.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.lidan.cavecrawlers.items.ItemsManager;
import me.lidan.cavecrawlers.prompt.PromptManager;
import me.lidan.cavecrawlers.utils.CustomConfig;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SellAdminGui {
    public static final CustomConfig config = SellMenu.config;
    private static final ItemsManager itemsManager = ItemsManager.getInstance();
    private final Player player;
    private final PaginatedGui gui;
    private final ConfigurationSection prices = config.getConfigurationSection("prices");

    public SellAdminGui(Player player) {
        this.player = player;
        this.gui = Gui.paginated()
                .title(MiniMessageUtils.miniMessage("Sell Admin"))
                .rows(6)
                .pageSize(45) // Set the size you want, or leave it to be automatic.
                .create();

        gui.disableAllInteractions();
        // filler
        gui.getFiller().fillBottom(GuiItems.GLASS_ITEM);
        // Previous item
        gui.setItem(6, 3, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Previous").asGuiItem(event -> gui.previous()));
        // Next item
        gui.setItem(6, 7, ItemBuilder.from(Material.ARROW).setName(ChatColor.BLUE + "Next").asGuiItem(event -> gui.next()));

        Map<String, Object> pricesMap = prices.getValues(false);

        List<AbstractMap.SimpleEntry<String, Double>> entries = pricesMap.entrySet().stream().map(
                        entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), Double.parseDouble(entry.getValue().toString())))
                .sorted(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                .toList();

        for (AbstractMap.SimpleEntry<String, Double> entry : entries) {
            String itemId = entry.getKey();
            ItemStack itemStack = itemsManager.buildItem(itemId, 1);
            gui.addItem(ItemBuilder.from(itemStack).addLore(ChatColor.GOLD + "Price: " + StringUtils.getNumberFormat(entry.getValue())).asGuiItem(
                    event -> PromptManager.getInstance().promptNumberMin(player, "Enter new price", 0).thenAccept(price -> {
                        config.set("prices." + itemId, price);
                        config.save();
                        player.sendMessage(MiniMessageUtils.miniMessage("<green>Price of <item> updated to <price>!", Map.of("item", itemId, "price", price)));
                    }).exceptionally(throwable -> {
                        Throwable root = throwable.getCause() != null ? throwable.getCause() : throwable;
                        player.sendMessage(MiniMessageUtils.miniMessage("<red>Error! <message>", Map.of("message", root.getMessage())));
                        return null;
                    }).whenComplete((unused, throwable) -> reopen())
            ));
        }

    }

    public void open() {
        gui.open(player);
    }

    public void reopen() {
        new SellAdminGui(player).open();
    }
}
