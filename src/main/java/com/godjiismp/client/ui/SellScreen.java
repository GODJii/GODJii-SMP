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
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 18, 0xFF8B8B8B);
        graphics.fill(leftPos + 2, topPos + 20, leftPos + imageWidth - 2, topPos + 128, 0xFF8B8B8B);
        graphics.fill(leftPos + 2, topPos + 132, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF8B8B8B);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 5 && col >= 7) continue;
                drawSlot(graphics, leftPos + 8 + col * 18, topPos + 18 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlot(graphics, leftPos + 8 + col * 18, topPos + 140 + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlot(graphics, leftPos + 8 + col * 18, topPos + 198);
        int x = leftPos + 8 + 7 * 18, y = topPos + 18 + 5 * 18;
        graphics.fill(x, y, x + 36, y + 18, 0xFF06360C);
        graphics.fill(x + 1, y + 1, x + 35, y + 17, 0xFF26B343);
        graphics.fill(x + 2, y + 2, x + 34, y + 3, 0xFF77F58C);
        graphics.drawCenteredString(font, "SELL", x + 18, y + 5, 0xFFFFFFFF);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFF373737);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Sell"), 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.literal("Inventory"), 8, inventoryLabelY, 0xFF404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + 8 + 7 * 18, y = topPos + 18 + 5 * 18;
        if (mouseX >= x && mouseX < x + 36 && mouseY >= y && mouseY < y + 18) {
            graphics.renderTooltip(font, Component.literal("Sell all sellable items"), mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos + 8 + 7 * 18, y = topPos + 18 + 5 * 18;
        if (button == 0 && mouseX >= x && mouseX < x + 36 && mouseY >= y && mouseY < y + 18) {
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
