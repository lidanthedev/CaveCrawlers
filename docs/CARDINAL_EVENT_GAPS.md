# CaveCrawlers event audit

Audited develop `cce5fda` before modifications. All proposed observation events below are public addon API, immutable and non-cancellable. Existing cancellable events retain their semantics. Additions belong on `codex/cardinal-events` and do not change player persistence or balances.

## CurrencyTransactionEvent, implemented for native grants and withdrawals

Needed for currency issuance and sinks. VaultUtils returns success but emits no event; its callers know the reason. Fire immediately after a successful positive rounded Vault deposit/withdrawal. Fields: owner UUID, currency `coins`, amount in coins, created/destroyed direction, reason and source identifier. Explicit native reasons: shop_purchase, shop_sell, drop_reward, admin_grant, admin_remove. Existing two-argument methods remain compatible and report `unspecified`; they cannot describe third-party intent. Set-balance is deliberately excluded because its current two-step implementation does not guarantee atomic success. Transfers must get their own correlated event rather than being reported as mint/burn. External Vault providers/addons are outside the coverage claim.

## ShopTransactionCompletedEvent, implemented

Needed for completed purchases, sales and ingredient/item supply. Current ShopPurchaseEvent and ShopSellEvent precede affordability, Vault success, delivery and cancellation. Listening at MONITOR still observes attempted operations. Fire purchase after successful debit, ingredient removal and item delivery; fire sale only after successful deposit. Fields: owner UUID, transaction UUID, purchase/sale kind, actual normalized price, immutable item-ID quantities, immutable consumed ingredient quantities. For sales capture the actual accepted stacks without exposing live inventories. Existing pre-events remain cancellable. Item construction is not creation: ItemBuildEvent fires for menus/previews, so it is never counted as supply.

## ItemGrantedEvent, implemented for native item drops

Needed for rolled item quantities after modifiers. DropGiveEvent occurs before the random quantity and before validation. Fire after ItemsManager.giveItem in Drop.giveItem with owner UUID, item ID, final positive quantity, reason drop_reward, source drop type. It reports the delivery API completion, including inventory overflow delivery. It cannot prove that a later world item survives ClearLag or pickup. Command rewards remain opaque and are not parsed. Generic crafting, altar consumption, upgrades and arbitrary ability item consumption need corresponding completed domain events at each validated mutation; do not infer those from inventory clicks. Public fields should include transaction UUID, actual inputs/outputs and reason. No speculative hooks have been added for those operations.

## DamageResolvedEvent, implemented

Needed for reliable crit/ability aggregates. DamageCalculationEvent precedes application and remains mutable/cancellable. Capture its semantic calculation context while CaveCrawlers handles the Bukkit event, then fire a completed observation at Bukkit MONITOR only if not cancelled with positive final damage. Fields: player, target, damage in Bukkit final-damage units, critical flag, ability flag from the actual AbilityDamage calculation type. Cancelled events are removed from the temporary context. This is resolved damage after modifiers, not measured health lost; overkill and absorption accounting must not be called effective damage. Other plugins must follow Bukkit's rule against mutating at MONITOR. No ability source is guessed from generic Bukkit damage.

## SkillProgressionChangedEvent, implemented for giveXp

Needed for immediate state snapshots and bucket boundaries. SkillXpGainEvent is cancellable and precedes mutation and level-up. Fire after successful XP and tryLevelUp with owner, skill ID, previous/new level and actual XP delta. Existing APIs remain compatible. Direct admin skill reset/set and persistence-session replacement still need a generic progression/reset event at their successful logical commit, including old/new values and reason. Periodic/logout snapshots observe current state without falsely assigning a reset cause. Never use an unloaded player's temporary default skills.

## Existing APIs reused

PlayerSkillsManager.isLoaded and PlayerDataManager.getSkills expose loaded public progression state. Paper's PLAY_ONE_MINUTE statistic supplies total playtime ticks; CaveCrawlers has no separate authoritative playtime history. StatsCalculateEvent is calculation, not a progression commit. PlayerItemAbilityUseEvent is a pre-operation signal and does not establish successful mana/item consumption. AltarUseEvent and BlockMineStartEvent likewise do not prove consumption or completed rewards. PlayerDataLoadEvent supports readiness but persistence saves do not identify gameplay reasons.

## Boss, reward and area coverage still needing API work

CaveCrawlers also has native `BossEntityData`, `BossManager` and `BossDrops`: BossEntityData records damage/points and runs guarded death reward handling, and BossDrops performs eligibility/track selection. These public objects expose mutable implementation collections and death callbacks, not a stable started/completed observation with immutable participant/reward context. Recommended native BossEncounterStarted/Completed observations should fire when EntityManager first registers a boss/claims its death, with entity/attempt ID, Mythic internal ID, immutable damage/points participant snapshots and result. Per-player RewardGranted should fire after each successful native reward mutation. No direct reads of damageMap/points or onDeath callback injection are used by Cardinal.

CoinSkillReward and ItemSkillReward currently create ordinary Drop objects; EntityDrops and BossDrops invoke the same Drop path. The new post-grant events truthfully identify `drop_reward` but do not distinguish skill rewards, normal mob rewards and point-selected boss rewards. Propagate a public immutable reward context containing reason, source ref and correlation ID through those existing operations before claiming that breakdown. Preserve BossDrop's overridden grant/announcement behavior when designing that API. This gap is documented rather than inferred from the call stack or mutable drop configuration.

MythicMobs public spawn/death/despawn events and internal IDs are available in 5.11.2. Cardinal requires configured boss IDs and starts an attempt on first observed player engagement. Initial party is the first engager; final party is observed contributors, not an authoritative party roster. Despawn, timeout and plugin shutdown complete attempts with explicit result and censoring semantics.

FishingAddon owns Kuudra's custom multi-stage encounter, rewards and party lifecycle. A Mythic mob spawn/death alone cannot describe that logical encounter. Recommended public EncounterStarted/EncounterCompleted/RewardGranted events must fire after authoritative encounter state transitions/reward grants, carry encounter ID, boss/dungeon ref, participant UUIDs, result, reward refs/quantities and failure reason, and distinguish victory from an internal stage mob death. Non-cancellable completed observations, additive API; no reflection into FishingAddon. Area transitions can use a dedicated post-region-change event if an authoritative progression area is introduced; v1 does not infer dungeon identity from coordinates.

Economy and item totals therefore describe the explicitly supported native flows, not the complete server economy. New source APIs are required before drawing server-wide inflation or item-supply conclusions.
