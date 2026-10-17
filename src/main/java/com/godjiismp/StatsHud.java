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

		// PLAY_TIME is the player's persistent lifetime stat; converting with Minecraft's
		// 24,000 ticks per day keeps it through deaths and displays in in-game time.
		String[][] rows = {
				{"$", Money.fmt(s.money()), "0xFF48E064"},
				{"M", Integer.toString(s.mobs()), "0xFFC36BFF"},
				{"P", Integer.toString(s.players()), "0xFFFF6262"},
				{"D", Integer.toString(s.deaths()), "0xFFFF9B45"},
				{"T", (s.playTicks() / 24_000) + "d " + ((s.playTicks() / 1_000) % 24) + "h", "0xFFFFD84A"}
		};

		int width = mc.font.width(s.name());
		for (String[] row : rows) width = Math.max(width, mc.font.width(row[1]) + 12);
		int rowH = 10;
		int x = mc.getWindow().getGuiScaledWidth() - width - 4;
		int y = (mc.getWindow().getGuiScaledHeight() - (10 + rows.length * rowH)) / 2;

		g.drawString(mc.font, Component.literal(s.name()), x, y, 0xFFFFFFFF, true);
		for (int i = 0; i < rows.length; i++) {
			int yy = y + 10 + i * rowH;
			g.drawString(mc.font, Component.literal(rows[i][0]), x, yy, Integer.decode(rows[i][2]), true);
			g.drawString(mc.font, Component.literal(rows[i][1]), x + width - mc.font.width(rows[i][1]), yy, 0xFFE8EDF2, true);
		}
	}
}
