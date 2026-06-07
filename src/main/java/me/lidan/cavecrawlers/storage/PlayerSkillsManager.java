package me.lidan.cavecrawlers.storage;

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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerSkillsManager {
    private static final boolean VERBOSE = false;
    private static final long LOCK_TIMEOUT_MS = 60_000L;
    private static final int LOCK_MAX_ATTEMPTS = 20;
    private static final long LOCK_RETRY_MS = 500L;
    private static PlayerSkillsManager instance;
    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledLoads = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingLoads = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, List<SkillRow>> pendingSaves = new ConcurrentHashMap<>();
    private final Set<UUID> preLoadDirty = ConcurrentHashMap.newKeySet();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    private volatile String serverId;

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

    private void loadPlayerData(UUID uuid) {
        Database.getInstance().loadPlayerDataTables(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerDataLoadEvent(uuid));
    }

    private void savePlayerData(UUID uuid) {
        Database.getInstance().savePlayerDataTables(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(uuid));
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
            if (!acquireLock(uuid)) {
                pendingLoads.add(uuid);
                return;
            }

            List<SkillRow> rows = loadRowsFromDb(uuid);
            List<SkillRow> snapshot = pendingSaves.get(uuid);
            if (snapshot != null && preLoadDirty.contains(uuid)) {
                rows = pendingSaves.remove(uuid);
                preLoadDirty.remove(uuid);
            } else {
                pendingSaves.remove(uuid);
            }
            verbose("[LOAD] {} — loaded {} skill row(s) from DB", uuid, rows.size());

            Skills skills = buildSkillsFromRows(uuid, rows);
            activeSkills.put(uuid, skills);
            loadedPlayers.add(uuid);
            pendingLoads.remove(uuid);
            loadPlayerData(uuid);

            if (Bukkit.getPlayer(uuid) == null) {
                verbose("[LOAD] {} — player offline by the time load finished, saving and releasing lock", uuid);
                savePlayerAsync(uuid, true);
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

    private boolean acquireLock(UUID uuid) {
        if (!isPersistenceAvailable()) {
            return false;
        }

        String uuidStr = uuid.toString();
        Database.getInstance().getJdbi().useHandle(h -> h.attach(PlayerSessionsDao.class).ensureRow(uuidStr));

        if (tryAcquireLockOnce(uuid, uuidStr)) {
            return true;
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
                return true;
            }
        }
        log.warn("[LOCK] {} — could not acquire lock after {} attempts, deferring load", uuid, LOCK_MAX_ATTEMPTS);
        return false;
    }

    private boolean tryAcquireLockOnce(UUID uuid, String uuidStr) {
        String currentServerId = serverId;
        if (!isPersistenceAvailable() || currentServerId == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long expiry = now - LOCK_TIMEOUT_MS;
        int affected = Database.getInstance().getJdbi().withHandle(h ->
                h.attach(PlayerSessionsDao.class).tryAcquireLock(uuidStr, currentServerId, now, expiry)
        );
        if (affected > 0) {
            verbose("[LOCK] {} — acquired [thread={}]", uuid, Thread.currentThread().getName());
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    /**
     * Saves synchronously on the calling thread AND releases the session lock atomically.
     * Use on player quit: guarantees the DB row is current and the lock is free before
     * BungeeCord/Velocity can route the player to another backend and trigger a load there.
     */
    public void savePlayerNow(UUID uuid) {
        savePlayerNow(uuid, false);
    }

    public void savePlayerNow(UUID uuid, boolean releaseLockAfterSave) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> savePlayerNow(uuid, releaseLockAfterSave));
            return;
        }

        SaveRequest request = createSaveRequest(uuid, releaseLockAfterSave);
        if (request == null) {
            return;
        }
        savePlayerNow(request);
    }

    private void savePlayerNow(SaveRequest request) {
        UUID uuid = request.uuid();
        Skills skills = request.snapshotSkills();
        if (skills == null) {
            verbose("[SAVE-NOW] {} — nothing in cache, releasing lock and skipping [thread={}]",
                    uuid, Thread.currentThread().getName());
            loadedPlayers.remove(uuid);
            preLoadDirty.remove(uuid);
            if (request.releaseLockAfterSave()) {
                releaseLock(uuid);
            }
            return;
        }

        List<SkillRow> rows = buildRows(uuid, skills);
        verbose("[SAVE-NOW] {} — writing {} row(s) + releasing lock [thread={}]",
                uuid, rows.size(), Thread.currentThread().getName());
        savePlayerData(uuid);

        String uuidStr = uuid.toString();
        String currentServerId = serverId;
        Database.getInstance().getJdbi().useTransaction(h -> {
            if (!rows.isEmpty()) {
                h.attach(SkillsDao.class).upsertSkills(rows);
            }
            if (request.releaseLockAfterSave() && currentServerId != null) {
                h.attach(PlayerSessionsDao.class).releaseLock(uuidStr, currentServerId);
            }
        });

        if (!request.onlineAtStart() && Bukkit.getPlayer(uuid) != null) {
            activeSkills.put(uuid, request.liveSkills());
            verbose("[SAVE-NOW] {} — player reconnected during save, restored cached skills", uuid);
        } else if (!request.onlineAtStart()) {
            loadedPlayers.remove(uuid);
        }
        preLoadDirty.remove(uuid);

        verbose("[SAVE-NOW] {} — done", uuid);
    }

    private SaveRequest createSaveRequest(UUID uuid, boolean releaseLockAfterSave) {
        if (!loadedPlayers.contains(uuid)) {
            if (activeSkills.containsKey(uuid) && preLoadDirty.contains(uuid)) {
                queuePendingSave(uuid);
                verbose("[SAVE-NOW] {} — not loaded yet, keeping placeholder state for retry [thread={}]",
                        uuid, Thread.currentThread().getName());
                return null;
            }
            activeSkills.remove(uuid);
            pendingSaves.remove(uuid);
            preLoadDirty.remove(uuid);
            pendingLoads.remove(uuid);
            scheduledLoads.remove(uuid);
            verbose("[SAVE-NOW] {} — not loaded, skipping [thread={}]",
                    uuid, Thread.currentThread().getName());
            return null;
        }

        if (!isPersistenceAvailable()) {
            queuePendingSave(uuid);
            return null;
        }

        boolean onlineAtStart = Bukkit.getPlayer(uuid) != null;
        Skills skills = onlineAtStart ? activeSkills.get(uuid) : activeSkills.remove(uuid);
        pendingSaves.remove(uuid);
        Skills snapshotSkills = skills == null ? null : copySkills(skills);
        return new SaveRequest(uuid, skills, snapshotSkills, onlineAtStart, releaseLockAfterSave);
    }

    public void savePlayerAsync(UUID uuid) {
        savePlayerAsync(uuid, false);
    }

    public void savePlayerAsync(UUID uuid, boolean releaseLockAfterSave) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> savePlayerAsync(uuid, releaseLockAfterSave));
            return;
        }

        if (!loadedPlayers.contains(uuid)) {
            if (activeSkills.containsKey(uuid)) {
                queuePendingSave(uuid);
            }
            return;
        }
        if (!isPersistenceAvailable()) {
            queuePendingSave(uuid);
            return;
        }
        SaveRequest request = createSaveRequest(uuid, releaseLockAfterSave);
        if (request == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerNow(request));
    }

    public void flushPendingSavesAsync() {
        if (!isPersistenceAvailable() || pendingSaves.isEmpty()) {
            return;
        }
        for (UUID uuid : new ArrayList<>(pendingSaves.keySet())) {
            if (!loadedPlayers.contains(uuid)) {
                scheduleLoadIfNeeded(uuid);
                continue;
            }
            savePlayerAsync(uuid, false);
        }
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
            savePlayerData(entry.getKey());
            List<SkillRow> rows = buildRows(entry.getKey(), entry.getValue());
            if (!rows.isEmpty()) {
                verbose("[SAVE-ALL] {} — writing {} row(s)", entry.getKey(), rows.size());
                writeRows(rows);
            }
        }
        for (Map.Entry<UUID, List<SkillRow>> entry : pendingSaves.entrySet()) {
            verbose("[SAVE-ALL] {} — flushing pending save ({} row(s))", entry.getKey(), entry.getValue().size());
            writeRows(entry.getValue());
            preLoadDirty.remove(entry.getKey());
        }
        pendingSaves.clear();

        String currentServerId = serverId;
        if (currentServerId != null) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).heartbeatAll(currentServerId, System.currentTimeMillis())
            );
        }
    }

    public void saveAllAsync() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::saveAllAsync);
            return;
        }
        if (!isPersistenceAvailable()) {
            verbose("[SAVE-ALL] persistence unavailable, skipping [thread={}]", Thread.currentThread().getName());
            return;
        }

        List<SaveAllSnapshot> activeSnapshots = new ArrayList<>();
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
            if (!loadedPlayers.contains(entry.getKey())) {
                continue;
            }
            activeSnapshots.add(new SaveAllSnapshot(entry.getKey(), copySkills(entry.getValue())));
        }

        List<PendingSaveBatch> pendingSnapshots = new ArrayList<>();
        for (Map.Entry<UUID, List<SkillRow>> entry : pendingSaves.entrySet()) {
            pendingSnapshots.add(new PendingSaveBatch(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        pendingSaves.keySet().removeAll(pendingSnapshots.stream().map(PendingSaveBatch::uuid).toList());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            verbose("[SAVE-ALL] Saving {} active player(s), {} pending [thread={}]",
                    activeSnapshots.size(), pendingSnapshots.size(), Thread.currentThread().getName());
            for (SaveAllSnapshot entry : activeSnapshots) {
                savePlayerData(entry.uuid());
                List<SkillRow> rows = buildRows(entry.uuid(), entry.skills());
                if (!rows.isEmpty()) {
                    verbose("[SAVE-ALL] {} — writing {} row(s)", entry.uuid(), rows.size());
                    writeRows(rows);
                }
            }
            for (PendingSaveBatch entry : pendingSnapshots) {
                verbose("[SAVE-ALL] {} — flushing pending save ({} row(s))", entry.uuid(), entry.rows().size());
                writeRows(entry.rows());
                preLoadDirty.remove(entry.uuid());
            }

            String currentServerId = serverId;
            if (currentServerId != null) {
                Database.getInstance().getJdbi().useHandle(h ->
                        h.attach(PlayerSessionsDao.class).heartbeatAll(currentServerId, System.currentTimeMillis())
                );
            }
        });
    }

    public void shutdown() {
        saveAll();
        String currentServerId = serverId;
        if (currentServerId != null && isPersistenceAvailable()) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).releaseAllLocks(currentServerId)
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
        pendingSaves.remove(uuid);
        preLoadDirty.remove(uuid);
    }

    public void resetPlayerData(UUID uuid) {
        verbose("[RESET] {} — clearing cache and deleting DB rows async", uuid);
        Skills skills = new Skills();
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        loadedPlayers.add(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
        pendingSaves.remove(uuid);
        preLoadDirty.remove(uuid);

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
        preLoadDirty.remove(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
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
        String currentServerId = serverId;
        if (!isPersistenceAvailable() || currentServerId == null) {
            return;
        }
        Database.getInstance().getJdbi().useHandle(h ->
                h.attach(PlayerSessionsDao.class).releaseLock(uuid.toString(), currentServerId)
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

    private Skills copySkills(Skills source) {
        List<Skill> skillCopies = new ArrayList<>();
        for (Skill skill : source) {
            Skill copy = new Skill(
                    skill.getType(),
                    skill.getLevel(),
                    skill.getXp(),
                    skill.getXpToLevel(),
                    skill.getTotalXp()
            );
            copy.setUuid(source.getUuid());
            skillCopies.add(copy);
        }

        Skills snapshot = new Skills(skillCopies);
        snapshot.setUuid(source.getUuid());
        return snapshot;
    }

    private void queuePendingSave(UUID uuid) {
        Skills skills = activeSkills.get(uuid);
        if (skills == null) {
            return;
        }
        pendingSaves.put(uuid, buildRows(uuid, skills));
    }

    public void markPreLoadDirty(UUID uuid) {
        if (uuid == null) {
            return;
        }
        preLoadDirty.add(uuid);
        if (!loadedPlayers.contains(uuid) && activeSkills.containsKey(uuid)) {
            queuePendingSave(uuid);
        }
    }

    private record SaveRequest(UUID uuid, Skills liveSkills, Skills snapshotSkills,
                               boolean onlineAtStart, boolean releaseLockAfterSave) {
    }

    private record SaveAllSnapshot(UUID uuid, Skills skills) {
    }

    private record PendingSaveBatch(UUID uuid, List<SkillRow> rows) {
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
