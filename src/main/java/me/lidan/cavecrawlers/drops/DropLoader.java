package me.lidan.cavecrawlers.drops;

import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
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
            builder.addCustomLogic("2", doc -> {
                if (doc.getFile() == null) {
                    return;
                }
                log.info("Migrating Drops to version 2 for {}...", doc.getFile().getName());
                for (Object key : doc.getKeys()) {
                    log.info("key {} class: {}", key, key.getClass());
                    if (key instanceof String keyString) {
                        String entityName = (String) doc.getSection(keyString).get("entityName");
                        log.info("Entity Name: {}", entityName);
                        if (entityName != null) {
                            MythicMob mythicMob = MythicMobsHook.getInstance().getMobByName(entityName);
                            log.info("Found MythicMob: {}", mythicMob);
                        }
                    }
                }
                throw new RuntimeException("Fail now!");
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
