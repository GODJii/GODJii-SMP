package com.godjiismp.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.godjiismp.mixin.accessor.AbstractContainerMenuAccessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Removes My Market's unused bottom control strip while retaining the 4 product rows. */
@Mixin(targets = "com.mymarket.BuyMenu")
public abstract class CompactMarketBuyMenuMixin {
	@Shadow @Final private SimpleContainer top;

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/ChestMenu;<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V"), index = 4)
	private int godjiismp$removeBottomControlRow(int rows) {
		return rows == 6 ? 5 : rows;
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void godjiismp$keepInventorySlotIds(CallbackInfo ci) {
		NonNullList<Slot> slots = ((AbstractContainerMenuAccessor) (Object) this).godjiismp$getSlots();
		// The hidden toolbar occupies ids 45-53 in My Market's click handler. Keep those
		// ids reserved so the player's first inventory row still clicks as inventory.
		for (int i = 0; i < 9; i++) slots.add(45 + i, new Slot(top, 45 + i, -1000, -1000));
	}
}
