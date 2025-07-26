# CaveCrawlers Plugin

A Minecraft plugin that makes creating mmorpg server easy with custom items, shops, mining, and more. Below are the
available commands and their usage.

## Main Commands

- `/ct help` — Show this help message
- `/ct item` — Item commands
- `/ct shop` — Shop commands

## Item Commands

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

## Shop Commands

- `/ct shop create <name>` — Create a shop item
- `/ct shop open <shop-name>` — Open the shop
- `/ct shop editor <shop-name>` — Open the shop editor

## Other Features

- Reload items, shops, blocks, drops, and plugin
- Mining commands for block info and hardness
- Player data management
- Integration with MythicMobs for custom abilities

---

For more details, use `/ct help` in-game or check the source code for advanced usage and customization.
