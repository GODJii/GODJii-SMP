package com.godjiismp.mixin;

import com.godjiismp.weapons.LegendaryItems;
import com.godjiismp.weapons.WeaponsConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** My Market doubles Prices.baseValue when it displays/bills a buy price. */
@Mixin(targets = "com.mymarket.Prices")
public abstract class MarketWeaponBuyPriceMixin {
	@Inject(method = "baseValue", at = @At("RETURN"), cancellable = true)
	private static void godjiismp$setLegendaryBuyBase(ItemStack stack, CallbackInfoReturnable<Long> callback) {
		if (stack.isEmpty()) return;
		for (int i = 0; i < WeaponsConfig.IDS.length; i++) {
			if (stack.getItem() == LegendaryItems.get(i)) {
				// Prices.buyPrice(value) is value * 2; this makes the displayed and charged
				// purchase price exactly the configured weapon price.
				callback.setReturnValue(WeaponsConfig.sellPrice[i] / 2L);
				return;
			}
		}
	}
}
