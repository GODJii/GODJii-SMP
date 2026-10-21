package com.godjiismp.mixin;

import com.godjiismp.accessory.ExtraElytraHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerExtraElytraMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void godjiismp$restoreExtraElytra(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (alive || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
			ItemStack old = ((ExtraElytraHolder) oldPlayer).godjiismp$getExtraElytra();
			((ExtraElytraHolder) player).godjiismp$setExtraElytra(old.copy());
		}
	}
}
