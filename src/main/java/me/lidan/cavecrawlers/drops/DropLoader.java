package me.lidan.cavecrawlers.drops;

import io.lumine.mythic.api.mobs.MythicMob;
import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
            });
        });
    }

    public static String getOrMigrateMobId(Map<String, Object> map) {
        String mobId = (String) map.get("mobId");
        if (mobId == null && map.containsKey("entityName")) {
            String entityName = (String) map.get("entityName");
            MythicMob mob = MythicMobsHook.getInstance().getMobByName(entityName);
            mobId = mob != null ? mob.getInternalName() : null;
            if (mobId == null) {
                log.warn("Failed to migrate entity {}", entityName);
                throw new IllegalArgumentException("Failed to migrate entity");
            }
            log.info("Migrated entity: {} from {}", mobId, entityName);
        }
        return mobId;
    }

    @Override
    public void register(String key, EntityDrops value) {
        dropsManager.register(value.getMobId(), value);
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
