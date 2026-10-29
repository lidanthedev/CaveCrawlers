# Player persistence

## Upgrade and deployment

All backends sharing a database must upgrade to the fencing protocol together. Stop every backend,
update every plugin, then restart the network. Pre-fencing versions bypass ownership validation and
can overwrite data written by fenced versions. Only consider rolling updates after all backends use
compatible fencing and schema versions. A newer stored schema version now rejects an older table implementation.

Use the same lease configuration on every backend. Acquisition, offline reset, heartbeat scheduling,
health checks and diagnostics use one `LeaseConfig`, validated once at startup. Configuration reload
requires a restart for these settings.

## Lease health and outages

The default lease timeout is 60 seconds and heartbeat interval is 10 seconds. Validation clamps the
timeout to 10 through 86,400 seconds, then clamps the heartbeat interval to 1 through timeout/4 seconds.
Invalid settings produce one startup warning.

The unsafe threshold is:

```
timeout - max(2 * heartbeatInterval, ceil(timeout / 4))
```

The defaults stop progression after 40 seconds without a successful heartbeat, leaving 20 seconds
before another backend can expire the database lease. This reserves two heartbeat intervals for
scheduling and network delays. MySQL remains the clock authority for leases. Local elapsed-time checks
use an injectable `System.nanoTime()` supplier.

Heartbeat success records the operation's monotonic **start** time. Using response time would wrongly
extend a lease after a stalled SQL call. The previous deadline is evaluated before success is recorded.
Crossing it increments a health epoch, permanently revoking sessions from the previous epoch. A late
response cannot restore those sessions.

Brief failures inside the safety window preserve the loaded session and queued snapshots. Heartbeat
failures log on transition, with per-attempt diagnostics available under verbose logging. Successful
heartbeats restart pending writers.

A main-thread watchdog runs every tick independently of SQL. Mutation guards also check elapsed time,
so they do not wait for the watchdog after a stalled main thread. Unsafe sessions reject XP, reset,
cache replacement, experimental level mutations, and mutations through previously returned `Skills` or `Skill` references. The watchdog
captures a final fenced snapshot, invalidates the session and disconnects the player once.

Repeated player-write failures have a separate per-session deadline using the same safety window.
This prevents healthy heartbeats from masking a permanently broken addon table or write permission.
Once this deadline is observed, that session also requires reconnecting. A successful write inside
the window clears the failure timer.

After recovery, retained snapshots use their original server ID, fence and revision. Transactions
accept them only if ownership still matches. Newer owners win. Revoked local sessions remain invalid;
reconnect goes through acquisition and loading again.

## Transaction and asynchronous ordering

- Acquisition and persistence lock the same session row with `SELECT ... FOR UPDATE`. Either a save
  commits before takeover, or takeover commits first and rejects the old save. The final quit write
  and release commit together.
- `data_revision` rejects older snapshots even within the same fence. Coalescing carries reset and quit
  flags onto the newest snapshot, without mixing fences.
- Load publication and quit run on the primary thread. The load remains marked in flight until
  publication, preventing duplicate late loads from overwriting progress. Publication checks the
  generation, ownership object and health epoch again.
- Delayed stale-write notifications only invalidate their original generation. Reconnect cannot reuse
  stale mutable references. Explicit reload first hands off the previous session.
- Failed/unpublished loads release their leases through a tracked, retryable fenced release. Quit during
  a failed load cannot leave a permanently heartbeating orphan lease.
- Migration flushing uses the same writer queue, avoiding a second writer that could bypass reset or
  quit barriers. A temporary mutation barrier covers the whole flush and target copy, and is removed
  on success or failure. This barrier covers the local backend. Stop other backends before a database
  cutover; the command creates a snapshot and does not switch the running database configuration.
- YAML and database imports lock the target session row and only import never-owned, never-reset
  players without existing skill data. A reset to empty state is authoritative. Imports cannot restore
  old YAML into that state. The import marker and skill insertion commit together.
- Online loads wait for legacy YAML migration. Failed files retry while the login/load gate stays closed.
  Deserialization derives levels from total XP without issuing historical rewards.
- Snapshot UUID validation prevents a fenced operation for one player from carrying another player's rows.

## Shutdown

Shutdown first rejects new mutations and admission of new database operations, then captures final
snapshots. It waits for already-admitted work, including writers, loads, releases, heartbeat and offline
reset. Cancelled scheduled writers cannot start SQL after this point.

Once active work has drained, shutdown synchronously flushes remaining snapshots, even when no writer
started before Bukkit cancelled its tasks. Bulk lease release happens only after all pending writes
have resolved. If active work outlasts the deadline or writes fail, snapshots remain in memory and bulk
release is skipped. The database closes in `finally`; remaining leases expire normally.

