package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /armor trimremove     take the trim (pattern) off the armor in your hand
 * /armor color <color>  dye the armor in your hand any color (name like "red" or hex like ff8800)
 * /armor color reset    turn it back into the original armor
 *
 * Only leather armor can be dyed in vanilla Minecraft. So "dyeing" a netherite piece swaps it for a dyed leather
 * piece that keeps the same protection, toughness, knockback resistance, durability, enchantments, fire
 * resistance and trim. It still accepts trims. "reset" gives the original armor back.
 */
public final class ArmorCommands {
	private ArmorCommands() {}

	private static final String SKIN_KEY = "godjiismp_skin";

	private static final Map<String, Integer> COLORS = new LinkedHashMap<>();

	static {
		COLORS.put("white", 0xF9FFFE);
		COLORS.put("light_gray", 0x9D9D97);
		COLORS.put("gray", 0x474F52);
		COLORS.put("black", 0x1D1D21);
		COLORS.put("brown", 0x835432);
		COLORS.put("red", 0xB02E26);
		COLORS.put("orange", 0xF9801D);
		COLORS.put("yellow", 0xFED83D);
		COLORS.put("lime", 0x80C71F);
		COLORS.put("green", 0x5E7C16);
		COLORS.put("cyan", 0x169C9C);
		COLORS.put("light_blue", 0x3AB3DA);
		COLORS.put("blue", 0x3C44AA);
		COLORS.put("purple", 0x8932B8);
		COLORS.put("magenta", 0xC74EBD);
		COLORS.put("pink", 0xF38BAA);
		COLORS.put("gold", 0xFFAA00);
	}

	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx,
								Commands.CommandSelection env) {
		d.register(Commands.literal("armor")
				.then(Commands.literal("trimremove").executes(c -> trimRemove(c.getSource().getPlayerOrException())))
				.then(Commands.literal("color")
						.then(Commands.literal("reset").executes(c -> reset(c.getSource().getPlayerOrException())))
						.then(Commands.argument("color", StringArgumentType.greedyString())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(COLORS.keySet(), b))
								.executes(c -> color(c.getSource().getPlayerOrException(),
										StringArgumentType.getString(c, "color")))))
				.executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					say(p, "Hold an armor piece, then:", ChatFormatting.AQUA);
					say(p, "/armor trimremove  - remove its trim ($" + Money.fmt(ExtrasConfig.trimRemoveCost) + ")", ChatFormatting.GRAY);
					say(p, "/armor color <color>  - dye it, e.g. red or ff8800 ($" + Money.fmt(ExtrasConfig.armorColorCost) + ")", ChatFormatting.GRAY);
					say(p, "/armor color reset  - back to the original (free)", ChatFormatting.GRAY);
					return 1;
				}));
	}

	// ---------------------------------------------------------------- helpers

	private static void say(ServerPlayer p, String s, ChatFormatting color) {
		p.sendSystemMessage(Component.literal(s).withStyle(color));
	}

	/** Takes the money, or tells the player why not. */
	private static boolean pay(ServerPlayer p, long cost) {
		if (cost <= 0) return true;
		MinecraftServer server = p.getServer();
		if (server == null) return false;
		if (!Money.take(server, p.getUUID(), cost)) {
			say(p, "Not enough money. You need $" + Money.fmt(cost) + " (you have $"
					+ Money.fmt(Money.balance(server, p.getUUID())) + ").", ChatFormatting.RED);
			return false;
		}
		return true;
	}

	private static boolean isSkin(ItemStack s) {
		CustomData d = s.get(DataComponents.CUSTOM_DATA);
		return d != null && !d.copyTag().getString(SKIN_KEY).isEmpty();
	}

	private static Integer parseColor(String text) {
		String t = text.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		Integer named = COLORS.get(t);
		if (named != null) return named;
		if (t.startsWith("#")) t = t.substring(1);
		if (t.matches("[0-9a-f]{6}")) return Integer.parseInt(t, 16);
		return null;
	}

	// ---------------------------------------------------------------- trim remover

	private static int trimRemove(ServerPlayer p) {
		ItemStack held = p.getMainHandItem();
		if (held.isEmpty() || !held.has(DataComponents.TRIM)) {
			say(p, "Hold an armor piece that has a trim.", ChatFormatting.RED);
			return 0;
		}
		if (!pay(p, ExtrasConfig.trimRemoveCost)) return 0;
		held.remove(DataComponents.TRIM);
		say(p, "Trim removed" + (ExtrasConfig.trimRemoveCost > 0 ? " (-$" + Money.fmt(ExtrasConfig.trimRemoveCost) + ")" : "")
				+ ". You can add a new one at a smithing table.", ChatFormatting.GREEN);
		return 1;
	}

	// ---------------------------------------------------------------- armor colors

	private static int color(ServerPlayer p, String text) {
		Integer rgb = parseColor(text);
		if (rgb == null) {
			say(p, "Unknown color. Use a name like red, light_blue, gold or hex like ff8800.", ChatFormatting.RED);
			return 0;
		}
		ItemStack held = p.getMainHandItem();

		// already a dyed piece: just change the color
		if (isSkin(held)) {
			if (!pay(p, ExtrasConfig.armorColorCost)) return 0;
			held.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, false));
			say(p, "Color changed" + costText() + ".", ChatFormatting.GREEN);
			return 1;
		}

		if (!(held.getItem() instanceof ArmorItem armor)) {
			say(p, "Hold an armor piece (helmet, chestplate, leggings or boots).", ChatFormatting.RED);
			return 0;
		}
		if (held.is(ItemTags.DYEABLE)) {
			say(p, "That is already leather. Dye it with a normal dye in a cauldron or crafting table.", ChatFormatting.RED);
			return 0;
		}
		Item leather = leatherFor(armor.getType());
		if (leather == null) {
			say(p, "That armor can't be dyed.", ChatFormatting.RED);
			return 0;
		}
		if (!pay(p, ExtrasConfig.armorColorCost)) return 0;

		p.setItemInHand(InteractionHand.MAIN_HAND, makeSkin(held, armor, leather, rgb));
		say(p, "Armor dyed" + costText() + ". Use /armor color reset to get the original back.", ChatFormatting.GREEN);
		return 1;
	}

	private static String costText() {
		return ExtrasConfig.armorColorCost > 0 ? " (-$" + Money.fmt(ExtrasConfig.armorColorCost) + ")" : "";
	}

	private static Item leatherFor(ArmorItem.Type type) {
		return switch (type) {
			case HELMET -> Items.LEATHER_HELMET;
			case CHESTPLATE -> Items.LEATHER_CHESTPLATE;
			case LEGGINGS -> Items.LEATHER_LEGGINGS;
			case BOOTS -> Items.LEATHER_BOOTS;
			default -> null;
		};
	}

	/** A dyed leather piece with the stats of the original. */
	private static ItemStack makeSkin(ItemStack orig, ArmorItem armor, Item leather, int rgb) {
		ItemStack s = new ItemStack(leather);
		s.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb, false));

		var enchants = orig.get(DataComponents.ENCHANTMENTS);
		if (enchants != null) s.set(DataComponents.ENCHANTMENTS, enchants);
		var trim = orig.get(DataComponents.TRIM);
		if (trim != null) s.set(DataComponents.TRIM, trim);

		s.set(DataComponents.MAX_DAMAGE, orig.getMaxDamage());
		s.set(DataComponents.DAMAGE, orig.getDamageValue());
		if (orig.has(DataComponents.FIRE_RESISTANT)) s.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);

		// same protection as the original material
		ArmorMaterial mat = armor.getMaterial().value();
		EquipmentSlot slot = armor.getType().getSlot();
		EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
		String name = armor.getType().getName();
		Integer defense = mat.defense().get(armor.getType());
		ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
		b.add(Attributes.ARMOR, new AttributeModifier(id("skin_armor_" + name),
				defense == null ? 0 : defense, AttributeModifier.Operation.ADD_VALUE), group);
		if (mat.toughness() > 0) {
			b.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id("skin_toughness_" + name),
					mat.toughness(), AttributeModifier.Operation.ADD_VALUE), group);
		}
		if (mat.knockbackResistance() > 0) {
			b.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id("skin_knockback_" + name),
					mat.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE), group);
		}
		s.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());

		// keep the original name (custom name if it had one)
		var custom = orig.get(DataComponents.CUSTOM_NAME);
		if (custom != null) s.set(DataComponents.CUSTOM_NAME, custom);
		else s.set(DataComponents.ITEM_NAME, Component.translatable(orig.getItem().getDescriptionId()));
		s.set(DataComponents.LORE, new ItemLore(List.of(
				Component.literal("Dyed armor").withStyle(ChatFormatting.GRAY),
				Component.literal("/armor color reset to restore").withStyle(ChatFormatting.DARK_GRAY))));

		// remember what it was (and keep any other custom data it had)
		CustomData old = orig.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = old == null ? new CompoundTag() : old.copyTag();
		tag.putString(SKIN_KEY, BuiltInRegistries.ITEM.getKey(orig.getItem()).toString());
		s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return s;
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(GodjiiSmpMod.MOD_ID, path);
	}

	private static int reset(ServerPlayer p) {
		ItemStack held = p.getMainHandItem();
		if (!isSkin(held)) {
			say(p, "Hold an armor piece you dyed with /armor color.", ChatFormatting.RED);
			return 0;
		}
		CompoundTag tag = held.get(DataComponents.CUSTOM_DATA).copyTag();
		Item orig = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(SKIN_KEY)));
		if (orig == Items.AIR) {
			say(p, "The original armor type no longer exists.", ChatFormatting.RED);
			return 0;
		}
		ItemStack r = new ItemStack(orig);
		var enchants = held.get(DataComponents.ENCHANTMENTS);
		if (enchants != null) r.set(DataComponents.ENCHANTMENTS, enchants);
		var trim = held.get(DataComponents.TRIM);
		if (trim != null) r.set(DataComponents.TRIM, trim);
		r.set(DataComponents.DAMAGE, held.getDamageValue());
		var custom = held.get(DataComponents.CUSTOM_NAME);
		if (custom != null) r.set(DataComponents.CUSTOM_NAME, custom);
		tag.remove(SKIN_KEY);
		if (!tag.isEmpty()) r.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

		p.setItemInHand(InteractionHand.MAIN_HAND, r);
		say(p, "Original armor restored.", ChatFormatting.GREEN);
		return 1;
	}
}
