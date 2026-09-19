# CaveCrawlers test coverage audit

Audit date: 2026-09-18

Scope: all production sources, resources, documentation, build configuration, and the complete existing test suite. This is a correctness-oriented plan, not a line-coverage target. No production code, tests, or dependencies were changed during the audit.

## Executive summary

CaveCrawlers has unusually strong tests for one subsystem: fenced player-skill persistence. The current tests exercise H2 transaction semantics, manager ordering, lease-health deadlines, reconnect generation handling, writer coalescing, resets, migration barriers, and shutdown. Real MySQL row-lock and advisory-lock tests also exist, but they only run when Docker is available.

The rest of the plugin has almost no automated protection. The largest unprotected risks are:

1. Economy operations consume or grant items even when Vault reports a failed transaction. Invalid negative, `NaN`, or infinite prices are also accepted by mutation paths.
2. Skill rewards are granted before progression is durably saved. A failed save or crash can replay coins, items, and commands after reconnect.
3. `getItemFromItemStackSafe` treats a display name as a custom-item ID. In altar and stat paths, a renamed item without CaveCrawlers PDC can impersonate a real custom item.
4. Plugin enable/disable orchestration is not exercised on a running Bukkit-like lifecycle. A regression in task cancellation, final saves, database retry, delayed initialization, or mining restoration would bypass the otherwise strong service-level tests.
5. YAML/configuration serialization has multiple fragile exact casts and at least one confirmed round-trip inconsistency (`Perk.serialize` omits `priority`). Item PDC/component preservation is untested.
6. Progression, stats, damage, loot, boss ranking, altar state, mining regeneration, GUIs, prompts, commands, and integrations have no meaningful tests.
7. Experimental levels remain in local `levels.yml`, outside the fenced database transaction. That is documented, but its failure and multi-server behavior are untested.

The goal should not be to blanket every listener or getter. Start with tests that protect item/money conservation, durable reward idempotency, custom-item identity, plugin shutdown, and the remaining persistence integration seams.

## Repository and test-suite snapshot

- Production Java: about 22,204 lines across the plugin lifecycle, persistence, items, skills, stats, economy, mining, drops, bosses, altars, GUIs, commands, and integrations.
- Test Java: about 1,330 lines in five test classes.
- Test stack already present: JUnit 5, Mockito, H2, Testcontainers MySQL, Jdbi, and HikariCP.
- Verification snapshot (local run on 2026-09-19 with
  `GRADLE_USER_HOME=/tmp/gradle-home ./gradlew build --no-daemon`): 191 tests passed,
  32 skipped, and 0 failed.
- `MySqlPersistenceTest` contributes the 32 skipped Testcontainers cases when no
  Docker-compatible runtime is available. Test totals vary by environment and Gradle
  filters; this snapshot is not a universal current count.
- The verification totals in `docs/player-persistence.md` are historical and should not
  be used as the current suite count.

The repository has no production implementation of compressed items, enchanted-item conversion, gem tiers, or `ROUGH_X -> FLAWED_X -> FINE_X` conversion. Names such as `ENCHANTED_DIAMOND` are ordinary configured item IDs. Tests for nonexistent conversion systems would be speculative and should not be added. The relevant real string-derived identities are `VANILLA-<MATERIAL>`, display-name fallback IDs, ability IDs with JSON settings, armor-set IDs, drop value/range strings, skill objectives, and reward strings.

## Existing test coverage

### Lease configuration and health

`LeaseHealthTest` has two focused unit tests.

Already protected:

- Timeout and heartbeat values are clamped to a conservative safety window.
- A late heartbeat response cannot resurrect an expired health epoch.
- Monotonic-clock wrap behavior is considered.

Quality: meaningful invariant tests. They protect time semantics instead of implementation details.

Remaining branches:

- Boundary combinations at exactly the minimum/maximum timeout and `timeout / 4` heartbeat.
- Warning emission only once for invalid configuration.
- Integration of the calculated unsafe threshold with real scheduled heartbeat/watchdog tasks.

### Player persistence manager lifecycle

`PlayerSkillsManagerLifecycleTest` currently runs 23 cases using H2, Mockito, a controlled Bukkit task queue, and a controlled clock.

Already protected:

- Brief database outage and recovery preserve a session.
- Startup heartbeat waits for database initialization.
- Prolonged heartbeat failure and prolonged write failure reject mutations.
- Experimental level mutation guards follow persistence health.
- Late heartbeat success does not revive an expired session.
- Stale writes invalidate only the matching player generation.
- A delayed invalidation cannot kick a newly reconnected generation.
- Rapid reconnect waits for quit handoff and rejects old mutable references.
- Repeated cache insertion does not loosen mutation guards.
- Explicit reload hands off old state before loading.
- A late load after quit cannot repopulate the cache.
- Superseded load publication retries for an online player.
- A duplicate load cannot overwrite progressed state.
- The writer coalesces snapshots while an older write is running.
- Reset and quit barriers survive real writer coalescing.
- Migration barrier success and failure both restore the correct mutation state.
- Failed load release is retried before reconnect.
- Shutdown flushes a writer whose scheduled task never started.
- Shutdown does not release leases while an active writer is still running.
- Failed shutdown flush retains the snapshot and avoids bulk release.

Quality: strong deterministic concurrency tests. They force task ordering rather than sleeping.

Remaining branches and seams:

- `PlayerDataLoadEvent` thread, count, and publication ordering.
- `PlayerDataSaveEvent` count and retry semantics. It is a pre-attempt notification, not a commit notification.
- A full autosave scan with multiple players, concurrent scan requests, and a scan exception.
- Heartbeat recovery restarting a writer that was queued while the database was unavailable.
- Loading-title scheduling/cancellation and one-minute state cleanup.
- Actual listener-driven join/quit and actual plugin enable/disable orchestration.
- Shutdown interaction with delayed database initialization and retry callbacks.

### Save-request ordering

`PlayerSkillsManagerOrderingTest` has two pure unit tests.

Already protected:

- Coalescing retains the latest rows and revision while preserving earlier reset/quit barriers.
- Requests from different fence tokens are never combined.

Quality: concise, high-value invariant tests.

Remaining branch:

- Equal revisions are currently resolved in favor of the left request. A test should document whether equal-revision requests are impossible or what flag/row behavior is required.

### Database persistence contract

`DatabasePersistenceTest` currently runs 21 H2 integration tests.

Already protected:

- Takeover fences old saves and old unlock attempts.
- Heartbeat uses database time without changing the fence.
- Older revisions cannot replace newer revisions.
- Quit data commits before the session becomes transferable.
- Rapid reconnect and transfer-back reject prior owners.
- A failed quit retry cannot overwrite a new owner.
- Addon-table failure rolls back skills, revision, and release.
- Reset is ordered with saves, including a coalesced post-reset snapshot.
- Offline reset refuses a live owner and fences an expired owner.
- Legacy import is non-destructive, one-time, and cannot resurrect reset/claimed-empty data.
- Player-session schema version 1 upgrades in place.
- Whole-player database migration skips an existing target player.
- Concurrent schema upgrade runs once.
- H2 migration ownership is not reclaimed incorrectly; losing the lease rolls back version publication.
- A released same-fence session is reported as lost ownership.
- A snapshot cannot write another player's rows.

Quality: meaningful transaction-contract coverage. These tests already protect most of the persistence invariants the audit brief calls out.

Remaining branches:

- Same live server reacquiring a lease should keep the fence and refresh database time.
- Fence/revision overflow behavior from `Math.addExact` should roll back without partial mutation.
- Multiple registered addon tables should load/save in registration order and roll back as a unit.
- Offline reset only deletes skill rows; addon reset semantics are absent and require a contract test before design changes.
- Invalid database type/host/SSL configuration and initialize-fail-reinitialize behavior.

### MySQL persistence

`MySqlPersistenceTest` declares 27 cases: the shared persistence contract plus MySQL-specific races and schema-lock behavior.

Already protected when Docker is available:

- Both save-versus-takeover row-lock orderings.
- Quit publication before takeover.
- MySQL schema upgrades and whole-player migration behavior.
- Connection-scoped advisory lock release, including physical disconnect after release failure.
- Failed upgrade/version publication and retry after implicit DDL commit.

Quality: essential because H2 is not an adequate substitute for MySQL locking semantics.

Current weakness: the whole class skipped in this audit environment. CI should make the Docker requirement explicit and fail or separately report when the MySQL job does not run.

### Entirely uncovered major subsystems

There are no current tests for items/PDC, shops and selling, Vault behavior, serialization, stats, damage, skills/reward formulas, drops, bosses, altars, mining, GUIs, commands, prompts, PlaceholderAPI, ProtocolLib, MythicMobs, LuckPerms, configuration loading/reload, or full plugin lifecycle.

