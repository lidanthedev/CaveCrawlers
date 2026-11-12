# CaveCrawlers

CaveCrawlers is a Java plugin for Paper Minecraft servers that provides RPG gameplay systems. It combines custom
items and abilities, combat stats, skill progression, mining, loot, bosses, summoning altars, and shops. Server owners
configure content in YAML and through in-game editors, while addon developers can extend gameplay through Java APIs
and Bukkit events.

## Upgrading

When upgrading older versions, stop and update **every backend sharing the database together**.
See the [migration guide](docs/migration.md) before upgrading or moving player data.

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

`<required>` `[optional]`

### Player commands

| Command | Aliases | Purpose | Permission |
| --- | --- | --- | --- |
| `/skills` | `/skills gui`, `/myskills gui`, `/skilladmin gui` | Open the skills menu | `cavecrawlers.skills` |
| `/stats list [player]` | `/stat list [player]`, `/statadmin list [player]` | Display stats, defaulting to yourself | `cavecrawlers.stats` |
| `/sell` | None | Open the sell menu | None |
| `/index` | `/drops`, `/bestiary`, `/wiki` | Open the mob, block, boss, and altar index | `cavecrawlers.index.view` |
| `/profile [player]` | `/playerviewer [player]`, `/myprofile [player]` | View a profile, defaulting to yourself | `cavecrawlers.playerviewer` |
| `/ccmenu` | `/ctmenu` | Open the configured menu | None |

Viewing another player's profile also requires `cavecrawlers.playerviewer.others`.

### Main administration commands

| Command prefix | Aliases |
| --- | --- |
| `/cc` | `/ct`, `/cavecrawlers` |

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

| Command | Aliases | Purpose |
| --- | --- | --- |
| `/cc shop create <name>` | `/ct shop create`, `/cavecrawlers shop create` | Create a shop |
| `/cc shop open <shop-name>` | `/ct shop open`, `/cavecrawlers shop open` | Open the shop |
| `/cc shop editor <shop-name>` | `/ct shop editor`, `/cavecrawlers shop editor` | Open the shop editor |
| `/setprice <price> [itemId]` | `/setsell <price> [itemId]` | Set a sell price, using the held custom item if the ID is omitted; requires `cavecrawlers.sell.setprice` |

## Installation

1. Install the required plugins and a Vault-compatible economy provider listed below.
2. Download the CaveCrawlers jar from [releases](https://github.com/lidanthedev/CaveCrawlers/releases).
3. Place the jar in your server's `plugins` folder and start the server to generate configuration and example content.
4. Review `plugins/CaveCrawlers/config.yml`, especially the vanilla gameplay options and database settings.
5. Configure content in the generated folders, then use `/cc reload` for gameplay changes or restart for database changes.

CaveCrawlers defaults are set up for RPG servers. Crafting, anvils, enchanting, item dropping outside creative mode,
hand swapping, and explosion block damage are turned off. To allow them, set the corresponding options under
`vanilla` to `true` in `plugins/CaveCrawlers/config.yml`. Set `hunger-disabled: false` to restore hunger and
`night-vision: false` to stop giving night vision. Restart the server after changing these settings.

## Requirements

- Java 21+
- [Paper](https://papermc.io/) Minecraft server
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
**MySQL support is experimental and not recommended.** See the [migration guide](docs/migration.md) for moving data.

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
- [Upgrades and database migration](docs/migration.md)
- [Player persistence and verification](docs/player-persistence.md)
- [Creating an addon](docs/creating-an-addon.md)
- [Addon event API](docs/addon-events.md)
- [Native absorption display](docs/absorption-display.md)

## Contributing

Contributions are welcome! Please open issues or pull requests for improvements and bug fixes.
New features should be useful to any RPG server. If a feature is specific to your server, make an
[addon](docs/creating-an-addon.md) instead. New abilities are welcome too.

## License

This project is licensed under the [GPLv3 License](LICENSE).
