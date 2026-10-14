# GODJii SMP (Fabric 1.21.1)

Server mod made to go with **My Market** (it uses its money). Needs My Market + Fabric API.
Players do not need it on their client.

## Home
- `/sethome`  save your base where you stand. Also your respawn point (can be turned off).
- `/home`     pays $1M (default) and teleports you to **your own** home straight away. No confirmation.
- `/delhome`  forget your home
One home per player. There is no command to teleport to other players.

## Bounties
- `/bounty set <player> <amount>`  amount can be `50000`, `50k`, `2.5M`, `1B` ...
- `/bounty list`  (or `/bounty`, `/bounties`)  who has a bounty, how much, time left, who set it

Rules
- You pay the full amount the moment you set it.
- Only ONE bounty per player at a time. Nobody can add another until it is gone.
- Whoever kills the player (a real player kill, arrows and so on count) gets all the money.
- Nobody can put a bounty on themselves. Dying to yourself, a mob, lava, falling or /kill pays nobody,
  so you can't cash in your own bounty. The bounty stays until someone kills you or time runs out.
- When time runs out with no kill, the money goes back to the player who set it.
- Everybody gets a big title in the middle of the screen + a chat message when a bounty is set or claimed.
- Lasts 24000 ticks by default = 1 Minecraft day = 20 real minutes.

## Arena (new)
- `/arena challenge <player> <stake>` sends a duel challenge; the minimum wager is `$10,000` and both players need enough funds to cover it.
- `/arena accept` accepts the pending challenge. The winner receives both wagers; arena gear is removed when the players return.
- `/arena start` starts a test loot event and puts the command user in the arena. `/arena join` lets other players join.
- `/arena claim` claims the event reward when close to the center chest. The reward is paid through My Market as a randomized `$1,000,000–$3,000,000` balance prize.
- `/arena leave` exits an event. `/arena stop` stops an event or refunds a duel wager. Start/stop require permission level 2 (server operator).
- The arena is generated underground about 10,000 blocks east of the Overworld spawn as a 60×30×15 glowstone shell. Player block breaking inside the shell is blocked, and missing shell blocks are repaired twice a second. The arena chunks stay loaded. Reduced debug info is enabled while the arena is active.
- Players receive the requested Netherite kit on entry. Their inventory, position, health, hunger, experience, and game mode are captured and restored after elimination or event end. Event eliminations become spectators until the event ends.
- A scheduled loot event opens every 72,000 game ticks (one real hour at 20 TPS). `/arena start` is intended for testing.
- Phantoms are removed when loaded, so they cannot attack players.

Notes: arena snapshots live in memory, so a server crash/restart during an active match cannot recover a snapshot. The debug-info gamerule is world-wide while an arena is active. The current event prize is paid as My Market currency rather than a physical item prize.

## Config (config/godjiismp.json, created on first run)
```json
{
  "cost": 1000000,
  "cooldownSeconds": 30,
  "minBounty": 10000,
  "bountyTicks": 24000,
  "respawnAtHome": true
}
```

## Install
Put `godjiismp-1.0.0.jar` in `mods` next to My Market and Fabric API 1.21.1 (on the server, or in your own mods folder for singleplayer).

## Build
JDK 25 is needed to run the current Fabric Loom Gradle plugin; the mod bytecode targets Java 21. Run `./gradlew build`. The remapped jar is in `build/libs/`. GitHub Actions builds it on every push and saves it as the `godjiismp-mod` workflow artifact.
