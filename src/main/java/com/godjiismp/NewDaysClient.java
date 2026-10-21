package com.godjiismp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client entry point reserved for the NEW DAYS HUD and client-only settings. */
public final class NewDaysClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		new com.godjiismp.client.ui.SkillsClient().onInitializeClient();
		ClientPlayNetworking.registerGlobalReceiver(StatsPayload.TYPE, (payload, context) -> StatsHud.update(payload));
		HudRenderCallback.EVENT.register((draw, tickCounter) -> StatsHud.render(draw));
	}
}
