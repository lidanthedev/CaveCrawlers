# CaveCrawlers

CaveCrawlers is a Paper plugin for RPG and mining servers. It adds custom items with stats and abilities, a custom
mining system, skills, shops, mob and boss drops, summoning altars, and a coin economy. MythicMobs handles the mobs.
Player data lives in an H2 or MySQL database that several backend servers can share.

## Features

- **Items.** Custom items with stats, abilities, rarities, and types. Create and edit them in game or in YAML.
- **Stats.** 16 stats, including health, defense, mana, crit chance, mining speed, and mining fortune. They come from
  items, skills, and perks. The action bar shows them, and absorption can show as
  [one golden-heart row](docs/absorption-display.md).
- **Mining.** Each block has its own hardness and break power, drops, and regeneration. Mining speed comes from stats.
- **Skills.** Players earn XP from configured objectives and get coins, items, stats, or commands as level rewards.
- **Shops and selling.** Shops have an in-game editor and browser. `/sell` sells items for their configured price.
- **Drops.** Mob drops have rarities and chances. See [drops](docs/drops.md).
- **Bosses and altars.** Players place items on an altar to summon a MythicMobs boss. Each player's drops depend on
  points from altar items plus a bonus for ranking high in damage.
- **Index.** A browsable guide to mobs, blocks, bosses, and altars, opened with `/index`.
- **Perks and levels.** Perks grant stat bonuses by permission, one per track. Levels have their own color.
- **Addon API.** Other plugins can listen to gameplay transactions through the [addon event API](docs/addon-events.md).

## Screenshots

Creating and editing items