## Critical missing tests

### C1. Durable progression and reward idempotency

- **Subsystem:** Skills, rewards, persistence
- **Location:** `Skill.levelUp`, `Skills.tryLevelUp`, `SkillsManager.giveXp`, `CoinSkillReward.applyReward`, `ItemSkillReward.applyReward`, `CommandSkillReward.applyReward`, `PlayerSkillsManager` save/reconnect flow
- **Behavior:** A level transition that grants persistent external value must not grant that value twice when the corresponding skill save fails, is retried, or is followed by reconnect/reload.
- **Risk:** Duplicate coins, items, or commands; players can repeat a level after their skill state rolls back.
- **Recommended type:** Regression test plus database integration test; deterministic failure injection.
- **Scenarios:** Normal one-level claim; one XP gain crossing several levels; save failure after reward; quit immediately after reward; reconnect while the failed snapshot is pending; server A grants then loses ownership to server B; duplicate XP event delivery; command reward that throws or partially succeeds.
- **Assertions:** Each level transition changes total XP/level once; each reward side effect is observed once; a failed durable transition either grants nothing or has a durable idempotency record; an old generation cannot grant after reconnect; multi-level rewards preserve level order.
- **Priority:** CRITICAL

The current implementation grants the external reward before the asynchronous persistence write. A test written against the intended contract will probably fail and should precede any design change.

### C2. Atomic shop purchases and Vault failures

- **Subsystem:** Economy, shop, inventory
- **Location:** `ShopItem.canBuy`, `ShopItem.buy`, `VaultUtils.takeCoins`, `ItemsManager.removeItems`, `ItemsManager.giveItem`
- **Behavior:** A purchase succeeds exactly once only when price withdrawal and ingredient removal succeed. Failure must leave balance and inventory unchanged.
- **Risk:** Free items, lost ingredients, or charged players without delivery.
- **Recommended type:** MockBukkit test with a fake Vault `Economy`; regression test.
- **Scenarios:** Coins-only purchase; ingredients-only purchase; mixed purchase; exact balance; one missing ingredient; provider returns failed `EconomyResponse`; provider throws; negative/zero/`NaN`/infinite price; zero/negative result amount; full inventory; two immediate clicks.
- **Assertions:** Exact balance delta; exact ingredient delta; exact result count or deliberate world drop; one Vault call; one success message; no mutation on any failed prerequisite/transaction; invalid monetary values cannot make a purchase affordable or credit the player.
- **Priority:** CRITICAL

### C3. Atomic selling and item conservation

- **Subsystem:** Economy, sell GUI
- **Location:** `SellMenu.sell`, `SellMenu.getPrice`, `SellMenu.getPrices`, close callback, `SellCommand.setPrice`, `VaultUtils.giveCoins`
- **Behavior:** Each accepted stack is exchanged for its configured value exactly once. Failed deposits return the item. Unsellable items are returned. Closing without selling returns every deposited item once.
- **Risk:** Item loss, free money, duplicated returns, or corrupted balances.
- **Recommended type:** MockBukkit GUI test with fake Vault; parameterized unit test for prices.
- **Scenarios:** One stack; several stacks; mixed sellable/unsellable items; close without sell; double-click sell; click then close; shift-click and drag around the sell button; provider failure on first/middle stack; missing price section; stale menu across price reload; negative/zero/`NaN`/infinite configured price.
- **Assertions:** Conservation equation `items_before = returned + sold`; balance increases by the rounded configured total only; each stack is paid or returned, never both/neither; GUI clear/close cannot replay; preview total equals actual payout.
- **Priority:** CRITICAL

### C4. Custom-item identity cannot be forged by display name

- **Subsystem:** Items, PDC, stats, altars, shops, Mythic integration
- **Location:** `ItemsManager.getItemFromItemStackSafe`, `ItemsManager.getIDofItemStack`, `StatsManager.getStatsFromItemStack`, `AltarListener`, `Altar.onPlayerInteract`, `ShopManager.addItemToShop`, `MythicItemSupport.isSimilar`
- **Behavior:** Gameplay value must require canonical CaveCrawlers identity (PDC or an explicitly configured vanilla conversion), not only a matching display name.
- **Risk:** Forged stat items, free altar contributions, item duplication, counterfeit shop/admin input.
- **Recommended type:** MockBukkit regression tests.
- **Scenarios:** Real PDC item; configured vanilla conversion; renamed vanilla item matching a custom ID; colored/case/space variants; stale/unknown PDC ID; copied display/lore without PDC; real item after definition reload.
- **Assertions:** Real identity resolves; counterfeit stack does not add stats, does not increment altar credit, does not change altar block, and is not consumed/accepted; configured vanilla conversion still follows its explicit contract.
- **Priority:** CRITICAL

### C5. Final save and clean plugin shutdown as one lifecycle

- **Subsystem:** Bukkit lifecycle, persistence, scheduled tasks
- **Location:** `CaveCrawlers.onDisable`, `PlayerLifecycleListener`, `PlayerSkillsManager.shutdown`, `Database.shutdown`, `MiningManager.regenBlocks`, GUI/Placeholder cleanup
- **Behavior:** Disabling the plugin stops new work, captures current player state, resolves or safely retains final snapshots, releases leases only after successful writes, restores changed blocks, cancels tasks, closes the database last, and unregisters optional hooks.
- **Risk:** Player data loss, stale ownership, task leaks, stale mining recovery files, or callbacks using a closed pool.
- **Recommended type:** Plugin lifecycle integration test; MockBukkit where supported, plus controlled database/scheduler fakes.
- **Scenarios:** Clean disable with loaded player; disable while autosave queued; disable during load; disable during heartbeat; final save failure; database unavailable; late initialization callback; open GUI; active altar; outstanding mining regen task.
- **Assertions:** Final committed snapshot matches latest main-thread state; no SQL begins after shutdown admission closes; bulk release occurs only with no pending writes; database closes after manager shutdown; plugin tasks/listeners/hooks cannot fire afterward; restored blocks and persistence file reflect clean shutdown.
- **Priority:** CRITICAL

### C6. Reset semantics include the complete logical player record

- **Subsystem:** Persistence addon API, reset
- **Location:** `PlayerSkillsManager.resetPlayerData`, `Database.resetOfflinePlayer`, `Database.persistPlayer(deleteBeforeWrite=true)`, `PlayerDataSqlTable`
- **Behavior:** Define and enforce whether an online/offline reset clears every registered player-data table or only skills. Reset must never leave addon state that can restore value on the next load.
- **Risk:** Partial resets, duplicated/restored addon rewards, privacy/admin-command failure.
- **Recommended type:** H2 and MySQL database integration tests with two fake addon tables.
- **Scenarios:** Online reset; offline reset with no owner; expired owner; live foreign owner; reset coalesced with later save; addon callback failure; reconnect after reset.
- **Assertions:** The chosen complete-record contract holds across every table in one transaction; fence and revision advance atomically; failure rolls back all tables; stale pending snapshots cannot resurrect pre-reset data.
- **Priority:** CRITICAL

The current addon interface has load/save callbacks but no reset/delete callback, and `resetOfflinePlayer` deletes only `skills`. This is a contract gap, not something a test should guess silently.

## High-priority tests

### H1. Persistence events, autosave, and recovery seam

- **Subsystem:** Persistence
- **Location:** `PlayerSkillsManager.loadFromDatabase`, `drainWriterQueue`, `saveAllAsync`, `captureAutosave`, `heartbeat`
- **Behavior:** Load publication and save-attempt notifications occur on documented threads and exactly at documented points. Autosave requests coalesce and recover after an outage.
- **Risk:** Addon incompatibility, duplicate callbacks, missed saves, unsafe Bukkit calls.
- **Recommended type:** Concurrency test with controlled scheduler/executor and latches.
- **Scenarios:** Successful load; superseded/quit load; two concurrent autosave triggers; exception during one player's snapshot; database disappears after capture; heartbeat recovery; failed write retry.
- **Assertions:** Load event occurs once, on primary thread, after cache publication; save event occurs once per actual persist attempt and may repeat on retry; only one autosave scan runs; all eligible players get a snapshot; scan flag resets after failure; recovery restarts retained writers.
- **Priority:** HIGH

### H2. YAML migration task file lifecycle

