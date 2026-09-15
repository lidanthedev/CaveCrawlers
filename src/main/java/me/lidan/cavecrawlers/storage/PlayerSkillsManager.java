package me.lidan.cavecrawlers.storage;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.skills.Skill;
import me.lidan.cavecrawlers.skills.SkillInfo;
import me.lidan.cavecrawlers.skills.Skills;
import me.lidan.cavecrawlers.skills.SkillsManager;
import me.lidan.cavecrawlers.storage.db.Database;
import me.lidan.cavecrawlers.storage.db.SkillRow;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the player persistence state machine.
 *
 * <p>Invariants:
 * <ol>
 *   <li>A player is loaded only after a fenced lease is acquired and DB data is loaded.</li>
 *   <li>Every durable write validates server ID, fence token, and revision in its DB transaction.</li>
 *   <li>One writer per player drains a latest-only queue; older revisions cannot overwrite newer ones.</li>
 *   <li>Pending data is eligible only while its original fence remains current.</li>
 *   <li>Reset is an ordered write, not an independent DELETE.</li>
 *   <li>Quit releases ownership in the same transaction as its final write.</li>
 *   <li>Old owners cannot write or unlock, including after a DB outage.</li>
 *   <li>Required addon tables share the skills transaction and fail it as a unit.</li>
 *   <li>Normal gameplay only captures snapshots on the primary thread; SQL runs asynchronously.</li>
 * </ol>
 */
