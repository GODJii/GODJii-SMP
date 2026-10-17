package com.godjiismp.mixin;

import com.godjiismp.DailyQuests;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks player-placed blocks so daily mining quests cannot be farmed by replacing blocks. */
@Mixin(BlockItem.class)
public abstract class BlockItemQuestMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void godjiismp$markPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
		if (callback.getReturnValue().consumesAction()) {
			DailyQuests.markPlaced(context.getLevel(), context.getClickedPos(), context.getPlayer());
		}
	}
}
