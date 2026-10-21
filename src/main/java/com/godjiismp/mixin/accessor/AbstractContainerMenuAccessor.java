package com.godjiismp.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Invoker("addSlot")
	Slot godjiismp$addSlot(Slot slot);

	@Accessor("slots")
	NonNullList<Slot> godjiismp$getSlots();
}
