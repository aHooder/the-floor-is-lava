/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2020, ConorLeckey <https://github.com/ConorLeckey>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.ahooder.the_floor_is_lava.overlays;

import com.github.ahooder.the_floor_is_lava.Config;
import com.github.ahooder.the_floor_is_lava.LavaPlugin;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

public class TileCounterOverlay extends OverlayPanel
{
	@Inject
	private Config config;
	private final LavaPlugin plugin;

	private final static String LAVA_TILES = "Lava tiles:";
	private final static String REMAINING_DOUSES = "Remaining douses:";
	private final static String TOTAL_DOUSES = "Tiles doused:";
	private final static String[] STRINGS = new String[]{ LAVA_TILES, REMAINING_DOUSES, TOTAL_DOUSES};

	@Inject
	private TileCounterOverlay(LavaPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.MED);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "TheFloorIsLava Mode overlay"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.tileCounterOverlay())
			return null;

		String lavaTiles = addCommasToNumber(plugin.getTotalTiles());
		String remainingDouses = addCommasToNumber(plugin.getRemainingDousePoints());
		String totalDouses = addCommasToNumber(config.getTilesDoused());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(LAVA_TILES)
			.right(lavaTiles)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(REMAINING_DOUSES)
			.right(remainingDouses)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(TOTAL_DOUSES)
			.right(totalDouses)
			.build());

		panelComponent.setPreferredSize(new Dimension(
			getLongestStringWidth(STRINGS, graphics)
				+ getLongestStringWidth(new String[]{lavaTiles, remainingDouses, totalDouses}, graphics),
			0));

		return super.render(graphics);
	}

	private int getLongestStringWidth(String[] strings, Graphics2D graphics)
	{
		int longest = graphics.getFontMetrics().stringWidth("000000");
		for (String i : strings)
		{
			int currentItemWidth = graphics.getFontMetrics().stringWidth(i);
			if (currentItemWidth > longest)
			{
				longest = currentItemWidth;
			}
		}
		return longest;
	}

	public static String addCommasToNumber(int number)
	{
		String input = Integer.toString(number);
		StringBuilder output = new StringBuilder();
		for (int x = input.length() - 1; x >= 0; x--)
		{
			int lastPosition = input.length() - x - 1;
			if (lastPosition != 0 && lastPosition % 3 == 0)
			{
				output.append(",");
			}
			output.append(input.charAt(x));
		}
		return output.reverse().toString();
	}
}
