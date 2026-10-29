package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.storage.db.SkillRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSkillsManagerOrderingTest {
    @Test
    void coalescingKeepsLatestSnapshotAndEarlierBarriers() {
        UUID uuid = UUID.randomUUID();
        var revision2 = request(uuid, 2, 200, true, false);
        var revision3 = request(uuid, 3, 300, false, true);
        var revision4 = request(uuid, 4, 400, false, false);

        var merged = PlayerSkillsManager.mergeRequests(
                PlayerSkillsManager.mergeRequests(revision2, revision3), revision4);

        assertEquals(4, merged.revision());
        assertEquals(400, merged.rows().getFirst().getTotalXp());
        assertTrue(merged.deleteBeforeWrite());
        assertTrue(merged.releaseAfterWrite());
    }

    @Test
    void coalescingNeverMixesDifferentFences() {
        UUID uuid = UUID.randomUUID();
        var oldOwner = new PlayerSkillsManager.SaveRequest(uuid,
                List.of(new SkillRow(uuid.toString(), "mining", 900, 0, 900)),
                7, 41, 99, true, true);
        var newOwner = new PlayerSkillsManager.SaveRequest(uuid,
                List.of(new SkillRow(uuid.toString(), "mining", 1_000, 0, 1_000)),
                8, 42, 12, false, false);

        var merged = PlayerSkillsManager.mergeRequests(oldOwner, newOwner);

        assertSame(newOwner, merged);
        assertFalse(merged.deleteBeforeWrite());
        assertFalse(merged.releaseAfterWrite());
    }

    private static PlayerSkillsManager.SaveRequest request(UUID uuid, long revision, double xp,
                                                            boolean reset, boolean release) {
        return new PlayerSkillsManager.SaveRequest(uuid,
                List.of(new SkillRow(uuid.toString(), "mining", xp, 0, xp)),
                7, 42, revision, reset, release);
    }
}
