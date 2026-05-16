package me.lidan.cavecrawlers.storage;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.storage.db.PlayerSessionsDao;
import me.lidan.cavecrawlers.storage.db.SkillRow;
import me.lidan.cavecrawlers.storage.db.SkillsDao;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerSkillsManager {
    private static PlayerSkillsManager instance;

    private static final boolean VERBOSE = false;
    private static final long LOCK_TIMEOUT_MS = 60_000L;
    private static final int LOCK_MAX_ATTEMPTS = 20;
    private static final long LOCK_RETRY_MS = 500L;

    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledLoads = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingLoads = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, List<SkillRow>> pendingSaves = new ConcurrentHashMap<>();

    @Setter
    private String serverId;

    private final CaveCrawlers plugin = CaveCrawlers.getInstance();

    private PlayerSkillsManager() {
    }

    public static PlayerSkillsManager getInstance() {
        if (instance == null) {
            instance = new PlayerSkillsManager();
        }
        return instance;
    }

    private void verbose(String msg, Object... args) {
        if (VERBOSE) {
            log.info(msg, args);
        }
    }

    private void fireLoadEvent(UUID uuid, Skills skills) {
        Bukkit.getPluginManager().callEvent(new PlayerSkillsLoadEvent(uuid, skills));
    }

    private void fireSaveEvent(UUID uuid, Skills skills) {
        Bukkit.getPluginManager().callEvent(new PlayerSkillsSaveEvent(uuid, skills));
    }

    public void loadPlayerAsync(UUID uuid) {
        scheduleLoadIfNeeded(uuid);
    }

    public Skills loadPlayerSync(UUID uuid) {
        scheduleLoadIfNeeded(uuid);
        return getOrCreateSkills(uuid);
    }

    public void scheduleLoadsForOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> scheduleLoadIfNeeded(player.getUniqueId()));
    }

    public void scheduleLoadsForPendingPlayers() {
        for (UUID uuid : new ArrayList<>(pendingLoads)) {
            scheduleLoadIfNeeded(uuid);
        }
    }

    public void scheduleLoadIfNeeded(UUID uuid) {
        getOrCreateSkills(uuid);

        if (loadedPlayers.contains(uuid)) {
            return;
        }

        if (!isPersistenceAvailable()) {
            pendingLoads.add(uuid);
            return;
        }

        pendingLoads.remove(uuid);
        if (!scheduledLoads.add(uuid)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerFromDatabase(uuid));
    }

    private void loadPlayerFromDatabase(UUID uuid) {
        if (!isPersistenceAvailable()) {
            pendingLoads.add(uuid);
            scheduledLoads.remove(uuid);
            return;
        }

        try {
            List<SkillRow> rows;
            List<SkillRow> snapshot = pendingSaves.get(uuid);
            if (snapshot != null) {
                acquireLock(uuid);
                rows = snapshot;
            } else {
                acquireLock(uuid);
                rows = loadRowsFromDb(uuid);
            }
            verbose("[LOAD] {} — loaded {} skill row(s) from DB", uuid, rows.size());

            Skills skills = buildSkillsFromRows(uuid, rows);
            activeSkills.put(uuid, skills);
            loadedPlayers.add(uuid);
            pendingLoads.remove(uuid);
            fireLoadEvent(uuid, skills);

            if (Bukkit.getPlayer(uuid) == null) {
                verbose("[LOAD] {} — player offline by the time load finished, saving and releasing lock", uuid);
                savePlayerNow(uuid);
            }
        } catch (Exception e) {
            log.warn("[LOAD] {} — failed to load player data: {}", uuid, e.getMessage(), e);
            pendingLoads.add(uuid);
        } finally {
            scheduledLoads.remove(uuid);
        }
    }

    private boolean isPersistenceAvailable() {
        Database database = Database.getInstance();
        return database.isAvailable() && database.getJdbi() != null;
    }

    private void acquireLock(UUID uuid) {
        if (!isPersistenceAvailable()) {
            return;
        }

        String uuidStr = uuid.toString();
        Database.getInstance().getJdbi().useHandle(h -> h.attach(PlayerSessionsDao.class).ensureRow(uuidStr));

        if (tryAcquireLockOnce(uuid, uuidStr)) {
            return;
        }

        for (int attempt = 1; attempt <= LOCK_MAX_ATTEMPTS; attempt++) {
            verbose("[LOCK] {} — waiting for lock (attempt {}/{})", uuid, attempt, LOCK_MAX_ATTEMPTS);
            try {
                Thread.sleep(LOCK_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (tryAcquireLockOnce(uuid, uuidStr)) {
                return;
            }
        }
        log.warn("[LOCK] {} — could not acquire lock after {} attempts, proceeding without it", uuid, LOCK_MAX_ATTEMPTS);
    }

    private boolean tryAcquireLockOnce(UUID uuid, String uuidStr) {
        if (!isPersistenceAvailable()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long expiry = now - LOCK_TIMEOUT_MS;
        int affected = Database.getInstance().getJdbi().withHandle(h ->
                h.attach(PlayerSessionsDao.class).tryAcquireLock(uuidStr, serverId, now, expiry)
        );
        if (affected > 0) {
            verbose("[LOCK] {} — acquired [thread={}]", uuid, Thread.currentThread().getName());
            return true;
        }
        return false;
    }

    public void savePlayerNow(UUID uuid) {
        if (!isPersistenceAvailable() || !loadedPlayers.contains(uuid)) {
            activeSkills.remove(uuid);
            pendingSaves.remove(uuid);
            pendingLoads.remove(uuid);
            scheduledLoads.remove(uuid);
            verbose("[SAVE-NOW] {} — not loaded or persistence unavailable, skipping [thread={}]",
                    uuid, Thread.currentThread().getName());
            return;
        }

        Skills skills = activeSkills.remove(uuid);
        pendingSaves.remove(uuid);
        if (skills == null) {
            verbose("[SAVE-NOW] {} — nothing in cache, releasing lock and skipping [thread={}]",
                    uuid, Thread.currentThread().getName());
            releaseLock(uuid);
            return;
        }

        List<SkillRow> rows = buildRows(uuid, skills);
        verbose("[SAVE-NOW] {} — writing {} row(s) + releasing lock [thread={}]",
                uuid, rows.size(), Thread.currentThread().getName());
        fireSaveEvent(uuid, skills);

        String uuidStr = uuid.toString();
        Database.getInstance().getJdbi().useTransaction(h -> {
            if (!rows.isEmpty()) {
                h.attach(SkillsDao.class).upsertSkills(rows);
            }
            h.attach(PlayerSessionsDao.class).releaseLock(uuidStr, serverId);
        });

        if (Bukkit.getPlayer(uuid) == null) {
            loadedPlayers.remove(uuid);
        }

        verbose("[SAVE-NOW] {} — done", uuid);
    }

    public void savePlayerAsync(UUID uuid) {
        if (!isPersistenceAvailable() || !loadedPlayers.contains(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerNow(uuid));
    }

    public void saveAll() {
        if (!isPersistenceAvailable()) {
            verbose("[SAVE-ALL] persistence unavailable, skipping [thread={}]", Thread.currentThread().getName());
            return;
        }

        verbose("[SAVE-ALL] Saving {} active player(s), {} pending [thread={}]",
                activeSkills.size(), pendingSaves.size(), Thread.currentThread().getName());
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
            if (!loadedPlayers.contains(entry.getKey())) {
                continue;
            }
            List<SkillRow> rows = buildRows(entry.getKey(), entry.getValue());
            if (!rows.isEmpty()) {
                verbose("[SAVE-ALL] {} — writing {} row(s)", entry.getKey(), rows.size());
                fireSaveEvent(entry.getKey(), entry.getValue());
                writeRows(rows);
            }
        }
        for (Map.Entry<UUID, List<SkillRow>> entry : pendingSaves.entrySet()) {
            verbose("[SAVE-ALL] {} — flushing pending save ({} row(s))", entry.getKey(), entry.getValue().size());
            writeRows(entry.getValue());
        }
        pendingSaves.clear();

        if (serverId != null) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).heartbeatAll(serverId, System.currentTimeMillis())
            );
        }
    }

    public void shutdown() {
        saveAll();
        if (serverId != null && isPersistenceAvailable()) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).releaseAllLocks(serverId)
            );
        }
    }

    public Skills getSkills(UUID uuid) {
        Skills cached = getOrCreateSkills(uuid);
        if (!loadedPlayers.contains(uuid)) {
            scheduleLoadIfNeeded(uuid);
        }
        return cached;
    }

    public Skills getSkills(Player player) {
        return getSkills(player.getUniqueId());
    }

    public void putSkills(UUID uuid, Skills skills) {
        verbose("[PUT] {} — inserting into cache directly [thread={}]", uuid, Thread.currentThread().getName());
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        loadedPlayers.add(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
    }

    public void resetPlayerData(UUID uuid) {
        verbose("[RESET] {} — clearing cache and deleting DB rows async", uuid);
        Skills skills = new Skills();
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        loadedPlayers.add(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);

        if (isPersistenceAvailable()) {
            String uuidStr = uuid.toString();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    Database.getInstance().getJdbi().useHandle(h -> h.attach(SkillsDao.class).deleteSkills(uuidStr))
            );
        }
    }

    public void removeFromCache(UUID uuid) {
        verbose("[EVICT] {} — removed from cache", uuid);
        activeSkills.remove(uuid);
        loadedPlayers.remove(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
        pendingSaves.remove(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }

    private Skills getOrCreateSkills(UUID uuid) {
        Skills cached = activeSkills.get(uuid);
        if (cached != null) {
            cached.setUuid(uuid);
            return cached;
        }

        Skills skills = new Skills();
        skills.setUuid(uuid);
        Skills existing = activeSkills.putIfAbsent(uuid, skills);
        return existing != null ? existing : skills;
    }

    private void releaseLock(UUID uuid) {
        if (!isPersistenceAvailable() || serverId == null) {
            return;
        }
        Database.getInstance().getJdbi().useHandle(h ->
                h.attach(PlayerSessionsDao.class).releaseLock(uuid.toString(), serverId)
        );
    }

    private List<SkillRow> loadRowsFromDb(UUID uuid) {
        return Database.getInstance().getJdbi().withHandle(handle ->
                handle.attach(SkillsDao.class).getSkills(uuid.toString())
        );
    }

    private Skills buildSkillsFromRows(UUID uuid, List<SkillRow> rows) {
        List<Skill> skillList = new ArrayList<>();
        for (SkillRow row : rows) {
            SkillInfo skillInfo = SkillsManager.getInstance().getSkillInfo(row.getType());
            if (skillInfo == null) {
                log.warn("[LOAD] {} — skipping unknown skill type '{}'", uuid, row.getType());
                continue;
            }
            Skill skill = new Skill(skillInfo, 0);
            skill.addXp(row.getTotalXp());
            skill.levelUp(false);
            skillList.add(skill);
        }

        Skills skills = new Skills(skillList);
        skills.setUuid(uuid);
        return skills;
    }

    private List<SkillRow> buildRows(UUID uuid, Skills skills) {
        List<SkillRow> rows = new ArrayList<>();
        for (Skill skill : skills) {
            rows.add(new SkillRow(
                    uuid.toString(),
                    skill.getType().getId(),
                    skill.getXp(),
                    skill.getLevel(),
                    skill.getTotalXp()
            ));
        }
        return rows;
    }

    private void writeRows(List<SkillRow> rows) {
        if (!isPersistenceAvailable()) {
            return;
        }
        Database.getInstance().getJdbi().useHandle(handle ->
                handle.attach(SkillsDao.class).upsertSkills(rows)
        );
    }
}