- **Subsystem:** Persistence migration
- **Location:** `YamlMigrationTask.run`, `PlayerData.loadPlayer`, `Database.importLegacySkills`
- **Behavior:** Legacy files are imported once, renamed only after a safe database outcome, and failed files alone retry while the login gate remains closed.
- **Risk:** Player-data loss, repeated migration, startup lockout, partial import.
- **Recommended type:** Database integration test with temporary files and controlled scheduler.
- **Scenarios:** No directory; empty directory; valid file; invalid UUID filename; malformed YAML; unknown skill type; DB already authoritative; import succeeds but rename fails; mixed success/failure; retry after recovery; plugin disabled before retry.
- **Assertions:** Authoritative DB rows are never overwritten; successful files become `.migrated`; failed files stay retryable; completion flag changes only when the defined migration set is complete; historical rewards are never emitted.
- **Priority:** HIGH

### H3. Database acquisition and overflow edges

- **Subsystem:** Persistence database
- **Location:** `Database.acquirePlayerSession`, `resetOfflinePlayer`, `registerTable`, `initialize`
- **Behavior:** Same live owner reacquisition keeps its fence; arithmetic overflow and initialization failures are atomic and recoverable.
- **Risk:** Accidental self-fencing, partial reset, permanently unavailable database.
- **Recommended type:** H2/MySQL database integration tests.
- **Scenarios:** Same server reacquires before expiry; same server reacquires after expiry; fence/revision at `Long.MAX_VALUE`; invalid/newer schema; first initialization fails then succeeds; repeated `registerTable` replacement for same addon table name.
- **Assertions:** Fence changes only when ownership epoch changes; overflow rolls back all changes; availability/initialized flags match documented startup gating; table callback registration contains one logical table per name.
- **Priority:** HIGH

### H4. Critical serialization round trips

- **Subsystem:** Serialization, configuration
- **Location:** `ItemInfo`, `Stats`, `Skill`, `Skills`, `SkillInfo`, all `SkillReward` types, `ShopItem`, `ShopMenu`, `Drop`, `EntityDrops`, `BossDrop`, `BossDrops`, `Altar`, `AltarDrop`, `BlockInfo`, `Perk`, `ConfigMessage`, `SoundOptions`, `TitleOptions`, `PlayerData`
- **Behavior:** `create -> serialize -> deserialize` preserves semantic fields that affect gameplay. Runtime-only caches/IDs should be reattached deliberately.
- **Risk:** Silent stat loss, wrong prices/rewards, broken reload, corrupt items, startup exceptions.
- **Recommended type:** Parameterized unit tests; MockBukkit for Bukkit serialization types.
- **Scenarios:** Complete values; optional null values; integer/long YAML numbers where a double is expected; empty collections; unknown IDs; malformed enum/material; legacy drop/block forms; component/PDC-rich `ItemStack`; nested shop/boss/altar objects.
- **Assertions:** Compare semantic fields rather than Lombok/object identity; deserialized values can be used by the owning manager; malformed entries follow an explicit skip/warn/fail contract; a bad entry cannot partially corrupt unrelated entries.
- **Priority:** HIGH

### H5. Item build/update and metadata preservation

- **Subsystem:** Items, PDC/components/NBT
- **Location:** `ItemsManager.buildItem`, `getItemByID`, `getIDofItemStack`, `updateItemStack`, `updatePlayerInventory`, `ItemInfo.clone`, `ItemImporter`
- **Behavior:** Canonical IDs and important item semantics survive construction, update, clone, and config round trip.
- **Risk:** Lost upgrades/enchants/custom metadata, item identity corruption, counterfeit conversion.
- **Recommended type:** MockBukkit parameterized tests and round-trip regression tests.
- **Scenarios:** Custom item; `VANILLA-DIAMOND`; unknown ID; amount boundaries; enchantments; string/integer/byte-array PDC; custom model/components; `NO_UPDATE`; missing/air base item; old definition updated to new lore/stats; importer with valid and malformed lore.
- **Assertions:** Exact canonical ID and amount; preserved supported metadata and enchants; refreshed definition-controlled fields; `NO_UPDATE` returns unchanged stack; unknown item remains unchanged; clone mutations do not affect the source.
- **Priority:** HIGH

### H6. Skill parsing, XP curve, and event contract

- **Subsystem:** Skills and progression
- **Location:** `SkillObjective.fromString`, `SkillReward.valueOf`, `SkillInfo.deserialize/generateRewards/generateStatsRewards`, `Skill.levelUp`, `SkillsManager.tryGiveXp/giveXp`
- **Behavior:** Valid configs produce deterministic objectives/rewards and XP boundaries; invalid configs fail or skip predictably without delayed null failures.
- **Risk:** Startup/reload failure, progression lock, wrong level/reward, duplicate event effects.
- **Recommended type:** Parameterized unit tests plus MockBukkit event tests.
- **Scenarios:** Exact threshold; just below/above; multiple levels; max level; excess XP; empty/short curve; negative/zero/`NaN`/infinite XP; auto-reward with first/middle gaps; unknown reward; command with spaces; objective with zero through four tokens; world-specific objective precedence; cancelled/modified `SkillXpGainEvent`; listener swaps player state during event.
- **Assertions:** Level/current XP/total XP/xp-to-next are exact; reward list for each level is non-null and isolated; cumulative stat rewards are exact; event changes apply once; cancelled/stale events change nothing; malformed config produces the selected warning/failure at load time.
- **Priority:** HIGH

### H7. Stats aggregation, event modification, and application

- **Subsystem:** Stats
- **Location:** `Stats`, `StatsManager.calculateBaseStats/calculateStats/getStatsFromPlayerEquipment/getStatsFromInventory/getStatsFromHotBar/applyStats`, `StatsCalculateEvent`, `PerksListener`
- **Behavior:** Base, equipment, skills, admin adders, inventory, hotbar, and perks combine once in a documented order. Recalculation removes stale equipment values and retains only intended mutable mana state.
- **Risk:** Permanent stat inflation, double counting, invalid player attributes, addon event incompatibility.
- **Recommended type:** Parameterized unit tests plus MockBukkit tests.
- **Scenarios:** Every item slot; equip/remove/re-equip; world change/join/respawn; repeated calculation; cap boundaries; negative stats; event listener adds/multiplies; two listeners; perk tracks/priorities; health/mana regeneration.
- **Assertions:** Exact value per source; no accumulation across recalculation; speed and attack-speed caps follow the chosen before/after-event contract; event fires once and its final mutation is stored; removed gear disappears; Bukkit attribute/walk-speed calls stay within legal ranges.
- **Priority:** HIGH

### H8. Damage and cooldown formulas

- **Subsystem:** Combat
- **Location:** `PlayerDamageCalculation.calculate/critRoll`, `AbilityDamage.calculate`, `DamageManager.calculateAttackSpeed/canAttack/launchProjectile`, `DamageEntityListener`
- **Behavior:** Formulas, cooldown boundaries, projectile metadata, server multiplier, and defense reduction are exact and finite.
- **Risk:** Major balance regression, negative/infinite damage, double damage attribution.
- **Recommended type:** Parameterized pure unit tests; MockBukkit listener tests.
- **Scenarios:** Zero/base stats; strength/crit/ability scaling; crit chance 0 and 100; defense 0, positive, `-100`, below `-100`; attack speed 0/100/out of range; melee versus projectile; missing projectile PDC; bow melee redirect; void damage.
- **Assertions:** Exact formula outputs with tolerance; finite nonnegative result under the intended input contract; one damage-map update; custom calculation consumed once; projectile stores calculated damage and crit consistently in both overloads.
- **Priority:** HIGH

### H9. Boss, drop, and loot idempotency

- **Subsystem:** Drops, bosses, entity rewards
- **Location:** `Drop`, `BossDrop`, `BossDrops.drop`, `EntityData.onDeath`, `LootShareEntityData.giveDropsToPlayers`, `BossEntityData.onDeath`, `EntityManager`
- **Behavior:** A death grants each eligible player/track exactly once; ranking and thresholds are numerically correct; delayed announcements use the originating reward's data.
- **Risk:** Duplicate rewards, wrong winners, shared mutable announcement data, lost loot.
- **Recommended type:** Parameterized unit tests, MockBukkit event tests, deterministic random source after a small future seam.
- **Scenarios:** Damage differences below 1.0; ties; offline contributors; threshold below/equal/above; summoner below threshold; null/non-null boss tracks; insufficient points; duplicate death callback; two players receiving the same `BossDrop` before delayed announcements run; provider/item failure.
- **Assertions:** Ranking uses full `double` ordering; bonus points and drops match placement; at most one success per non-null track; duplicate death is idempotent; each announcement contains its own player's placeholders; offline behavior follows an explicit policy.
- **Priority:** HIGH

### H10. Altar state machine

