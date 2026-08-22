package me.lidan.cavecrawlers.storage;

import lombok.Getter;
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
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class PlayerSkillsManager {
    private static final long LOCK_TIMEOUT_MS = 60_000L;
    private static final int LOCK_MAX_ATTEMPTS = 20;
    private static final long LOCK_RETRY_MS = 500L;
    private static final long STATE_LOCK_TIMEOUT_MS = 250L;
    private static final long STATE_CLEANUP_DELAY_TICKS = 20L * 60L;
    private static final long STATE_SAVE_RETRY_DELAY_TICKS = 1L;
    private static final long LOADING_TITLE_DELAY_TICKS = 20L;
    private static final long LOAD_RETRY_DELAY_TICKS = 20L;
    private static final Component LOADING_TITLE = MiniMessageUtils.miniMessage("<gold><bold>Loading your data...");
    private static final Component LOADING_SUBTITLE = MiniMessageUtils.miniMessage("<gray>Please wait.");
    private static final Title LOADING_TITLE_TEMPLATE = Title.title(
            LOADING_TITLE,
            LOADING_SUBTITLE,
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250))
    );
    private static final Component LOADED_TITLE = MiniMessageUtils.miniMessage("<green><bold>Data loaded");
    private static final Component LOADED_SUBTITLE = MiniMessageUtils.miniMessage("<gray>You can continue playing.");
    private static final Title LOADED_TITLE_TEMPLATE = Title.title(
            LOADED_TITLE,
            LOADED_SUBTITLE,
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))
    );
    private static PlayerSkillsManager instance;
    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> scheduledLoads = new ConcurrentHashMap<>();
    private final Set<UUID> pendingLoads = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> loadGenerations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ReentrantLock> playerStateLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> stateCleanupTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> loadingTitleTasks = new ConcurrentHashMap<>();
    private final Set<UUID> loadingTitleShown = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, PendingSaveData> pendingSaves = new ConcurrentHashMap<>();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    @Getter
    private volatile String serverId;

    private PlayerSkillsManager() {
    }

    public static PlayerSkillsManager getInstance() {
        if (instance == null) {
            instance = new PlayerSkillsManager();
        }
        return instance;
    }

    /** Uses a fresh process ID so an old process cannot share the new lock owner. */
    public void setServerId() {
        this.serverId = UUID.randomUUID().toString();
    }

    private void verbose(String msg, Object... args) {
        if (plugin.getConfig().getBoolean("database.verbose-logging", false)) {
            log.info(msg, args);
        }
    }

    private long currentLoadGeneration(UUID uuid) {
        return loadGenerations.getOrDefault(uuid, 0L);
    }

    private long invalidateLoadGeneration(UUID uuid) {
        return loadGenerations.merge(uuid, 1L, Long::sum);
    }

    private boolean isCurrentLoadGeneration(UUID uuid, long generation) {
        return currentLoadGeneration(uuid) == generation;
    }

    private ReentrantLock stateLockFor(UUID uuid) {
        return playerStateLocks.computeIfAbsent(uuid, ignored -> new ReentrantLock());
    }

    private void cancelStateCleanup(UUID uuid) {
        BukkitTask task = stateCleanupTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean hasInFlightState(UUID uuid) {
        return scheduledLoads.containsKey(uuid) || pendingSaves.containsKey(uuid);
    }

    private void scheduleStateCleanup(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> scheduleStateCleanup(uuid));
            return;
        }

        cancelStateCleanup(uuid);
        long generation = currentLoadGeneration(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stateCleanupTasks.remove(uuid);
            if (Bukkit.getPlayer(uuid) != null || hasInFlightState(uuid)) {
                scheduleStateCleanup(uuid);
                return;
            }

            ReentrantLock stateLock = playerStateLocks.get(uuid);
            boolean locked = stateLock == null || stateLock.tryLock();
            if (!locked) {
                scheduleStateCleanup(uuid);
                return;
            }

            try {
                if (currentLoadGeneration(uuid) != generation
                        || Bukkit.getPlayer(uuid) != null
                        || hasInFlightState(uuid)) {
                    scheduleStateCleanup(uuid);
                    return;
                }
                activeSkills.remove(uuid);
                loadedPlayers.remove(uuid);
                pendingLoads.remove(uuid);
                clearLoadingTitleState(uuid);
                loadGenerations.remove(uuid, generation);
                if (stateLock != null && !stateLock.hasQueuedThreads()) {
                    playerStateLocks.remove(uuid, stateLock);
                }
            } finally {
                if (stateLock != null) {
                    stateLock.unlock();
                }
            }
        }, STATE_CLEANUP_DELAY_TICKS);
        stateCleanupTasks.put(uuid, task);
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
        cancelStateCleanup(uuid);
        invalidateLoadGeneration(uuid);
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
        cancelStateCleanup(uuid);
        getOrCreateSkills(uuid);

        if (loadedPlayers.contains(uuid)) {
            return;
        }

        if (!isPersistenceAvailable()) {
            pendingLoads.add(uuid);
            return;
        }

        pendingLoads.remove(uuid);
        long generation = currentLoadGeneration(uuid);
        if (!markLoadScheduled(uuid, generation)) {
            return;
        }

        scheduleLoadingTitle(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerFromDatabase(uuid, generation));
    }

    private boolean markLoadScheduled(UUID uuid, long generation) {
        for (;;) {
            Long existing = scheduledLoads.putIfAbsent(uuid, generation);
            if (existing == null) {
                return true;
            }
            if (existing == generation) {
                return false;
            }
            if (scheduledLoads.replace(uuid, existing, generation)) {
                return true;
            }
        }
    }

    private void scheduleLoadingTitle(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> scheduleLoadingTitle(uuid));
            return;
        }

        if (loadedPlayers.contains(uuid)) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadingTitleTasks.remove(uuid);
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer == null || loadedPlayers.contains(uuid)) {
                return;
            }
            loadingTitleShown.add(uuid);
            onlinePlayer.showTitle(LOADING_TITLE_TEMPLATE);
        }, LOADING_TITLE_DELAY_TICKS);

        BukkitTask previous = loadingTitleTasks.putIfAbsent(uuid, task);
        if (previous != null) {
            task.cancel();
        }
    }

    private void cancelLoadingTitleTask(UUID uuid) {
        BukkitTask task = loadingTitleTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void clearLoadingTitleState(UUID uuid) {
        cancelLoadingTitleTask(uuid);
        loadingTitleShown.remove(uuid);
    }

    private void showLoadedTitleIfNeeded(UUID uuid) {
        if (!loadingTitleShown.remove(uuid)) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        player.showTitle(LOADED_TITLE_TEMPLATE);
    }

    private void loadPlayerFromDatabase(UUID uuid, long generation) {
        ReentrantLock stateLock = null;
        boolean stateLockHeld = false;
        try {
            if (!isPersistenceAvailable()) {
                pendingLoads.add(uuid);
                return;
            }

            if (!acquireLock(uuid)) {
                pendingLoads.add(uuid);
                scheduleLoadRetry(uuid, generation);
                return;
            }

            stateLock = stateLockFor(uuid);
            stateLock.lock();
            stateLockHeld = true;
            List<SkillRow> rows = loadRowsFromDb(uuid);
            verbose("[LOAD] {} — loaded {} skill row(s) from DB: {}", uuid, rows.size(), rows);

            if (!isCurrentLoadGeneration(uuid, generation) || Bukkit.getPlayer(uuid) == null) {
                verbose("[LOAD] {} — stale or offline load discarded, releasing lock [generation={}]", uuid, generation);
                releaseLock(uuid);
                return;
            }

            PendingSaveData pending = pendingSaves.get(uuid);
            if (pending != null && areRowsEquivalent(rows, pending.rows())) {
                pendingSaves.remove(uuid, pending);
            } else if (pending != null) {
                rows = pending.rows();
                verbose("[LOAD] {} — using pending quit/save snapshot because it is newer than DB", uuid);
            }

            Skills skills = buildSkillsFromRows(uuid, rows);
            activeSkills.put(uuid, skills);
            loadedPlayers.add(uuid);
            pendingLoads.remove(uuid);
            cancelLoadingTitleTask(uuid);
            loadPlayerData(uuid);
            showLoadedTitleIfNeeded(uuid);

            if (Bukkit.getPlayer(uuid) == null) {
                verbose("[LOAD] {} — player offline by the time load finished, saving and releasing lock", uuid);
                savePlayerAsync(uuid, true);
            }
        } catch (Exception e) {
            log.warn("[LOAD] {} — failed to load player data: {}", uuid, e.getMessage(), e);
            pendingLoads.add(uuid);
        } finally {
            if (stateLockHeld) {
                stateLock.unlock();
            }
            scheduledLoads.remove(uuid, generation);
            if (Bukkit.getPlayer(uuid) == null) {
                scheduleStateCleanup(uuid);
            }
        }
    }

    private void scheduleLoadRetry(UUID uuid, long generation) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isCurrentLoadGeneration(uuid, generation) && Bukkit.getPlayer(uuid) != null) {
                scheduleLoadIfNeeded(uuid);
            }
        }, LOAD_RETRY_DELAY_TICKS);
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

    /** Captures the current state on the primary thread and persists it asynchronously. */
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerNow(request));
    }

    /**
     * Captures and queues a player's quit state before another login can reuse the UUID.
     * The generation bump also makes any older asynchronous load or save ineligible to
     * replace the new session's cache.
     */
    public void savePlayerNowOnQuit(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> savePlayerNowOnQuit(uuid));
            return;
        }

        cancelStateCleanup(uuid);
        invalidateLoadGeneration(uuid);
        if (!isPersistenceAvailable()) {
            queuePendingSave(uuid);
            verbose("[SAVE-NOW] {} — persistence unavailable on quit, keeping cache for retry", uuid);
            scheduleStateCleanup(uuid);
            return;
        }

        SaveRequest request = createSaveRequest(uuid, true);
        if (request == null) {
            evictQuitState(uuid);
            scheduleStateCleanup(uuid);
            return;
        }

        loadedPlayers.remove(uuid);
        pendingLoads.remove(uuid);
        clearLoadingTitleState(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerNow(request));
        scheduleStateCleanup(uuid);
        verbose("[SAVE-NOW] {} — quit snapshot queued for async persistence", uuid);
    }

    private void evictQuitState(UUID uuid) {
        activeSkills.remove(uuid);
        loadedPlayers.remove(uuid);
        pendingLoads.remove(uuid);
        clearLoadingTitleState(uuid);
        verbose("[SAVE-NOW] {} — quit state evicted", uuid);
    }

    private void savePlayerNow(SaveRequest request) {
        UUID uuid = request.uuid();
        ReentrantLock stateLock = stateLockFor(uuid);
        boolean locked = false;
        try {
            locked = stateLock.tryLock(STATE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!locked) {
                verbose("[SAVE-NOW] {} — state lock busy, retrying asynchronously", uuid);
                scheduleSaveRetry(request);
                return;
            }

            if (!isCurrentLoadGeneration(uuid, request.generation())) {
                verbose("[SAVE-NOW] {} — stale save discarded [generation={}]", uuid, request.generation());
                return;
            }

            if (!isPersistenceAvailable()) {
                restoreFailedSaveRequest(request);
                return;
            }

            Skills skills = request.snapshotSkills();
            if (skills == null) {
                verbose("[SAVE-NOW] {} — nothing in cache, releasing lock and skipping [thread={}]",
                        uuid, Thread.currentThread().getName());
                loadedPlayers.remove(uuid);
                if (request.releaseLockAfterSave()) {
                    releaseLock(uuid);
                }
                return;
            }

            List<SkillRow> rows = buildRows(uuid, skills);
            verbose("[SAVE-NOW] {} — writing {} row(s) + releasing lock [thread={}]: {}",
                    uuid, rows.size(), Thread.currentThread().getName(), rows);
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

            PendingSaveData pending = pendingSaves.get(uuid);
            if (pending != null && areRowsEquivalent(rows, pending.rows())) {
                pendingSaves.remove(uuid, pending);
            }

            boolean currentGeneration = isCurrentLoadGeneration(uuid, request.generation());
            if (!request.onlineAtStart() && Bukkit.getPlayer(uuid) != null && currentGeneration) {
                activeSkills.put(uuid, copySkills(skills));
                loadedPlayers.add(uuid);
                verbose("[SAVE-NOW] {} — player reconnected during save, restored cached skills", uuid);
            } else if (!request.onlineAtStart() && Bukkit.getPlayer(uuid) == null && currentGeneration) {
                evictQuitState(uuid);
            }

            verbose("[SAVE-NOW] {} — done", uuid);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            restoreFailedSaveRequest(request);
            log.warn("[SAVE-NOW] {} — interrupted while saving; retaining cache for retry", uuid);
        } catch (Exception e) {
            restoreFailedSaveRequest(request);
            log.warn("[SAVE-NOW] {} — save failed; retaining cache for retry: {}", uuid, e.getMessage(), e);
        } finally {
            if (locked) {
                stateLock.unlock();
            }
            if (Bukkit.getPlayer(uuid) == null) {
                scheduleStateCleanup(uuid);
            }
        }
    }

    private void scheduleSaveRetry(SaveRequest request) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                plugin,
                () -> savePlayerNow(request),
                STATE_SAVE_RETRY_DELAY_TICKS
        );
    }

    private void restoreFailedSaveRequest(SaveRequest request) {
        UUID uuid = request.uuid();
        Skills snapshot = request.snapshotSkills();
        if (snapshot != null) {
            if (isCurrentLoadGeneration(uuid, request.generation())) {
                pendingSaves.put(uuid, new PendingSaveData(buildRows(uuid, snapshot), System.currentTimeMillis()));
            }
            if (request.releaseLockAfterSave()
                    && isCurrentLoadGeneration(uuid, request.generation())
                    && !activeSkills.containsKey(uuid)) {
                activeSkills.put(uuid, copySkills(snapshot));
                loadedPlayers.add(uuid);
            }
        } else if (request.releaseLockAfterSave()
                && isCurrentLoadGeneration(uuid, request.generation())) {
            loadedPlayers.remove(uuid);
        }
        if (request.releaseLockAfterSave()
                && isCurrentLoadGeneration(uuid, request.generation())) {
            releaseLock(uuid);
        }
    }

    private SaveRequest createSaveRequest(UUID uuid, boolean releaseLockAfterSave) {
        if (!loadedPlayers.contains(uuid)) {
            PendingSaveData pending = pendingSaves.get(uuid);
            if (pending != null) {
                pendingLoads.add(uuid);
                scheduledLoads.remove(uuid);
                verbose("[SAVE-NOW] {} — not loaded and pending save is newer candidate (ts={}), keeping for retry [thread={}]",
                        uuid, pending.updatedAt(), Thread.currentThread().getName());
                return null;
            }
            activeSkills.remove(uuid);
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

        boolean onlineAtStart = activeSkills.containsKey(uuid) && !releaseLockAfterSave;
        Skills skills = onlineAtStart ? activeSkills.get(uuid) : activeSkills.remove(uuid);
        pendingSaves.remove(uuid);
        Skills snapshotSkills = skills == null ? null : copySkills(skills);
        if (releaseLockAfterSave && snapshotSkills != null) {
            pendingSaves.put(uuid, new PendingSaveData(buildRows(uuid, snapshotSkills), System.currentTimeMillis()));
        }
        return new SaveRequest(uuid, snapshotSkills, onlineAtStart, releaseLockAfterSave,
                currentLoadGeneration(uuid));
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
        Set<UUID> activeRowUuids = new HashSet<>();
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
            if (!loadedPlayers.contains(entry.getKey())) {
                continue;
            }
            ReentrantLock stateLock = stateLockFor(entry.getKey());
            stateLock.lock();
            try {
                if (!loadedPlayers.contains(entry.getKey())) {
                    continue;
                }
                savePlayerData(entry.getKey());
                List<SkillRow> rows = buildRows(entry.getKey(), entry.getValue());
                activeRowUuids.add(entry.getKey());
                if (!rows.isEmpty()) {
                    verbose("[SAVE-ALL] {} — writing {} row(s): {}", entry.getKey(), rows.size(), rows);
                    writeRows(rows);
                }
            } finally {
                stateLock.unlock();
            }
        }

        Map<UUID, PendingSaveData> pendingSnapshot = new HashMap<>(pendingSaves);
        for (Map.Entry<UUID, PendingSaveData> entry : pendingSnapshot.entrySet()) {
            ReentrantLock stateLock = stateLockFor(entry.getKey());
            stateLock.lock();
            try {
                List<SkillRow> filteredRows = filterPendingRows(entry.getValue().rows(), activeRowUuids);
                verbose("[SAVE-ALL] {} — flushing pending save ({} row(s))", entry.getKey(), filteredRows.size());
                if (!filteredRows.isEmpty()) {
                    writeRows(filteredRows);
                }
            } finally {
                stateLock.unlock();
            }
        }
        for (Map.Entry<UUID, PendingSaveData> entry : pendingSnapshot.entrySet()) {
            pendingSaves.remove(entry.getKey(), entry.getValue());
        }

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
        Set<UUID> activeRowUuids = new HashSet<>();
        for (Map.Entry<UUID, Skills> entry : activeSkills.entrySet()) {
            if (!loadedPlayers.contains(entry.getKey())) {
                continue;
            }
            activeSnapshots.add(new SaveAllSnapshot(entry.getKey(), copySkills(entry.getValue()),
                    currentLoadGeneration(entry.getKey())));
            activeRowUuids.add(entry.getKey());
        }

        List<PendingSaveBatch> pendingSnapshots = new ArrayList<>();
        for (Map.Entry<UUID, PendingSaveData> entry : pendingSaves.entrySet()) {
            pendingSnapshots.add(new PendingSaveBatch(entry.getKey(), new ArrayList<>(entry.getValue().rows()),
                    currentLoadGeneration(entry.getKey())));
        }
        pendingSaves.keySet().removeAll(pendingSnapshots.stream().map(PendingSaveBatch::uuid).toList());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            verbose("[SAVE-ALL] Saving {} active player(s), {} pending [thread={}]",
                    activeSnapshots.size(), pendingSnapshots.size(), Thread.currentThread().getName());
            for (SaveAllSnapshot entry : activeSnapshots) {
                ReentrantLock stateLock = stateLockFor(entry.uuid());
                stateLock.lock();
                try {
                    if (!isCurrentLoadGeneration(entry.uuid(), entry.generation())) {
                        verbose("[SAVE-ALL] {} — stale snapshot discarded [generation={}]",
                                entry.uuid(), entry.generation());
                        continue;
                    }
                    savePlayerData(entry.uuid());
                    List<SkillRow> rows = buildRows(entry.uuid(), entry.skills());
                    if (!rows.isEmpty()) {
                        verbose("[SAVE-ALL] {} — writing {} row(s): {}", entry.uuid(), rows.size(), rows);
                        writeRows(rows);
                    }
                } finally {
                    stateLock.unlock();
                }
            }
            for (PendingSaveBatch entry : pendingSnapshots) {
                ReentrantLock stateLock = stateLockFor(entry.uuid());
                stateLock.lock();
                try {
                    if (!isCurrentLoadGeneration(entry.uuid(), entry.generation())) {
                        verbose("[SAVE-ALL] {} — stale pending snapshot discarded [generation={}]",
                                entry.uuid(), entry.generation());
                        continue;
                    }
                    List<SkillRow> filteredRows = filterPendingRows(entry.rows(), activeRowUuids);
                    verbose("[SAVE-ALL] {} — flushing pending save ({} row(s))", entry.uuid(), filteredRows.size());
                    if (!filteredRows.isEmpty()) {
                        writeRows(filteredRows);
                    }
                } finally {
                    stateLock.unlock();
                }
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
        invalidateLoadGeneration(uuid);
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        loadedPlayers.add(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
        pendingSaves.remove(uuid);
        clearLoadingTitleState(uuid);
    }

    public void resetPlayerData(UUID uuid) {
        verbose("[RESET] {} — clearing cache and deleting DB rows async", uuid);
        invalidateLoadGeneration(uuid);
        Skills skills = new Skills();
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        loadedPlayers.add(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
        pendingSaves.remove(uuid);
        clearLoadingTitleState(uuid);

        if (isPersistenceAvailable()) {
            String uuidStr = uuid.toString();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    Database.getInstance().getJdbi().useHandle(h -> h.attach(SkillsDao.class).deleteSkills(uuidStr))
            );
        }
    }

    public void removeFromCache(UUID uuid) {
        verbose("[EVICT] {} — removed from cache", uuid);
        cancelStateCleanup(uuid);
        invalidateLoadGeneration(uuid);
        activeSkills.remove(uuid);
        loadedPlayers.remove(uuid);
        scheduledLoads.remove(uuid);
        pendingLoads.remove(uuid);
        pendingSaves.remove(uuid);
        clearLoadingTitleState(uuid);
        scheduleStateCleanup(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
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
        pendingSaves.put(uuid, new PendingSaveData(buildRows(uuid, skills), System.currentTimeMillis()));
    }

    private boolean areRowsEquivalent(List<SkillRow> left, List<SkillRow> right) {
        return toRowSignature(left).equals(toRowSignature(right));
    }

    private Map<String, SkillRowSignature> toRowSignature(List<SkillRow> rows) {
        Map<String, SkillRowSignature> signature = new HashMap<>();
        for (SkillRow row : rows) {
            signature.put(row.getType(), new SkillRowSignature(row.getXp(), row.getLevel(), row.getTotalXp()));
        }
        return signature;
    }

    private List<SkillRow> filterPendingRows(List<SkillRow> rows, Set<UUID> activeRowUuids) {
        if (rows.isEmpty() || activeRowUuids.isEmpty()) {
            return rows;
        }

        List<SkillRow> filtered = new ArrayList<>(rows.size());
        for (SkillRow row : rows) {
            UUID rowUuid;
            try {
                rowUuid = UUID.fromString(row.getPlayerUuid());
            } catch (Exception ignored) {
                filtered.add(row);
                continue;
            }
            if (!activeRowUuids.contains(rowUuid)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    private record SaveRequest(UUID uuid, Skills snapshotSkills, boolean onlineAtStart,
                               boolean releaseLockAfterSave, long generation) {
    }

    private record SaveAllSnapshot(UUID uuid, Skills skills, long generation) {
    }

    private record PendingSaveBatch(UUID uuid, List<SkillRow> rows, long generation) {
    }

    private record PendingSaveData(List<SkillRow> rows, long updatedAt) {
    }

    private record SkillRowSignature(double xp, int level, double totalXp) {
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
