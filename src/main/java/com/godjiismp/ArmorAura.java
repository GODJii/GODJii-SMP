package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.joml.Vector3f;

/** Cosmetic effect only: four pieces dyed by /armor color with one matching RGB value. */
public final class ArmorAura {
	private ArmorAura() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
								CommandBuildContext context, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("aura")
				.then(Commands.literal("on").executes(c -> set(c.getSource().getPlayerOrException(), true)))
				.then(Commands.literal("off").executes(c -> set(c.getSource().getPlayerOrException(), false)))
				.executes(c -> {
					ServerPlayer player = c.getSource().getPlayerOrException();
					player.sendSystemMessage(Component.literal("Armor aura is "
							+ (AuraPreferences.enabled(player.getUUID()) ? "ON" : "OFF")
							+ ". Use /aura on or /aura off.").withStyle(ChatFormatting.AQUA));
					return 1;
				}));
	}

	public static void init() {
		AuraPreferences.load();
		ServerTickEvents.END_SERVER_TICK.register(ArmorAura::tick);
	}

	private static int set(ServerPlayer player, boolean enabled) {
		AuraPreferences.setEnabled(player.getUUID(), enabled);
		player.sendSystemMessage(Component.literal("Armor aura " + (enabled ? "enabled" : "hidden") + ".")
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
		return 1;
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!AuraPreferences.enabled(player.getUUID())) continue;
			Integer color = matchingColor(player);
			if (color == null) continue;
			float red = ((color >> 16) & 0xff) / 255.0f;
			float green = ((color >> 8) & 0xff) / 255.0f;
			float blue = (color & 0xff) / 255.0f;
			DustParticleOptions dust = new DustParticleOptions(new Vector3f(red, green, blue), 0.65f);
			ServerLevel level = player.serverLevel();
			level.sendParticles(dust, player.getX(), player.getY() + 0.9, player.getZ(),
					8, 0.28, 0.72, 0.28, 0.005);
		}
	}

	private static Integer matchingColor(ServerPlayer player) {
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		Integer expected = null;
		for (EquipmentSlot slot : slots) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!ArmorCommands.isAuraArmor(stack)) return null;
			DyedItemColor dye = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
			if (dye == null) return null;
			if (expected == null) expected = dye.rgb();
			else if (expected != dye.rgb()) return null;
		}
		return expected;
	}
}