- **Subsystem:** Altars, rewards, Bukkit events
- **Location:** `AltarListener`, `Altar.onPlayerInteract/roll/onSpawn/resetAltar/refundAltar`, `AltarManager`
- **Behavior:** Each valid contribution consumes one real item, updates one slot, spawns at most one boss, transfers points once, and resets/refunds consistently.
- **Risk:** Duplication, item loss, permanently stuck altar, duplicate boss/reward.
- **Recommended type:** MockBukkit state-machine/regression tests.
- **Scenarios:** Wrong block/location/item/hand/game mode; repeated click on used location; last contribution; no spawn roll succeeds; spawn returns null; duplicate interaction event; death schedules one reset; disable/reload with contributions; offline contributor refund; stale `ItemInfo` object after reload.
- **Assertions:** Item, contribution count, block material, boss count, damage credit, points, announcements, and scheduled reset all change exactly once; failed completion has a defined refund/reset outcome; disable leaves no trapped value.
- **Priority:** HIGH

### H11. Mining regeneration and crash-recovery persistence

- **Subsystem:** Mining, scheduled tasks, filesystem persistence
- **Location:** `MiningManager.handleBlockRegen/restoreBlock/regenBlocks/markDirtyBrokenBlocks/flushBrokenBlocksAsync/loadBrokenBlocks/rewritePendingBlocksAtomically`, `ChunkUnloadListener`
- **Behavior:** The latest set of replaced blocks is durably recorded, valid entries restore once, invalid/unavailable entries remain retryable, and clean shutdown leaves no stale recovery work.
- **Risk:** World corruption, stale block overwrite on next boot, task/file leaks.
- **Recommended type:** MockBukkit lifecycle integration test with temporary data directory; deterministic scheduler/concurrency test.
- **Scenarios:** One/many blocks; repeated break same block; restore before debounce; two mutations during an active flush; chunk unload; unloaded/missing world; invalid Y/block data; persistence disabled; atomic move unavailable; clean disable after tasks are cancelled.
- **Assertions:** File snapshot is the latest logical state; restored block uses original `BlockData`; task and maps are cleared; failed entries alone remain; clean shutdown deletes/empties pending file; no Bukkit world/block API is used from an async thread if the platform contract forbids it.
- **Priority:** HIGH

### H12. Configuration load and reload atomicity

- **Subsystem:** Configuration
- **Location:** `ConfigLoader.registerItemsFromFolder/registerItemsFromFile/registerItemsFromConfig`, every loader, `CaveCrawlers.reloadLite/reloadContent`, `CustomConfig`, `BoostedCustomConfig`
- **Behavior:** Missing, malformed, duplicate, and partially valid files follow a deliberate contract. Reload does not leave a half-cleared runtime registry.
- **Risk:** Plugin starts with missing content, live reload deletes valid gameplay definitions, duplicate IDs silently change behavior.
- **Recommended type:** Filesystem integration tests with temporary YAML.
- **Scenarios:** Missing directory/section/key; wrong types; null section; empty file; nested files; duplicate ID across files; one good then one bad entry in a file; save failure; reload with invalid replacement; database settings changed during lite reload; unknown references.
- **Assertions:** Exact registered IDs and source-file mapping; deterministic duplicate winner or hard failure; warnings identify file/key; no unrelated partial registration; old live registry remains if atomic reload is the intended contract; database runtime settings stay unchanged until restart.
- **Priority:** HIGH

### H13. Dynamic ability configuration and independent runtime state

- **Subsystem:** Abilities, configuration, scheduled tasks
- **Location:** `AbilityManager.getAbilityByID/registerAbility`, `ItemAbility.clone/buildAbilityWithSettings`, `ChargedItemAbility`, `BuffAbility`, listener abilities
- **Behavior:** A configured ability ID parses once, caches safely, has the intended independent/shared cooldown and charge state, and does not register duplicate listeners/tasks.
- **Risk:** One item variant blocks another, memory/listener leaks, repeated ability effects.
- **Recommended type:** Unit test plus MockBukkit listener/task test.
- **Scenarios:** Base ID; valid JSON; malformed JSON; unknown base; same configured ID twice; two different settings for one base; listener subclass; charged ability; reload/disable.
- **Assertions:** Parsed fields exact; invalid input returns documented fallback/null; cache size stable; listener registered once per logical ability; cooldown/charge/buff state does not leak between variants unless explicitly intended; tasks stop on disable.
- **Priority:** HIGH

### H14. Prompt completion races

- **Subsystem:** CompletableFuture, listeners, GUI/editor flows
- **Location:** `PromptManager.prompt/promptNumber/promptNumberMin`, `ChatPromptListener`
- **Behavior:** Exactly one terminal action wins among chat, click-cancel, quit, and replacement prompt. Completion callbacks that touch Bukkit run on the primary thread.
- **Risk:** Orphan futures, memory leaks, stale admin/editor action, double completion.
- **Recommended type:** Deterministic concurrency test with a controlled scheduler; MockBukkit test.
- **Scenarios:** Chat response; invalid number; below minimum; click cancel; quit; chat and click racing; second prompt for same player; move reminder cooldown; plugin disable.
- **Assertions:** Map entry removed once; one future completes and one cause wins; replaced future is explicitly completed/cancelled per contract; title reset once; parsing failures are exceptional; callback thread is primary.
- **Priority:** HIGH

### H15. Mutating command boundaries

- **Subsystem:** Commands, admin operations
- **Location:** coin commands and data/migration/shop/item/mining/altar/level commands in `CaveCrawlersMainCommand`, `SellCommand`, `SkillCommand`, `StatCommand`
- **Behavior:** Mutating commands validate permission, sender type, IDs, indexes, amounts, prices, online state, and persistence health before side effects.
- **Risk:** Economy exploit, corrupt config, wrong-player mutation, exceptions from console/invalid arguments.
- **Recommended type:** Command integration tests with the command framework and MockBukkit; unit tests for manager methods beneath commands.
- **Scenarios:** Player/console; permitted/denied; offline target; unknown item/shop/skill/stat/altar; negative/zero/overflow amount; invalid slot; negative/`NaN`/infinite money; unsafe persistence session; async migration success/failure.
- **Assertions:** One intended state mutation and message; invalid input has no file/inventory/economy/database side effect; persistence guards are not bypassed; asynchronous result messages return on a safe thread.
- **Priority:** HIGH

### H16. Optional and hard integration matrix

- **Subsystem:** Plugin lifecycle and integrations
- **Location:** `plugin.yml`, `CaveCrawlers.onEnable/registerPlaceholders/registerMythicHook/onDisable`, `PacketManager`, `LuckPermsUtils`, `CaveCrawlersExpansion`, `MythicMobsHook`
- **Behavior:** Startup behavior matches the declared dependency policy, and optional hooks do not break startup when absent or failing.
- **Risk:** Plugin cannot load, stale hook/listener, `NoClassDefFoundError`, addon incompatibility.
- **Recommended type:** Plugin lifecycle integration tests with dependency stubs; focused unit tests for hooks.
- **Scenarios:** Vault plugin absent; Vault present without economy provider; ProtocolLib absent; MythicMobs absent; PlaceholderAPI absent/present/register failure; LuckPerms absent; hook reload; disable/re-enable.
- **Assertions:** Hard dependencies prevent/disable startup as intended; soft dependency absence is harmless; successful hooks register once and unregister on disable; failure is logged and isolated; `usePlaceholderAPI` and caches reset correctly across lifecycle.
- **Priority:** HIGH

## Medium-priority tests

### M1. Mining calculation and event flow

- **Subsystem:** Mining
- **Location:** `MiningManager.getTicksToBreak/breakBlock/handleBreak/handleHammer`, `MiningRunnable`, `MiningListener`, `BlockInfo`
- **Behavior:** Strength/power/tool requirements, progress timing, drops/XP, hammer selection, and abort behavior are exact.
- **Risk:** Instant/unbreakable blocks, duplicate drops/XP, wrong blocks broken.
- **Recommended type:** Parameterized unit tests and MockBukkit tests.
- **Scenarios:** Speed/strength zero, one, normal, negative, extreme; wrong/no tool; insufficient/equal power; replace active progress; abort; survival/creative/blacklisted world; hammer face/distance sorting and budget.
- **Assertions:** Expected tick count and break tick; packet stages reset; old runnable cancelled; one primary drop/XP event; hammer never breaks the origin, wrong material, or blocks outside its budget; configuration-invalid blocks are skipped.
- **Priority:** MEDIUM

### M2. Drop parsing and amount/chance modifiers

- **Subsystem:** Drops and economy
- **Location:** `Drop.getItemDropInfo/getNewAmount/getNewDropChance/drop`, `Range`, `RandomUtils`
- **Behavior:** Drop strings, inclusive ranges, chance modifiers, fortune amounts, and bounds are exact.
- **Risk:** Wrong quantity, exceptions from config, negative rewards, overflow.
- **Recommended type:** Parameterized unit tests; property tests for parsing/ranges; deterministic RNG seam later.
- **Scenarios:** Single amount; positive/negative range; whitespace; malformed/missing tokens; min greater than max; `Long.MAX_VALUE`; modifiers at 0/99/100/150/200 and negative; item amount over `Integer.MAX_VALUE`; coins over safe integer.
- **Assertions:** Parser result exact; invalid input follows load-time policy; item amounts never negative/overflow; coin clamping exact; 0% and 100% have strict semantics.
- **Priority:** MEDIUM

