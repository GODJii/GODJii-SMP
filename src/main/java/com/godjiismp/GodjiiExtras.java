package com.godjiismp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Second entry point of GODJii SMP (see fabric.mod.json): coordinates bar, trim remover, armor colors.
 * It lives in its own files so the arena / home / bounty code is not touched.
 */
public class GodjiiExtras implements ModInitializer {
	@Override
	public void onInitialize() {
		ExtrasConfig.load();
		CommandRegistrationCallback.EVENT.register(CoordsHud::register);
		CommandRegistrationCallback.EVENT.register(ArmorCommands::register);
		CommandRegistrationCallback.EVENT.register(ArmorAura::register);
		CoordsHud.init();
		ArmorAura.init();
		GodjiiSmpMod.LOGGER.info("GODJii SMP extras loaded (trim remover {}, armor color {})",
				ExtrasConfig.trimRemoveCost, ExtrasConfig.armorColorCost);
	}
}
