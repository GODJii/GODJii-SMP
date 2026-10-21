package com.godjiismp.accessory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A one-slot inventory view backed by a player's synchronized extra Elytra slot. */
public final class ExtraElytraContainer implements Container {
	private final ExtraElytraHolder holder;
	private final Player player;

	public ExtraElytraContainer(Player player) {
		this.player = player;
		this.holder = (ExtraElytraHolder) player;
	}

	@Override public int getContainerSize() { return 1; }
	@Override public boolean isEmpty() { return holder.godjiismp$getExtraElytra().isEmpty(); }
	@Override public ItemStack getItem(int slot) { return slot == 0 ? holder.godjiismp$getExtraElytra() : ItemStack.EMPTY; }
	@Override public ItemStack removeItem(int slot, int amount) {
		if (slot != 0 || amount <= 0) return ItemStack.EMPTY;
		ItemStack current = holder.godjiismp$getExtraElytra();
		if (current.isEmpty()) return ItemStack.EMPTY;
		ItemStack removed = current.split(amount);
		if (current.isEmpty()) holder.godjiismp$setExtraElytra(ItemStack.EMPTY);
		else holder.godjiismp$setExtraElytra(current);
		return removed;
	}
	@Override public ItemStack removeItemNoUpdate(int slot) {
		if (slot != 0) return ItemStack.EMPTY;
		ItemStack current = holder.godjiismp$getExtraElytra();
		holder.godjiismp$setExtraElytra(ItemStack.EMPTY);
		return current;
	}
	@Override public void setItem(int slot, ItemStack stack) {
		if (slot == 0) holder.godjiismp$setExtraElytra(stack.is(Items.ELYTRA) ? stack : ItemStack.EMPTY);
	}
	@Override public void setChanged() { }
	@Override public boolean stillValid(Player player) { return player == this.player && player.isAlive(); }
	@Override public void clearContent() { holder.godjiismp$setExtraElytra(ItemStack.EMPTY); }
}
