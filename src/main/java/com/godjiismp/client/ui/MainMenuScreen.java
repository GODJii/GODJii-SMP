package com.godjiismp.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact server hub inspired by the button-grid layout used on DrDonut. */
public final class MainMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 5;

    public MainMenuScreen() { super(Component.literal("GODJii SMP")); }

    @Override protected void init() {
        int left = (width - BUTTON_WIDTH * 2 - GAP) / 2;
        int top = (height - 4 * (BUTTON_HEIGHT + GAP) - 43) / 2 + 28;
        String[][] entries = {
                {"Home", "home"}, {"Shop", "shop"},
                {"Sell", "sell"}, {"Skills", "skills"},
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
        graphics.fill(0, 0, width, height, 0x99000000);
        int panelWidth = BUTTON_WIDTH * 2 + GAP + 24;
        int panelHeight = 4 * (BUTTON_HEIGHT + GAP) + 43;
        int x = (width - panelWidth) / 2, y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xDD161616);
        graphics.fill(x, y, x + panelWidth, y + 2, 0xFF38B56C);
        graphics.drawCenteredString(font, title, width / 2, y + 9, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
