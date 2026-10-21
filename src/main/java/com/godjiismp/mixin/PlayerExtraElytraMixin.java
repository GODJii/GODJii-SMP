package com.godjiismp.mixin;

import com.godjiismp.accessory.ExtraElytraHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerExtraElytraMixin implements ExtraElytraHolder {
	@Unique private static final EntityDataAccessor<ItemStack> GODJIISMP_EXTRA_ELYTRA =
			SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
	@Unique private static final String GODJIISMP_EXTRA_ELYTRA_TAG = "GODJiiExtraElytra";


	@Redirect(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"), require = 0)
	private ItemStack godjiismp$useExtraElytraWhenStartingFlight(Player player, EquipmentSlot slot) {
		if (slot == EquipmentSlot.CHEST) {
			ItemStack extra = godjiismp$getExtraElytra();
			if (!extra.isEmpty()) return extra;
		}
		return player.getItemBySlot(slot);
	}

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void godjiismp$defineExtraElytra(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(GODJIISMP_EXTRA_ELYTRA, ItemStack.EMPTY);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void godjiismp$saveExtraElytra(CompoundTag tag, CallbackInfo ci) {
		ItemStack stack = godjiismp$getExtraElytra();
		if (!stack.isEmpty()) tag.put(GODJIISMP_EXTRA_ELYTRA_TAG, stack.save(((Player) (Object) this).level().registryAccess()));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void godjiismp$loadExtraElytra(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains(GODJIISMP_EXTRA_ELYTRA_TAG)) {
			ItemStack.parse(((Player) (Object) this).level().registryAccess(), tag.getCompound(GODJIISMP_EXTRA_ELYTRA_TAG))
					.filter(stack -> stack.is(Items.ELYTRA)).ifPresentOrElse(this::godjiismp$setExtraElytra,
						() -> godjiismp$setExtraElytra(ItemStack.EMPTY));
		} else {
			godjiismp$setExtraElytra(ItemStack.EMPTY);
		}
	}

	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void godjiismp$dropExtraElytra(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
		ItemStack stack = godjiismp$getExtraElytra();
		if (!stack.isEmpty()) {
			player.drop(stack.copy(), true);
			godjiismp$setExtraElytra(ItemStack.EMPTY);
		}
	}

	@Override public ItemStack godjiismp$getExtraElytra() {
		return ((Player) (Object) this).getEntityData().get(GODJIISMP_EXTRA_ELYTRA);
	}

	@Override public void godjiismp$setExtraElytra(ItemStack stack) {
		ItemStack value = stack != null && stack.is(Items.ELYTRA) ? stack.copy() : ItemStack.EMPTY;
		if (!value.isEmpty()) value.setCount(1);
		((Player) (Object) this).getEntityData().set(GODJIISMP_EXTRA_ELYTRA, value);
	}
}