### M3. Loot-share boundaries

- **Subsystem:** Entity rewards
- **Location:** `LootShareEntityData.giveDropsToPlayers`, `SkillXpGainingListener.onEntityDeath`
- **Behavior:** Threshold and summoner rules select each player once for both loot and skill XP.
- **Risk:** Missing or duplicate cooperative rewards.
- **Recommended type:** Parameterized unit/MockBukkit test.
- **Scenarios:** Below/equal/above threshold; zero/negative/>100 configured threshold; summoner without damage; duplicate damage additions; offline player; no killer; ordinary versus Mythic entity.
- **Assertions:** Exact recipients, message recipients, and callback count; summoner override follows contract; loot and XP use the same eligible set where intended.
- **Priority:** MEDIUM

### M4. GUI navigation and stale-state behavior

- **Subsystem:** GUIs
- **Location:** `PaginatedRowGui`, `GuiItems`, `ShopMenu`, `SkillsGui`, `SkillsRewardsGui`, editors, confirm GUI
- **Behavior:** Only actionable slots mutate state; navigation respects boundaries; a stale GUI cannot repeat or target a replaced object.
- **Risk:** Duplicate purchase, wrong item edit, index exception, bypassed requirement.
- **Recommended type:** MockBukkit behavioral tests.
- **Scenarios:** Empty/one/full/multi-page; first/last page; previous/next; filler slot; shift/double/clone/right click; backing list changed after open; permission denied; editor prompt cancelled; close.
- **Assertions:** Page/slot contents map to the intended model; filler does nothing; mutation callback once; invalid/stale action fails safely; purchase/sell is covered by critical economy tests.
- **Priority:** MEDIUM

### M5. Join, respawn, world-change, and inventory-update listeners

- **Subsystem:** Bukkit lifecycle
- **Location:** `PlayerLifecycleListener`, `UpdateItemsListener`, `ItemChangeListener`, `FirstJoinListener`, `WorldChangeListener`, `MenuItemListener`
- **Behavior:** State initializes and recalculates at the correct lifecycle points without duplicate tasks/effects.
- **Risk:** Default state overwrite, duplicate first-join commands, stale stats, task/map leaks.
- **Recommended type:** MockBukkit listener tests.
- **Scenarios:** First/new join; returning player; join before delayed data ready; respawn; rapid world changes; held-item change; creative preservation; menu item enabled/disabled/occupied slot; quit cleanup.
- **Assertions:** Load scheduled once; first-join commands once with player substitution; inventory items updated once; stat recalculation occurs after the inventory transition; cooldown suppresses only intended repeats; repeating tasks cancel on disable.
- **Priority:** MEDIUM

### M6. Experimental level persistence

- **Subsystem:** Levels, persistence
- **Location:** `LevelConfigManager`, `FirstJoinListener`, `Skills.tryLevelUp`, `CaveCrawlersExpansion.getLevel`
- **Behavior:** Local level XP/level/color obey persistence health and carry excess XP correctly; documented non-transactional limitations are explicit.
- **Risk:** Lost or duplicated level XP, split-brain across servers, setting read from wrong file.
- **Recommended type:** Filesystem/MockBukkit regression tests.
- **Scenarios:** Feature disabled/enabled; exact/over threshold; several thresholds in one award; failed skill save; two managers/files representing servers; unsafe session; invalid color.
- **Assertions:** Correct source config controls enablement; exact XP carry/level increments; unsafe state changes nothing; placeholder read does not unexpectedly mutate unrelated config; tests document that this state is not part of database rollback.
- **Priority:** MEDIUM

### M7. Addon API contracts

- **Subsystem:** Public APIs
- **Location:** `CaveCrawlersAPI` and sub-APIs; `register` methods for items, stats, drops, bosses, skills, abilities; `PlayerDataSqlTable`
- **Behavior:** Registration, replacement/duplicate handling, lookup, and thread requirements are stable for addons.
- **Risk:** Addon breakage or corrupt shared registries.
- **Recommended type:** Unit contract tests and one integration fixture addon.
- **Scenarios:** Valid registration; duplicate ID; case differences; same instance under two IDs; register during/after startup; addon table throws; API use off main thread where prohibited.
- **Assertions:** Deterministic result/error; built-in registries remain valid; addon exception isolation or transaction rollback matches documentation.
- **Priority:** MEDIUM

### M8. Placeholder expansion

- **Subsystem:** PlaceholderAPI
- **Location:** `CaveCrawlersExpansion.getStat/getLevel/getAltar/getSkill`
- **Behavior:** Valid placeholders format exact values; invalid/offline/missing state returns the documented null/fallback without mutation or exception.
- **Risk:** Placeholder spam exceptions, wrong public values, disk writes during formatting.
- **Recommended type:** Unit tests with mocked actors plus integration smoke test when PlaceholderAPI is present.
- **Scenarios:** Multi-word stat; unknown stat; offline actor; invalid skill/field; zero XP-to-level; altar missing/no boss/live/dead; invalid level color.
- **Assertions:** Exact strings and caps; no exception; no unexpected state write for a read-only placeholder; register/unregister once.
- **Priority:** MEDIUM

### M9. Scheduled ability cleanup and one-shot effects

- **Subsystem:** Abilities, tasks, listeners
- **Location:** `SandStorm`, `EarthShooterAbility`, `SpiritSpectreAbility`, `MidasAbility`, `HulkAbility`, `ArrowSpiralAbility`, `TargetAbility`, `ChargedItemAbility`
- **Behavior:** Scheduled effects stop at their intended bound and clean entities/maps on completion, player quit, entity removal, and plugin disable.
- **Risk:** Entity/task/memory leaks or repeated damage.
- **Recommended type:** MockBukkit scheduler tests.
- **Scenarios:** Normal completion; target removed early; player offline/dead/world change; duplicate activation; plugin disable; event lands before delayed map cleanup.
- **Assertions:** Exact tick/effect/damage count; spawned entities removed; ownership/charge/target maps emptied; no task executes after disable.
- **Priority:** MEDIUM

### M10. Perk selection

- **Subsystem:** Perks and stats
- **Location:** `PerksManager.getPerks`, `PerksListener`, `Perk` serialization
- **Behavior:** One highest-priority permitted perk per track contributes stats once.
- **Risk:** Wrong rank stats or nondeterministic tie behavior.
- **Recommended type:** Parameterized unit test plus serialization regression.
- **Scenarios:** No permission; one perk; several tracks; higher/lower/equal priority; null/empty track; reload.
- **Assertions:** Exact selected IDs and stat sum; tie behavior deterministic; round trip preserves priority.
- **Priority:** MEDIUM

## Low-priority tests

### L1. Read-only index and viewer GUIs

- **Subsystem:** Index/profile UI
- **Location:** `IndexManager`, index category menus, `PlayerViewer`
- **Behavior:** Search/filter/hide and navigation return the intended entries without mutating gameplay state.
- **Risk:** Broken discovery UI, mostly cosmetic.
- **Recommended type:** Unit tests for filtering; a few MockBukkit GUI tests.
- **Scenarios:** Empty registry; hidden entry/drop; unknown Mythic ID; query case; pagination.
- **Assertions:** Correct visible ID set and safe fallback item/text.
- **Priority:** LOW

### L2. Message and formatting utilities

- **Subsystem:** Presentation utilities
- **Location:** `ConfigMessage`, `MiniMessageUtils`, `StringUtils`, `MessageUtils`, `TitleBuilder`, `ActionBarManager`
- **Behavior:** Placeholder replacement and number/text formatting do not corrupt control tokens or shared message state.
- **Risk:** Misleading messages; low data risk.
- **Recommended type:** Parameterized unit tests.
- **Scenarios:** Missing/extra placeholders; `%` in key/value; null optional title/sound/actionbar; repeated sends with different players; large/negative/decimal numbers.
- **Assertions:** Source `ConfigMessage` remains unchanged; output exact for representative cases; malformed formatting fails safely.
- **Priority:** LOW

### L3. Deprecated/simple utilities

- **Subsystem:** Legacy utilities
- **Location:** `SimpleDrop`, `Serializer`, basic `Cuboid`/color/font helpers
- **Behavior:** Only test code still reached by supported configs or public API.
- **Risk:** Low unless compatibility is promised.
- **Recommended type:** Focused regression/unit tests.
- **Scenarios:** Legacy `SimpleDrop` deserialize; malformed Base64; cuboid boundaries used by gameplay.
- **Assertions:** Legacy data either loads semantically or is rejected clearly; malformed input does not leak payloads or crash startup.
- **Priority:** LOW

