package com.godjiismp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class StatsHud {
	private StatsHud() {}
	private static volatile StatsPayload stats;
	static void update(StatsPayload value) { stats = value; }
	static void render(GuiGraphics g) {
		StatsPayload s = stats;
		Minecraft mc = Minecraft.getInstance();
		if (s == null || !s.visible() || mc.player == null) return;
		String[][] rows = {{"$", Money.fmt(s.money())}, {"M", Integer.toString(s.mobs())}, {"P", Integer.toString(s.players())}, {"D", Integer.toString(s.deaths())}, {"T", (s.playTicks() / 1_728_000) + "d " + ((s.playTicks() / 72_000) % 24) + "h"}};
		int panelWidth = 112, rowH = 13, panelHeight = 10 + rows.length * rowH;
		float scale = 0.5f;
		float x = mc.getWindow().getGuiScaledWidth() - panelWidth * scale - 5;
		float y = (mc.getWindow().getGuiScaledHeight() - panelHeight * scale) / 2.0f;
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1.0f);
		g.fill(0, 0, panelWidth, panelHeight, 0xA910131B);
		g.fill(0, 0, panelWidth, 1, 0xFF35B96F);
		g.drawString(mc.font, Component.literal(s.name()), 5, 4, 0xFFFFFFFF, false);
		for (int i = 0; i < rows.length; i++) {
			int yy = 15 + i * rowH;
			g.drawString(mc.font, Component.literal(rows[i][0]), 6, yy, 0xFF55D68A, false);
			g.drawString(mc.font, Component.literal(rows[i][1]), panelWidth - mc.font.width(rows[i][1]) - 6, yy, 0xFFE8EDF2, false);
		}
		g.pose().popPose();
	}
}