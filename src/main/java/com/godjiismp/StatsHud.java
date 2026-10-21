package com.godjiismp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact server statistics panel with crisp, hand-drawn pixel icons. */
public final class StatsHud {
    private StatsHud() {}
    private static volatile StatsPayload stats;
    private static final String[][] ICONS = {
            {"..GGGG..", ".GwwwwG.", "GwwggwwG", "GwwggwwG", "GwwggwwG", ".GggggG.", "..GGGG..", "...GG..."},
            {"..BBBB..", ".BBBBBB.", ".GggggG.", "GggggggG", "GgxGxggG", "GggggggG", "GgxggxgG", ".GggggG."},
            {"SS....SS", ".SS..SS.", "..SSSS..", "...SS...", "..SSSS..", ".SS..SS.", "SS....SS", "........"},
            {"..WWWW..", ".WWWWWW.", "WWXWWXWW", "WWWWWWWW", "WWXWWXWW", ".WWWWWW.", "..W..W..", "........"},
            {"..YYYY..", ".YddddY.", "YdYYYYdY", "YdY..YdY", "YdY..YdY", "YdYYYYdY", ".YddddY.", "..YYYY.."}
    };
    private static int color(char pixel) {
        return switch (pixel) {
            case 'G' -> 0xFF087E42; case 'g' -> 0xFF18C96A; case 'w' -> 0xFFB5FF69;
            case 'B' -> 0xFF513C2D; case 'b' -> 0xFF75563C; case 'S' -> 0xFFCED8E3;
            case 'x' -> 0xFF0A0A0A;
            case 'W' -> 0xFFE8EDF2; case 'X' -> 0xFF30333B;
            case 'Y' -> 0xFFFFD249; case 'd' -> 0xFFB77A18;
            default -> 0;
        };
    }
    private static void drawIcon(GuiGraphics g, int index, int x, int y) {
        String[] pattern = ICONS[index];
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length(); col++) {
                int color = color(pattern[row].charAt(col));
                if (color != 0) g.fill(x + col * 2, y + row * 2, x + col * 2 + 2, y + row * 2 + 2, color);
            }
        }
    }
    static void update(StatsPayload value) { stats = value; }
    static void render(GuiGraphics g) {
        StatsPayload s = stats;
        Minecraft mc = Minecraft.getInstance();
        if (s == null || !s.visible() || mc.player == null) return;
        String[] values = {"$" + Money.fmt(s.money()), Integer.toString(s.mobs()), Integer.toString(s.players()), Integer.toString(s.deaths()), (s.playTicks() / 1_728_000) + "d " + ((s.playTicks() / 72_000) % 24) + "h"};
        int panelWidth = 112, rowH = 13, panelHeight = 10 + values.length * rowH;
        float scale = 0.5f;
        float x = mc.getWindow().getGuiScaledWidth() - panelWidth * scale - 5;
        float y = (mc.getWindow().getGuiScaledHeight() - panelHeight * scale) / 2.0f;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.fill(0, 0, panelWidth, panelHeight, 0xA910131B);
        g.fill(0, 0, panelWidth, 1, 0xFF35B96F);
        g.drawString(mc.font, Component.literal(s.name()), 5, 4, 0xFFFFFFFF, false);
        for (int i = 0; i < values.length; i++) {
            int yy = 14 + i * rowH;
            drawIcon(g, i, 4, yy - 1);
            g.drawString(mc.font, Component.literal(values[i]), panelWidth - mc.font.width(values[i]) - 6, yy, 0xFFE8EDF2, false);
        }
        g.pose().popPose();
    }
}
