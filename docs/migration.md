# Upgrades and database migration

H2 is the default storage option. **MySQL support is experimental and not recommended.**
Changing `database.type` does not move existing data.

## Upgrading older versions

Older CaveCrawlers versions do not validate database ownership tokens when saving player data.
Do not run those versions alongside newer versions against the same database.

1. Stop every CaveCrawlers backend sharing the database and back up the database and plugin files.
2. Update CaveCrawlers on every backend.
3. Start the servers again.

Keep database lease settings consistent across backends. Rolling updates are only an option after every backend
uses compatible ownership checks and database schemas. See [player persistence](player-persistence.md) for details.

## Moving between H2 and MySQL

These commands copy **skill data only**. They do not move Vault balances, addon tables, configuration files,
or experimental levels stored in `levels.yml`. Check each addon's migration requirements separately.
Existing target data and players previously owned or reset on the target are not overwritten.

| Direction | Command | Source `database.type` | Target `database.type` |
| --- | --- | --- | --- |
| H2 to MySQL | `/cc data migrate h2-to-mysql` | `h2` | `mysql` |
| MySQL to H2 | `/cc data migrate mysql-to-h2` | `mysql` | `h2` |

Run commands from the console or with `cavecrawlers.admin` and `cavecrawlers.admin.data` permissions.

1. Stop the backends and back up the source and target databases, plus `plugins/CaveCrawlers/`.
2. Keep other backends stopped and keep players off the server used for migration until verification is complete.
3. Leave `database.type` set to the **source**. Configure the MySQL host, port, database, username, password,
   and connection options in `config.yml`. Restart only the migration server to load these settings.
4. Run the command for the required direction. Wait for successful completion and check the console for errors.
5. Stop the migration server. Set `database.type` to the **target** on the backends that will use it, then restart
   the migration server to verify skill progress before reopening the network.

H2 uses a local file on each server, and the command reads or writes only the H2 file of the server it runs on:

- **H2 to MySQL with several backends:** run steps 3 and 4 on every backend that has its own H2 file, one at a time.
  A player already in MySQL is skipped, so the first backend migrated wins for players stored on more than one.
  Migrate the backend with the most current progress for those players first. The command does not merge data, so
  if two backends each hold the newest progress for different players, no order keeps both. Compare players found
  on more than one backend before migrating. If the newest progress is split, do not switch until you have reconciled
  that data yourself or verified another safe way to keep it.
- **MySQL to H2 on several backends:** run steps 3 and 4 on every backend that will switch to H2. Each one gets its own
  copy, and progress is no longer shared between servers after the switch.

Verify skill progress on every backend that switched, not only on the migration server.

The command copies a snapshot and does not change the running database. Keep players off the source after copying
so new progress is not left behind. The reported count includes only skill records from imported players. Players that already have
target data, or whose target session is locked or already used, are skipped without being named, even when the count
is above zero. Compare source and target players before switching. If migration fails, keep the source selected and
inspect the error before retrying.

Keep the original database backup until the migration is verified. Switching back later does not copy new progress
back to the old database.
