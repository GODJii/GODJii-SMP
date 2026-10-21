package com.godjiismp.mixin;

import com.godjiismp.accessory.ExtraElytraHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows a dim Elytra icon in the additional slot while it is empty. */
@Mixin(InventoryScreen.class)
public abstract class ExtraElytraSlotIconMixin {
	@Inject(method = "renderBg", at = @At("TAIL"))
	private void godjiismp$drawExtraSlotHint(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !((ExtraElytraHolder) minecraft.player).godjiismp$getExtraElytra().isEmpty()) return;
		int left = (minecraft.getWindow().getGuiScaledWidth() - 176) / 2;
		int top = (minecraft.getWindow().getGuiScaledHeight() - 166) / 2;
		graphics.renderFakeItem(new ItemStack(Items.ELYTRA), left + 77, top + 44);
	}
}
