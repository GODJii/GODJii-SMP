# GODJii SMP NEW DAYS (Fabric 1.21.1)

This is an in-progress integration branch for GODJii SMP and the supplied My Market patch. It is not a release build yet; keep it on the test server until every requested feature is finished and verified.

## Included features

- My Market 17537, bundled inside the combined JAR.
- Homes: `/sethome`, `/home`, `/delhome`.
- Bounties: `/bounty set <player> <amount>` and `/bounty list`.
- Player transfers: `/pay <player> <amount>` (examples: `250K`, `1.5M`).
- Teleport requests: `/tpa <player>`, `/tpaccept`, `/tpdeny`, `/tpa cancel`.
- Armor dye reset: `/armor color reset all` restores every dyed armor piece in inventory.
- Matching-color armor aura: `/aura on` and `/aura off` toggle the cosmetic particles.
- Daily quests: `/quest` displays the current mining, mob-hunting, farming, or logging objective. Progress persists and completion pays $1M–$3M automatically.
- Market event: `/market` shows the daily HOT/COLD categories. The event changes sale prices only and persists through restarts.
- Phantoms disabled through the vanilla `doInsomnia` gamerule.

Arena and scheduled Arena events are intentionally not included in this NEW DAYS build, following the latest project specification. The requested titles, full quest pool/HUD, Blood Moon, Skeleton King, and custom top-right notices/sidebar are still being implemented. Do not install this draft on the live server.

## Install

When the draft is complete, the single JAR will go in the server and client `mods` folders with Fabric API 1.21.1. The My Market mod is nested inside and should not be installed separately.

## Build

GitHub Actions builds on pushes and manual workflow runs. Download the `godjiismp-new-days-jar` artifact from the workflow run. Locally, use JDK 25 to run Gradle and build Java 21-compatible bytecode:

```sh
./gradlew build
```

The JAR is written to `build/libs/`.
