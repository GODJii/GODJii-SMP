package com.godjiismp.mixin;

import com.godjiismp.accessory.ExtraElytraHolder;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraLayer.class)
public abstract class ExtraElytraRenderMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"), require = 0)
	private ItemStack godjiismp$renderExtraElytra(LivingEntity entity, EquipmentSlot slot) {
		if (slot == EquipmentSlot.CHEST && entity instanceof Player player) {
			ItemStack extra = ((ExtraElytraHolder) player).godjiismp$getExtraElytra();
			if (!extra.isEmpty()) return extra;
		}
		return entity.getItemBySlot(slot);
	}
}
