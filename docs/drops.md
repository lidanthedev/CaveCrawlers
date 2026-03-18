# CaveCrawlers — Drops System

## Overview

The Drops system is the core loot engine of CaveCrawlers. It powers **four** different features, all built on the same
`Drop` object:

| Feature                            | Folder    | Description                                    |
|------------------------------------|-----------|------------------------------------------------|
| [Entity Drops](#entity-drops)      | `drops/`  | Loot tables for regular MythicMobs entities    |
| [Block Drops](#block-drops-mining) | `blocks/` | Rewards from the custom mining system          |
| [Boss Drops](#boss-drops)          | `bosses/` | Point-based loot for boss encounters           |
| [Altar Drops](#altar-system)       | `altars/` | Mob spawning via collaborative altar mechanics |

Every drop entry uses the same set of properties (`type`, `chance`, `value`, etc.), so once you understand one, you
understand them all.

---

## Quick Start

1. Navigate to the appropriate folder inside `plugins/CaveCrawlers/` (e.g. `drops/`, `blocks/`, `bosses/`, or
   `altars/`).
2. Create a new `.yml` file (any name works, e.g. `floor1.yml`).
3. Define your entries using the formats described below.
4. Reload the plugin.

---

## The Drop Object

The `Drop` is the shared building block used by every feature in the drops system. Whether it's a mob kill, a mined
block, a boss reward, or an altar spawn — they all use the same `Drop` object under the hood.

> **Important:** The `==:` lines in YAML files are Bukkit serialization tags. **Do not remove them** — they tell
> CaveCrawlers how to load the data.

### Drop Properties

Each drop entry has the following fields:

| Property         | Type   | Required | Description                                                                                           |
|------------------|--------|----------|-------------------------------------------------------------------------------------------------------|
| `type`           | String | ✅        | The type of drop. One of: `ITEM`, `MOB`, `COINS`, `COMMAND`.                                          |
| `chance`         | Double | ✅        | Base drop chance as a percentage (`0.0` – `100.0`).                                                   |
| `value`          | String | ✅        | What to drop. Format depends on `type` (see [Drop Types](#drop-types) below).                         |
| `announce`       | String | ❌        | Key of a message from `messages.yml` to send when the drop occurs. Set to `null` for no announcement. |
| `chanceModifier` | String | ❌        | A player stat that modifies the drop chance. Set to `null` to disable. Common value: `MAGIC_FIND`.    |
| `amountModifier` | String | ❌        | A player stat that modifies the drop amount. Set to `null` to disable.                                |

### Drop Types

#### `ITEM` — Give an Item

Gives the player a custom CaveCrawlers item.

**Value format:** `<ItemID>` or `<ItemID> <amount>`

- `<ItemID>` — The ID of the item registered in CaveCrawlers' item system.
- `<amount>` — A fixed number or a **range** (e.g. `1-5`). Defaults to `1` if omitted.

**Examples:**

```yaml
# Always drop exactly 1 Gold Ingot
- type: ITEM
  chance: 100.0
  value: GOLD_INGOT

# 50% chance to drop 2 to 5 Enchanted Diamonds
- type: ITEM
  chance: 50.0
  value: ENCHANTED_DIAMOND 2-5

# 0.5% chance rare drop with announcement
- type: ITEM
  chance: 0.5
  value: LEGENDARY_SWORD 1
  announce: rare_drop_message
  chanceModifier: MAGIC_FIND
```

---

#### `MOB` — Spawn a MythicMobs Entity

Spawns a MythicMobs mob at the kill location.

**Value format:** `<MythicMobInternalName>`

**Example:**

```yaml
# 5% chance to spawn a miniboss on kill
- type: MOB
  chance: 5.0
  value: floor1_miniboss
  announce: null
  chanceModifier: null
  amountModifier: null
```

---

#### `COINS` — Give Coins (Vault Economy)

Gives the player coins through the Vault economy.

**Value format:** `<amount>` or `<min>-<max>`

**Examples:**

```yaml
# Always give exactly 100 coins
- type: COINS
  chance: 100.0
  value: '100'

# 75% chance to give 50–200 coins
- type: COINS
  chance: 75.0
  value: '50-200'
  announce: null
  chanceModifier: MAGIC_FIND
  amountModifier: null
```

---

#### `COMMAND` — Run a Console Command

Executes a command from the console. Supports `%player%` as a placeholder for the player's name, and any PlaceholderAPI
placeholders if installed.

**Value format:** `<command string>`

**Examples:**

```yaml
# Give the player a permission via command
- type: COMMAND
  chance: 1.0
  value: lp user %player% permission set some.permission true

# Send a message to the player
- type: COMMAND
  chance: 100.0
  value: msg %player% You found a secret!
```

### Chance & Stat Modifiers

#### How Chance Works

- `chance` is a percentage between `0.0` and `100.0`.
- Each drop is rolled **independently** — a mob can drop multiple items in one kill.
- A chance of `100.0` means the drop is **guaranteed**.
- A chance of `0.01` means a 1-in-10,000 chance.

#### `chanceModifier` — Boosting Drop Chance with Player Stats

When `chanceModifier` is set (e.g. `MAGIC_FIND`), the effective drop chance is calculated as:

```
effectiveChance = chance × (1 + playerStatValue / 100)
```

**Example:** A player with **50 Magic Find** rolling a drop with `chance: 10.0` and `chanceModifier: MAGIC_FIND`:

```
effectiveChance = 10.0 × (1 + 50 / 100) = 10.0 × 1.5 = 15.0%
```

#### `amountModifier` — Boosting Drop Amount with Player Stats

When `amountModifier` is set, the drop amount is multiplied based on the player's stat:

- The stat value is divided by 100 to get a whole-number multiplier.
- The **remainder** is used as a percentage chance for an extra multiplier.

**Example:** A player with **250** of the modifier stat dropping an item with amount `3`:

```
multiplier = 1 + floor(250 / 100) = 3
remainder  = 250 % 100 = 50  →  50% chance to add +1 to multiplier
finalAmount = 3 × 3 = 9   (or 3 × 4 = 12 if the 50% roll succeeds)
```

#### Available Stats

| Stat Name      | ID               | Description                    |
|----------------|------------------|--------------------------------|
| Magic Find     | `MAGIC_FIND`     | Boosts rare drop chances       |
| Health         | `HEALTH`         | Player max health              |
| Defense        | `DEFENSE`        | Damage reduction               |
| Mana           | `MANA`           | Player max mana                |
| Intelligence   | `INTELLIGENCE`   | Mana-related                   |
| Speed          | `SPEED`          | Movement speed                 |
| Damage         | `DAMAGE`         | Attack damage                  |
| Strength       | `STRENGTH`       | Damage bonus                   |
| Crit Damage    | `CRIT_DAMAGE`    | Critical hit damage multiplier |
| Crit Chance    | `CRIT_CHANCE`    | Critical hit chance            |
| Attack Speed   | `ATTACK_SPEED`   | Attack speed                   |
| Ability Damage | `ABILITY_DAMAGE` | Ability damage bonus           |
| Mining Speed   | `MINING_SPEED`   | Block breaking speed           |
| Mining Fortune | `MINING_FORTUNE` | Extra mining drops             |
| Mining Power   | `MINING_POWER`   | Ability to mine harder blocks  |
| Mining Hammer  | `MINING_HAMMER`  | AoE mining range               |

> Any of these can be used as `chanceModifier` or `amountModifier`, but `MAGIC_FIND` is the most common choice for drop
> chance boosting.

### Announcements

The `announce` field references a **message key** defined in `messages.yml`. When a drop occurs and `announce` is set,
the message is sent to the player.

#### Built-in Message: `rare_drop_message`

```yaml
rare_drop_message:
  ==: me.lidan.cavecrawlers.objects.ConfigMessage
  message: '%dropRarity% %name%'
  actionbar: ''
  title: null
  sound:
    ==: me.lidan.cavecrawlers.objects.SoundOptions
    volume: 1.0
    sound: BLOCK_NOTE_BLOCK_PLING
    pitch: 1.0
```

#### Available Placeholders in Announce Messages

Depending on the drop type, the following placeholders are available:

| Placeholder    | Available For | Description                                  |
|----------------|---------------|----------------------------------------------|
| `%player%`     | All types     | Player's name                                |
| `%chance%`     | All types     | Base drop chance                             |
| `%newChance%`  | All types     | Effective chance after stat modifiers        |
| `%amount%`     | ITEM, COINS   | Number of items/coins dropped                |
| `%name%`       | ITEM, MOB     | Display name of the item or spawned mob      |
| `%rarity%`     | ITEM          | Item rarity (COMMON, UNCOMMON, RARE, etc.)   |
| `%dropRarity%` | ITEM          | Drop rarity tier based on chance (see below) |

#### Drop Rarity Tiers

The `%dropRarity%` placeholder is automatically determined by the drop's base chance:

| Chance  | Rarity Label   |
|---------|----------------|
| > 1%    | **RARE**       |
| ≤ 1%    | **VERY RARE**  |
| ≤ 0.2%  | **CRAZY RARE** |
| ≤ 0.01% | **INSANE**     |

---

## Entity Drops

**Folder:** `plugins/CaveCrawlers/drops/`

Entity drops define loot tables for regular MythicMobs entities. When a player kills a mob, CaveCrawlers rolls each drop
independently and rewards the player.

Each top-level key is a **MythicMobs internal name**. Under it you define an `EntityDrops` entry.

### EntityDrops Properties

| Property     | Type          | Description                                                                  |
|--------------|---------------|------------------------------------------------------------------------------|
| `entityName` | String        | Display name of the entity (supports `&` color codes). Used for UI/display.  |
| `xp`         | Integer       | Amount of vanilla XP given to the player when the mob is killed.             |
| `drops`      | List of Drops | A list of individual drop entries (see [Drop Properties](#drop-properties)). |

### Entity Drops Example

Here is a complete entity drop file for a floor with two mobs:

```yaml
# plugins/CaveCrawlers/drops/floor1.yml

SKELETAL_KNIGHT:
  entityName: '&aSkeletal Knight'
  xp: 10
  drops:
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 100.0
      value: BONE_FRAGMENT 1-3
      announce: null
      chanceModifier: null
      amountModifier: null
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 2.0
      value: ENCHANTED_BONE 1
      announce: rare_drop_message
      chanceModifier: MAGIC_FIND
      amountModifier: null
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: COINS
      chance: 100.0
      value: '10-50'
      announce: null
      chanceModifier: null
      amountModifier: null
  ==: me.lidan.cavecrawlers.drops.EntityDrops

CAVE_SPIDER_QUEEN:
  entityName: '&cCave Spider Queen'
  xp: 50
  drops:
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 100.0
      value: SPIDER_SILK 2-5
      announce: null
      chanceModifier: null
      amountModifier: MAGIC_FIND
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 0.1
      value: SPIDER_QUEEN_FANG 1
      announce: rare_drop_message
      chanceModifier: MAGIC_FIND
      amountModifier: null
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: MOB
      chance: 5.0
      value: cave_spider_minion
      announce: null
      chanceModifier: null
      amountModifier: null
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: COINS
      chance: 100.0
      value: '100-500'
      announce: null
      chanceModifier: MAGIC_FIND
      amountModifier: null
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: COMMAND
      chance: 0.01
      value: broadcast &6%player% &ehas received the &d&lSpider Queen's Blessing&e!
      announce: null
      chanceModifier: MAGIC_FIND
      amountModifier: null
  ==: me.lidan.cavecrawlers.drops.EntityDrops
```

---

## Block Drops (Mining)

**Folder:** `plugins/CaveCrawlers/blocks/`

Block drops define loot and mining behavior for CaveCrawlers' custom mining system. Each top-level key is a **Minecraft
Material name** (e.g. `DIAMOND_ORE`). When a player mines a registered block, it temporarily turns into a replacement
block, then regenerates after a delay. The drops use the standard `Drop` object.

### BlockInfo Properties

| Property               | Type          | Required | Default      | Description                                                                 |
|------------------------|---------------|----------|--------------|-----------------------------------------------------------------------------|
| `blockStrength`        | Integer       | ✅        | —            | How long it takes to mine the block. Higher = slower to break.              |
| `blockPower`           | Integer       | ✅        | —            | The minimum Mining Power stat a player needs to break this block.           |
| `drops`                | List of Drops | ✅        | —            | Standard drop entries (same `Drop` object as entity drops).                 |
| `brokenBy`             | String        | ❌        | `PICKAXE`    | The item type required to break the block (e.g. `PICKAXE`, `DRILL`, `AXE`). |
| `replacementBlockData` | String        | ❌        | `BLACK_WOOL` | The block data string to display while the block is regenerating.           |

### How Mining Works

1. A player starts mining a registered block with the correct tool type.
2. The player's **Mining Power** must be ≥ the block's `blockPower`, or the block won't break.
3. Break time is calculated from the player's **Mining Speed** stat and the block's `blockStrength`.
4. Once broken, each drop in the list is rolled independently (just like entity drops).
5. The block is replaced with `replacementBlockData` and **regenerates after 5 seconds** (100 ticks).

> **Tip:** The `MINING_FORTUNE` stat is commonly used as the `amountModifier` for block drops to give bonus ore.

### Block Drops Example

```yaml
# plugins/CaveCrawlers/blocks/ores.yml

DIAMOND_ORE:
  blockStrength: 500
  blockPower: 3
  brokenBy: PICKAXE
  replacementBlockData: minecraft:black_wool
  drops:
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 100.0
      value: ENCHANTED_DIAMOND 1
      announce: null
      chanceModifier: null
      amountModifier: MINING_FORTUNE
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: COINS
      chance: 100.0
      value: '5-15'
      announce: null
      chanceModifier: null
      amountModifier: null
  ==: me.lidan.cavecrawlers.mining.BlockInfo

GOLD_ORE:
  blockStrength: 300
  blockPower: 2
  brokenBy: PICKAXE
  drops:
    - ==: me.lidan.cavecrawlers.drops.Drop
      type: ITEM
      chance: 100.0
      value: ENCHANTED_GOLD 1-2
      announce: null
      chanceModifier: null
      amountModifier: MINING_FORTUNE
  ==: me.lidan.cavecrawlers.mining.BlockInfo
```

---

## Boss Drops

**Folder:** `plugins/CaveCrawlers/bosses/`

Boss drops are an advanced version of entity drops designed for **boss encounters**. They extend the standard `Drop`
with two extra concepts:

- **Points** — Players earn points during a boss fight based on damage dealt. Top damage dealers receive **bonus points
  **. Each drop can require a minimum number of points to be eligible.
- **Tracks** — Drops can be grouped into "tracks". A player can only receive **one drop per track**, preventing
  duplicate rewards from the same loot category.

When a boss dies, the system:

1. Sorts all players by damage dealt.
2. Awards **bonus points** to the top damage dealers (configurable via `bonusPoints`).
3. For each player, iterates through the drops list — a drop is eligible only if the player has enough points and the
   chance roll succeeds.
4. If a drop has a `track`, that track is marked as "received" and no other drop on the same track can be given to that
   player.
5. **Announcements are broadcast** to all players in the same world (not just the recipient).

### BossDrops Properties

| Property      | Type              | Required | Default                     | Description                                                                                |
|---------------|-------------------|----------|-----------------------------|--------------------------------------------------------------------------------------------|
| `entityName`  | String            | ✅        | —                           | Display name of the boss (supports `&` color codes).                                       |
| `drops`       | List of BossDrops | ✅        | —                           | List of boss drop entries (see below).                                                     |
| `announce`    | String            | ❌        | `null`                      | Message key from `messages.yml` sent to all players in the world when the boss dies.       |
| `bonusPoints` | List of Integers  | ❌        | `[300, 250, 200, 150, 100]` | Bonus points awarded to top damage dealers. Index 0 = 1st place, index 1 = 2nd place, etc. |

### BossDrop Properties

Each boss drop has all the standard [Drop Properties](#drop-properties), plus:

| Property         | Type    | Required | Default | Description                                                                                      |
|------------------|---------|----------|---------|--------------------------------------------------------------------------------------------------|
| `requiredPoints` | Integer | ✅        | —       | Minimum points the player needs to be eligible for this drop. Set to `0` for no requirement.     |
| `track`          | String  | ❌        | `null`  | A track name. Only one drop per track can be received per player. Set to `null` for no tracking. |

### Boss Announce Placeholders

The boss-level `announce` message (sent on death) supports these placeholders:

| Placeholder              | Description                           |
|--------------------------|---------------------------------------|
| `%boss_name%`            | Name of the boss                      |
| `%boss_time%`            | Time in seconds the boss was alive    |
| `%attacker%`             | Display name of the killing player    |
| `%player_damage%`        | Damage dealt by the receiving player  |
| `%leaderboard_N_name%`   | Display name of Nth place (1-indexed) |
| `%leaderboard_N_points%` | Points of the Nth place player        |
| `%leaderboard_N_damage%` | Damage dealt by the Nth place player  |

Individual boss drops also broadcast their `announce` to all players in the same world (not just the recipient).

### How Tracks Work

Tracks let you create mutually exclusive drop groups. For example, if you have three armor pieces on the track
`"armor"`, a player can only receive **one** of them per kill — whichever one rolls first.

Drops with `track: null` are independent and do not block other drops.

### Boss Drops Example

```yaml
# plugins/CaveCrawlers/bosses/dragon.yml

CAVE_DRAGON:
  entityName: '&4&lCave Dragon'
  announce: boss_death_message
  bonusPoints:
    - 300
    - 250
    - 200
    - 150
    - 100
  drops:
    # Guaranteed coins for anyone with 0+ points
    - ==: me.lidan.cavecrawlers.bosses.BossDrop
      type: COINS
      chance: 100.0
      value: '500-1000'
      announce: null
      chanceModifier: MAGIC_FIND
      amountModifier: null
      requiredPoints: 0
      track: null
    # Rare weapon — only top contributors (200+ points), on "weapon" track
    - ==: me.lidan.cavecrawlers.bosses.BossDrop
      type: ITEM
      chance: 5.0
      value: DRAGON_SWORD 1
      announce: rare_drop_message
      chanceModifier: MAGIC_FIND
      amountModifier: null
      requiredPoints: 200
      track: weapon
    # Another weapon on the same track — can't get both
    - ==: me.lidan.cavecrawlers.bosses.BossDrop
      type: ITEM
      chance: 10.0
      value: DRAGON_BOW 1
      announce: rare_drop_message
      chanceModifier: MAGIC_FIND
      amountModifier: null
      requiredPoints: 150
      track: weapon
    # Armor piece — separate track, so player can get this AND a weapon
    - ==: me.lidan.cavecrawlers.bosses.BossDrop
      type: ITEM
      chance: 8.0
      value: DRAGON_CHESTPLATE 1
      announce: rare_drop_message
      chanceModifier: MAGIC_FIND
      amountModifier: null
      requiredPoints: 100
      track: armor
  ==: me.lidan.cavecrawlers.bosses.BossDrops
```

---

## Altar System

**Folder:** `plugins/CaveCrawlers/altars/`

Altars are collaborative world structures where players place items to summon a boss. They use `AltarDrop` (a simplified
version of `Drop`) to decide **which mob** to spawn.

### How Altars Work

1. An altar consists of multiple **altar block locations** in the world (e.g. End Portal Frames).
2. Players right-click an altar block while holding the required item to **place** it.
3. Each item placed is consumed and tracked per-player.
4. Once **all** altar locations have been filled, the altar **rolls** its spawn list to summon a mob.
5. The spawns list is iterated in order — the first spawn that passes its chance roll is selected.
6. Each player earns **points** based on how many items they contributed (`items placed × pointsPerItem`).
7. The spawned entity is registered as a **boss** — it uses the Boss Drops system for loot distribution.
8. After the boss dies, the altar **resets** after `altarRechargeTime` ticks, restoring the altar blocks.

### Altar Properties

| Property            | Type              | Required | Default            | Description                                                                                   |
|---------------------|-------------------|----------|--------------------|-----------------------------------------------------------------------------------------------|
| `altarLocations`    | List of Location  | ✅        | —                  | World locations of each altar block.                                                          |
| `spawnLocation`     | Location          | ✅        | —                  | Where the mob spawns when the altar is fully activated.                                       |
| `spawns`            | List of AltarDrop | ✅        | —                  | List of possible mob spawns with chances (see below).                                         |
| `itemToSpawn`       | String            | ✅        | —                  | The CaveCrawlers item ID that players must place on the altar.                                |
| `altarMaterial`     | String            | ❌        | `END_PORTAL_FRAME` | The block material of altar locations in their default (unfilled) state.                      |
| `alterUsedMaterial` | String            | ❌        | `BEDROCK`          | The block material altar locations change to after an item is placed.                         |
| `placeAnnounce`     | String            | ❌        | `null`             | Message key from `messages.yml` broadcast to the world when a player places an item.          |
| `spawnAnnounce`     | String            | ❌        | `null`             | Message key from `messages.yml` broadcast to the world when the boss spawns.                  |
| `pointsPerItem`     | Integer           | ❌        | `100`              | Points awarded per item placed. Used for boss drop point requirements.                        |
| `altarRechargeTime` | Integer           | ❌        | `200`              | Ticks (20 ticks = 1 second) before the altar resets after the boss dies. Default: 10 seconds. |

### AltarDrop Properties

Altar drops are simplified — they only spawn **MOB** type drops at the altar's spawn location:

| Property | Type   | Required | Description                                                      |
|----------|--------|----------|------------------------------------------------------------------|
| `type`   | String | ✅        | Should be `MOB`.                                                 |
| `chance` | Double | ✅        | Chance for this mob to be selected (percentage `0.0` – `100.0`). |
| `value`  | String | ✅        | The MythicMobs internal name of the mob to spawn.                |

> **Note:** Altar drops are rolled **sequentially** — the first spawn that passes its chance check is used, and no
> further spawns are checked. Place your rarest/most powerful mob first.

### Altar Announce Placeholders

**Place announce** (`placeAnnounce`):

| Placeholder       | Description                                   |
|-------------------|-----------------------------------------------|
| `%player%`        | Display name of the player who placed an item |
| `%item%`          | Name of the item placed                       |
| `%amount%`        | Total items placed on the altar so far        |
| `%player_amount%` | Items placed by this specific player          |
| `%max_amount%`    | Total altar slots (number of altar locations) |

**Spawn announce** (`spawnAnnounce`):

| Placeholder | Description                |
|-------------|----------------------------|
| `%entity%`  | Name of the spawned entity |

### Altar + Boss Drops Flow

Altars and Boss Drops are designed to work together:

1. **Altar config** → defines the structure, item cost, and which mob(s) can spawn.
2. **Boss drops config** → defines what loot the spawned boss drops, using points earned from altar contributions.
3. Players who contributed more items earn more points, making them eligible for rarer drops.

```
Player places items → Altar fills → Mob spawns (AltarDrop) → Players fight boss
→ Boss dies → Points calculated (items × pointsPerItem + bonusPoints for top damage)
→ Boss drops rolled per player based on points → Loot distributed
```

### Altar Example

```yaml
# plugins/CaveCrawlers/altars/dragon_altar.yml

DRAGON_ALTAR:
  altarLocations:
    - ==: org.bukkit.Location
      world: dungeon_world
      x: 100.0
      y: 64.0
      z: 200.0
    - ==: org.bukkit.Location
      world: dungeon_world
      x: 102.0
      y: 64.0
      z: 200.0
    - ==: org.bukkit.Location
      world: dungeon_world
      x: 104.0
      y: 64.0
      z: 200.0
    - ==: org.bukkit.Location
      world: dungeon_world
      x: 106.0
      y: 64.0
      z: 200.0
  spawnLocation:
    ==: org.bukkit.Location
    world: dungeon_world
    x: 103.0
    y: 65.0
    z: 203.0
  itemToSpawn: DRAGON_SCALE
  altarMaterial: END_PORTAL_FRAME
  alterUsedMaterial: BEDROCK
  placeAnnounce: altar_place_message
  spawnAnnounce: altar_spawn_message
  pointsPerItem: 100
  altarRechargeTime: 200
  spawns:
    # 5% chance for the rare variant (checked first!)
    - ==: me.lidan.cavecrawlers.altar.AltarDrop
      type: MOB
      chance: 5.0
      value: cave_dragon_enraged
    # 100% fallback — if the rare roll fails, always spawn the normal boss
    - ==: me.lidan.cavecrawlers.altar.AltarDrop
      type: MOB
      chance: 100.0
      value: cave_dragon
  ==: me.lidan.cavecrawlers.altar.Altar
```

---

## Tips

- **Multiple files:** You can create as many `.yml` files as you want in each folder. Organize by floor, dungeon, area,
  etc.
- **Multiple drops per entry:** Each mob/block/boss can have any number of drops. They are all rolled independently.
- **Guaranteed drops:** Set `chance: 100.0` for items that should always drop.
- **Range amounts:** Use the `min-max` format (e.g. `1-5`) for random amounts. Use a single number (e.g. `3`) for a
  fixed amount.
- **Testing:** Use the in-game drop editor command (`/cc test dropEditor`) to visually create and test drops.
- **PlaceholderAPI:** The `COMMAND` drop type supports PlaceholderAPI placeholders if the plugin is installed.
- **Altar + Boss synergy:** Set up an altar to spawn a boss, and a matching boss drops config to distribute loot based
  on contribution.
- **Tracks for bosses:** Use tracks to create "pick one" loot categories — e.g. one weapon OR one armor piece, not both.




