package com.godjiismp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GodjiiSmpMod implements ModInitializer {
	public static final String MOD_ID = "godjiismp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Config.load();
		CommandRegistrationCallback.EVENT.register(HomeCommands::register);
		CommandRegistrationCallback.EVENT.register(BountyCommands::register);
		CommandRegistrationCallback.EVENT.register(ArenaCommands::register);
		ServerTickEvents.END_SERVER_TICK.register(ArenaManager::tick);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof net.minecraft.world.entity.monster.Phantom) entity.discard();
		});
		ArenaManager.registerHooks();
		Bounties.init();
		LOGGER.info("GODJii SMP loaded (home cost {}, min bounty {})", Config.cost, Config.minBounty);
	}
}
