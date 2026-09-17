package me.lidan.cavecrawlers.storage;

import java.time.Duration;
import java.util.function.Consumer;

/** Validated once at startup; configuration reload requires a restart. */
public record LeaseConfig(Duration timeout, Duration heartbeatInterval, Duration unsafeAfter) {
    public LeaseConfig {
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()
                || timeout.compareTo(Duration.ofSeconds(10)) < 0
                || unsafeAfter.compareTo(heartbeatInterval) <= 0 || unsafeAfter.compareTo(timeout) >= 0) {
            throw new IllegalArgumentException("Unsafe lease configuration");
        }
    }

    public static LeaseConfig validated(long timeoutSeconds, long heartbeatSeconds, Consumer<String> warn) {
        // Bound arithmetic and leave at least two heartbeat intervals before lease expiry.
        long timeout = Math.clamp(timeoutSeconds, 10L, 86_400L);
        long heartbeat = Math.clamp(heartbeatSeconds, 1L, timeout / 4);
        if (timeout != timeoutSeconds || heartbeat != heartbeatSeconds) {
            warn.accept("Unsafe database lease configuration; using timeout=" + timeout
                    + "s, heartbeat=" + heartbeat + "s");
        }
        long margin = Math.max(2 * heartbeat, (timeout + 3) / 4);
        return new LeaseConfig(Duration.ofSeconds(timeout), Duration.ofSeconds(heartbeat),
                Duration.ofSeconds(timeout - margin));
    }
}
