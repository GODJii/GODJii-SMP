package com.godjiismp.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the vanilla recessed slot background behind the extra Elytra slot. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenExtraElytraSlotMixin {
	@Inject(method = "renderBg", at = @At("TAIL"))
	private void godjiismp$drawExtraElytraSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
		graphics.blitSprite(ResourceLocation.withDefaultNamespace("container/slot"),
				screen.godjiismp$getLeftPos() + 77, screen.godjiismp$getTopPos() + 44, 18, 18);
	}
}
