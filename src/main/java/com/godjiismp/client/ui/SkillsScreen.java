package com.godjiismp.client.ui;

import com.godjiismp.skills.SkillPayload;
import com.godjiismp.skills.SkillUpgradePayload;
import com.godjiismp.skills.SkillOpenPayload;
import com.godjiismp.skills.SkillRequestPayload;
import com.godjiismp.weapons.LegendaryItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Sharp, vanilla-font skill tree. It deliberately avoids texture-sheet sampling. */
public final class SkillsScreen extends Screen {
    private static final String[] LINES = {"attack", "defense", "mining", "farming", "fishing", "exploration"};
    private static final int[] COLORS = {0xFFFF7967, 0xFF83B9FF, 0xFFDCECF8, 0xFF99E279, 0xFF74DDF2, 0xFFFFD66B};
    private static final int[][] CENTERS = {{70,82},{108,82},{146,82},{184,82},{222,82},{222,122},{184,122},{146,122},{108,122},{70,122}};
    private static SkillPayload data;
    private int line, selected = 1, savedBlur;
    private boolean capturedBlur;

    public SkillsScreen() { super(Component.literal("Skills")); }
    public static void receive(SkillPayload payload) {
        data = payload;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof SkillsScreen screen) screen.clampSelection();
    }
    public static void open() { Minecraft.getInstance().setScreen(new SkillsScreen()); }
    private void clampSelection() { if (data != null) selected = Math.max(1, Math.min(10, data.levels()[line] + 1)); }

    @Override protected void init() {
        if (!capturedBlur) {
            savedBlur = minecraft.options.getMenuBackgroundBlurriness();
            minecraft.options.menuBackgroundBlurriness().set(0);
            capturedBlur = true;
        }
        if (data != null) clampSelection();
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int left = (width - 256) / 2, top = (height - 200) / 2;
        panel(g, left, top);
        g.drawString(font, "SKILLS", left + 14, top + 10, 0xFFF6CF6A, true);
        if (data == null) {
            g.drawString(font, "Loading...", left + 88, top + 94, 0xFFFFFFFF);
            super.render(g, mouseX, mouseY, delta);
            return;
        }
        g.renderItem(new ItemStack(LegendaryItems.get(12)), left + 188, top + 5);
        String points = "Points: " + data.points();
        g.drawString(font, points, left + 244 - font.width(points), top + 10, 0xFFF6CF6A, true);
        drawSkillTabs(g, left, top, mouseX, mouseY);

        int color = COLORS[line];
        g.drawString(font, LINES[line].toUpperCase(java.util.Locale.ROOT), left + 52, top + 31, color, true);
        String level = "Level " + data.levels()[line] + "/10";
        g.drawString(font, level, left + 240 - font.width(level), top + 31, 0xFFE4DDF5, true);
        g.fill(left + 52, top + 45, left + 232, top + 54, 0xFF171426);
        g.fill(left + 52, top + 45, left + 52 + Math.round(180f * data.levels()[line] / 10f), top + 54, color);
        drawNodes(g, left, top, mouseX, mouseY, color);
        drawPerk(g, left, top);
        super.render(g, mouseX, mouseY, delta);
    }

    private void panel(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 256, y + 200, 0xFF090813);
        g.fill(x + 2, y + 2, x + 254, y + 198, 0xFF171426);
        g.fill(x + 3, y + 3, x + 253, y + 5, 0xFFFFD36A);
        g.fill(x + 3, y + 193, x + 253, y + 195, 0xFFFFD36A);
        g.fill(x + 3, y + 3, x + 5, y + 197, 0xFFFFD36A);
        g.fill(x + 251, y + 3, x + 253, y + 197, 0xFFFFD36A);
        g.fill(x + 8, y + 25, x + 43, y + 194, 0xFF0D0B17);
        g.fill(x + 47, y + 58, x + 247, y + 59, 0xFF514329);
        g.fill(x + 48, y + 137, x + 245, y + 138, 0xFF514329);
    }

    private void drawSkillTabs(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        for (int i = 0; i < 6; i++) {
            int x = left + 8, y = top + 27 + i * 28;
            boolean active = i == line, hover = mouseX >= x && mouseX < x + 35 && mouseY >= y && mouseY < y + 26;
            g.fill(x, y, x + 35, y + 26, active ? 0xFF3A3352 : 0xFF211D32);
            g.fill(x, y, x + 35, y + 1, active || hover ? 0xFFFFD36A : 0xFF534626);
            g.fill(x, y + 25, x + 35, y + 26, 0xFF534626);
            g.renderItem(new ItemStack(LegendaryItems.get(6 + i)), x + 9, y + 5);
        }
    }

    private void drawNodes(GuiGraphics g, int left, int top, int mouseX, int mouseY, int color) {
        for (int i = 0; i < CENTERS.length - 1; i++) {
            int x1 = left + CENTERS[i][0], y1 = top + CENTERS[i][1];
            int x2 = left + CENTERS[i + 1][0], y2 = top + CENTERS[i + 1][1];
            if (i < data.levels()[line]) drawLine(g, x1, y1, x2, y2, color);
            else drawLine(g, x1, y1, x2, y2, 0xFF454057);
        }
        for (int i = 0; i < CENTERS.length; i++) {
            int cx = left + CENTERS[i][0], cy = top + CENTERS[i][1];
            int state = i < data.levels()[line] ? 2 : (i == data.levels()[line] ? 1 : 0);
            int nodeColor = state == 2 ? color : state == 1 ? 0xFF573A42 : 0xFF242033;
            g.fill(cx - 13, cy - 13, cx + 13, cy + 13, 0xFF070610);
            g.fill(cx - 11, cy - 11, cx + 11, cy + 11, nodeColor);
            if (i + 1 == selected || (mouseX >= cx - 12 && mouseX <= cx + 12 && mouseY >= cy - 12 && mouseY <= cy + 12)) {
                g.fill(cx - 14, cy - 14, cx + 14, cy - 12, 0xFFFFD36A);
                g.fill(cx - 14, cy + 12, cx + 14, cy + 14, 0xFFFFD36A);
                g.fill(cx - 14, cy - 12, cx - 12, cy + 12, 0xFFFFD36A);
                g.fill(cx + 12, cy - 12, cx + 14, cy + 12, 0xFFFFD36A);
            }
            String n = Integer.toString(i + 1);
            g.drawString(font, n, cx - font.width(n) / 2, cy - 4, state == 0 ? 0xFF817B98 : 0xFFFFFFFF, true);
        }
    }

    private void drawPerk(GuiGraphics g, int left, int top) {
        int index = selected - 1, color = COLORS[line];
        g.fill(left + 48, top + 141, left + 246, top + 190, 0xFF0D0B17);
        g.fill(left + 48, top + 141, left + 246, top + 142, 0xFF514329);
        String[] split = data.perks()[line][index].split("\n", 2);
        g.drawString(font, split[0], left + 55, top + 147, 0xFFF6CF6A, true);
        if (split.length > 1) {
            int row = 0;
            for (var wrapped : font.split(Component.literal(split[1]), 126)) {
                g.drawString(font, wrapped, left + 55, top + 158 + row * 10, 0xFFCFC8E8);
                row++;
            }
        }
        int cost = data.costs()[index];
        boolean next = selected == data.levels()[line] + 1, canUpgrade = next && data.points() >= cost;
        boolean maxed = data.levels()[line] >= 10;
        int bx = left + 179, by = top + 149;
        int buttonColor = canUpgrade ? 0xFF8A6828 : 0xFF363143;
        g.fill(bx, by, bx + 64, by + 20, 0xFF090813);
        g.fill(bx + 2, by + 2, bx + 62, by + 18, buttonColor);
        g.fill(bx + 3, by + 3, bx + 61, by + 4, canUpgrade ? 0xFFFFD36A : 0xFF5B566B);
        String label = maxed ? "Maxed" : "Upgrade";
        g.drawString(font, label, bx + 32 - font.width(label) / 2, by + 6, 0xFFFFE7A1, true);
        String costText = "Cost: " + cost + " pt";
        g.drawString(font, costText, left + 212 - font.width(costText) / 2, top + 174, data.points() >= cost ? 0xFF8CF29B : 0xFFFF6B6B);
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps, y = y1 + (y2 - y1) * i / steps;
            g.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || data == null) return super.mouseClicked(mx, my, button);
        int left = (width - 256) / 2, top = (height - 200) / 2, x = (int) mx - left, y = (int) my - top;
        for (int i = 0; i < 6; i++) if (x >= 8 && x < 43 && y >= 27 + i * 28 && y < 53 + i * 28) {
            line = i; selected = Math.min(10, data.levels()[line] + 1); return true;
        }
        for (int i = 0; i < 10; i++) if (Math.abs(x - CENTERS[i][0]) <= 13 && Math.abs(y - CENTERS[i][1]) <= 13) { selected = i + 1; return true; }
        if (x >= 179 && x < 243 && y >= 149 && y < 169 && selected == data.levels()[line] + 1 && data.points() >= data.costs()[selected - 1]) {
            ClientPlayNetworking.send(new SkillUpgradePayload(line)); return true;
        }
        return super.mouseClicked(mx, my, button);
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
