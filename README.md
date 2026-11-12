# CaveCrawlers

CaveCrawlers is a Java plugin for Paper Minecraft servers that provides RPG gameplay systems. It combines custom
items and abilities, combat stats, skill progression, mining, loot, bosses, summoning altars, and shops. Server owners
configure content in YAML and through in-game editors, while addon developers can extend gameplay through Java APIs
and Bukkit events.

## Important upgrade note: fenced player persistence

Upgrade **every CaveCrawlers backend sharing a database together**. Do not run pre-fencing and
post-fencing versions concurrently. Older versions write skill rows without validating fencing tokens.

For the first upgrade:

1. Stop all CaveCrawlers backend servers.
2. Update the plugin on every backend.
3. Start the network again.

Once every backend uses a compatible fenced version, rolling updates can be considered when schema
compatibility permits. Keep lease settings consistent across the network.
See [player persistence and verification](docs/player-persistence.md) for outage behavior, addon requirements,
and the database test suite.

## Screenshots
Creating and editing items

![CaveCrawlers-item-create-optimize](https://github.com/user-attachments/assets/8bbd0b55-e199-40ae-a6d4-af1e5506f1f7)

Demo item

![CaveCrawlers-item-create-result](https://github.com/user-attachments/assets/a3945f4c-cc73-45f9-9fc6-34b75b4a6964)

Shop editor

![CaveCrawlers-shop-editor-optimize](https://github.com/user-attachments/assets/c3b0ae12-2032-4383-b4c2-e24b3704df95)

## Features

- Custom items with stats, abilities, rarities, and creation/editing commands
- Custom damage calculations, health, defense, mana, and an action bar
- Skills, XP, level rewards, and perks
- Custom mining with block hardness, drops, and block restoration
- MythicMobs integration, configurable loot tables, and cooperative loot sharing
- Boss rewards and collaborative altars that summon mobs
- Shops with an in-game editor, ingredient costs, and coin prices, plus a sell menu
- An in-game index for mobs, blocks, bosses, altars, and their drops
- Vault economy integration and H2 or MySQL persistence for player progression

Addon developers can hook gameplay transactions through the [addon event API](docs/addon-events.md).

Native absorption can display as [one golden-heart row](docs/absorption-display.md), with the actual amount in the action bar.

## Commands

Arguments in `<angle brackets>` are required; arguments in `[square brackets]` are optional.
This is a reference for common commands, not every administrative or diagnostic command.

### Player commands

| Command | Purpose | Permission |
| --- | --- | --- |
| `/skills` | Open the skills menu | `cavecrawlers.skills` |
| `/stats list [player]` | Display stats, defaulting to yourself | `cavecrawlers.stats` |
| `/sell` | Open the sell menu | No command permission required |
| `/index` | Open the mob, block, boss, and altar index | `cavecrawlers.index.view` |
| `/profile [player]` | View a profile, defaulting to yourself | `cavecrawlers.playerviewer` |
| `/ccmenu` | Open the configured menu or execute its configured command | No command permission required |

Viewing another player's profile also requires `cavecrawlers.playerviewer.others`.
The index also has `/drops`, `/bestiary`, and `/wiki` aliases.

### Main administration commands

- `/cc help`: Show administration help
- `/cc item`: Item commands
- `/cc shop`: Shop commands
- `/cc altar`: Altar command help
- `/cc reload` or `/cc reload lite`: Reload gameplay configuration and content
- `/cc reload items`, `/cc reload shops`, `/cc reload blocks`, `/cc reload drops`: Reload a content category
- `/cc reload plugin`, `/cc reload addons`, `/cc reload all`: Reload plugins through PlugMan, which must be enabled

The `cavecrawlers.admin` permission defaults to operators and includes the main help, item, shop, altar,
reload, and data administration permissions. Player commands use the permissions listed above.
Database and lease configuration changes require a server restart.

See [player persistence](docs/player-persistence.md) for data ownership, outage behavior, and addon requirements.

### Item commands

- `/cc item give <player> <Item id> [amount]`: Give a player an item
- `/cc item get <Item ID> [amount]`: Give yourself an item
- `/cc item browse`: Open the item browser
- `/cc item create <id> <material>`: Create an item
- `/cc item clone <originId> <id>`: Clone an item
- `/cc item edit stat <stat> <number>`: Edit an item's stat
- `/cc item edit ability <ability>`: Edit an item's ability
- `/cc item edit name <name>`: Edit an item's name
- `/cc item edit description <description>`: Edit an item's description
- `/cc item edit type <type>`: Edit an item's type
- `/cc item edit rarity <rarity>`: Edit an item's rarity
- `/cc item edit baseItem <material>`: Edit an item's base item
- `/cc item edit baseItemToHand <id>`: Edit an item's base item to the item in your hand
- `/cc item import <id>`: Import the item in your hand (advanced)

Most `item edit` commands edit the registered custom item held in your main hand. `baseItemToHand <id>` instead
copies the held item into the specified custom item's base item; the held item must not already have a custom item ID.

### Shop and selling commands

- `/cc shop create <name>`: Create a shop
- `/cc shop open <shop-name>`: Open the shop
- `/cc shop editor <shop-name>`: Open the shop editor
- `/setprice <price> [itemId]`: Set a sell price, using the held custom item when the ID is omitted;
  requires `cavecrawlers.sell.setprice`

## Installation

1. Install the required plugins and a Vault-compatible economy provider listed below.
2. Download the CaveCrawlers jar from [releases](https://github.com/lidanthedev/CaveCrawlers/releases), or build it locally.
3. Place the jar in your server's `plugins` folder and start the server to generate configuration and example content.
4. Review `plugins/CaveCrawlers/config.yml`, especially the vanilla gameplay options and database settings.
5. Configure content in the generated folders, then use `/cc reload` for gameplay changes or restart for database changes.

The default configuration disables vanilla crafting, anvils, enchanting, item dropping outside creative mode,
hand swapping, and explosion block damage. Night vision is enabled and hunger is disabled. Review these settings
before using the plugin on an existing survival server.

## Requirements

- Java 21+
- [Paper](https://papermc.io/) Minecraft server. The current build targets the **1.21.11 Paper API**;
  this is the compilation target, not a verified minimum supported Minecraft version.
- [Vault](https://www.spigotmc.org/resources/vault.34315/)
- A Vault-compatible economy provider that registers an economy service. CaveCrawlers disables itself if no provider is available.
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
- [MythicMobs](https://mythiccraft.io/index.php?resources/mythicmobs.1/)

Optional integrations are PlaceholderAPI, LuckPerms, and PlugMan. PlugMan is needed only for the plugin/addon reload
commands above; `/cc reload` does not require it.

## Configuration and storage

Configuration and content live under `plugins/CaveCrawlers/`:

| Path | Contents |
| --- | --- |
| `config.yml` | Gameplay defaults, action bar, loot sharing, mining, and database settings |
| `messages.yml` | Configurable messages |
| `items/` | Custom item definitions |
| `shops/` | Shop definitions |
| `skills/` | Skill definitions and rewards |
| `perks/` | Perk definitions |
| `blocks/` | Mineable blocks and their drops |
| `drops/` | Mob loot tables |
| `bosses/` | Boss reward definitions |
| `altars/` | Summoning altars |

Start with the generated example YAML files. The [drops guide](docs/drops.md) explains mob loot, block drops,
boss rewards, and altar configuration.

Player progression uses a local H2 database by default, so a separate database server is not required.
To use MySQL, configure `database.type: mysql`, the connection settings, and credentials in `config.yml`, then restart.
Changing the database type does not migrate existing data. Follow the migration and shared-database upgrade guidance
in [player persistence](docs/player-persistence.md) before moving existing progression or connecting multiple backends.

The commands `/cc data migrate h2-to-mysql` and `/cc data migrate mysql-to-h2` copy skill data to the target database
and require `cavecrawlers.admin.data`. Keep `database.type` set to the source during the copy and configure the MySQL
connection settings before migrating. Stop other backends sharing the database before a cutover. The command does
not switch the running database; wait for successful completion before changing `database.type` and restarting.

## Development

Clone the repository and use Java 21 with the included Gradle wrapper:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The build runs tests and produces the shaded plugin jar in `build/libs/`.
To run tests separately:

```sh
./gradlew test
```

The MySQL persistence tests require a Docker-compatible runtime and are skipped when none is available.
See [persistence verification](docs/player-persistence.md#verification) for setup and coverage details.

## Documentation

- [Drops, bosses, and altars](docs/drops.md)
- [Player persistence, upgrades, migrations, and verification](docs/player-persistence.md)
- [Addon event API](docs/addon-events.md)
- [Native absorption display](docs/absorption-display.md)

Java entry points for addons are in [`me.lidan.cavecrawlers.api`](src/main/java/me/lidan/cavecrawlers/api).
Obtain the API through `CaveCrawlers.getAPI()` and declare CaveCrawlers as a dependency in your addon's plugin metadata.

## Contributing

Contributions are welcome! Please open issues or pull requests for improvements and bug fixes.

## License

This project is licensed under the [GPLv3 License](LICENSE).
