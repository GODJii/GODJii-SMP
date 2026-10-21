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
- The right-side stats HUD uses compact 8-bit icons for balance, mob kills, player kills, deaths, and play time.
- Permanent player titles: `/title`, `/title buy <id>`, `/title set <id>`, and `/title off` manage titles shown above players and in the tab list.
- Blood Moon: naturally has a 1-in-7 chance at night; OPs can use `/bloodmoon start|stop|status` for testing. Blood Moon mobs gain 100% health and 30% attack damage, and player kills pay $10K.
- Phantoms disabled through the vanilla `doInsomnia` gamerule.
- The DrDonut-inspired server hub opens with M and provides direct buttons for Home, Shop (`/shop`), Sell (`/sellgui`), Skills, Daily Quests, Stats, Bounties, and Titles. Sell uses Minecraft's standard 6-row container window and slots. Players drag items into the grid and click the green sell slot; unsold or unpriced items are returned when they close it. The My Market `/sell` command remains available as before.
- Skills: `/skills` or K opens the skill tree; quests award skill points; OPs can use `/skills give <player> <points>`; `/skills reset` refunds spent points subject to config.
- Skill progress is stored per player in world SavedData. Skill balance is in `config/godjiismp_skills.json` and weapon balance is in `config/godjiismp_weapons.json`; both files are created on first server start.
- Six skill lines and the designer-provided skill screen are included. Fishing uses the allowed Luck-effect fallback; `/tpa` currently has no cooldown to reduce.
- Six registered legendary items use the supplied art: Skeleton King Blade, Stormcaller Spear, Earthbreaker Hammer, Crimson Moon Scythe, Frostbite Bow, and Emberfang Axe. Only the first three have phase-one abilities; the remaining phase-two abilities are not implemented yet.
- Legendary weapons have 2031 durability, accept Netherite Blocks as anvil repair material, can receive compatible weapon/bow and Unbreaking enchantments, and sell for $350M–$400M each. Their melee baseline is set so Sharpness V yields about 80% more damage than a Sharpness V Netherite Sword for sword-like weapons, and about 80% more than a Sharpness V Netherite Axe for axe-like weapons. Special attack damage is also raised by 80%; Frostbite Bow arrows receive an 80% damage increase after bow enchantments. PvP ability damage remains governed by its separate configurable multiplier. Values are configurable in `config/godjiismp_weapons.json`.
- Legendary item crafting recipes are intentionally not included until the owner approves the proposed recipes.

Arena and scheduled Arena events are intentionally not included in this NEW DAYS build, following the latest project specification. Keep new builds on a test server until the owner completes the in-game checklist.

## Install

When the draft is complete, the single JAR will go in the server and client `mods` folders with Fabric API 1.21.1. The My Market mod is nested inside and should not be installed separately.

## Build

GitHub Actions builds on pushes and manual workflow runs. Download the `godjiismp-new-days-jar` artifact from the workflow run. Locally, use JDK 25 to run Gradle and build Java 21-compatible bytecode:

```sh
./gradlew build
```

The JAR is written to `build/libs/`.
