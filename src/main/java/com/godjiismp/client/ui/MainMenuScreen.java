package com.godjiismp.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact, transparent server hub inspired by DrDonut's two-column menu. */
public final class MainMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 5;
    private int savedBlur;
    private boolean capturedBlur;

    public MainMenuScreen() { super(Component.literal("GODJii SMP")); }

    @Override protected void init() {
        if (!capturedBlur) {
            savedBlur = minecraft.options.getMenuBackgroundBlurriness();
            minecraft.options.menuBackgroundBlurriness().set(0);
            capturedBlur = true;
        }
        int left = (width - BUTTON_WIDTH * 2 - GAP) / 2;
        int top = (height - 4 * (BUTTON_HEIGHT + GAP) - 43) / 2 + 39;
        String[][] entries = {
                {"Home", "home"}, {"Shop", "shop"},
                {"Sell", "sellgui"}, {"Skills", "skills"},
                {"Daily Quests", "quest"}, {"Stats", "sidebar"},
                {"Bounties", "bounty"}, {"Titles", "title"}
        };
        for (int i = 0; i < entries.length; i++) {
            int col = i % 2, row = i / 2;
            int x = left + col * (BUTTON_WIDTH + GAP);
            int y = top + row * (BUTTON_HEIGHT + GAP);
            String command = entries[i][1];
            addRenderableWidget(Button.builder(Component.literal(entries[i][0]), button -> runCommand(command))
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    private void runCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendCommand(command);
            minecraft.setScreen(null);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int titleY = (height - 4 * (BUTTON_HEIGHT + GAP) - 43) / 2 + 16;
        graphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public void removed() {
        if (capturedBlur && minecraft != null) {
            minecraft.options.menuBackgroundBlurriness().set(savedBlur);
            capturedBlur = false;
        }
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }
}
