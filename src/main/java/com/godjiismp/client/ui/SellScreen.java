package com.godjiismp.client.ui;

import com.godjiismp.market.SellMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

/** Inventory GUI with a dedicated sell staging grid and an explicit confirmation button. */
public final class SellScreen extends AbstractContainerScreen<SellMenu> {
    private int savedBlur;
    private boolean capturedBlur;

    public SellScreen(SellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        inventoryLabelY = 130;
    }

    @Override protected void init() {
        if (!capturedBlur) {
            savedBlur = Minecraft.getInstance().options.getMenuBackgroundBlurriness();
            Minecraft.getInstance().options.menuBackgroundBlurriness().set(0);
            capturedBlur = true;
        }
        super.init();
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png"),
                leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        int x = leftPos + 8 + 8 * 18, y = topPos + 18 + 5 * 18;
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF4BAE43);
        graphics.drawCenteredString(font, "$", x + 9, y + 4, 0xFFFFFFFF);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Sell"), 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.literal("Inventory"), 8, inventoryLabelY, 0xFF404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + 8 + 8 * 18, y = topPos + 18 + 5 * 18;
        if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            graphics.renderTooltip(font, Component.literal("Sell all sellable items"), mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos + 8 + 8 * 18, y = topPos + 18 + 5 * 18;
        if (button == 0 && mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void removed() {
        if (capturedBlur && Minecraft.getInstance() != null) {
            Minecraft.getInstance().options.menuBackgroundBlurriness().set(savedBlur);
            capturedBlur = false;
        }
        super.removed();
    }
}
