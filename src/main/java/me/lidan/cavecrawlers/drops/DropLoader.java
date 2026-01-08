package me.lidan.cavecrawlers.drops;

import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropLoader extends ConfigLoader<EntityDrops> {

    private static final Logger log = LoggerFactory.getLogger(DropLoader.class);
    private static DropLoader instance;
    private final DropsManager dropsManager = DropsManager.getInstance();

    private DropLoader() {
        super(EntityDrops.class, "drops");
        setupMigrations(builder -> {
            builder.addCustomLogic("1", doc -> {
                if (doc.getFile() == null) {
                    return;
                }
                log.info("Migrating Drops to version 1 for {}...", doc.getFile().getName());
            });
        });
    }

    @Override
    public void register(String key, EntityDrops value) {
        dropsManager.register(value.getEntityName(), value);
    }

    public static DropLoader getInstance() {
        if (instance == null){
            instance = new DropLoader();
        }
        return instance;
    }

    @Override
    public void clear() {
        super.clear();
        dropsManager.clear();
    }
}
