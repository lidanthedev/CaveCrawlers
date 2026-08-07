package me.lidan.cavecrawlers.bosses;

import me.lidan.cavecrawlers.integration.mythic.MythicMobsHook;
import me.lidan.cavecrawlers.objects.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BossLoader extends ConfigLoader<BossDrops> {
    private static BossLoader instance;
    private static final Logger log = LoggerFactory.getLogger(BossLoader.class);

    private BossLoader() {
        super(BossDrops.class, "bosses");
        setupMigrations(builder -> builder.addCustomLogic("2", doc -> {
            if (doc.getFile() == null) return;
            for (Object keyObject : doc.getKeys()) {
                String key = String.valueOf(keyObject);
                if (key.equals(VERSION_KEY)) continue;
                String entityName = doc.getString(key + ".entityName");
                if (entityName == null) continue;
                var mob = MythicMobsHook.getInstance().getMobByName(entityName);
                if (mob == null) {
                    log.warn("Could not migrate boss {} in {}: MythicMobs display name '{}' was not found", key, doc.getFile().getName(), entityName);
                    continue;
                }
                doc.set(key + ".mobId", mob.getInternalName());
                doc.remove(key + ".entityName");
            }
        }));
    }

    @Override
    public void register(String key, BossDrops value) {
        BossManager.getInstance().registerEntityDrops(value.getMobId(), value);
    }

    public static BossLoader getInstance() {
        if (instance == null){
            instance = new BossLoader();
        }
        return instance;
    }
}