The configured deadline bounds waiting and admission of further flushes. It cannot interrupt every
in-progress JVM/JDBC operation at an exact instant. MySQL connections use 10-second connect/socket
timeouts. A single operation or connection-pool shutdown can therefore exceed the flush deadline.

## Addon contract

Live player-state changes must run on the primary thread and check `canPersistPlayer(uuid)`.
`putSkills`, reset, quit and direct skill mutation reject unsafe or stale state. Queue asynchronous
work with its original session context; do not fetch a newer session to apply an old task's result.

`PlayerDataLoadEvent` now fires on the primary thread after publication. SQL table callbacks remain
asynchronous and must use the supplied transaction handle. An addon exception rolls back skills,
addon writes, the revision update and quit release together. Save events are notifications before
SQL, not evidence that a write committed.

Custom addon caches and external side effects remain the addon's responsibility. Table callbacks
must preserve the data associated with the queued save across quit and retries, and must not perform
independent commits or issue rewards during loading. Fencing cannot protect arbitrary SQL issued by
an addon outside the supplied handle, or external systems that do not participate in the transaction.

## MySQL schema locks

`GET_LOCK`, migration work and `RELEASE_LOCK` share one Jdbi handle and physical connection.
If explicit release fails, the connection is aborted rather than returning a possibly locked physical
connection to Hikari. MySQL DDL can implicitly commit, so migrations must be retryable after partial
DDL. The version is published only after the migration callback succeeds.

See [MySQL locking functions](https://dev.mysql.com/doc/refman/8.4/en/locking-functions.html).

## Verification

Run with Java 21:

```sh
./gradlew test
./gradlew build
```

`MySqlPersistenceTest` uses `mysql:8.4.8`, Jdbi/Hikari pool A and an independent Jdbi/Hikari pool B.
It runs the shared persistence contract and forced row-lock races against real MySQL. Coverage includes
fence takeover, stale unlock, both save/takeover orderings, atomic quit, addon rollback including release,
revision rejection, reset barriers, concurrent schema upgrades, failed creation/upgrade, connection-scoped
advisory locks and physical disconnect, and non-destructive imports.

The lifecycle tests run the actual manager against H2 with a controlled Bukkit task queue and clock.
They test brief/prolonged outages, delayed heartbeat responses, recovery, mutation guards, stale-write
invalidation, reconnect, delayed publication, writer coalescing, barriers and shutdown. They use Mockito
for Bukkit services, not MockBukkit or a complete running Paper server.

The MySQL class is explicitly skipped when no Docker-compatible runtime exists. A successful H2 run is
not equivalent to a MySQL run. With rootless Podman, configure `DOCKER_HOST` for its API socket and
`TESTCONTAINERS_RYUK_DISABLED=true`, as described in the
[Testcontainers Podman instructions](https://java.testcontainers.org/supported_docker_environment/).
The test container is stopped after the test class.

## Remaining limits

- Experimental levels still use the existing local `levels.yml` storage. Their mutation entry points
  now obey session health, but the file is not part of the shared MySQL transaction.
- Snapshots are kept in memory. A hard crash or process termination during an outage can lose unsaved
  progress, including progress accepted inside the conservative safety window. This system does not
  provide a disk-backed retry journal.
- MySQL fencing proves database write ordering. The local safety margin bounds normal scheduling delays;
  it is not a guarantee against an arbitrarily paused JVM between an authorization check and a gameplay
  side effect. No synchronous database operation runs for each XP event.
- The tests force transaction and task orderings, but do not emulate every packet loss, JVM pause, Paper
  lifecycle hook or third-party addon. Production network-fault testing remains useful.
- H2 schema migration still uses its existing five-minute lock lease. Long H2 migrations require a
  larger lease or renewal. MySQL uses connection-scoped advisory locking instead.
- Local generation tombstones remain for the process lifetime to prevent callback generation reuse.

## Verification of this change, 2026-09-15

The final `./gradlew test build` run passed on Java 21 with rootless Podman.
The individual `./gradlew test` and `./gradlew build` commands also passed during verification.
No tests were skipped. `git diff --check` passed.

| Suite | Passed |
| --- | ---: |
| LeaseHealthTest | 2 |
| PlayerSkillsManagerLifecycleTest | 21 |
| PlayerSkillsManagerOrderingTest | 2 |
| DatabasePersistenceTest | 19 |
| MySqlPersistenceTest | 25 |
| Total | 69 |

Only test dependencies were added. The build still reports existing Gradle deprecation,
Java deprecation/unchecked-operation and Mockito bootstrap-classpath warnings.
