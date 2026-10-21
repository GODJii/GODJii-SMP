package com.godjiismp.mixin;

import com.godjiismp.accessory.ExtraElytraContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import com.godjiismp.mixin.accessor.AbstractContainerMenuAccessor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuExtraElytraMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void godjiismp$addExtraElytraSlot(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		ExtraElytraContainer container = new ExtraElytraContainer(owner);
		((AbstractContainerMenuAccessor) this).godjiismp$addSlot(new Slot(container, 0, 77, 44) {
			@Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.ELYTRA); }
			@Override public int getMaxStackSize() { return 1; }
		});
	}
}