![CaveCrawlers-item-create-optimize](https://github.com/user-attachments/assets/8bbd0b55-e199-40ae-a6d4-af1e5506f1f7)

Demo item

![CaveCrawlers-item-create-result](https://github.com/user-attachments/assets/a3945f4c-cc73-45f9-9fc6-34b75b4a6964)

Shop editor

![CaveCrawlers-shop-editor-optimize](https://github.com/user-attachments/assets/c3b0ae12-2032-4383-b4c2-e24b3704df95)

## Requirements

- Java 21+
- [Paper](https://papermc.io/) 1.21
- [Vault](https://www.spigotmc.org/resources/vault.34315/)
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
- [MythicMobs](https://mythiccraft.io/index.php?resources/mythicmobs.1/)

Optional:

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholders
- [LuckPerms](https://luckperms.net/)
- [PlugMan](https://www.spigotmc.org/resources/plugmanx.88135/), needed by `/cc reload plugin`, `/cc reload addons`, and
  `/cc reload all`

## Installation

1. Download the latest jar from the releases page.
2. Put it in your server's `plugins` folder with the required plugins.
3. Start the server once to generate the config files.
4. Edit `plugins/CaveCrawlers/config.yml` and the content folders, then run `/cc reload`.

## Configuration

`config.yml` holds global settings: menus, action bar, mining, skill XP curves, vanilla tweaks, and the database. The
plugin generates an example file in each content folder on first start.

| Folder    | Contents                       |
|-----------|--------------------------------|
| `items/`  | Custom items                   |
| `shops/`  | Shops                          |
| `blocks/` | Mineable blocks and hardness   |
| `drops/`  | Mob drops                      |
| `skills/` | Skills, objectives, rewards    |
| `bosses/` | Boss drops                     |
| `altars/` | Summoning altars               |
| `perks/`  | Perks                          |

`/cc reload` reloads gameplay config. `/cc reload items`, `shops`, `blocks`, and `drops` reload one folder.

### Database

The default is a local H2 file, which needs no setup. To share data between servers, set `database.type` to `mysql`
and fill in the connection settings. `/cc data migrate h2-to-mysql` and `/cc data migrate mysql-to-h2` copy player data
between the two.

Servers that share a database use leases so only one server writes a player's data at a time. Use the same lease
settings on every server. See [player persistence](docs/player-persistence.md) for outage behavior and the database
tests.

> [!IMPORTANT]
> When upgrading from a version without fenced persistence, stop every backend that shares the database, update the
> plugin on all of them, then start them again. Older versions write skill rows without checking fencing tokens, so
> old and new versions must not run at the same time. After that, rolling updates are fine as long as the schema stays
> compatible.

## Commands

`/cc` also works as `/ct` and `/cavecrawlers`. Run `/cc help` in game for the item, shop, and altar help pages.

### Player commands

| Command                                  | Description                                  |
|------------------------------------------|----------------------------------------------|
| `/ccmenu`                                | Open the main menu                           |
| `/stats [list\|lore] [player]`           | Show your stats                              |
| `/skills`                                | Open the skills menu                         |
| `/index [mobs\|blocks\|bosses\|altars]`  | Open the index (`/drops`, `/bestiary`, `/wiki`) |
| `/sell`                                  | Open the sell menu                           |
| `/profile [player]`                      | View a player's profile (`/playerviewer`)    |

### Item commands

| Command                                         | Description                                 |
|-------------------------------------------------|---------------------------------------------|
| `/cc item give <player> <id> [amount]`          | Give a player an item                       |
| `/cc item get <id> [amount]`                    | Give yourself an item                       |
| `/cc item browse`                               | Open the item browser                       |
| `/cc item create <id> <material>`               | Create an item                              |
| `/cc item clone <originId> <id>`                | Clone an item                               |
| `/cc item edit stat <stat> <number>`            | Set a stat on the held item                 |
| `/cc item edit ability <ability>`               | Set the held item's ability                 |
| `/cc item edit name <name>`                     | Set the held item's name                    |
| `/cc item edit description <description>`       | Set the held item's description             |
| `/cc item edit type <type>`                     | Set the held item's type                    |
| `/cc item edit rarity <rarity>`                 | Set the held item's rarity                  |
| `/cc item edit baseItem <material>`             | Set the held item's base material           |
| `/cc item edit baseItemToHand <id>`             | Use the held item as the base for `<id>`    |
| `/cc item import <id>`                          | Import the held item as a new item          |
| `/cc item remove <id> [confirm]`                | Delete an item                              |
| `/cc item update`                               | Refresh the held item from its config       |

### Shop commands

| Command                           | Description              |
|-----------------------------------|--------------------------|
| `/cc shop create <name>`          | Create a shop            |
| `/cc shop open <shop>`            | Open a shop              |
| `/cc shop editor <shop>`          | Open the shop editor     |
| `/setprice <price> [item]`        | Set an item's sell price |

### Altar commands

| Command                                                  | Description                                    |
|----------------------------------------------------------|------------------------------------------------|
| `/cc altar create <name>`                                | Create an altar                                |
| `/cc altar addSpawn <altar> <mob> <chance>`              | Add a boss the altar can summon                |
| `/cc altar addSummonBlock <altar>`                       | Add the block you're looking at as a summon block |
| `/cc altar setSpawnLocation <altar>`                     | Set where the boss spawns                      |
| `/cc altar info <altar>`                                 | Show altar details                             |

### Admin commands

| Command                                         | Description                                   |
|-------------------------------------------------|-----------------------------------------------|
| `/cc coins <get\|give\|set\|take> <player> ...` | Manage a player's coins                       |
| `/stats <add\|set> <stat> <amount> [player]`    | Change a player's base stats                  |
| `/skills giveXp <player> <skill> <amount>`      | Give skill XP                                 |
| `/cc mining setHardness <strength> <power>`     | Set hardness for the block you're looking at  |
| `/cc level set lvl <level>`                     | Set your level                                |
| `/cc data <load\|save\|reset> [player]`         | Load, save, or reset player data              |
| `/cc reload [items\|shops\|blocks\|drops]`      | Reload config                                 |
| `/cc reload plugin\|addons\|all`                | Reload with PlugMan                           |

## Permissions

Every permission defaults to op.

| Permission                   | Grants                                                          |
|------------------------------|-----------------------------------------------------------------|
| `cavecrawlers.admin`         | All `/cc` admin commands and `cavecrawlers.stats`               |
| `cavecrawlers.admin.<area>`  | One area, such as `item`, `shop`, `coins`, `reload`, or `data`  |
| `cavecrawlers.stats`         | `/stats`                                                        |
| `cavecrawlers.stats.admin`   | `/stats add`, `set`, and `apply`                                |
| `cavecrawlers.skills`        | `/skills`                                                       |
| `cavecrawlers.skills.admin`  | Skill XP commands                                               |
| `cavecrawlers.index.view`    | `/index`                                                        |
| `cavecrawlers.playerviewer`  | `/profile`                                                      |
| `cavecrawlers.sell.setprice` | `/setprice`                                                     |

Give players `cavecrawlers.stats`, `cavecrawlers.skills`, `cavecrawlers.index.view`, and `cavecrawlers.playerviewer`
for the player commands.

## Development

```
./gradlew build
```

The shaded jar lands in `build/libs`. `./gradlew test` runs the unit tests. The MySQL tests need Docker, see
[player persistence](docs/player-persistence.md).

## Contributing

Open an issue or a pull request.

## License

GPLv3.
