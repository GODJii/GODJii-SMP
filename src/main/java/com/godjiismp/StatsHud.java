package com.godjiismp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StatsHud {
	private StatsHud() {}
	private static volatile StatsPayload stats;
	static void update(StatsPayload value) { stats = value; }
	static void render(GuiGraphics g) {
		StatsPayload s = stats;
		Minecraft mc = Minecraft.getInstance();
		if (s == null || !s.visible() || mc.player == null) return;
		String[] values = {"$" + Money.fmt(s.money()), Integer.toString(s.mobs()), Integer.toString(s.players()), Integer.toString(s.deaths()), (s.playTicks() / 1_728_000) + "d " + ((s.playTicks() / 72_000) % 24) + "h"};
		ItemStack[] icons = {new ItemStack(Items.EMERALD), new ItemStack(Items.ZOMBIE_HEAD), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SKELETON_SKULL), new ItemStack(Items.CLOCK)};
		int panelWidth = 112, rowH = 13, panelHeight = 10 + values.length * rowH;
		float scale = 0.5f;
		float x = mc.getWindow().getGuiScaledWidth() - panelWidth * scale - 5;
		float y = (mc.getWindow().getGuiScaledHeight() - panelHeight * scale) / 2.0f;
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1.0f);
		int width = panelWidth, height = panelHeight;
		g.fill(0, 0, width, height, 0xA910131B);
		g.fill(0, 0, width, 1, 0xFF35B96F);
		g.drawString(mc.font, Component.literal(s.name()), 5, 4, 0xFFFFFFFF, false);
		for (int i = 0; i < values.length; i++) {
			int yy = 14 + i * rowH;
			g.renderItem(icons[i], 5, yy - 2);
			g.drawString(mc.font, Component.literal(values[i]), width - mc.font.width(values[i]) - 6, yy, 0xFFE8EDF2, false);
		}
		g.pose().popPose();
	}
}
