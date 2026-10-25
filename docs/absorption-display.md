# Native absorption display

CaveCrawlers scales the owning player's absorption display to at most ten golden hearts, or 20 display points. The existing normal-health scale stays at 40 points, so the HUD can show two normal rows plus one absorption row.

The real absorption amount remains on the server. Minecraft consumes it through its normal damage handling. CaveCrawlers changes only absorption metadata sent to that player's client. It does not change maximum health, absorption attributes, damage events, regeneration, or effect durations.

## Scaling

The largest absorption amount observed during a nonempty pool fills the golden row. Damage makes the display shrink proportionally. Increasing absorption above the previous peak establishes a new peak. Emptying the pool resets the scale.

| Native absorption | Display points | Golden hearts |
|---|---:|---:|
| 3,200, initial pool | 20 | 10 |
| 1,600 remaining | 10 | 5 |
| 800 remaining | 5 | 2.5 |
| 0 remaining | 0 | 0 |

This applies to native absorption from any source, including existing abilities and vanilla effects. Topping up a pool below its previous peak restores only the corresponding portion of the row. A relog, respawn, world change, or display reload starts a new display session with any absorption still present as its initial peak.

No shield-grant or expiry API is added here. Addons continue using native absorption and managing their own effect durations. Hollow armor and its healing wand are separate work.

## Configuration

```yaml
absorption-display:
  enabled: true
```

This defaults to true even when the key is absent from an older configuration. `/cc reload lite` applies the setting. Disabling it or unloading CaveCrawlers removes interception and restores the client's actual native absorption value without deleting absorption.

The default action-bar format includes `<absorption-display>`, which inserts a gold ` +3200❤` while absorption is present and disappears when empty. `<absorption>` supplies the actual numeric amount for custom formats. Fractions round up to the next point for display. Existing customized action-bar formats are preserved; add one of these placeholders where the amount should appear.

Server limits are separate from display scaling. On versions with the native `MAX_ABSORPTION` attribute, a server intending to grant thousands of absorption points must raise `settings.attribute.maxAbsorption.max` in `spigot.yml` and set an appropriate attribute value when granting absorption. Older versions can have different limits and APIs. CaveCrawlers does not change these limits or grant absorption itself.

## Compatibility and verification

The packet adapter resolves the player absorption metadata accessor at startup instead of assuming a fixed index. It prefers the named accessor on mapped servers. On obfuscated servers, it requires exactly one float metadata accessor declared in the player-specific class hierarchy. It excludes `LivingEntity`, whose float metadata includes health, and rejects ambiguous results. A ProtocolLib release supporting the running server is required.

Before installing interception, the adapter verifies the resolved serializer and index, packet fields, and copy/write/read behavior. Startup incompatibility leaves native metadata untouched. A later packet or refresh error disables scaling once, lets subsequent native metadata pass through immediately, and schedules listener removal and native-value restoration on the server thread. Reload uses separate session state so delayed callbacks cannot change a new pool's peak or disable a new session. Cleanup errors are logged without changing real absorption or stopping plugin shutdown.

Adapter construction, startup, packet-event access, and player refreshes run inside `try/catch` guards for `RuntimeException` and `LinkageError`, including missing classes or methods on unsupported server/ProtocolLib versions. A disabled display does not construct the adapter. Fatal JVM errors such as exhausted memory are not treated as version incompatibilities.

Packets for other entities are untouched. Rewritten packet lists and absorption entries are copied so other viewers cannot inherit the owner's display value. This is a compatibility fallback, not a guarantee that an unsupported ProtocolLib or future Minecraft release can load the entire plugin.

Automated tests cover mapped and obfuscated accessor resolution, ambiguous or missing accessors, health exclusion, proportional scaling, replenishment, fractional and extreme values, independent players, replacement entity IDs, configuration changes, join/respawn/world refreshes, death/quit cleanup, stale reload callbacks, runtime/linkage errors, shutdown restoration, and the numeric action-bar readout.

### Isolated server checks

The compatibility probe loads only the production absorption classes with ProtocolLib on disposable loopback-only servers. It does not certify the rest of CaveCrawlers or its addons.

| Paper | Java | ProtocolLib | Result |
|---|---|---|---|
| 1.20 build 17 | 21 | 5.1.0 | Own-player metadata checked over a real protocol connection; resolved index 15 |
| 1.21.11 build 132 | 21 | 5.4.0 | Own-player metadata checked over a real protocol connection; resolved index 17 |
| 26.2 build 135 | 25 | 5.5.0-SNAPSHOT-6720a70 | Startup accessor and packet schema checks passed; resolved index 17; no client connection or rendered HUD check |

The 1.20 fixture logs `Unsupported class file major version 65` from its older Commodore bytecode converter before loading the original Java 21 classes successfully. This is a separate limitation of running Java 21 plugin bytecode through that older Paper tooling. The absorption probe passing does not resolve or certify whole-plugin support on 1.20.

Build the test-only plugin with `./gradlew absorptionCompatibilityJar`. Its artifact is `build/libs/CaveCrawlers-dev-absorption-probe.jar`; it is not included in the main plugin jar. Install it with a compatible ProtocolLib on a disposable offline server bound to `127.0.0.1`. Do not install the probe on a gameplay server: every joining test player is put in creative mode and receives temporary absorption.

The probe grants 400 native absorption points, reduces them to 200, injects a packet-handler linkage error at 150, re-enables scaling, then disables it through configuration. With `minecraft-protocol` installed outside the repository and exposed through `NODE_PATH`, run:

```bash
node tools/check-absorption-client.cjs 1.21.11 25583 17
```

Use the fixture's version, loopback port, and index reported by `ABS_PROBE_READY`. The client requires the ordered metadata values `20, 10, 150, 20, 150`, covering proportional scaling, automatic native fallback, re-enabling, and configuration disable. Its protocol library must support the tested server version; the available client library did not support 26.2 during these checks.

### Gameplay checks

The original deployed build's rendered HUD was confirmed on the live server. The compatibility hardening also passed the isolated checks above and was subsequently deployed with a successful core/addon reload. Remaining manual checks with a graphical client include:

1. Raise the absorption limit, grant a 3,200-point native pool, and confirm at most one golden row beside the existing two normal rows.
2. Consume half the absorption through ordinary damage. Confirm the native value is 1,600, health has not lost that absorbed damage, and five golden hearts remain.
3. Deplete and regrant absorption, test fractional values, and test existing Reaper Impact and vanilla absorption effects.
4. Relog, respawn, change dimensions, and reload CaveCrawlers. Check that metadata refreshes and effect expiry removes the display.
5. Disable display scaling with `/cc reload lite`. Verify native absorption remains and the client receives its unscaled amount. Re-enable and confirm the scaled row returns.
6. Check two players simultaneously and confirm observing another player's metadata does not change their own absorption pool.

The API and state tests do not replace these client gameplay checks.
