/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2020, Sam Ramirez <https://github.com/sram1337>
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

package com.github.ahooder.the_floor_is_lava;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/***
 * This class handles evaluating the config basedon which game mode is selected, and whether or not the user
 * has selected to customize their game mode.
 */
@Slf4j
class ConfigEvaluator implements Config
{

	/***
	 * Instead of injecting TheFloorIsLavaModeConfig into TheFloorIsLavaModePlugin, we inject it here and use it to evaluate
	 * what the final permissions should be. Then we should instead inject this class into TheFloorIsLavaModePlugin.
	 * Because this class also implements TheFloorIsLavaModeConfig, we only need to override the methods we care about ie.
	 * the ones controlled by the Game Mode dropdown.
	 */
	@Inject
	private Config config;

	// TheFloorIsLava Game Mode
	private static final int TILEMAN_TILE_OFFSET = 9;
	private static final boolean TILEMAN_INCLUDE_TOTAL_LEVEL = false;

	// Strict TheFloorIsLava Game Mode
	private static final int STRICT_TILEMAN_TILE_OFFSET = 0;
	private static final boolean STRICT_TILEMAN_INCLUDE_TOTAL_LEVEL = false;

	// Expeditious TheFloorIsLava Game Mode
	private static final int EXPEDITIOUS_TILEMAN_TILE_OFFSET = 0;
	private static final boolean EXPEDITIOUS_TILEMAN_INCLUDE_TOTAL_LEVEL = true;

	private static final Map<GameMode, Integer> gameModeToTilesOffsetDefault;
	private static final Map<GameMode, Boolean> gameModeToIncludeTotalLevelDefault;

	static
	{
		// Load Game Mode defaults for Tiles Offset
		gameModeToTilesOffsetDefault = new HashMap<>();
		gameModeToTilesOffsetDefault.put(GameMode.COMMUNITY, TILEMAN_TILE_OFFSET);
		gameModeToTilesOffsetDefault.put(GameMode.STRICT, STRICT_TILEMAN_TILE_OFFSET);
		gameModeToTilesOffsetDefault.put(GameMode.ACCELERATED, EXPEDITIOUS_TILEMAN_TILE_OFFSET);

		// Load Game Mode defaults for Include Total Levels
		gameModeToIncludeTotalLevelDefault = new HashMap<>();
		gameModeToIncludeTotalLevelDefault.put(GameMode.COMMUNITY, TILEMAN_INCLUDE_TOTAL_LEVEL);
		gameModeToIncludeTotalLevelDefault.put(GameMode.STRICT, STRICT_TILEMAN_INCLUDE_TOTAL_LEVEL);
		gameModeToIncludeTotalLevelDefault.put(GameMode.ACCELERATED, EXPEDITIOUS_TILEMAN_INCLUDE_TOTAL_LEVEL);
	}

	@Override
	public int tilesOffset()
	{
		if (config.enableCustomGameMode())
		{
			return config.tilesOffset();
		}
		else
		{
			return gameModeToTilesOffsetDefault.get(config.gameMode());
		}
	}

	@Override
	public boolean includeTotalLevel()
	{
		if (config.enableCustomGameMode())
		{
			return config.includeTotalLevel();
		}
		else
		{
			return gameModeToIncludeTotalLevelDefault.get(config.gameMode());
		}
	}

	@Override
	public int expPerTile()
	{
		if (config.enableCustomGameMode())
		{
			return config.expPerTile();
		}
		else
		{
			return 1000;
		}
	}

	@Override
	public boolean excludeExp()
	{
		if (config.enableCustomGameMode())
		{
			return config.excludeExp();
		}
		else
		{
			return false;
		}
	}

	@Override
	public boolean automarkTiles()
	{
		return config.automarkTiles();
	}

	@Override
	public boolean allowTileDeficit()
	{
		return config.allowTileDeficit();
	}
}
