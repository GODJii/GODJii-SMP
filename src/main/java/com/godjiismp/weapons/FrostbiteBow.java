package com.godjiismp.weapons;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FrostbiteBow extends BowItem {
	public FrostbiteBow(Properties properties) { super(properties); }
	@Override public int getEnchantmentValue() { return WeaponsConfig.enchantability[4]; }
	@Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) { return repair.is(Items.NETHERITE_BLOCK); }
	@Override protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity,
			float inaccuracy, float angle, LivingEntity target) {
		if (projectile instanceof AbstractArrow arrow) arrow.setBaseDamage(arrow.getBaseDamage() * WeaponsConfig.bowProjectileDamageMultiplier);
		super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);
	}
}
