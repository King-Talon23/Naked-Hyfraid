# Naked & Hyfraid

A Hytale server plugin that makes any world it runs on brutal:

- **One life (hardcore).** When you die you're disconnected and can't rejoin that save. Hytale only has Adventure and Creative modes (no spectator), so being locked out is the penalty. Admins can let someone back in with `/hardcore revive <player>`.
- **No world map.** Every world uses the server's built-in `Disabled` map provider, so the map won't open.
- **Armour hurts.** Every few seconds you take damage for each armour piece you're wearing. Go naked and you're fine.
- **Food doesn't heal.** No instant heals and no health regen buffs from food. At startup the mod follows every food item's interaction chains (including nested and linked ones like the `HealthRegen_TierCheck` regen buffs, and effects defined inline in the item) and removes the Health gain from every effect they apply, plus any effect named `Food_*`. Instant heals and regen over time both heal through that same Health entry, so both stop. Other food buffs, like stamina, still work. Any effect a `Potion_` item uses is never touched, so **health potions still heal**. The server log lists every effect it changed.

## Install

1. Build it: `./gradlew build` (needs JDK 25; the jar lands in `build/libs/`).
2. Drop `NakedHyfraid-<version>.jar` into your server's `mods/` folder (or your singleplayer world's mods) and start the server.

## Config

`mods/KingTalon23_NakedHyfraid/config.json` is created on first start:

| Key | Default | What it does |
| --- | --- | --- |
| `Hardcore` | `true` | One life per player. |
| `DeathKickDelaySeconds` | `3` | How long after death before the player is disconnected. |
| `DisableWorldMap` | `true` | Turns the map off in every world. |
| `ArmourDamagePerPiece` | `1` | Damage per armour piece each interval (`0` turns it off). |
| `ArmourDamageIntervalSeconds` | `4` | Seconds between armour damage ticks. A full set (4 pieces) costs 1 HP a second. |
| `DisableFoodHealing` | `true` | Stops food from healing. |
| `FoodEffectPrefixes` | `["Food_"]` | Item and effect id prefixes that count as food (items that use a `Consume_Food` interaction count too). |

The list of dead players is kept per save in `universe/naked-hyfraid/dead-players.txt`.

## Commands

Need the `nakedhyfraid.command.hardcore` permission.

- `/hardcore list`: who has died.
- `/hardcore revive <player>`: let a dead player rejoin (username or UUID).

## Notes

- Disabling the map writes `"WorldMap": { "Type": "Disabled" }` into each world's config. If you remove the mod and want the map back, change that setting back in the world's `config.json`.
- Written against the Update 5 server API (from the decompiled server source). If a later Hytale update renames something, the build will tell you where.
