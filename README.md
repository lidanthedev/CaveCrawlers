# CaveCrawlers

CaveCrawlers is a powerful Minecraft plugin that adds custom items, shops, and mining mechanics to your server. It
features advanced item management, player progression, and seamless integration with MythicMobs, making it ideal for RPG
adventures or custom survival worlds.

## Screenshots
Creating and editing items

![CaveCrawlers-item-create-optimize](https://github.com/user-attachments/assets/8bbd0b55-e199-40ae-a6d4-af1e5506f1f7)

Demo item

![CaveCrawlers-item-create-result](https://github.com/user-attachments/assets/a3945f4c-cc73-45f9-9fc6-34b75b4a6964)

Shop editor

![CaveCrawlers-shop-editor-optimize](https://github.com/user-attachments/assets/c3b0ae12-2032-4383-b4c2-e24b3704df95)

## Features

- Custom items with stats, abilities, and rarities
- Shops with editor and browser
- Mining system with block info and hardness
- Coin and player data management
- Integration with MythicMobs

## Commands

### Main Commands
- `/cc help` — Show this help message
- `/cc item` — Item commands
- `/cc shop` — Shop commands

### Item Commands
- `/cc item give <player> <Item id> [amount]` — Give a player an item
- `/cc item get <Item ID> [amount]` — Give yourself an item
- `/cc item browse` — Open the item browser
- `/cc item create <id> <material>` — Create an item
- `/cc item clone <originId> <id>` — Clone an item
- `/cc item edit stat <stat> <number>` — Edit an item's stat
- `/cc item edit ability <ability>` — Edit an item's ability
- `/cc item edit name <name>` — Edit an item's name
- `/cc item edit description <description>` — Edit an item's description
- `/cc item edit type <type>` — Edit an item's type
- `/cc item edit rarity <rarity>` — Edit an item's rarity
- `/cc item edit baseItem <material>` — Edit an item's base item
- `/cc item edit baseItemToHand <id>` — Edit an item's base item to the item in your hand
- `/cc item import <id>` — Import the item in your hand (advanced)

### Shop Commands
- `/cc shop create <name>` — Create a shop item
- `/cc shop open <shop-name>` — Open the shop
- `/cc shop editor <shop-name>` — Open the shop editor

## Installation

1. Download the latest CaveCrawlers jar from the releases.
2. Place the jar file in your server's `plugins` folder.
3. Start or restart your server.
4. Configure items, shops, and other features in the config files.

## Requirements

- Paper Minecraft server
- Java 17+
- (Optional) MythicMobs for advanced features

## Development

Clone the repository and import it into your IDE. Build using Gradle:

```
gradlew build
```

## Contributing

Contributions are welcome! Please open issues or pull requests for improvements and bug fixes.

## License

This project is licensed under the GPLv3 License.
