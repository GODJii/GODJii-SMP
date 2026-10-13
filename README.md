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
JDK 21, `./gradlew build`, jar in `build/libs/godjiismp-1.0.0.jar`. Or use the GitHub Actions tab.
