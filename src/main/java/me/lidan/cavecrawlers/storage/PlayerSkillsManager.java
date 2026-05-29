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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerSkillsManager {
    private static PlayerSkillsManager instance;

    /**
     * Set to true to log every cache hit/miss, lock attempt, and save operation.
     */
    private static final boolean VERBOSE = false;

    /**
     * How long (ms) a lock can go un-heartbeated before it is considered abandoned (crash recovery).
     */
    private static final long LOCK_TIMEOUT_MS = 60_000L;
    /** How many times to retry lock acquisition before giving up and loading anyway. */
    private static final int LOCK_MAX_ATTEMPTS = 20;
    /** Sleep between lock retry attempts (ms). Max wait = LOCK_MAX_ATTEMPTS × LOCK_RETRY_MS = 10 s. */
    private static final long LOCK_RETRY_MS = 500L;

    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    /**
     * Holds rows snapshotted at quit time until the async write completes.
     * On rapid reconnect to the same server, loadPlayerSync reads from here instead of the DB
     * so the player sees their correct quit-time state immediately.
     */
    private final ConcurrentHashMap<UUID, List<SkillRow>> pendingSaves = new ConcurrentHashMap<>();

    /** Unique ID for this server process. Used to identify lock ownership. */
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
        if (VERBOSE) log.info(msg, args);
    }

    private void loadPlayerData(UUID uuid) {
        Database.getInstance().loadPlayerDataTables(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerDataLoadEvent(uuid));
    }

    private void savePlayerData(UUID uuid) {
        Database.getInstance().savePlayerDataTables(uuid);
        Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(uuid));
    }

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    /** Schedules a load on an async thread. Use from main-thread events like PlayerJoinEvent. */
    public void loadPlayerAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerSync(uuid));
    }

    public Skills loadPlayerSync(UUID uuid) {
        List<SkillRow> pending = pendingSaves.get(uuid);
        List<SkillRow> rows;

        if (pending != null) {
            // Same-server rapid reconnect: the async task hasn't flushed yet.
            // Use the in-memory snapshot — it is newer than anything in the DB.
            verbose("[LOAD] {} — pending-save snapshot (rapid reconnect, skipping lock+DB) [thread={}]",
                    uuid, Thread.currentThread().getName());
            rows = pending;
            // Still need to hold the lock so quit-save can release it cleanly.
            acquireLock(uuid);
        } else {
            verbose("[LOAD] {} — cache miss, acquiring lock then fetching from DB [thread={}]",
                    uuid, Thread.currentThread().getName());
            acquireLock(uuid);

            // Re-check cache: a concurrent thread on this server may have loaded while
            // we were waiting for the lock (e.g. TAB scoreboard + server thread both miss).
            Skills concurrent = activeSkills.get(uuid);
            if (concurrent != null) {
                verbose("[LOAD] {} — data populated by concurrent thread, skipping DB read", uuid);
                return concurrent;
            }

            rows = loadRowsFromDb(uuid);
            verbose("[LOAD] {} — loaded {} skill row(s) from DB", uuid, rows.size());
        }

        Skills skills = buildSkillsFromRows(uuid, rows);
        activeSkills.put(uuid, skills);
        loadPlayerData(uuid);

        // Guard: quit may have fired before this async task acquired the lock.
        // If the player is already offline, save immediately and release the lock.
        if (Bukkit.getPlayer(uuid) == null) {
            verbose("[LOAD] {} — player offline by the time load finished, saving and releasing lock", uuid);
            savePlayerNow(uuid);
        }

        return skills;
    }

    /**
     * Tries to acquire the session lock for {@code uuid}.
     *
     * <p>On the <b>main thread</b>: attempts once; if contended, loads stale data immediately and
     * schedules a background task that waits for the lock and refreshes the cache when free.
     *
     * <p>On an <b>async thread</b>: polls every {@value LOCK_RETRY_MS} ms for up to
     * {@value LOCK_MAX_ATTEMPTS} attempts before giving up and loading whatever is in the DB.
     */
    private void acquireLock(UUID uuid) {
        String uuidStr = uuid.toString();
        Database.getInstance().getJdbi().useHandle(h -> h.attach(PlayerSessionsDao.class).ensureRow(uuidStr));

        if (tryAcquireLockOnce(uuid, uuidStr)) return;

        if (Bukkit.isPrimaryThread()) {
            // Never block the main thread — load stale data now and fix it async once the lock is free.
            log.info("[LOCK] {} — main thread contention, loading stale data; background refresh scheduled", uuid);
            scheduleBackgroundRefresh(uuid);
            return;
        }

        // Async thread — poll until acquired or timed out.
        for (int attempt = 1; attempt <= LOCK_MAX_ATTEMPTS; attempt++) {
            verbose("[LOCK] {} — waiting for lock (attempt {}/{})", uuid, attempt, LOCK_MAX_ATTEMPTS);
            try {
                Thread.sleep(LOCK_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (tryAcquireLockOnce(uuid, uuidStr)) return;
        }
        log.warn("[LOCK] {} — could not acquire lock after {} attempts, proceeding without it", uuid, LOCK_MAX_ATTEMPTS);
    }

    private boolean tryAcquireLockOnce(UUID uuid, String uuidStr) {
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

    /**
     * Waits async for the lock to be released by another server, then reloads the player's
     * skills from the DB and updates the cache with the fresh data.
     */
    private void scheduleBackgroundRefresh(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuidStr = uuid.toString();
            boolean acquired = false;
            for (int attempt = 1; attempt <= LOCK_MAX_ATTEMPTS; attempt++) {
                try {
                    Thread.sleep(LOCK_RETRY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (tryAcquireLockOnce(uuid, uuidStr)) {
                    acquired = true;
                    break;
                }
                verbose("[LOCK] {} — background refresh waiting (attempt {}/{})", uuid, attempt, LOCK_MAX_ATTEMPTS);
            }

            if (!acquired) {
                log.warn("[LOCK] {} — background refresh: lock never acquired, keeping stale data", uuid);
                return;
            }

            List<SkillRow> rows = loadRowsFromDb(uuid);
            Skills fresh = buildSkillsFromRows(uuid, rows);
            activeSkills.put(uuid, fresh);
            loadPlayerData(uuid);
            verbose("[LOAD] {} — background refresh complete ({} skill row(s))", uuid, rows.size());

            if (Bukkit.getPlayer(uuid) == null) {
                verbose("[LOAD] {} — player offline after background refresh, saving and releasing", uuid);
                savePlayerNow(uuid);
            }
        });
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
        boolean onlineAtStart = Bukkit.getPlayer(uuid) != null;
        Skills skills = onlineAtStart ? activeSkills.get(uuid) : activeSkills.remove(uuid);
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
        savePlayerData(uuid);

        String uuidStr = uuid.toString();
        Database.getInstance().getJdbi().useTransaction(h -> {
            if (!rows.isEmpty()) {
                h.attach(SkillsDao.class).upsertSkills(rows);
            }
            h.attach(PlayerSessionsDao.class).releaseLock(uuidStr, serverId);
        });

        if (!onlineAtStart && Bukkit.getPlayer(uuid) != null) {
            activeSkills.put(uuid, skills);
            verbose("[SAVE-NOW] {} — player reconnected during save, restored cached skills", uuid);
        }

        verbose("[SAVE-NOW] {} — done", uuid);
    }

    /**
     * Runs savePlayerNow on an async thread so the main thread is not blocked.
     */
    public void savePlayerAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerNow(uuid));
    }

    /**
     * Saves all active players and heartbeats their session locks.
     * Called by the periodic auto-save task and by shutdown.
     */
    public void saveAll() {
        verbose("[SAVE-ALL] Saving {} active player(s), {} pending [thread={}]",
                activeSkills.size(), pendingSaves.size(), Thread.currentThread().getName());
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
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
        }
        pendingSaves.clear();

        if (serverId != null) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).heartbeatAll(serverId, System.currentTimeMillis())
            );
        }
    }

    /**
     * Saves all active players and releases every session lock held by this server.
     * Call on clean shutdown so other servers don't have to wait for crash-recovery expiry.
     */
    public void shutdown() {
        saveAll();
        if (serverId != null) {
            Database.getInstance().getJdbi().useHandle(h ->
                    h.attach(PlayerSessionsDao.class).releaseAllLocks(serverId)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Cache access
    // -------------------------------------------------------------------------

    /**
     * Returns cached skills, lazily loading from the database on a cache miss.
     * This maintains the old PlayerDataManager contract and handles reload scenarios
     * where players are already online but the cache was cleared.
     */
    public Skills getSkills(UUID uuid) {
        Skills cached = activeSkills.get(uuid);
        if (cached != null) return cached;
        verbose("[GET] {} — cache miss, triggering load [thread={}]", uuid, Thread.currentThread().getName());
        return loadPlayerSync(uuid);
    }

    public Skills getSkills(Player player) {
        return getSkills(player.getUniqueId());
    }

    public void putSkills(UUID uuid, Skills skills) {
        verbose("[PUT] {} — inserting into cache directly [thread={}]", uuid, Thread.currentThread().getName());
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
    }

    public void resetPlayerData(UUID uuid) {
        verbose("[RESET] {} — clearing cache and deleting DB rows async", uuid);
        Skills skills = new Skills();
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        String uuidStr = uuid.toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                Database.getInstance().getJdbi().useHandle(h -> h.attach(SkillsDao.class).deleteSkills(uuidStr))
        );
    }

    public void removeFromCache(UUID uuid) {
        verbose("[EVICT] {} — removed from cache", uuid);
        activeSkills.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void releaseLock(UUID uuid) {
        if (serverId == null) return;
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
            // Mirrors Skills.deserialize(): recompute level+xp from totalXp so any
            // XP-requirement config changes are reflected on next login.
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
        Database.getInstance().getJdbi().useHandle(handle ->
                handle.attach(SkillsDao.class).upsertSkills(rows)
        );
    }
}
