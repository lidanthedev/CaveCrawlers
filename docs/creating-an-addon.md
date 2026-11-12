# Creating a CaveCrawlers addon

An addon is a separate Paper plugin that depends on CaveCrawlers. Use one for server-specific mechanics,
custom content, or integrations. Features useful to any RPG server and new abilities are also welcome as
contributions to CaveCrawlers itself.

## Set up the project

Create a Java 21 Gradle project with the Gradle wrapper and add CaveCrawlers through JitPack.

`settings.gradle.kts`:

```kotlin
rootProject.name = "MyAddon"
```

`build.gradle.kts`:

```kotlin
plugins {
    java
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.lidanthedev:CaveCrawlers:VERSION")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

Replace `VERSION` with the release tag or commit matching your server's CaveCrawlers version from
[JitPack](https://jitpack.io/#lidanthedev/CaveCrawlers).
Keep Paper and CaveCrawlers as `compileOnly` dependencies. The server provides them; do not bundle their classes
in your addon jar. Use the Paper version that matches your development server.

## Declare the plugin

Create `src/main/resources/plugin.yml`:

```yaml
name: MyAddon
version: '1.0.0'
main: com.example.myaddon.MyAddon
api-version: '1.21'
depend:
  - CaveCrawlers
```

`depend` makes Paper load CaveCrawlers before your addon.

## Register an ability

Create `src/main/java/com/example/myaddon/MyAddon.java`:

```java
package com.example.myaddon;

import me.lidan.cavecrawlers.CaveCrawlers;
import me.lidan.cavecrawlers.items.abilities.ClickAbility;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyAddon extends JavaPlugin {
    @Override
    public void onEnable() {
        CaveCrawlers.getAPI().getAbilityAPI().registerAbility("MYADDON_SPARK",
                new ClickAbility("Spark", "Create a burst of particles", 10, 1000) {
                    @Override
                    protected boolean useAbility(PlayerEvent event) {
                        Player player = event.getPlayer();
                        player.getWorld().spawnParticle(Particle.END_ROD,
                                player.getEyeLocation(), 12, 0.3, 0.3, 0.3, 0);
                        return true;
                    }
                });
    }
}
```

`ClickAbility` handles right-click detection. CaveCrawlers handles mana costs and cooldowns, measured in milliseconds,
and registers the ability's listener automatically. Return `true` when the ability succeeds; return `false` to
avoid charging mana or starting its cooldown. Prefix ability IDs with your addon's name to avoid collisions.

Build with `./gradlew build` or `gradlew.bat build` on Windows. Put the addon jar from `build/libs/` in the server's
`plugins/` folder alongside CaveCrawlers and its required plugins, then restart.

Create a test item, hold it, and assign the ability:

```text
/cc item create SPARK_WAND BLAZE_ROD
/cc item edit ability MYADDON_SPARK
```

Right-click with the wand to test the particles, mana cost, and cooldown. Restart the server when replacing an
addon that registers abilities; its listeners are registered under CaveCrawlers.

## Use APIs and events

[`CaveCrawlers.getAPI()`](../src/main/java/me/lidan/cavecrawlers/api/CaveCrawlersAPI.java) provides APIs for items,
stats, skills, damage, mining, drops, bosses, abilities, action bars, prompts, and entities.
Register your own Bukkit event listeners with `getServer().getPluginManager().registerEvents(listener, this)`.
See the [addon event reference](addon-events.md) for changing or cancelling purchases, damage, drops, and ability use.

To load item, shop, block, drop, boss, perk, or altar YAML files from your addon's data folder, save those resources
into the matching subfolders and call `CaveCrawlers.getAPI().registerFromConfigs(this)` after registering your abilities.
Use the core plugin's [example resources](../src/main/resources) and [drops guide](drops.md) for the formats.
`/cc reload` reloads the core plugin's content; your addon must reload its own files if needed.

If you store player progression, follow the [persistence addon contract](player-persistence.md#addon-contract).
Keep live player-state mutations on the server thread and check that the player's persistence session is valid.

## Public examples

| Addon | Examples to read |
| --- | --- |
| [GriffinAddon](https://github.com/lidanthedev/GriffinAddon) | [Ability registration and plugin setup](https://github.com/lidanthedev/GriffinAddon/blob/master/src/main/java/me/lidan/griffinAddon/GriffinAddon.java), [Spade ability](https://github.com/lidanthedev/GriffinAddon/blob/master/src/main/java/me/lidan/griffinAddon/abilities/SpadeAbility.java), custom stats and content loading |
| [DungeonCrawlers](https://github.com/lidanthedev/DungeonCrawlers) | A larger addon with dungeons, classes, rewards, and integrations |

Older examples may use older API and dependency versions. Use the setup above for a new addon.
