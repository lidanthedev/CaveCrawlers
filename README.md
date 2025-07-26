# CaveCrawlers

CaveCrawlers is a powerful Minecraft plugin that adds custom items, shops, and mining mechanics to your server. It
features advanced item management, player progression, and seamless integration with MythicMobs, making it ideal for RPG
adventures or custom survival worlds.

## Screenshots
Creating and editing items

![CaveCrawlers-item-create](https://github.com/user-attachments/assets/667da10b-52e5-4be8-b521-523341979857)



## Features

- Custom items with stats, abilities, and rarities
- Shops with editor and browser
- Mining system with block info and hardness
- Coin and player data management
- Integration with MythicMobs

## Commands

### Main Commands
- `/ct help` — Show this help message
- `/ct item` — Item commands
- `/ct shop` — Shop commands

### Item Commands
- `/ct item give <player> <Item id> [amount]` — Give a player an item
- `/ct item get <Item ID> [amount]` — Give yourself an item
- `/ct item browse` — Open the item browser
- `/ct item create <id> <material>` — Create an item
- `/ct item clone <originId> <id>` — Clone an item
- `/ct item edit stat <stat> <number>` — Edit an item's stat
- `/ct item edit ability <ability>` — Edit an item's ability
- `/ct item edit name <name>` — Edit an item's name
- `/ct item edit description <description>` — Edit an item's description
- `/ct item edit type <type>` — Edit an item's type
- `/ct item edit rarity <rarity>` — Edit an item's rarity
- `/ct item edit baseItem <material>` — Edit an item's base item
- `/ct item edit baseItemToHand <id>` — Edit an item's base item to the item in your hand
- `/ct item import <id>` — Import the item in your hand (advanced)

### Shop Commands
- `/ct shop create <name>` — Create a shop item
- `/ct shop open <shop-name>` — Open the shop
- `/ct shop editor <shop-name>` — Open the shop editor

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

This project is licensed under the MIT License.
