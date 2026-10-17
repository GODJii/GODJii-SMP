package com.godjiismp.mixin;

import com.godjiismp.BossSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
abstract class SkeletonKingLootMixin {
	@Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
	private void godjiismp$disableKingVanillaLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
		if (BossSystem.isKing((Entity)(Object)this)) ci.cancel();
	}
}