## Persistence invariants

| Invariant | Existing protection | Additional test needed |
| --- | --- | --- |
| Stale fences never mutate data or release a newer owner | Strong H2/shared MySQL contract | Plugin-level transfer/reconnect smoke test |
| Older revisions never replace newer revisions | Direct DB test and queue ordering tests | Overflow rollback test |
| Only active/current generation may mutate | Strong lifecycle tests | Real join/quit listener test |
| Load publishes only after fenced load and only once | Duplicate/superseded load tests | Assert load event thread/count/order |
| One per-player writer drains a latest-only queue | Strong controlled ordering test | Multi-player autosave scan test |
| Reset is ordered with saves | Strong DB and manager tests | Complete-addon reset contract |
| Quit save and release commit atomically | H2 and MySQL tests | Full plugin disable orchestration |
| Failed quit retry cannot overwrite takeover | Direct DB test | End-to-end listener-driven transfer test |
| Pending snapshot never becomes valid after ownership change | Direct DB and manager tests | Heartbeat recovery with retained stale snapshot |
| Lease expiry stops mutations before takeover is safe | Strong clock/manager tests | Actual scheduled heartbeat/watchdog integration |
| Addon failure rolls back the logical save | Direct DB test | Multiple addon tables, load failure, reset semantics |
| Migration does not overwrite authoritative/reset state | Strong DB tests | YAML file rename/retry lifecycle |
| Shutdown never releases before durable final state | Strong manager test | `CaveCrawlers.onDisable` integration |
| Async work from an old reconnect generation stays invalid | Strong manager test | External event/reward callbacks carrying old state |
| Retries are idempotent | Save revision/fence tests | Reward and addon-side-effect idempotency |

## Economy and item invariants

1. Money and items obey conservation: a shop/sell operation either commits all intended deltas once or commits none.
2. Vault failure is a failed operation even if the pre-check balance was sufficient.
3. Prices and amounts must be finite and within a deliberate nonnegative domain before side effects.
4. Rounding must be consistent between affordability, displayed price, withdrawal/deposit, and success messages. Current Vault helpers floor to one decimal; tests should freeze or replace that contract intentionally.
5. A custom item is identified by canonical PDC, not display text. Explicit vanilla conversion is the only planned no-PDC mapping.
6. `VANILLA-<MATERIAL>` parsing must remove exactly one leading prefix. Representative mapping assertions: `VANILLA-DIAMOND -> DIAMOND`; malformed/internal repeated prefix -> rejected.
7. `build -> update -> identify` preserves ID, amount, supported custom PDC/components, and enchants.
8. `itemMapToStringMap` and `stringMapToItemMap` round-trip all known IDs and reject/skip unknown IDs without inserting a null key.
9. Drop parsing preserves exact item ID and inclusive amount range.
10. Boss non-null tracks grant at most one reward per track; `null` tracks are independent.
11. No compression, gem-tier, enchanted conversion, or decompression invariant exists in current production code. Do not add placeholder tests for those systems.

## Property and invariant testing opportunities

- **Serialization:** For generated valid semantic objects, `deserialize(serialize(x))` preserves all gameplay-relevant fields. Good candidates are `Stats`, reward variants, `Drop`, `ShopItem`, `BlockInfo`, and `Perk`.
- **Item-map conversion:** For maps containing registered non-null item IDs and nonnegative counts, `stringMapToItemMap(itemMapToStringMap(x))` is semantically equal to `x`.
- **Range parser:** For valid `a` or `a-b`, the parsed range contains both endpoints, string conversion reparses to the same endpoints, and every random result lies inside it. Bound generated sizes to avoid materializing huge ranges.
- **Skill curve:** For nonnegative finite total XP and a positive finite threshold list, derived level/current XP obey `0 <= currentXp < nextThreshold` unless at max level, and total XP is conserved.
- **Stats:** Cloning and serialization do not alias mutable `Stat` values. `add` and `multiply` follow algebraic expectations for ordinary finite inputs, with a separate explicit test for the special `MANA -> INTELLIGENCE` behavior.
- **Drop modifier:** For nonnegative base amount and nonnegative fortune, output is nonnegative and follows the guaranteed-multiplier plus one probabilistic remainder model.

Use property testing only for these compact parsers/value objects. Bukkit lifecycle and database races need deterministic scenario tests, not random generation.

## Possible bugs / regression test candidates

No production fixes were made. Each candidate below needs a failing regression test that establishes the intended contract first.

### 1. Vault failures are ignored

- **Location:** `VaultUtils.giveCoins/takeCoins/setCoins`, `ShopItem.buy`, `SellMenu.sell`
- **Classification:** Likely bug
- **Suspicious behavior:** Economy responses are discarded. Shop/sell code continues to remove or grant items. `setCoins` can withdraw the old balance and then fail to deposit the replacement.
- **Expected behavior:** Inventory and balance change atomically from the player's perspective, or the operation fails without loss/duplication.
- **Regression test:** Fake Vault provider returns a failed response at each transaction step; assert no irreversible inventory/balance mutation and no success message.
- **Confidence:** HIGH

### 2. Negative and non-finite economy values are accepted

- **Location:** `SellCommand.setPrice`, coin admin commands, `ShopManager.updateShopCoins`, `ShopItem.canBuy`, `SellMenu.sell`, `Drop.giveCoins`
- **Classification:** Likely bug
- **Suspicious behavior:** A negative shop price is always affordable and may turn withdrawal into a deposit. `NaN` is not `<= 0`, so `SellMenu.sell` can clear an item while trying to deposit `NaN` even though preview excludes it.
- **Expected behavior:** Money values are finite and nonnegative, with zero semantics explicitly defined.
- **Regression test:** Parameterize negative, `NaN`, positive/negative infinity, and huge values across command, config, shop, and sell paths.
- **Confidence:** HIGH

### 3. Display-name fallback can forge a custom item

- **Location:** `ItemsManager.getItemFromItemStackSafe`, `StatsManager.getStatsFromItemStack`, `AltarListener`, `Altar.onPlayerInteract`
- **Classification:** Likely exploit
- **Suspicious behavior:** A no-PDC stack derives its ID from stripped display name. Altar acceptance compares the resolved `ItemInfo`, but removal uses the canonical PDC/vanilla ID, so a forged item may add altar credit without being removed. The same fallback may grant item stats.
- **Expected behavior:** Value-bearing paths require canonical identity.
- **Regression test:** Rename a vanilla stack to a registered custom item and assert zero stats, no altar credit, no block change, and no spawn.
- **Confidence:** HIGH

### 4. Reward grants can replay after persistence failure

- **Location:** `Skill.levelUp`, reward implementations, asynchronous `PlayerSkillsManager` saves
- **Classification:** Confirmed design inconsistency with durable idempotency
- **Suspicious behavior:** External rewards happen synchronously before level state is durable. Fencing protects DB writes, not Vault/inventory/commands.
- **Expected behavior:** A durable level claim and its persistent-value rewards are idempotent across retries/reconnects.
- **Regression test:** Inject a save failure after a level-up reward, reconnect from older DB state, repeat XP, and assert the reward cannot be claimed twice.
- **Confidence:** HIGH

### 5. Offline reset does not clear addon tables

- **Location:** `Database.resetOfflinePlayer`, `PlayerDataSqlTable`
- **Classification:** Confirmed contract gap; likely bug if addon data is part of player reset
- **Suspicious behavior:** Offline reset deletes only skill rows. The addon API has no reset/delete callback.
- **Expected behavior:** Admin reset clears the complete logical player record, or API/docs explicitly limit reset to skills.
- **Regression test:** Register a fake value-bearing addon table, reset offline player, reload, and assert the chosen contract.
- **Confidence:** HIGH

### 6. `Perk` cannot round-trip its priority

- **Location:** `Perk.serialize`, `Perk.deserialize`
- **Classification:** Confirmed logical inconsistency
- **Suspicious behavior:** `serialize` omits `priority`; `deserialize` requires and unboxes it.
- **Expected behavior:** Priority survives serialization.
- **Regression test:** Round-trip a nonzero-priority perk and assert all fields and selected-track behavior.
- **Confidence:** HIGH

### 7. Two-token skill objectives pass the length check then crash

- **Location:** `SkillObjective.fromString`
- **Classification:** Confirmed logical inconsistency
- **Suspicious behavior:** It checks `split.length < 2` but always reads `split[2]`.
- **Expected behavior:** Fewer than three tokens produce the documented `IllegalArgumentException` and a loader-level warning/skip.
- **Regression test:** Inputs with zero, one, two, three, and four tokens plus repeated whitespace.
- **Confidence:** HIGH

