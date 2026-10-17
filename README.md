# GODJii SMP NEW DAYS (Fabric 1.21.1)

This distribution combines the GODJii SMP server features with the supplied latest My Market mod. Install the single built JAR on the server. Players need it client-side only when using the market's client features.

## Included features

- My Market, bundled from the supplied `mymarket-latest.jar`.
- Homes: `/sethome`, `/home`, `/delhome`.
- Bounties: `/bounty set <player> <amount>` and `/bounty list`.
- Player transfers: `/pay <player> <amount>` (examples: `250K`, `1.5M`).
- Teleport requests: `/tpa <player>`, `/tpaccept`, `/tpdeny`, `/tpa cancel`.
- Phantoms disabled through the vanilla `doInsomnia` gamerule.

Arena and scheduled Arena events are intentionally not included in this NEW DAYS build, following the latest project specification. Arena commands should not be used from this build.

## Install

Put the built `godjiismp-new-days-1.0.0.jar` in the server's `mods` folder alongside Fabric API 1.21.1. The My Market mod is nested inside this JAR and should not be installed separately with this combined build.

## Build

GitHub Actions builds on pushes and manual workflow runs. Download the `godjiismp-new-days-jar` artifact from the workflow run. Locally, use JDK 25 to run Gradle and build Java 21-compatible bytecode:

```sh
./gradlew build
```

The JAR is written to `build/libs/`.
