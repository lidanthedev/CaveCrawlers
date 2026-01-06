package me.lidan.cavecrawlers.mining;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.bukkit.Material;

public class BlockLoader extends ConfigLoader<BlockInfo> {
    private static BlockLoader instance;
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private final MiningManager miningManager = MiningManager.getInstance();

    private BlockLoader() {
        super(BlockInfo.class, "blocks");
        setupMigrations(builder -> {
        });
    }

    @Override
    public void register(String key, BlockInfo value) {
        try {
            miningManager.registerBlock(Material.getMaterial(key), value);
        }
        catch (Exception exception){
            plugin.getLogger().severe("Failed to load block: " + key);
        }
    }

    @Override
    public void clear() {
        super.clear();
        miningManager.clear();
    }

    public static BlockLoader getInstance() {
        if (instance == null){
            instance = new BlockLoader();
        }
        return instance;
    }
}