### 8. Auto-reward generation can copy a null level-zero list

- **Location:** `SkillInfo.generateRewards`
- **Classification:** Likely bug
- **Suspicious behavior:** With `autoReward=true` and no level-1 rewards, level 1 copies `rewards.get(0)`, which is normally null.
- **Expected behavior:** Invalid config fails clearly, or missing first reward means an empty baseline.
- **Regression test:** Auto-reward with empty map, first reward at level 2, and gaps after a valid first reward.
- **Confidence:** HIGH

### 9. Deserializing a skill may mutate the global default XP list

- **Location:** `SkillInfo.deserialize`, `Skill.getDefaultXpToLevelList`
- **Classification:** Likely bug
- **Suspicious behavior:** When `xpToLevelList` is missing, the shared default list is returned and extended with `Double.MAX_VALUE` for a larger max level.
- **Expected behavior:** Each skill owns an independent curve; one config cannot alter another or the global default.
- **Regression test:** Deserialize two skills with different max levels and no explicit list; assert the default and first skill remain unchanged.
- **Confidence:** HIGH

### 10. Integer YAML stat values are silently discarded

- **Location:** `Stats.deserialize`
- **Classification:** Likely compatibility bug
- **Suspicious behavior:** Values are cast specifically to `Double`; YAML integers become `Integer` and are caught/skipped.
- **Expected behavior:** Any supported finite `Number` is converted to double, or config validation rejects it clearly.
- **Regression test:** Same stats expressed as integer, long, double, numeric string, null, and unknown key.
- **Confidence:** HIGH

### 11. Boss ranking truncates double differences

- **Location:** `BossEntityData.onDeath`
- **Classification:** Confirmed comparator flaw
- **Suspicious behavior:** Comparator casts `(damage2 - damage1)` to `int`; differences below 1.0 compare equal.
- **Expected behavior:** Descending `Double.compare` order with defined tie behavior.
- **Regression test:** Insert 10.1 and 10.9 damage in both map orders; assert 10.9 receives first-place points/reward.
- **Confidence:** HIGH

### 12. Delayed boss announcements share mutable placeholders

- **Location:** `Drop.placeholders`, `BossDrop.announceToPlayersInSameWorld`
- **Classification:** Likely asynchronous bug
- **Suspicious behavior:** The delayed task captures the same mutable map reused by subsequent drops.
- **Expected behavior:** Each scheduled announcement captures an immutable snapshot for its originating player/drop.
- **Regression test:** Award the same `BossDrop` to two players before the 20-tick tasks run; assert each message has its own amount/player/chance.
- **Confidence:** HIGH

### 13. Replacing a prompt abandons the old future

- **Location:** `PromptManager.prompt`
- **Classification:** Likely bug
- **Suspicious behavior:** `futureMap.put` replaces an existing future without completing or cancelling it.
- **Expected behavior:** The old future terminates explicitly when superseded.
- **Regression test:** Start two prompts for one player; assert the first completes exceptionally/cancels and cannot later trigger its editor callback.
- **Confidence:** HIGH

### 14. Dynamic ability clones share base cooldown state

- **Location:** `ItemAbility.clone`, `ChargedItemAbility` mutable fields, `AbilityManager.getAbilityByID`
- **Classification:** Likely bug or undocumented coupling
- **Suspicious behavior:** `Object.clone` is shallow and `abilityCooldown` is final. Configured variants share the same `Cooldown`; charged variants also share final maps/cooldowns.
- **Expected behavior:** Distinct configured ability IDs have independent runtime state unless explicitly documented otherwise.
- **Regression test:** Build two variants from one base, activate one for the same player, and assert the other variant's cooldown/charges per the chosen contract.
- **Confidence:** HIGH

### 15. Altar can remain consumed when no spawn succeeds

- **Location:** `Altar.roll`
- **Classification:** Likely bug
- **Suspicious behavior:** If every roll fails or spawning returns null, contributions and used blocks remain, with no retry/refund/reset scheduled.
- **Expected behavior:** Completion either spawns one boss or resolves contributions through a defined retry/refund/reset path.
- **Regression test:** Fill altar with all chances at 0 and with a spawn hook returning null; assert the selected recovery behavior.
- **Confidence:** HIGH

### 16. Offline altar contributors are not refunded

- **Location:** `Altar.refundAltar`
- **Classification:** Risky behavior; intended policy unclear
- **Suspicious behavior:** Offline players are skipped and then `playerPlacedMap` is cleared.
- **Expected behavior:** Persistent contributions are refunded later or the no-refund policy is explicit.
- **Regression test:** Contributor disconnects before disable/reset; assert queued refund or documented loss policy.
- **Confidence:** MEDIUM

### 17. Clean disable may leave a stale mining recovery file

- **Location:** `CaveCrawlers.onDisable`, `MiningManager.regenBlocks/markDirtyBrokenBlocks/scheduleBrokenBlocksFlush`
- **Classification:** Likely lifecycle race
- **Suspicious behavior:** Plugin tasks are cancelled before `regenBlocks`; `regenBlocks` marks persistence dirty and may schedule a new async flush while disable is in progress. That task may never run, leaving stale coordinates for next boot.
- **Expected behavior:** Clean shutdown synchronously records the empty pending-block set after restoration.
- **Regression test:** Break a tracked block, persist it, disable cleanly, restart with a changed block at that coordinate, and assert stale recovery is not replayed.
- **Confidence:** MEDIUM

### 18. Mining persistence reads Bukkit block/world state asynchronously

- **Location:** `MiningManager.flushBrokenBlocksAsync/writeBrokenBlocksSnapshot`
- **Classification:** Platform-contract risk
- **Suspicious behavior:** The async write iterates `Block` keys and calls world/name/coordinate and `BlockData` methods off the primary thread.
- **Expected behavior:** Bukkit state is snapshotted on the primary thread; only immutable file data is written asynchronously.
- **Regression test:** Thread-checking block/world fakes fail if accessed off-main; assert async work receives primitives/serialized block data only.
- **Confidence:** MEDIUM

### 19. Experimental leveling enablement appears to read the wrong config

- **Location:** `LevelConfigManager` constructor
- **Classification:** Likely bug
- **Suspicious behavior:** It reads `experimental.enable-leveling` from `levels.yml`, while the shipped setting is in main `config.yml`.
- **Expected behavior:** The documented main config controls the feature, including reload/restart semantics.
- **Regression test:** Toggle only the shipped main-config key and assert level-up behavior.
- **Confidence:** HIGH

### 20. Experimental levels are outside fenced persistence

- **Location:** `LevelConfigManager`, `Skills.tryLevelUp`
- **Classification:** Known documented correctness limitation
- **Suspicious behavior:** Level XP is saved to local YAML separately from the skill transaction/reward side effects.
- **Expected behavior:** Tests should document current loss/duplication boundaries until a transactional design is authorized.
- **Regression test:** Skill save fails after level-file save; two simulated servers award concurrently; assert/document divergence.
- **Confidence:** HIGH

### 21. `VANILLA-` parsing replaces every occurrence

- **Location:** `ItemsManager.getItemByID`
- **Classification:** Fragile code; low-probability bug
- **Suspicious behavior:** `replace("VANILLA-", "")` removes all occurrences after only checking the prefix.
- **Expected behavior:** Remove one leading prefix and validate the remaining material token.
- **Regression test:** Valid ID, repeated prefix, embedded prefix, empty suffix, and case variants.
- **Confidence:** MEDIUM

### 22. Unknown item-map IDs become a null key

- **Location:** `ItemsManager.stringMapToItemMap`
- **Classification:** Likely bug
- **Suspicious behavior:** Unknown IDs are inserted as `null`, causing later NPEs or misleading ingredient behavior.
- **Expected behavior:** Reject the owning config entry or skip the unknown ID with a precise warning.
- **Regression test:** Deserialize a shop/block legacy map containing one known and one unknown item.
- **Confidence:** HIGH

### 23. Configuration reload can publish partial registries

- **Location:** `ConfigLoader.registerItemsFromFile`, `CaveCrawlers.reloadLite`
- **Classification:** Risky behavior
- **Suspicious behavior:** Registries are cleared before new files load; a file can register earlier entries and then fail on a later entry. Duplicate IDs overwrite despite warning.
- **Expected behavior:** Either validated all-or-nothing reload or an explicitly documented partial-load policy with deterministic duplicate handling.
- **Regression test:** Existing valid registry, replacement file with good first/bad second entry, plus duplicate IDs across files.
- **Confidence:** HIGH