@Slf4j
public class PlayerSkillsManager {
    private static final int LOCK_MAX_ATTEMPTS = 20;
    private static final long LOCK_RETRY_MS = 500L;
    private static final long STATE_CLEANUP_DELAY_TICKS = 20L * 60L;
    private static final long LOADING_TITLE_DELAY_TICKS = 20L;
    private static final long LOAD_RETRY_DELAY_TICKS = 20L;
    private static final Component LOADING_TITLE = MiniMessageUtils.miniMessage("<gold><bold>Loading your data...");
    private static final Component LOADING_SUBTITLE = MiniMessageUtils.miniMessage("<gray>Please wait.");
    private static final Title LOADING_TITLE_TEMPLATE = Title.title(LOADING_TITLE, LOADING_SUBTITLE,
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250)));
    private static final Component LOADED_TITLE = MiniMessageUtils.miniMessage("<green><bold>Data loaded");
    private static final Component LOADED_SUBTITLE = MiniMessageUtils.miniMessage("<gray>You can continue playing.");
    private static final Title LOADED_TITLE_TEMPLATE = Title.title(LOADED_TITLE, LOADED_SUBTITLE,
            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200)));
    private static final Component LOST_SESSION_MESSAGE = MiniMessageUtils.miniMessage(
            "<red>Your player-data session moved to another server. Please reconnect.");

    private static PlayerSkillsManager instance;
    private final ConcurrentHashMap<UUID, Skills> activeSkills = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> scheduledLoads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> pendingLoads = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> loadGenerations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ReentrantLock> playerStateLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> stateCleanupTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> loadingTitleTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> loadingTitleShown = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Ownership> ownerships = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SaveRequest> pendingWrites = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> runningWriters = ConcurrentHashMap.newKeySet();
    // Leave one of the five pool connections available for lease heartbeats.
    private final Semaphore databaseWriterSlots = new Semaphore(4);
    private final AtomicBoolean autosaveScanRunning = new AtomicBoolean();
    private final CaveCrawlers plugin = CaveCrawlers.getInstance();
    @Getter
    private volatile String serverId;
    private volatile boolean shuttingDown;

    private PlayerSkillsManager() {
    }

    public static PlayerSkillsManager getInstance() {
        if (instance == null) {
            instance = new PlayerSkillsManager();
        }
        return instance;
    }

    /** A process ID must never survive a restart because it identifies lease ownership. */
    public void setServerId() {
        serverId = UUID.randomUUID().toString();
        shuttingDown = false;
    }

    private long leaseTimeoutMillis() {
        return plugin.getConfig().getLong("database.lease-timeout", 60L) * 1000L;
    }

    private void verbose(String message, Object... args) {
        if (plugin.getConfig().getBoolean("database.verbose-logging", false)) {
            log.info(message, args);
        }
    }

    private boolean persistenceAvailable() {
        Database database = Database.getInstance();
        return database.isAvailable() && database.getJdbi() != null;
    }

    private long currentGeneration(UUID uuid) {
        return loadGenerations.getOrDefault(uuid, 0L);
    }

    private long nextGeneration(UUID uuid) {
        return loadGenerations.merge(uuid, 1L, Long::sum);
    }

    private boolean currentGeneration(UUID uuid, long generation) {
        return currentGeneration(uuid) == generation;
    }

    private ReentrantLock stateLock(UUID uuid) {
        return playerStateLocks.computeIfAbsent(uuid, ignored -> new ReentrantLock());
    }

    public void loadPlayerAsync(UUID uuid) {
        cancelStateCleanup(uuid);
        nextGeneration(uuid);
        loadedPlayers.remove(uuid);
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
        new ArrayList<>(pendingLoads).forEach(this::scheduleLoadIfNeeded);
    }

    public void scheduleLoadIfNeeded(UUID uuid) {
        cancelStateCleanup(uuid);
        getOrCreateSkills(uuid);
        if (shuttingDown || loadedPlayers.contains(uuid)) {
            return;
        }
        if (!persistenceAvailable()) {
            pendingLoads.add(uuid);
            return;
        }

        long generation = currentGeneration(uuid);
        Ownership previous = ownerships.get(uuid);
        if (previous != null && previous.generation() != generation) {
            pendingLoads.add(uuid);
            scheduleLoadRetry(uuid, generation);
            return;
        }
        if (scheduledLoads.putIfAbsent(uuid, generation) != null) {
            return;
        }

        pendingLoads.remove(uuid);
        scheduleLoadingTitle(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadFromDatabase(uuid, generation));
    }

    private void loadFromDatabase(UUID uuid, long generation) {
        ReentrantLock lock = stateLock(uuid);
        lock.lock();
        try {
            if (!persistenceAvailable()) {
                pendingLoads.add(uuid);
                return;
            }
            Ownership ownership = acquireOwnership(uuid, generation);
            if (ownership == null) {
                pendingLoads.add(uuid);
                scheduleLoadRetry(uuid, generation);
                return;
            }

            List<SkillRow> rows = Database.getInstance()
                    .loadPlayer(uuid, serverId, ownership.fenceToken());
            verbose("[LOAD] uuid={} server={} fence={} generation={} rows={}",
                    uuid, serverId, ownership.fenceToken(), generation, rows.size());

            if (!currentGeneration(uuid, generation) || Bukkit.getPlayer(uuid) == null) {
                releaseOwnership(uuid, ownership);
                return;
            }

            activeSkills.put(uuid, buildSkillsFromRows(uuid, rows));
            loadedPlayers.add(uuid);
            pendingLoads.remove(uuid);
            Bukkit.getPluginManager().callEvent(new PlayerDataLoadEvent(uuid));
            showLoadedTitleIfNeeded(uuid);
        } catch (Database.StaleSessionException e) {
            invalidateStaleSession(uuid, generation, e.currentFence());
        } catch (Exception e) {
            pendingLoads.add(uuid);
            log.warn("[LOAD] uuid={} generation={} failed: {}", uuid, generation, e.getMessage(), e);
            scheduleLoadRetry(uuid, generation);
        } finally {
            lock.unlock();
            scheduledLoads.remove(uuid, generation);
            if (Bukkit.getPlayer(uuid) == null) {
                scheduleStateCleanup(uuid);
            }
        }
    }

    private Ownership acquireOwnership(UUID uuid, long generation) {
        Ownership existing = ownerships.get(uuid);
        if (existing != null) {
            return existing.generation() == generation ? existing : null;
        }

        for (int attempt = 0; attempt <= LOCK_MAX_ATTEMPTS; attempt++) {
            if (!persistenceAvailable() || !currentGeneration(uuid, generation)) {
                return null;
            }
            Optional<Database.PlayerLease> lease = Database.getInstance()
                    .acquirePlayerSession(uuid, serverId, leaseTimeoutMillis());
            if (lease.isPresent()) {
                Ownership acquired = new Ownership(generation, lease.get().fenceToken(),
                        new AtomicLong(lease.get().dataRevision()));
                Ownership raced = ownerships.putIfAbsent(uuid, acquired);
                Ownership result = raced == null ? acquired : raced;
                verbose("[LOCK] acquired uuid={} server={} fence={} generation={}",
                        uuid, serverId, result.fenceToken(), generation);
                return result.generation() == generation ? result : null;
            }
            if (attempt == LOCK_MAX_ATTEMPTS) {
                break;
            }
            try {
                Thread.sleep(LOCK_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("[LOCK] uuid={} could not acquire ownership after {} attempts", uuid, LOCK_MAX_ATTEMPTS);
        return null;
    }

    private void scheduleLoadRetry(UUID uuid, long generation) {
        if (shuttingDown) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentGeneration(uuid, generation) && Bukkit.getPlayer(uuid) != null) {
                scheduleLoadIfNeeded(uuid);
            }
        }, LOAD_RETRY_DELAY_TICKS);
    }

    public void savePlayerNow(UUID uuid) {
        savePlayerNow(uuid, false);
    }

    public void savePlayerNow(UUID uuid, boolean releaseAfterSave) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> savePlayerNow(uuid, releaseAfterSave));
            return;
        }
        SaveRequest request = captureSave(uuid, releaseAfterSave, false);
        if (request != null) {
            enqueue(request);
        }
    }

    public void savePlayerNowOnQuit(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> savePlayerNowOnQuit(uuid));
            return;
        }

        cancelStateCleanup(uuid);
        SaveRequest request = captureSave(uuid, true, false);
        nextGeneration(uuid);
        loadedPlayers.remove(uuid);
        pendingLoads.remove(uuid);
        activeSkills.remove(uuid);
        clearLoadingTitleState(uuid);
        if (request != null) {
            enqueue(request);
            verbose("[SAVE] quit queued uuid={} fence={} generation={} revision={}",
                    uuid, request.fenceToken(), request.generation(), request.revision());
        }
        scheduleStateCleanup(uuid);
    }

    public void savePlayerAsync(UUID uuid) {
        savePlayerAsync(uuid, false);
    }

    public void savePlayerAsync(UUID uuid, boolean releaseAfterSave) {
        savePlayerNow(uuid, releaseAfterSave);
    }

    private SaveRequest captureSave(UUID uuid, boolean releaseAfterSave, boolean deleteBeforeWrite) {
        if (!loadedPlayers.contains(uuid)) {
            return null;
        }
        Ownership ownership = ownerships.get(uuid);
        Skills skills = activeSkills.get(uuid);
        if (ownership == null || skills == null || ownership.generation() != currentGeneration(uuid)) {
            return null;
        }
        long revision = ownership.nextRevision().incrementAndGet();
        return new SaveRequest(uuid, List.copyOf(buildRows(uuid, copySkills(skills))),
                ownership.generation(), ownership.fenceToken(), revision,
                deleteBeforeWrite, releaseAfterSave);
    }

    private void enqueue(SaveRequest request) {
        pendingWrites.merge(request.uuid(), request, PlayerSkillsManager::mergeRequests);
        startWriter(request.uuid());
    }

    static SaveRequest mergeRequests(SaveRequest left, SaveRequest right) {
        if (left.fenceToken() != right.fenceToken()) {
            return left.fenceToken() > right.fenceToken() ? left : right;
        }
        SaveRequest newest = left.revision() >= right.revision() ? left : right;
        return new SaveRequest(newest.uuid(), newest.rows(), newest.generation(), newest.fenceToken(),
                newest.revision(), left.deleteBeforeWrite() || right.deleteBeforeWrite(),
                left.releaseAfterWrite() || right.releaseAfterWrite());
    }

    private void startWriter(UUID uuid) {
        if (shuttingDown || !persistenceAvailable() || !runningWriters.add(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> drainWrites(uuid));
    }

    private void drainWrites(UUID uuid) {
        while (!shuttingDown) {
            SaveRequest request = pendingWrites.remove(uuid);
            if (request == null) {
                runningWriters.remove(uuid);
                if (!pendingWrites.containsKey(uuid) || !runningWriters.add(uuid)) {
                    return;
                }
                continue;
            }

            try {
                verbose("[SAVE] start uuid={} server={} fence={} generation={} revision={} reset={} release={}",
                        uuid, serverId, request.fenceToken(), request.generation(), request.revision(),
                        request.deleteBeforeWrite(), request.releaseAfterWrite());
                Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(uuid));
                databaseWriterSlots.acquire();
                Database.WriteOutcome outcome;
                try {
                    outcome = persist(request);
                } finally {
                    databaseWriterSlots.release();
                }
                if (!outcome.committed()) {
                    if (outcome.currentFenceToken() != request.fenceToken()) {
                        discardStaleFence(request, outcome);
                        return;
                    }
                    verbose("[SAVE] skipped old revision uuid={} fence={} revision={} currentRevision={}",
                            uuid, request.fenceToken(), request.revision(), outcome.currentRevision());
                    continue;
                }
                verbose("[SAVE] committed uuid={} server={} fence={} generation={} revision={}",
                        uuid, serverId, request.fenceToken(), request.generation(), request.revision());
                if (request.releaseAfterWrite()) {
                    finishHandoff(request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pendingWrites.merge(uuid, request, PlayerSkillsManager::mergeRequests);
                runningWriters.remove(uuid);
                return;
            } catch (Exception e) {
                pendingWrites.merge(uuid, request, PlayerSkillsManager::mergeRequests);
                runningWriters.remove(uuid);
                log.warn("[SAVE] uuid={} fence={} generation={} revision={} failed; retained for retry: {}",
                        uuid, request.fenceToken(), request.generation(), request.revision(), e.getMessage(), e);
                return;
            }
        }
        runningWriters.remove(uuid);
    }

    private Database.WriteOutcome persist(SaveRequest request) {
        if (!persistenceAvailable()) {
            throw new IllegalStateException("Database unavailable");
        }
        return Database.getInstance().persistPlayer(request.uuid(), serverId, request.fenceToken(),
                request.revision(), request.rows(), request.deleteBeforeWrite(), request.releaseAfterWrite());
    }

    private void discardStaleFence(SaveRequest request, Database.WriteOutcome outcome) {
        pendingWrites.computeIfPresent(request.uuid(), (uuid, pending) ->
                pending.fenceToken() == request.fenceToken() ? null : pending);
        runningWriters.remove(request.uuid());
        log.warn("[SAVE] rejected stale write uuid={} server={} ours={} current={} generation={} revision={} currentRevision={}",
                request.uuid(), serverId, request.fenceToken(), outcome.currentFenceToken(),
                request.generation(), request.revision(), outcome.currentRevision());
        invalidateStaleSession(request.uuid(), request.generation(), outcome.currentFenceToken());
    }

    private void finishHandoff(SaveRequest request) {
        Ownership ownership = ownerships.get(request.uuid());
        if (ownership != null && ownership.fenceToken() == request.fenceToken()) {
            ownerships.remove(request.uuid(), ownership);
        }
        if (Bukkit.getPlayer(request.uuid()) != null) {
            Bukkit.getScheduler().runTask(plugin, () -> scheduleLoadIfNeeded(request.uuid()));
        } else {
            scheduleStateCleanup(request.uuid());
        }
    }

    private void invalidateStaleSession(UUID uuid, long generation, long currentFence) {
        Ownership ownership = ownerships.get(uuid);
        if (ownership != null && ownership.generation() == generation) {
            ownerships.remove(uuid, ownership);
        }
        loadedPlayers.remove(uuid);
        pendingLoads.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.kick(LOST_SESSION_MESSAGE);
            }
        });
        verbose("[LOCK] invalidated uuid={} server={} generation={} currentFence={}",
                uuid, serverId, generation, currentFence);
    }

    public void flushPendingSavesAsync() {
        if (!persistenceAvailable()) {
            return;
        }
        new ArrayList<>(pendingWrites.keySet()).forEach(this::startWriter);
    }

    public void saveAll() {
        saveAllAsync();
    }

    /** Flushes a stable source snapshot before the explicit DB migration command copies it. */
    public void flushAllForMigration() {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Database migration flush must run asynchronously");
        }
        CompletableFuture<List<SaveRequest>> captured = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Map<UUID, SaveRequest> latest = new HashMap<>(pendingWrites);
            for (UUID uuid : new ArrayList<>(loadedPlayers)) {
                SaveRequest request = captureSave(uuid, false, false);
                if (request != null) {
                    latest.merge(uuid, request, PlayerSkillsManager::mergeRequests);
                    pendingWrites.merge(uuid, request, PlayerSkillsManager::mergeRequests);
                }
            }
            captured.complete(List.copyOf(latest.values()));
        });

        try {
            for (SaveRequest request : captured.get()) {
                Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(request.uuid()));
                databaseWriterSlots.acquire();
                Database.WriteOutcome outcome;
                try {
                    outcome = persist(request);
                } finally {
                    databaseWriterSlots.release();
                }
                if (!outcome.committed() && outcome.currentFenceToken() != request.fenceToken()) {
                    discardStaleFence(request, outcome);
                    continue;
                }
                pendingWrites.computeIfPresent(request.uuid(), (uuid, pending) ->
                        pending.fenceToken() == request.fenceToken()
                                && pending.revision() <= request.revision() ? null : pending);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while flushing player data", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not flush player data before migration", e);
        }
    }

    public void saveAllAsync() {
        if (!Bukkit.isPrimaryThread()) {
            if (autosaveScanRunning.compareAndSet(false, true)) {
                Bukkit.getScheduler().runTask(plugin, this::captureAutosave);
            }
            return;
        }
        if (autosaveScanRunning.compareAndSet(false, true)) {
            captureAutosave();
        }
    }

    private void captureAutosave() {
        try {
            for (UUID uuid : new ArrayList<>(loadedPlayers)) {
                SaveRequest request = captureSave(uuid, false, false);
                if (request != null) {
                    enqueue(request);
                }
            }
            flushPendingSavesAsync();
        } finally {
            autosaveScanRunning.set(false);
        }
    }

    /** Called by the independent async heartbeat task. */
    public void heartbeat() {
        if (!persistenceAvailable() || serverId == null || shuttingDown) {
            return;
        }
        try {
            int refreshed = Database.getInstance().heartbeatAll(serverId);
            verbose("[LOCK] heartbeat refreshed server={} sessions={}", serverId, refreshed);
        } catch (Exception e) {
            log.warn("[LOCK] heartbeat failed for server {}: {}", serverId, e.getMessage(), e);
        }
    }

    /** Shutdown may block briefly; normal gameplay save paths never do SQL on the primary thread. */
    public void shutdown() {
        shuttingDown = true;
        long deadline = System.nanoTime() + Duration.ofSeconds(
                plugin.getConfig().getLong("database.shutdown-flush-timeout", 10L)).toNanos();
        for (UUID uuid : new ArrayList<>(loadedPlayers)) {
            SaveRequest request = captureSave(uuid, true, false);
            if (request != null) {
                pendingWrites.merge(uuid, request, PlayerSkillsManager::mergeRequests);
            }
        }

        while (!runningWriters.isEmpty() && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        for (UUID uuid : new ArrayList<>(pendingWrites.keySet())) {
            if (!persistenceAvailable() || System.nanoTime() >= deadline) {
                break;
            }
            SaveRequest request = pendingWrites.remove(uuid);
            try {
                Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(uuid));
                Database.WriteOutcome outcome = persist(request);
                if (!outcome.committed()) {
                    log.warn("[SHUTDOWN] discarded stale write uuid={} fence={} current={}",
                            uuid, request.fenceToken(), outcome.currentFenceToken());
                }
            } catch (Exception e) {
                log.warn("[SHUTDOWN] failed to flush uuid={}: {}", uuid, e.getMessage());
            }
        }
        if (persistenceAvailable() && serverId != null) {
            Database.getInstance().releaseAllLocks(serverId);
        }
        pendingWrites.clear();
        runningWriters.clear();
        ownerships.clear();
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

    /** Replaces loaded state without replacing its lease metadata, then marks it dirty. */
    public void putSkills(UUID uuid, Skills skills) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> putSkills(uuid, skills));
            return;
        }
        if (!loadedPlayers.contains(uuid) || !ownerships.containsKey(uuid)) {
            log.warn("[PUT] rejected cache replacement for unloaded player {}", uuid);
            return;
        }
        skills.setUuid(uuid);
        activeSkills.put(uuid, skills);
        savePlayerAsync(uuid);
    }

    public void resetPlayerData(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> resetPlayerData(uuid));
            return;
        }
        if (loadedPlayers.contains(uuid) && ownerships.containsKey(uuid)) {
            Skills reset = new Skills();
            reset.setUuid(uuid);
            activeSkills.put(uuid, reset);
            SaveRequest request = captureSave(uuid, false, true);
            if (request != null) {
                enqueue(request);
                verbose("[RESET] queued uuid={} fence={} generation={} revision={}",
                        uuid, request.fenceToken(), request.generation(), request.revision());
            }
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!Database.getInstance().resetOfflinePlayer(uuid, leaseTimeoutMillis())) {
                    log.warn("[RESET] refused offline reset for {} because another server owns a live session", uuid);
                }
            } catch (Exception e) {
                log.warn("[RESET] offline reset failed for {}: {}", uuid, e.getMessage(), e);
            }
        });
    }

    /** Loaded state is handed off safely; placeholders can be removed immediately. */
    public void removeFromCache(UUID uuid) {
        if (loadedPlayers.contains(uuid)) {
            savePlayerNowOnQuit(uuid);
            return;
        }
        activeSkills.remove(uuid);
        pendingLoads.remove(uuid);
        clearLoadingTitleState(uuid);
        scheduleStateCleanup(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return loadedPlayers.contains(uuid);
    }

    private void releaseOwnership(UUID uuid, Ownership ownership) {
        if (persistenceAvailable()) {
            Database.getInstance().releasePlayerSession(uuid, serverId, ownership.fenceToken());
        }
        ownerships.remove(uuid, ownership);
        verbose("[LOCK] released uuid={} server={} fence={} generation={}",
                uuid, serverId, ownership.fenceToken(), ownership.generation());
    }

    private Skills getOrCreateSkills(UUID uuid) {
        Skills existing = activeSkills.get(uuid);
        if (existing != null) {
            existing.setUuid(uuid);
            return existing;
        }
        Skills created = new Skills();
        created.setUuid(uuid);
        Skills raced = activeSkills.putIfAbsent(uuid, created);
        return raced == null ? created : raced;
    }

    private Skills buildSkillsFromRows(UUID uuid, List<SkillRow> rows) {
        List<Skill> loaded = new ArrayList<>();
        for (SkillRow row : rows) {
            SkillInfo info = SkillsManager.getInstance().getSkillInfo(row.getType());
            if (info == null) {
                log.warn("[LOAD] {} has unknown skill type '{}'", uuid, row.getType());
                continue;
            }
            // totalXp is the source of truth. Level/current XP are derived caches.
            Skill skill = new Skill(info, 0);
            skill.addXp(row.getTotalXp());
            skill.levelUp(false);
            loaded.add(skill);
        }
        Skills skills = new Skills(loaded);
        skills.setUuid(uuid);
        return skills;
    }

    private List<SkillRow> buildRows(UUID uuid, Skills skills) {
        List<SkillRow> rows = new ArrayList<>();
        for (Skill skill : skills) {
            rows.add(new SkillRow(uuid.toString(), skill.getType().getId(), skill.getXp(),
                    skill.getLevel(), skill.getTotalXp()));
        }
        return rows;
    }

    private Skills copySkills(Skills source) {
        List<Skill> copies = new ArrayList<>();
        for (Skill skill : source) {
            Skill copy = new Skill(skill.getType(), skill.getLevel(), skill.getXp(),
                    skill.getXpToLevel(), skill.getTotalXp());
            copy.setUuid(source.getUuid());
            copies.add(copy);
        }
        Skills snapshot = new Skills(copies);
        snapshot.setUuid(source.getUuid());
        return snapshot;
    }

    private void scheduleLoadingTitle(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> scheduleLoadingTitle(uuid));
            return;
        }
        if (loadedPlayers.contains(uuid) || Bukkit.getPlayer(uuid) == null) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadingTitleTasks.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !loadedPlayers.contains(uuid)) {
                loadingTitleShown.add(uuid);
                player.showTitle(LOADING_TITLE_TEMPLATE);
            }
        }, LOADING_TITLE_DELAY_TICKS);
        BukkitTask previous = loadingTitleTasks.putIfAbsent(uuid, task);
        if (previous != null) {
            task.cancel();
        }
    }

    private void showLoadedTitleIfNeeded(UUID uuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            cancelLoadingTitleTask(uuid);
            if (!loadingTitleShown.remove(uuid)) {
                return;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.showTitle(LOADED_TITLE_TEMPLATE);
            }
        });
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

    private boolean hasInFlightState(UUID uuid) {
        return scheduledLoads.containsKey(uuid) || pendingWrites.containsKey(uuid)
                || runningWriters.contains(uuid) || ownerships.containsKey(uuid);
    }

    private void cancelStateCleanup(UUID uuid) {
        BukkitTask task = stateCleanupTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void scheduleStateCleanup(UUID uuid) {
        if (!Bukkit.isPrimaryThread()) {
            if (!shuttingDown) {
                Bukkit.getScheduler().runTask(plugin, () -> scheduleStateCleanup(uuid));
            }
            return;
        }
        cancelStateCleanup(uuid);
        long generation = currentGeneration(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stateCleanupTasks.remove(uuid);
            if (Bukkit.getPlayer(uuid) != null || hasInFlightState(uuid)
                    || !currentGeneration(uuid, generation)) {
                scheduleStateCleanup(uuid);
                return;
            }
            activeSkills.remove(uuid);
            loadedPlayers.remove(uuid);
            pendingLoads.remove(uuid);
            clearLoadingTitleState(uuid);
            loadGenerations.remove(uuid, generation);
            ReentrantLock lock = playerStateLocks.get(uuid);
            if (lock != null && !lock.isLocked() && !lock.hasQueuedThreads()) {
                playerStateLocks.remove(uuid, lock);
            }
        }, STATE_CLEANUP_DELAY_TICKS);
        stateCleanupTasks.put(uuid, task);
    }

    record SaveRequest(UUID uuid, List<SkillRow> rows, long generation, long fenceToken,
                       long revision, boolean deleteBeforeWrite, boolean releaseAfterWrite) {
    }

    private record Ownership(long generation, long fenceToken, AtomicLong nextRevision) {
    }
}
