# CaveCrawlers Addon Events

CaveCrawlers exposes Bukkit events at the main gameplay transaction boundaries so addons can inspect, modify, or
cancel actions before their side effects occur.

## Registering a listener

Register the listener from your addon as usual:

```java
public final class MyAddon extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new CaveCrawlersListener(), this);
    }
}
```

## Event reference

| Event | Fired before | Addon controls |
| --- | --- | --- |
| `PlayerItemAbilityUseEvent` | Ability cooldown and mana checks | `cost`, `cooldown`, cancellation |
| `DamageCalculationEvent` | Calculated damage is applied to a mob | `damage`, `critical`, cancellation |
| `ShopPurchaseEvent` | Purchase validation and side effects | `price`, ingredients, result amount, cancellation |
| `ShopSellEvent` | Sale payout and menu close | sale price, unsellable-item handling, cancellation |
| `BlockMineStartEvent` | Mining progress starts | required ticks, cancellation |
| `DropGiveEvent` | A rolled drop is delivered | inspection, cancellation |
| `AltarUseEvent` | Altar item consumption and block update | contribution points, cancellation |
| `ItemBuildEvent` | A built item is returned | replacement `ItemStack` |
| `ItemUpdateEvent` | An updated item is returned | replacement `ItemStack` |

All event classes use standard Bukkit `HandlerList` registration. Cancellable events stop the corresponding action
before its side effects.

## Ability use

`me.lidan.cavecrawlers.items.PlayerItemAbilityUseEvent` is fired by normal abilities, charged abilities, and Short
Bow abilities. It includes the ability, the held `ItemStack`, and the resolved `ItemInfo` when available.

```java
@EventHandler
public void onAbilityUse(PlayerItemAbilityUseEvent event) {
    if (event.getItemInfo() != null && event.getItemInfo().getID().equals("FREE_WAND")) {
        event.setCost(0);
        event.setCooldown(100);
    }
}
```

The modified cost is used for the mana check and deduction. The modified cooldown is used for the cooldown check
and cooldown display. Costs must be finite and non-negative; cooldowns must not be negative.

## Damage calculation

`me.lidan.cavecrawlers.damage.DamageCalculationEvent` is fired after CaveCrawlers calculates player damage and after
the configured server damage multiplier is applied, but before the Bukkit damage event receives the value.

```java
@EventHandler
public void onDamageCalculation(DamageCalculationEvent event) {
    if (event.getTarget().getScoreboardTags().contains("resistant")) {
        event.setDamage(event.getDamage() * 0.5);
    }
}
```

`getCalculation()` contains the original `DamageCalculation` for direct damage. It can be `null` for projectile hits,
because projectiles store their already-calculated damage and critical state. `setCritical` changes the critical
indicator used by CaveCrawlers; it does not recalculate the damage formula. Bukkit's later final-damage modifiers may
still apply.

## Shop events

`ShopPurchaseEvent` fires before the shop checks the player's balance and ingredients. `getIngredients()` returns a
mutable map, and `setIngredients` can replace the map entirely. Price and result amount changes are used for the
actual transaction.

`ShopSellEvent` fires after the sell menu has identified sellable and unsellable stacks, but before coins are paid or
the menu is closed. The item lists are read-only snapshots. `setPrice` changes the payout, and
`setTrashUnsellable` controls whether unsellable stacks are returned. Cancelling leaves the menu contents untouched.

Shop prices are normalized to one decimal place by the existing economy helpers.

## Mining, drops, and altars

`BlockMineStartEvent` fires only after the block, tool type, and mining power have passed validation. Changing
`requiredTicks` changes the mining task; cancelling prevents progress from starting.

`DropGiveEvent` fires from `Drop.drop` after the chance roll succeeds and before the item, coin, command, or mob reward
is delivered. The player may be `null` for non-player drop sources. The event currently supports inspection and
cancellation; it does not change the already-rolled chance or reward amount.

`AltarUseEvent` fires after the altar and held item have been validated, but before the item is consumed or the altar
block changes. `setPoints` changes the contribution recorded for that placement. `getFinalPlacement()` indicates that
the placement fills the final altar location; it is read-only.

## Item build and update

`ItemBuildEvent` fires after CaveCrawlers constructs an item. `ItemUpdateEvent` fires after CaveCrawlers rebuilds an
existing item while preserving its metadata. Both events expose `setBuiltItem(ItemStack)`, and the replacement stack is
the one returned to the caller. They are not cancellable; use the original stack as the replacement when an update
should be effectively skipped.