### 24. `RandomUtils.chanceOf(0)` is not mathematically impossible

- **Location:** `RandomUtils.chanceOf`
- **Classification:** Low-probability boundary flaw
- **Suspicious behavior:** It uses `random <= percent`; a generated exact zero makes 0% succeed.
- **Expected behavior:** `percent <= 0` is always false and `percent >= 100` always true.
- **Regression test:** Requires an injectable RNG or pure boundary guard test.
- **Confidence:** MEDIUM

### 25. MythicMobs is coded as optional but declared as required

- **Location:** `plugin.yml`, `CaveCrawlers.onEnable/registerMythicHook`, Mythic-aware completion and drop code
- **Classification:** Dependency-policy inconsistency; intended behavior unclear
- **Suspicious behavior:** Runtime code repeatedly handles a null/missing MythicMobs instance, but `plugin.yml` lists MythicMobs under `depend`, so Paper should prevent CaveCrawlers from loading when it is absent.
- **Expected behavior:** Either absence is a supported mode and the descriptor is soft, or MythicMobs is required and null-mode branches are not a supported contract.
- **Regression test:** Load the plugin with and without a MythicMobs stub and assert the selected dependency policy, including drop/index behavior.
- **Confidence:** HIGH that the declarations disagree; LOW on which policy is intended

## Testability concerns

### Global mutable singletons and static state

Managers, `CaveCrawlers.economy`, `usePlaceholderAPI`, registries, message config, prompt futures, and several caches are static/global.

- **Why it hurts:** Tests leak state and listener/task registrations between cases; parallel execution is unsafe.
- **Affected tests:** Almost every gameplay, lifecycle, and integration test.
- **Small future refactor:** Add package-private reset hooks only if needed, or make managers plugin-owned instances passed to listeners. Do not introduce interfaces solely for aesthetic consistency.

### Hard-coded Bukkit scheduler and singleton access

Production code directly calls `Bukkit`, `CaveCrawlers.getInstance`, and the global scheduler.

- **Why it hurts:** Opposite-order completion and disable races require heavy static mocking.
- **Affected tests:** Persistence lifecycle, mining, prompts, delayed announcements, abilities, stats.
- **Small future refactor:** Inject a narrow scheduler/main-thread executor and plugin reference into the few high-risk services. The persistence tests already demonstrate the value of controlled task queues.

### Hard-coded wall clock and random source

`Cooldown`, abilities, boss elapsed time, and `RandomUtils` use system time or `ThreadLocalRandom` directly.

- **Why it hurts:** Boundary tests become slow or probabilistic.
- **Affected tests:** Cooldowns, abilities, drops, altar rolls, crits, boss timing.
- **Small future refactor:** Inject `LongSupplier`/`Clock` and a minimal random-number supplier at the shared utility boundary. Avoid Awaitility/sleeps when a fake clock will do.

### Economy results are hidden behind void helpers

`VaultUtils` discards `EconomyResponse`.

- **Why it hurts:** Callers cannot make an atomic decision or expose failure in tests.
- **Affected tests:** Shop, sell, drops, rewards, admin coin commands.
- **Small future refactor:** Return the existing Vault response or a boolean/result from the shared helper; do not add a custom economy framework.

### SQL addon callbacks mix persistence with external cache ownership

`PlayerDataSqlTable.saveForPlayer` receives only UUID and handle; snapshot ownership across quit/retry is delegated to addons, and there is no reset callback.

- **Why it hurts:** Complete logical snapshots and reset semantics cannot be verified generically.
- **Affected tests:** Addon rollback, quit retry, reset, transfer.
- **Small future refactor:** First define the contract with fixture tests; then add the minimum snapshot/reset hook required by that contract.

### Configuration deserialization performs exact casts deep in constructors

Many deserializers cast directly to `int`, `double`, lists, enums, and nested objects.

- **Why it hurts:** Tests must boot much of Bukkit to reach a parse error, and one malformed field can partially register prior content.
- **Affected tests:** Items, skills, drops, shops, altars, blocks, perks, messages.
- **Small future refactor:** Centralize numeric coercion and validate a file before registration. Avoid a broad configuration abstraction.

### GUI callbacks directly perform economy/config mutations

- **Why it hurts:** Logic can only be tested through Triumph GUI event plumbing.
- **Affected tests:** Shop, sell, editors.
- **Small future refactor:** Extract only the transaction decision/result functions used by GUI callbacks; keep layout in the GUI.

### Constructors and registration create tasks/listeners

Examples include `ChargedItemAbility` and `MenuItemListener`.

- **Why it hurts:** Merely constructing a fixture changes global scheduler state; dynamic clones/listeners are hard to clean up.
- **Affected tests:** Ability/config/lifecycle tests.
- **Small future refactor:** Start tasks during explicit plugin/service startup and keep returned task handles for cancellation.

### Plugin hard dependencies complicate lifecycle tests

Vault, ProtocolLib, and MythicMobs are hard dependencies in `plugin.yml`; PlaceholderAPI and LuckPerms are soft.

- **Why it hurts:** A MockBukkit plugin-load test needs compatible stubs or descriptor overrides, and “Mythic absent” cannot be tested as a normal load while it remains a hard dependency.
- **Affected tests:** Enable/disable and integration matrix.
- **Small future refactor:** None until the intended dependency policy is decided. Test the descriptor as it exists first.

## Recommended test infrastructure

No dependency should be added until implementation is approved.

### Reuse now

- **JUnit 5:** Keep as the primary framework. Use parameterized tests for formulas, parsers, numeric boundaries, and serialization cases.
- **Mockito:** Keep for narrow Bukkit services, addon tables, and Vault provider behavior. Avoid mocking whole gameplay flows when a stateful fake is clearer.
- **H2/Jdbi/Hikari:** Continue using for fast transaction-contract tests. Do not infer MySQL row-lock behavior from H2.
- **Testcontainers MySQL:** Keep for fences, row locks, advisory locks, DDL/migration behavior, and the complete shared persistence contract. Add a CI job that clearly reports whether these tests ran.
- **JDK concurrency primitives:** Prefer `CountDownLatch`, `CyclicBarrier`, controlled executors/task queues, and fake clocks. The existing persistence tests already use the right pattern.

### Consider for the implementation phase

- **MockBukkit:** Use for player inventories, PDC/item metadata, events, schedulers, commands, and GUI behavior. Verify a version compatible with Paper 1.21.11 before adopting it. Do not use it as proof of MySQL or ProtocolLib behavior.
- **Awaitility:** Optional only for Testcontainers/integration polling where the system cannot expose a deterministic latch. It is unnecessary for most unit/concurrency tests.
- **jqwik:** Optional for the five compact invariants listed above. Do not use it for Bukkit lifecycles or random gameplay event sequences.
- **JaCoCo:** Optional diagnostic to find untouched packages after risk tests exist. Do not establish a repository-wide line percentage gate; it would reward trivial tests and penalize integration-heavy correctness checks.
- **A small fake Vault economy:** Prefer a test fixture in source over another dependency. It must support successful/failed deposits and withdrawals, call counts, and controlled balance changes.
- **A controlled Bukkit scheduler/main-thread fixture:** Extend the pattern already in `PlayerSkillsManagerLifecycleTest`; use it for prompts, mining, announcements, and shutdown ordering where MockBukkit scheduling is insufficient.

## Suggested implementation order

1. Write failing regression tests for Vault transaction failure, invalid monetary values, and shop/sell item conservation.
2. Write the custom-item identity forgery tests for altar and stats.
3. Define the durable reward/idempotency contract, then write failure/reconnect tests before changing progression code.
4. Define complete reset semantics for addon tables and add H2/MySQL contract tests.
5. Add full plugin shutdown/startup lifecycle tests around final saves, delayed DB initialization, task cancellation, and mining recovery.
6. Add serialization round trips, starting with `Perk`, `Stats`, `ItemInfo`/PDC, rewards, shops, and nested drop/boss/altar types.
7. Add skill parser/curve/event tests and stat aggregation/event tests.
8. Add boss ranking, duplicate-death, loot-share, altar state-machine, and delayed-announcement tests.
9. Add mining formula, progress, hammer, regeneration, chunk-unload, and crash-file tests.
10. Add config loader/reload atomicity tests and mutating command validation tests.
11. Add prompt race, ability state/task cleanup, and meaningful GUI behavior tests.
12. Add integration matrix tests for Vault/ProtocolLib/MythicMobs/PlaceholderAPI/LuckPerms and an addon fixture.
13. Add targeted property tests and low-priority presentation/index coverage only after the value-bearing invariants are protected.

This order reflects the repository as found: fenced persistence is already the strongest area, while economy conservation, reward durability, item identity, lifecycle orchestration, and serialization have the highest unprotected correctness cost.
