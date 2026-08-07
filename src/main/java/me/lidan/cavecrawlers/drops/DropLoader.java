package me.lidan.cavecrawlers.drops;

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
                if (doc.getFile() == null) return;
                for (Object keyObject : doc.getKeys()) {
                    String key = String.valueOf(keyObject);
                    if (key.equals(VERSION_KEY)) continue;
                    String entityName = doc.getString(key + ".entityName");
                    if (entityName == null) continue;
                    var mob = MythicMobsHook.getInstance().getMobByName(entityName);
                    if (mob == null) {
                        log.warn("Could not migrate drop {} in {}: MythicMobs display name '{}' was not found", key, doc.getFile().getName(), entityName);
                        continue;
                    }
                    doc.set(key + ".mobId", mob.getInternalName());
                    doc.remove(key + ".entityName");
                }
            });
        });
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
