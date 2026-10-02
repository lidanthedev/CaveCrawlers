package me.lidan.cavecrawlers.storage;

import java.util.function.LongSupplier;

/** Epochs permanently revoke sessions that crossed a local lease deadline. */
final class LeaseHealth {
    private final LongSupplier clock;
    private final long unsafeNanos;
    private long lastSuccess;
    private long epoch;
    private boolean healthy;

    LeaseHealth(LeaseConfig config, LongSupplier clock) {
        this.clock = clock;
        this.unsafeNanos = config.unsafeAfter().toNanos();
    }

    synchronized boolean healthy() {
        if (healthy && elapsedNanos() >= unsafeNanos) {
            healthy = false;
            epoch++;
        }
        return healthy;
    }

    synchronized long epoch() {
        healthy();
        return epoch;
    }

    synchronized long elapsedNanos() {
        return clock.getAsLong() - lastSuccess;
    }

    synchronized void succeeded(long startedAt) {
        // Evaluate the OLD deadline first, including when the JVM or SQL thread stalled.
        healthy();
        if (clock.getAsLong() - startedAt >= unsafeNanos) {
            return;
        }
        // SQL time is sampled before the response arrives. Completion time is not lease time.
        lastSuccess = startedAt;
        healthy = true;
    }
}
