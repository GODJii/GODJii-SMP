package com.godjiismp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
		Bounties.init();
		LOGGER.info("GODJii SMP loaded (home cost {}, min bounty {})", Config.cost, Config.minBounty);
	}
}
