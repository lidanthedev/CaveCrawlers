package me.lidan.cavecrawlers.storage.db;

import org.jdbi.v3.core.Handle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Award receipts commit in the same fenced transaction as their skill XP. */
public final class SkillAwardReceipts extends PlayerDataSqlTable {
    public static final SkillAwardReceipts INSTANCE = new SkillAwardReceipts();
    private final Map<UUID, Map<UUID, Receipt>> players = new ConcurrentHashMap<>();
    public CompletableFuture<Boolean> existing(UUID player, UUID award) {
        Receipt receipt = players.getOrDefault(player, Map.of()).get(award);
        return receipt == null ? null : receipt.result;
    }
    public CompletableFuture<Boolean> reserve(UUID player, UUID award, String skill, double totalXp) {
        Receipt receipt = new Receipt(skill, totalXp, new CompletableFuture<>());
        Receipt previous = players.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>()).putIfAbsent(award, receipt);
        return (previous == null ? receipt : previous).result;
    }
    @Override public String getTableName() { return "skill_awards"; }
    @Override public int getVersion() { return 1; }
    @Override public String getCreateCommand() {
        return "CREATE TABLE IF NOT EXISTS skill_awards (player_uuid VARCHAR(36) NOT NULL, award_id VARCHAR(36) NOT NULL, PRIMARY KEY (player_uuid, award_id))";
    }
    @Override public void onCreate(Handle handle) { handle.execute(getCreateCommand()); }
    @Override public void onUpgrade(Handle handle, int oldVersion, int newVersion) { }
    @Override public void loadForPlayer(Handle handle, UUID player) {
        Map<UUID, Receipt> receipts = new ConcurrentHashMap<>();
        for (String id : ids(handle, player)) receipts.put(UUID.fromString(id), new Receipt("", 0, CompletableFuture.completedFuture(true)));
        Map<UUID, Receipt> previous = players.put(player, receipts);
        if (previous != null) previous.values().forEach(receipt -> receipt.result.complete(false));
    }
    @Override public void saveForPlayer(Handle handle, UUID player) {
        for (var entry : players.getOrDefault(player, Map.of()).entrySet()) {
            Receipt receipt = entry.getValue();
            if (receipt.result.isDone()) continue;
            double persisted = handle.createQuery("SELECT total_xp FROM skills WHERE player_uuid = :player AND type = :skill")
                    .bind("player", player.toString()).bind("skill", receipt.skill).mapTo(Double.class).findFirst().orElse(-1D);
            // An older queued snapshot must not acknowledge a newer award.
            if (persisted < receipt.totalXp) continue;
            handle.createUpdate("INSERT INTO skill_awards (player_uuid, award_id) VALUES (:player, :award) ON DUPLICATE KEY UPDATE award_id = award_id")
                    .bind("player", player.toString()).bind("award", entry.getKey().toString()).execute();
        }
    }
    public void confirmCommitted(UUID player, Database database) {
        if (players.getOrDefault(player, Map.of()).values().stream().noneMatch(r -> !r.result.isDone())) return;
        database.getJdbi().useHandle(handle -> {
            for (String id : ids(handle, player)) {
                Receipt receipt = players.getOrDefault(player, Map.of()).get(UUID.fromString(id));
                if (receipt != null) receipt.result.complete(true);
            }
        });
    }
    private java.util.List<String> ids(Handle handle, UUID player) {
        return handle.createQuery("SELECT award_id FROM skill_awards WHERE player_uuid = :player")
                .bind("player", player.toString()).mapTo(String.class).list();
    }
    @Override public void resetForPlayer(Handle handle, UUID player) {
        // Keep committed receipt history: resetting a skill must not replay old awards.
    }
    private record Receipt(String skill, double totalXp, CompletableFuture<Boolean> result) { }
}
