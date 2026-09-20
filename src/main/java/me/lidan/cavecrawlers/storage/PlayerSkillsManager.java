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
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

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
    private static final Component DATABASE_LOST_MESSAGE = MiniMessageUtils.miniMessage(
            "<red>Lost connection to the player data database.<newline><gray>Please reconnect in a moment.");
    private static final Component DATABASE_LOAD_FAILED_MESSAGE = MiniMessageUtils.miniMessage(
            "<red>Could not load your player data safely.<newline><gray>Please reconnect in a moment.");
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
    private final ConcurrentHashMap<UUID, Ownership> pendingReleases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, SaveRequest> pendingWrites = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<UUID, Boolean> runningWriters = ConcurrentHashMap.newKeySet();
    // Leave one of the five pool connections available for lease heartbeats.
    private final Semaphore databaseWriterSlots = new Semaphore(4);
    private final AtomicBoolean migrationInProgress = new AtomicBoolean();
    private final AtomicBoolean autosaveScanRunning = new AtomicBoolean();
    private final Plugin plugin;
    private final Database database;
    @Getter
    private final LeaseConfig leaseConfig;
    private final LongSupplier clock;
    private final LeaseHealth leaseHealth;
    private final AtomicBoolean heartbeatRunning = new AtomicBoolean();
    private final AtomicBoolean heartbeatFailed = new AtomicBoolean();
    private final ConcurrentHashMap<UUID, Long> invalidatedGenerations = new ConcurrentHashMap<>();
    @Getter
    private volatile String serverId;
    private volatile boolean shuttingDown;
    private final Object ioMonitor = new Object();
    private int activeOperations;

    private PlayerSkillsManager() {
        this(CaveCrawlers.getInstance(), Database.getInstance(), System::nanoTime,
                LeaseConfig.validated(CaveCrawlers.getInstance().getConfig().getLong("database.lease-timeout", 60),
                        CaveCrawlers.getInstance().getConfig().getLong("database.heartbeat-interval", 10), log::warn));
    }

    PlayerSkillsManager(Plugin plugin, Database database, LongSupplier clock, LeaseConfig leaseConfig) {
        this.plugin = plugin;
        this.database = database;
        this.clock = clock;
        this.leaseConfig = leaseConfig;
        this.leaseHealth = new LeaseHealth(leaseConfig, clock);
    }

    public static synchronized PlayerSkillsManager getInstance() {
        if (instance == null) {
            instance = new PlayerSkillsManager();
        }
        return instance;
    }

    /** Rotate the process ID only after every lease owned by the current ID is released. */
    public void setServerId() {
        if (ownerships.isEmpty()) serverId = UUID.randomUUID().toString();
        shuttingDown = false;
        loadedPlayers.clear();
        scheduledLoads.clear();
        invalidatedGenerations.clear();
    }

    private long leaseTimeoutMillis() {
        return leaseConfig.timeout().toMillis();
    }

    private boolean verboseLoggingEnabled() {
        return plugin.getConfig().getBoolean("database.verbose-logging", false);
    }

    private void verbose(String message, Object... args) {
        if (verboseLoggingEnabled()) {
            log.info(message, args);
        }
    }

    private boolean persistenceAvailable() {
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
        requirePrimaryThread();
        if (ownerships.containsKey(uuid) || scheduledLoads.containsKey(uuid)) {
            savePlayerNowOnQuit(uuid);
        }
        invalidatedGenerations.remove(uuid);
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
        if (!Bukkit.isPrimaryThread()) {
            if (!shuttingDown) Bukkit.getScheduler().runTask(plugin, () -> scheduleLoadIfNeeded(uuid));
            return;
        }
        cancelStateCleanup(uuid);
        getOrCreateSkills(uuid);
        if (shuttingDown || loadedPlayers.contains(uuid) || invalidatedGenerations.containsKey(uuid)) {
            return;
        }
        if (migrationInProgress.get()
                || (plugin instanceof CaveCrawlers caveCrawlers && !caveCrawlers.isLoginAllowed())) {
            pendingLoads.add(uuid);
            scheduleLoadRetry(uuid, currentGeneration(uuid));
            return;
        }
        if (!persistenceAvailable() || !leaseHealth.healthy()) {
            invalidatePlayer(uuid, currentGeneration(uuid), DATABASE_LOAD_FAILED_MESSAGE, false);
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
        if (!beginOperation()) return;
        boolean publicationQueued = false;
        Ownership ownership = null;
        ReentrantLock lock = stateLock(uuid);
        lock.lock();
        try {
            if (!persistenceAvailable()) {
                failLoad(uuid, generation, null);
                return;
            }
            ownership = acquireOwnership(uuid, generation);
            if (ownership == null) {
                failLoad(uuid, generation, null);
                return;
            }

            List<SkillRow> rows = database
                    .loadPlayer(uuid, serverId, ownership.fenceToken());
            Ownership loadedOwnership = ownership;
            verbose("[LOAD] uuid={} server={} fence={} generation={} dataRevision={} rows={}",
                    uuid, serverId, ownership.fenceToken(), generation,
                    ownership.nextRevision().get(), rows.size());
            if (verboseLoggingEnabled()) {
                for (SkillRow row : rows) {
                    verbose("[LOAD] db-row uuid={} type={} storedLevel={} storedXp={} totalXp={}",
                            uuid, row.getType(), row.getLevel(), row.getXp(), row.getTotalXp());
                }
            }

            if (shuttingDown) return;
            Skills loaded = buildSkillsFromRows(uuid, rows);
            // Publication and quit both run on the main thread. Never publish from a late SQL callback.
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean superseded = !currentGeneration(uuid, generation);
                try {
                    if (shuttingDown) return;
                    if (superseded || Bukkit.getPlayer(uuid) == null
                            || ownerships.get(uuid) != loadedOwnership
                            || !Objects.equals(loadedOwnership.serverId(), serverId)
                            || !leaseHealth.healthy()
                            || loadedOwnership.healthEpoch() != leaseHealth.epoch()
                            || invalidatedGenerations.containsKey(uuid)) {
                        queueOwnershipRelease(uuid, loadedOwnership);
                        return;
                    }
                    bindSkills(uuid, loaded);
                    activeSkills.put(uuid, loaded);
                    loadedOwnership.dataLoaded().set(true);
                    loadedPlayers.add(uuid);
                    pendingLoads.remove(uuid);
                    Bukkit.getPluginManager().callEvent(new PlayerDataLoadEvent(uuid));
                    showLoadedTitleIfNeeded(uuid);
                } finally {
                    scheduledLoads.remove(uuid, generation);
                    if (superseded && !shuttingDown && Bukkit.getPlayer(uuid) != null) {
                        scheduleLoadIfNeeded(uuid);
                    }
                }
            });
            publicationQueued = true;
        } catch (Database.StaleSessionException e) {
            invalidateStaleSession(uuid, generation, e.currentFence());
        } catch (Exception e) {
            log.warn("[LOAD] uuid={} generation={} failed: {}", uuid, generation, e.getMessage(), e);
            failLoad(uuid, generation, ownership);
        } finally {
            lock.unlock();
            if (!publicationQueued) scheduledLoads.remove(uuid, generation);
            if (!shuttingDown && Bukkit.getPlayer(uuid) == null) {
                scheduleStateCleanup(uuid);
            }
            endOperation();
        }
    }

    private Ownership acquireOwnership(UUID uuid, long generation) {
        Ownership existing = ownerships.get(uuid);
        if (existing != null) {
            return existing.generation() == generation && Objects.equals(existing.serverId(), serverId)
                    ? existing : null;
        }

        for (int attempt = 0; attempt <= LOCK_MAX_ATTEMPTS; attempt++) {
            if (shuttingDown || !persistenceAvailable() || !leaseHealth.healthy() || !currentGeneration(uuid, generation)) {
                return null;
            }
            long healthEpoch = leaseHealth.epoch();
            long startedAt = clock.getAsLong();
            Optional<Database.PlayerLease> lease = database
                    .acquirePlayerSession(uuid, serverId, leaseTimeoutMillis());
            if (lease.isPresent()) {
                Ownership acquired = new Ownership(serverId, generation, lease.get().fenceToken(),
                        new AtomicLong(lease.get().dataRevision()), healthEpoch, new AtomicReference<>(),
                        new AtomicBoolean(), new AtomicBoolean());
                if (shuttingDown || clock.getAsLong() - startedAt >= leaseConfig.unsafeAfter().toNanos()
                        || healthEpoch != leaseHealth.epoch()) {
                    database.releasePlayerSession(uuid, serverId, acquired.fenceToken());
                    return null;
                }
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

    private void failLoad(UUID uuid, long generation, Ownership ownership) {
        if (ownership != null) queueOwnershipRelease(uuid, ownership);
        Bukkit.getScheduler().runTask(plugin,
                () -> invalidatePlayer(uuid, generation, DATABASE_LOAD_FAILED_MESSAGE, false));
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
            long generation = currentGeneration(uuid);
            if (!shuttingDown) Bukkit.getScheduler().runTask(plugin, () -> {
                if (currentGeneration(uuid, generation)) savePlayerNow(uuid, releaseAfterSave);
            });
            return;
        }
        if (!canPersistPlayer(uuid)) return;
        SaveRequest request = captureSave(uuid, releaseAfterSave, false);
        if (request != null) {
            if (releaseAfterSave) loadedPlayers.remove(uuid);
            enqueue(request);
        }
    }

    public void savePlayerNowOnQuit(UUID uuid) {
        requirePrimaryThread();

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
        } else if (!pendingWrites.containsKey(uuid) && !runningWriters.contains(uuid)) {
            Ownership ownership = ownerships.get(uuid);
            if (ownership != null) queueOwnershipRelease(uuid, ownership);
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
        if (!hasCurrentLoadProof(uuid, ownership) || skills == null) {
            return null;
        }
        long revision = ownership.nextRevision().incrementAndGet();
        List<SkillRow> rows = List.copyOf(buildRows(uuid, copySkills(skills)));
        verbose("[SAVE] snapshot uuid={} server={} fence={} generation={} revision={} reset={} release={} rows={}",
                uuid, serverId, ownership.fenceToken(), ownership.generation(), revision,
                deleteBeforeWrite, releaseAfterSave, rows.size());
        if (verboseLoggingEnabled()) {
            for (SkillRow row : rows) {
                verbose("[SAVE] snapshot-row uuid={} revision={} type={} level={} xp={} totalXp={}",
                        uuid, revision, row.getType(), row.getLevel(), row.getXp(), row.getTotalXp());
            }
        }
        return new SaveRequest(uuid, rows,
                ownership.generation(), ownership.fenceToken(), revision,
                deleteBeforeWrite, releaseAfterSave);
    }

    void enqueue(SaveRequest request) {
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
        if (!beginOperation()) {
            runningWriters.remove(uuid);
            return;
        }
        try {
            drainWriterQueue(uuid);
        } finally {
            endOperation();
        }
    }

    private void drainWriterQueue(UUID uuid) {
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
                Ownership owner = ownerships.get(uuid);
                if (owner != null && owner.fenceToken() == request.fenceToken() && outcome.owned()) {
                    writeHealthy(owner); // Latch an expired failure window before recording recovery.
                    owner.writeFailureSince().set(null);
                }
                if (!outcome.committed()) {
                    if (!outcome.owned()) {
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
                Ownership owner = ownerships.get(uuid);
                if (owner != null && owner.fenceToken() == request.fenceToken()) {
                    owner.writeFailureSince().compareAndSet(null, clock.getAsLong());
                }
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
        Ownership ownership = ownerships.get(request.uuid());
        if (ownership == null || !ownership.dataLoaded().get()
                || ownership.generation() != request.generation()
                || ownership.fenceToken() != request.fenceToken()
                || !Objects.equals(ownership.serverId(), serverId)) {
            throw new IllegalStateException("Refusing to write player data before its database load completed");
        }
        return database.persistPlayer(request.uuid(), serverId, request.fenceToken(),
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
            if (currentGeneration(request.uuid(), request.generation())) loadedPlayers.remove(request.uuid());
        }
        if (shuttingDown) return;
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
        Bukkit.getScheduler().runTask(plugin, () -> invalidatePlayer(uuid, generation, LOST_SESSION_MESSAGE, false));
        verbose("[LOCK] invalidated uuid={} server={} generation={} currentFence={}",
                uuid, serverId, generation, currentFence);
    }

    private void invalidatePlayer(UUID uuid, long generation, Component message, boolean retainSnapshot) {
        if (shuttingDown || !currentGeneration(uuid, generation)
                || invalidatedGenerations.putIfAbsent(uuid, generation) != null) return;
        if (retainSnapshot) {
            log.warn("[LOCK] lease unsafe uuid={} server={} elapsedMs={} timeout={} interval={} owned={}",
                    uuid, serverId, leaseHealth.elapsedNanos() / 1_000_000,
                    leaseConfig.timeout(), leaseConfig.heartbeatInterval(), ownerships.size());
            SaveRequest request = captureSave(uuid, true, false);
            if (request != null) {
                enqueue(request);
            } else if (!pendingWrites.containsKey(uuid) && !runningWriters.contains(uuid)) {
                Ownership ownership = ownerships.get(uuid);
                if (ownership != null) queueOwnershipRelease(uuid, ownership);
            }
        }
        loadedPlayers.remove(uuid);
        pendingLoads.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.kick(message);
    }

    /** Runs every tick independently of SQL; guards also evaluate time after a stalled main thread. */
    public void checkLeaseHealth() {
        if (shuttingDown) return;
        long epoch = leaseHealth.epoch();
        for (var entry : ownerships.entrySet()) {
            Ownership ownership = entry.getValue();
            if (ownership.healthEpoch() != epoch || !writeHealthy(ownership)) {
                invalidatePlayer(entry.getKey(), ownership.generation(), DATABASE_LOST_MESSAGE, true);
            }
        }
    }

    public boolean canPersistPlayer(UUID uuid) {
        Ownership ownership = ownerships.get(uuid);
        return !shuttingDown && !migrationInProgress.get() && persistenceAvailable() && leaseHealth.healthy()
                && loadedPlayers.contains(uuid) && !invalidatedGenerations.containsKey(uuid)
                && hasCurrentLoadProof(uuid, ownership)
                && ownership.healthEpoch() == leaseHealth.epoch() && writeHealthy(ownership);
    }

    private boolean hasCurrentLoadProof(UUID uuid, Ownership ownership) {
        return ownership != null && ownership.dataLoaded().get()
                && ownership.generation() == currentGeneration(uuid)
                && Objects.equals(ownership.serverId(), serverId);
    }

    private boolean writeHealthy(Ownership ownership) {
        Long failedSince = ownership.writeFailureSince().get();
        if (failedSince != null && clock.getAsLong() - failedSince >= leaseConfig.unsafeAfter().toNanos()) {
            ownership.writeUnsafe().set(true);
        }
        return !ownership.writeUnsafe().get();
    }

    private void bindSkills(UUID uuid, Skills skills) {
        skills.bindMutationGuard(() -> Bukkit.isPrimaryThread() && activeSkills.get(uuid) == skills
                && canPersistPlayer(uuid));
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Player state must be mutated on the primary thread");
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

    /** Freezes this backend's mutations for the complete source-flush/target-copy operation. */
    public void withMigrationBarrier(Runnable copy) {
        if (Bukkit.isPrimaryThread()) throw new IllegalStateException("Migration must run asynchronously");
        if (!migrationInProgress.compareAndSet(false, true)) throw new IllegalStateException("Migration already running");
        try {
            flushAllForMigration();
            copy.run();
        } finally {
            migrationInProgress.set(false);
            if (!shuttingDown) Bukkit.getScheduler().runTask(plugin, this::scheduleLoadsForPendingPlayers);
        }
    }

    /** Flushes a stable source snapshot before the explicit DB migration command copies it. */
    public void flushAllForMigration() {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Database migration flush must run asynchronously");
        }
        CompletableFuture<Void> captured = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (shuttingDown) throw new IllegalStateException("Server is shutting down");
                captureAutosave();
                captured.complete(null);
            } catch (Exception e) {
                captured.completeExceptionally(e);
            }
        });
        try {
            captured.get(30, java.util.concurrent.TimeUnit.SECONDS);
            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (!pendingWrites.isEmpty() || !runningWriters.isEmpty() || hasActiveOperations()) {
                if (shuttingDown || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Player writes did not drain before migration");
                }
                flushPendingSavesAsync();
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while flushing player data", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not flush player data before migration", e);
        }
    }

    public void saveAllAsync() {
        if (shuttingDown || migrationInProgress.get()) return;
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
        if (serverId == null || shuttingDown || !persistenceAvailable()
                || !heartbeatRunning.compareAndSet(false, true)) return;
        if (!beginOperation()) {
            heartbeatRunning.set(false);
            return;
        }
        long startedAt = clock.getAsLong();
        try {
            int refreshed = database.heartbeatAll(serverId);
            leaseHealth.succeeded(startedAt);
            if (heartbeatFailed.getAndSet(false)) log.info("[LOCK] database heartbeat recovered server={}", serverId);
            verbose("[LOCK] heartbeat server={} refreshed={} elapsedMs={} timeout={} interval={} owned={}",
                    serverId, refreshed, leaseHealth.elapsedNanos() / 1_000_000,
                    leaseConfig.timeout(), leaseConfig.heartbeatInterval(), ownerships.size());
            flushPendingSavesAsync();
        } catch (Exception e) {
            leaseHealth.healthy();
            if (!heartbeatFailed.getAndSet(true)) {
                log.warn("[LOCK] heartbeat failed server={}: {}", serverId, e.getMessage());
            }
            verbose("[LOCK] heartbeat failure server={} elapsedMs={} timeout={} interval={} owned={}",
                    serverId, leaseHealth.elapsedNanos() / 1_000_000,
                    leaseConfig.timeout(), leaseConfig.heartbeatInterval(), ownerships.size());
        } finally {
            heartbeatRunning.set(false);
            endOperation();
        }
    }

    /** Shutdown may block briefly; normal gameplay save paths never do SQL on the primary thread. */
    public void shutdown() {
        requirePrimaryThread();
        synchronized (ioMonitor) { shuttingDown = true; }
        long deadline = System.nanoTime() + Duration.ofSeconds(Math.clamp(
                plugin.getConfig().getLong("database.shutdown-flush-timeout", 10L), 1L, 120L)).toNanos();
        for (UUID uuid : new ArrayList<>(loadedPlayers)) {
            SaveRequest request = captureSave(uuid, true, false);
            if (request != null) pendingWrites.merge(uuid, request, PlayerSkillsManager::mergeRequests);
        }
        synchronized (ioMonitor) {
            while (activeOperations > 0 && System.nanoTime() < deadline) {
                try { ioMonitor.wait(10); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            if (activeOperations > 0) {
                log.warn("[SHUTDOWN] {} operations still active; retaining {} snapshots and leaving leases to expire",
                        activeOperations, pendingWrites.size());
                return;
            }
        }
        // Scheduled tasks may have been cancelled before they started. They cannot enter SQL now.
        runningWriters.clear();
        for (UUID uuid : new ArrayList<>(pendingWrites.keySet())) {
            if (!persistenceAvailable() || System.nanoTime() >= deadline) break;
            SaveRequest request = pendingWrites.get(uuid);
            if (request == null) continue;
            try {
                Bukkit.getPluginManager().callEvent(new PlayerDataSaveEvent(uuid));
                Database.WriteOutcome outcome = persist(request);
                pendingWrites.remove(uuid, request);
                if (!outcome.committed() && !outcome.owned()) {
                    log.warn("[SHUTDOWN] rejected stale write uuid={} fence={} current={}",
                            uuid, request.fenceToken(), outcome.currentFenceToken());
                }
            } catch (Exception e) {
                log.warn("[SHUTDOWN] retained failed snapshot uuid={}: {}", uuid, e.getMessage());
            }
        }
        if (pendingWrites.isEmpty() && persistenceAvailable() && serverId != null) {
            database.releaseAllLocks(serverId);
            ownerships.clear();
        } else {
            log.warn("[SHUTDOWN] {} snapshots unflushed; leases will expire without bulk release", pendingWrites.size());
        }
        loadedPlayers.clear();
    }

    private boolean hasActiveOperations() {
        synchronized (ioMonitor) { return activeOperations > 0; }
    }

    private boolean beginOperation() {
        synchronized (ioMonitor) {
            if (shuttingDown) return false;
            activeOperations++;
            return true;
        }
    }

    private void endOperation() {
        synchronized (ioMonitor) { activeOperations--; ioMonitor.notifyAll(); }
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
        requirePrimaryThread();
        if (!canPersistPlayer(uuid)) {
            log.warn("[PUT] rejected cache replacement for unloaded player {}", uuid);
            return;
        }
        if (activeSkills.get(uuid) == skills) {
            savePlayerAsync(uuid);
            return;
        }
        skills.checkMutationAllowed();
        skills.setUuid(uuid);
        bindSkills(uuid, skills);
        activeSkills.put(uuid, skills);
        savePlayerAsync(uuid);
    }

    public void resetPlayerData(UUID uuid) {
        requirePrimaryThread();
        if (shuttingDown || migrationInProgress.get()) return;
        if (canPersistPlayer(uuid)) {
            Skills reset = new Skills();
            reset.setUuid(uuid);
            bindSkills(uuid, reset);
            activeSkills.put(uuid, reset);
            SaveRequest request = captureSave(uuid, false, true);
            if (request != null) {
                enqueue(request);
                verbose("[RESET] queued uuid={} fence={} generation={} revision={}",
                        uuid, request.fenceToken(), request.generation(), request.revision());
            }
            return;
        }

        if (Bukkit.getPlayer(uuid) != null || ownerships.containsKey(uuid)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!beginOperation()) return;
            try {
                if (migrationInProgress.get()) return;
                if (!database.resetOfflinePlayer(uuid, leaseTimeoutMillis())) {
                    log.warn("[RESET] refused offline reset for {} because another server owns a live session", uuid);
                }
            } catch (Exception e) {
                log.warn("[RESET] offline reset failed for {}: {}", uuid, e.getMessage(), e);
            } finally {
                endOperation();
            }
        });
    }

    /** Loaded state is handed off safely; placeholders can be removed immediately. */
    public void removeFromCache(UUID uuid) {
        requirePrimaryThread();
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
        return canPersistPlayer(uuid);
    }

    private void queueOwnershipRelease(UUID uuid, Ownership ownership) {
        if (shuttingDown || pendingReleases.putIfAbsent(uuid, ownership) != null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> retryOwnershipRelease(uuid, ownership));
    }

    private void retryOwnershipRelease(UUID uuid, Ownership ownership) {
        if (!beginOperation()) return;
        boolean retry = false;
        try {
            if (ownerships.get(uuid) == ownership) releaseOwnership(uuid, ownership);
            pendingReleases.remove(uuid, ownership);
        } catch (Exception e) {
            retry = true;
            verbose("[LOCK] release retained uuid={} fence={}: {}", uuid, ownership.fenceToken(), e.getMessage());
        } finally {
            endOperation();
        }
        if (retry && !shuttingDown) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> retryOwnershipRelease(uuid, ownership),
                    leaseConfig.heartbeatInterval().toSeconds() * 20);
        }
    }

    private void releaseOwnership(UUID uuid, Ownership ownership) {
        if (!persistenceAvailable()) throw new IllegalStateException("Database unavailable during release");
        database.releasePlayerSession(uuid, ownership.serverId(), ownership.fenceToken());
        ownerships.remove(uuid, ownership);
        verbose("[LOCK] released uuid={} server={} fence={} generation={}",
                uuid, serverId, ownership.fenceToken(), ownership.generation());
    }

    private Skills getOrCreateSkills(UUID uuid) {
        Skills existing = activeSkills.get(uuid);
        if (existing != null) {
            return existing;
        }
        Skills created = new Skills();
        created.setUuid(uuid);
        bindSkills(uuid, created);
        Skills raced = activeSkills.putIfAbsent(uuid, created);
        return raced == null ? created : raced;
    }

    private Skills buildSkillsFromRows(UUID uuid, List<SkillRow> rows) {
        List<Skill> loaded = new ArrayList<>();
        for (SkillRow row : rows) {
            SkillInfo info = SkillsManager.getInstance().getSkillInfo(row.getType());
            if (info == null) {
                log.warn("[LOAD] {} has unknown skill type '{}'", uuid, row.getType());
                verbose("[LOAD] skipped-row uuid={} type={} reason=skill-type-not-registered storedLevel={} storedXp={} totalXp={}",
                        uuid, row.getType(), row.getLevel(), row.getXp(), row.getTotalXp());
                continue;
            }
            // totalXp is the source of truth. Level/current XP are derived caches.
            Skill skill = new Skill(info, 0);
            if (!info.getXpToLevelList().isEmpty()) {
                skill.setXpToLevel(info.getXpToLevelList().getFirst());
            }
            skill.addXp(row.getTotalXp());
            skill.levelUp(false);
            loaded.add(skill);
            verbose("[LOAD] applied-row uuid={} type={} configuredLevels={} derivedLevel={} derivedXp={} derivedXpToLevel={} totalXp={}",
                    uuid, row.getType(), info.getXpToLevelList().size(), skill.getLevel(),
                    skill.getXp(), skill.getXpToLevel(), skill.getTotalXp());
        }
        Skills skills = new Skills(loaded);
        skills.setUuid(uuid);
        if (verboseLoggingEnabled()) {
            for (Skill skill : skills) {
                if (loaded.stream().noneMatch(restored -> restored == skill)) {
                    verbose("[LOAD] defaulted-skill uuid={} type={} reason=no-database-row level={} xp={} totalXp={}",
                            uuid, skill.getType().getId(), skill.getLevel(), skill.getXp(), skill.getTotalXp());
                }
            }
        }
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
            invalidatedGenerations.remove(uuid, generation);
            // Keep the generation tombstone: late callbacks must never match a future session.
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

    private record Ownership(String serverId, long generation, long fenceToken, AtomicLong nextRevision,
                             long healthEpoch, AtomicReference<Long> writeFailureSince,
                             AtomicBoolean writeUnsafe, AtomicBoolean dataLoaded) {
    }
}
