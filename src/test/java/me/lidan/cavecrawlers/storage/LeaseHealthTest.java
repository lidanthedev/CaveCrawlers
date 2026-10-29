package me.lidan.cavecrawlers.storage;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LeaseHealthTest {
    @Test
    void configAlwaysLeavesConservativeSafetyWindow() {
        for (long timeout : new long[]{Long.MIN_VALUE, 0, 1, 10, 60, Long.MAX_VALUE}) {
            for (long heartbeat : new long[]{Long.MIN_VALUE, 0, 1, 10, Long.MAX_VALUE}) {
                LeaseConfig config = LeaseConfig.validated(timeout, heartbeat, ignored -> {});
                assertTrue(config.heartbeatInterval().toNanos() > 0);
                assertTrue(config.unsafeAfter().compareTo(config.heartbeatInterval()) > 0);
                assertTrue(config.unsafeAfter().compareTo(config.timeout()) < 0);
                assertTrue(config.timeout().minus(config.unsafeAfter()).compareTo(config.heartbeatInterval().multipliedBy(2)) >= 0);
            }
        }
        assertEquals(Duration.ofSeconds(40), LeaseConfig.validated(60, 10, ignored -> {}).unsafeAfter());
    }

    @Test
    void monotonicWrapAndLateSuccessDoNotResurrectOldEpoch() {
        AtomicLong now = new AtomicLong(Long.MAX_VALUE - Duration.ofSeconds(20).toNanos());
        LeaseHealth health = new LeaseHealth(LeaseConfig.validated(60, 10, ignored -> {}), now::get);
        health.succeeded(now.get());
        long epoch = health.epoch();
        now.addAndGet(Duration.ofSeconds(39).toNanos());
        assertTrue(health.healthy());
        now.addAndGet(Duration.ofSeconds(2).toNanos());
        health.succeeded(now.get());
        assertTrue(health.healthy());
        assertEquals(epoch + 1, health.epoch());
    }
}
