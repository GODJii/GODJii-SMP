package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

import java.util.concurrent.ThreadLocalRandom;

/** Scheduled 1-in-7 Blood Moons with boosted hostiles and automatic player kill rewards. */
public final class BloodMoon {
	private BloodMoon() {}
	private static final ResourceLocation HEALTH_MODIFIER = ResourceLocation.fromNamespaceAndPath("godjiismp_newdays", "bloodmoon_health");
	private static final ResourceLocation DAMAGE_MODIFIER = ResourceLocation.fromNamespaceAndPath("godjiismp_newdays", "bloodmoon_damage");
	private static final String MOB_TAG = "godjiismp_bloodmoon_mob";

	public static void init() {
		CommandRegistrationCallback.EVENT.register(BloodMoon::register);
		ServerTickEvents.END_SERVER_TICK.register(BloodMoon::tick);
		ServerEntityEvents.ENTITY_LOAD.register(BloodMoon::onEntityLoad);
		ServerLivingEntityEvents.AFTER_DEATH.register(BloodMoon::onDeath);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
							 Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("bloodmoon").requires(source -> source.hasPermission(2))
				.then(Commands.literal("start").executes(c -> { start(c.getSource().getServer(), true); return 1; }))
				.then(Commands.literal("stop").executes(c -> { stop(c.getSource().getServer()); return 1; }))
				.then(Commands.literal("status").executes(c -> {
					boolean active = BloodMoonData.get(c.getSource().getServer()).active();
					c.getSource().sendSuccess(() -> Component.literal("Blood Moon is " + (active ? "ACTIVE" : "inactive") + "."), false);
					return 1;
				})));
	}

	private static void tick(MinecraftServer server) {
		ServerLevel world = server.overworld();
		long time = world.getDayTime();
		long day = Math.floorDiv(time, 24_000L);
		int phase = (int) Math.floorMod(time, 24_000L);
		BloodMoonData data = BloodMoonData.get(server);
		if (world.getGameTime() % 20 != 0) return;
		if (phase >= 11_000 && phase < 12_000 && data.warnedDay() != day) {
			data.setWarnedDay(day);
			for (ServerPlayer p : server.getPlayerList().getPlayers()) p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Blood Moon rises tonight!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
			broadcast(server, "A Blood Moon may rise tonight. Prepare before dusk!", ChatFormatting.RED);
		}
		if (phase >= 12_000 && phase < 13_000 && data.checkedDay() != day) {
			data.setCheckedDay(day);
			if (ThreadLocalRandom.current().nextInt(7) == 0) {
				for (ServerPlayer p : server.getPlayerList().getPlayers()) p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("BLOOD MOON").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
				start(server, false);
			}
		}
		if (data.active() && day > data.startedDay() && phase < 1_000) stop(server);
	}

	private static void start(MinecraftServer server, boolean manual) {
		BloodMoonData data = BloodMoonData.get(server);
		if (data.active()) {
			broadcast(server, "A Blood Moon is already active.", ChatFormatting.RED);
			return;
		}
		long day = Math.floorDiv(server.overworld().getDayTime(), 24_000L);
		data.setActive(true, day);
		broadcast(server, manual ? "Blood Moon started by an operator!" : "The Blood Moon has begun! Hostile mobs are stronger; each player kill pays $10K.", ChatFormatting.DARK_RED);
	}

	private static void stop(MinecraftServer server) {
		BloodMoonData data = BloodMoonData.get(server);
		if (!data.active()) return;
		data.setActive(false, data.startedDay());
		broadcast(server, "The Blood Moon has ended.", ChatFormatting.GRAY);
	}

	private static void onEntityLoad(Entity entity, ServerLevel world) {
		if (!BloodMoonData.get(world.getServer()).active() || world.dimension() != Level.OVERWORLD
				|| !(entity instanceof Mob mob) || mob.getType().getCategory() != MobCategory.MONSTER || mob.getTags().contains(MOB_TAG)) return;
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (health != null) health.addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		if (damage != null) damage.addPermanentModifier(new AttributeModifier(DAMAGE_MODIFIER, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		mob.addTag(MOB_TAG);
		if (health != null) mob.setHealth(mob.getMaxHealth());
	}

	private static void onDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!entity.getTags().contains(MOB_TAG)) return;
		if (entity instanceof net.minecraft.world.entity.monster.Zombie && ThreadLocalRandom.current().nextDouble() < 0.025) {
			ItemEntity crystal = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.4, entity.getZ(), BossSystem.makeCrystal());
			entity.level().addFreshEntity(crystal);
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof Projectile projectile) attacker = projectile.getOwner();
		if (attacker instanceof ServerPlayer player) {
			Money.give(player.getServer(), player.getUUID(), 10_000L);
			player.displayClientMessage(Component.literal("Blood Moon kill · +$10K").withStyle(ChatFormatting.RED), true);
		}
	}

	private static void broadcast(MinecraftServer server, String text, ChatFormatting color) {
		server.getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
	}
}

