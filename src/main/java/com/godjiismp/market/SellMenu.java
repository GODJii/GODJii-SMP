package com.godjiismp.market;

import com.godjiismp.Money;
import com.mymarket.Prices;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;

/** Server-authoritative drag-to-sell container. */
public final class SellMenu extends AbstractContainerMenu {
    private static final int SELL_ROWS = 6;
    private static final int SELL_COLUMNS = 9;
    private static final int BUTTON_SLOT = 53;
    private static final int SELLABLE_SLOTS = 52;
    public static final MenuType<SellMenu> TYPE = Registry.register(BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath("godjiismp_newdays", "sell_menu"), new MenuType<>(SellMenu::new, FeatureFlags.VANILLA_SET));

    private final SimpleContainer sellItems = new SimpleContainer(SELL_ROWS * SELL_COLUMNS);

    public SellMenu(int containerId, Inventory playerInventory) {
        super(TYPE, containerId);
        for (int row = 0; row < SELL_ROWS; row++) {
            for (int column = 0; column < SELL_COLUMNS; column++) {
                int slotIndex = row * SELL_COLUMNS + column;
                int x = 8 + column * 18, y = 18 + row * 18;
                if (slotIndex < SELLABLE_SLOTS) {
                    addSlot(new Slot(sellItems, slotIndex, x, y));
                } else {
                    addSlot(new Slot(sellItems, slotIndex, x, y) {
                        @Override public boolean mayPlace(ItemStack stack) { return false; }
                        @Override public boolean mayPickup(Player player) { return false; }
                    });
                }
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    public SellMenu(int containerId, Inventory playerInventory, Player player) {
        this(containerId, playerInventory);
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> registerCommand(dispatcher));
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sellgui").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(SellMenu::new, Component.translatable("container.godjiismp.sell")));
            return 1;
        }));
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(player instanceof ServerPlayer serverPlayer)) return false;
        long total = 0;
        for (int i = 0; i < SELLABLE_SLOTS; i++) {
            ItemStack stack = sellItems.getItem(i);
            if (stack.isEmpty()) continue;
            long unit = Prices.unitSell(stack);
            if (unit <= 0) continue;
            long count = stack.getCount();
            if (unit > (Long.MAX_VALUE - total) / count) {
                player.sendSystemMessage(Component.literal("Sale total is too large to process safely."));
                return true;
            }
            total += unit * count;
        }
        if (total <= 0) {
            player.sendSystemMessage(Component.literal("There are no sellable items in the sell area."));
            return true;
        }
        long balance = Money.balance(serverPlayer.serverLevel().getServer(), serverPlayer.getUUID());
        if (total > Long.MAX_VALUE - balance) {
            player.sendSystemMessage(Component.literal("Your balance is at its maximum. Remove some items before selling."));
            return true;
        }
        Money.give(serverPlayer.serverLevel().getServer(), serverPlayer.getUUID(), total);
        int soldStacks = 0;
        for (int i = 0; i < SELLABLE_SLOTS; i++) {
            ItemStack stack = sellItems.getItem(i);
            if (!stack.isEmpty() && Prices.unitSell(stack) > 0) {
                sellItems.setItem(i, ItemStack.EMPTY);
                soldStacks++;
            }
        }
        player.sendSystemMessage(Component.literal("Sold " + soldStacks + " stacks for $" + Money.fmt(total) + "."));
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moved = original.copy();
        if (index < SELLABLE_SLOTS) {
            if (!moveItemStackTo(original, SELL_ROWS * SELL_COLUMNS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(original, 0, SELLABLE_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return moved;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;
        for (int i = 0; i < sellItems.getContainerSize(); i++) {
            ItemStack stack = sellItems.getItem(i);
            if (!stack.isEmpty()) {
                sellItems.setItem(i, ItemStack.EMPTY);
                ItemStack returned = stack.copy();
                if (!player.getInventory().add(returned)) player.drop(returned, false);
            }
        }
    }

    @Override public boolean stillValid(Player player) { return true; }
}
