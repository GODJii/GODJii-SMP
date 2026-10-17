package com.godjiismp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GodjiiSmpMod implements ModInitializer {
	public static final String MOD_ID = "godjiismp_newdays";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Config.load();
		CommandRegistrationCallback.EVENT.register(HomeCommands::register);
		CommandRegistrationCallback.EVENT.register(BountyCommands::register);
		CommandRegistrationCallback.EVENT.register(EconomyCommands::register);
		CommandRegistrationCallback.EVENT.register(TeleportRequests::register);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> server.getAllLevels().forEach(level ->
				level.getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, server)));
		Bounties.init();
		TeleportRequests.init();
		LOGGER.info("GODJii SMP loaded (home cost {}, min bounty {})", Config.cost, Config.minBounty);
	}
}
