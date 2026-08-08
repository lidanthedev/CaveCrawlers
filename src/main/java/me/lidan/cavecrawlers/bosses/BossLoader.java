package me.lidan.cavecrawlers.bosses;

import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.objects.ConfigLoader;

@Slf4j
public class BossLoader extends ConfigLoader<BossDrops> {
    private static BossLoader instance;

    private BossLoader() {
        super(BossDrops.class, "bosses");
    }

    @Override
    public void register(String key, BossDrops value) {
        BossManager.getInstance().registerEntityDrops(value.getMobId(), value);
        setupMigrations(builder ->
                builder.addCustomLogic("1", doc -> {
                    if (doc.getFile() == null) {
                        return;
                    }
                    // will run deserialize and serialize and that will actually do the migration
                    log.info("Migrating Boss to version 1 for {}...", doc.getFile().getName());
                })
        );
    }

    public static BossLoader getInstance() {
        if (instance == null){
            instance = new BossLoader();
        }
        return instance;
    }

    @Override
    public void clear() {
        super.clear();
        BossManager.getInstance().clear();
    }
}
