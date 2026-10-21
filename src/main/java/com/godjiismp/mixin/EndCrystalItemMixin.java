package com.godjiismp.mixin;

import com.godjiismp.BossSystem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes only tagged Summon Crystals through the Skeleton King summon flow. */
@Mixin(EndCrystalItem.class)
abstract class EndCrystalItemMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void godjiismp$useSummonCrystal(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (BossSystem.isSummonCrystal(context.getItemInHand())) {
			cir.setReturnValue(BossSystem.useCrystal(context));
		}
	}
}
