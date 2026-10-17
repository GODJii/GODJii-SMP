package com.godjiismp.mixin;

import com.godjiismp.MarketFluctuations;
import com.mymarket.Prices;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adjust only sale values; market buy values are left unchanged. */
@Mixin(Prices.class)
public abstract class MarketPricesMixin {
	@Inject(method = "unitSell", at = @At("RETURN"), cancellable = true)
	private static void godjiismp$applySaleEvent(ItemStack stack, CallbackInfoReturnable<Long> callback) {
		callback.setReturnValue(MarketFluctuations.applySellPrice(stack, callback.getReturnValue()));
	}
}
