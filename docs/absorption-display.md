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

Server limits are separate from display scaling. A server intending to grant thousands of absorption points must raise `settings.attribute.maxAbsorption.max` in `spigot.yml` and set an appropriate native `MAX_ABSORPTION` attribute when granting absorption. CaveCrawlers does not change either limit or grant absorption itself.

## Compatibility and verification

The packet adapter resolves Paper's named player absorption metadata accessor at startup instead of assuming a fixed index. This targets Paper 1.21.11+ with a ProtocolLib version supporting the running server. If the accessor cannot be resolved, CaveCrawlers logs an error and leaves native absorption unchanged. Packets for other entities are untouched, and rewritten packet lists and absorption entries are copied so other viewers cannot inherit the owner's display value.

Automated tests cover proportional scaling, replenishment, fractional and extreme values, independent players, replacement entity IDs, configuration changes, join/respawn/world refreshes, death/quit cleanup, shutdown restoration, and the numeric action-bar readout.

Before deployment, check on Paper with a real client:

1. Raise the absorption limit, grant a 3,200-point native pool, and confirm at most one golden row beside the existing two normal rows.
2. Consume half the absorption through ordinary damage. Confirm the native value is 1,600, health has not lost that absorbed damage, and five golden hearts remain.
3. Deplete and regrant absorption, test fractional values, and test existing Reaper Impact and vanilla absorption effects.
4. Relog, respawn, change dimensions, and reload CaveCrawlers. Check that metadata refreshes and effect expiry removes the display.
5. Disable display scaling with `/cc reload lite`. Verify native absorption remains and the client receives its unscaled amount. Re-enable and confirm the scaled row returns.
6. Check two players simultaneously and confirm observing another player's metadata does not change their own absorption pool.

The API and state tests do not replace this client HUD check. This change does not deploy to or alter the live server.
