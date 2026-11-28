# OpenItemPuller

[![Watch the video](https://img.youtube.com/vi/pHaO0sO9Cjo/maxresdefault.jpg)](https://youtu.be/pHaO0sO9Cjo)

## Features
- Easy to use menu
- Modifiable pull range
- Item searching
- Named chest compatible
- Tells items apart by their components: enchanted, renamed or filled stacks (e.g. shulker boxes) are separate entries and are never mixed with plain ones
- Scrollable chest list (mouse wheel over the chest list)
- Quick amount buttons (1 / Stack / All / clear) that fill the amounts over your chests, top of the list first
- Shows beforehand how much of your selection fits in your inventory (not for a target storage)
- S+ like UI
- Server-configurable maximum pull range

## Configuration
Two separate files in `config/openitempuller/`:
- `server.json` (read by the server, also by the integrated server in singleplayer): `max_radius`, the largest pull radius the server accepts. Values above 128 are clamped to 128. `blocked_blocks`: block ids that are never listed and never pulled from or into (default `["minecraft:ender_chest"]`; an empty list blocks nothing).
- `client.json` (per player): `radius` (what you ask for, clamped to the server limit), `priority_type`, `display_mode`, `sorting_mode`.

An old `config.json` is imported once and renamed to `config.json.migrated`.

## Security & permissions
The server re-validates every request: distance, dimension, loaded chunk, container type, loot tables and vanilla lock codes. Requests are rate limited per player.

**There is no ownership or claim check built in.** Any player can pull from any unlocked container within range, including through walls and from containers other players built. On a server where that matters, install a claim/protection mod that hooks into `PullPermissionCallback` (`com.retrotrack.openitempuller.api`), or keep `max_radius` small. Example:

```java
PullPermissionCallback.EVENT.register((player, level, pos) -> myClaims.canAccess(player, level, pos));
```

Returning `false` hides the container from the list and rejects pulls from or into it.

## Requirements
- Fabric-api
- Mod needs to be installed both server & client side to work!

## Development
- `./gradlew build` builds the mod and runs the unit tests (`src/test`).
- GitHub Actions (`.github/workflows/build.yml`) runs the tests on every push and pull request, and builds the jar only on `main`.
- Releasing: set `mod_version` in `gradle.properties`, then push a tag with the same name, e.g. `git tag v0.2.0-beta.1 && git push origin v0.2.0-beta.1`. `release.yml` builds, tests and attaches the jar to a GitHub release (a version with a `-` such as `-beta.1` becomes a pre-release). Uploading to Modrinth is done by hand.

## 
This mod is made by a student, so updates may differ based on workload

