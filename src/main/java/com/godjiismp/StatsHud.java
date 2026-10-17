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
		int width = 112, rowH = 13, height = 10 + rows.length * rowH;
		int x = mc.getWindow().getGuiScaledWidth() - width - 5;
		int y = (mc.getWindow().getGuiScaledHeight() - height) / 2;
		g.fill(x, y, x + width, y + height, 0xA910131B);
		g.fill(x, y, x + width, y + 1, 0xFF35B96F);
		g.drawString(mc.font, Component.literal(s.name()), x + 5, y + 4, 0xFFFFFFFF, false);
		for (int i = 0; i < rows.length; i++) {
			int yy = y + 15 + i * rowH;
			g.drawString(mc.font, Component.literal(rows[i][0]), x + 6, yy, 0xFF55D68A, false);
			g.drawString(mc.font, Component.literal(rows[i][1]), x + width - mc.font.width(rows[i][1]) - 6, yy, 0xFFE8EDF2, false);
		}
	}
}
