/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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
package com.github.ahooder.the_floor_is_lava;

import static com.github.ahooder.the_floor_is_lava.gpu.GpuPlugin.MAX_DISTANCE;
import static com.github.ahooder.the_floor_is_lava.gpu.GpuPlugin.MAX_FOG_DEPTH;
import com.github.ahooder.the_floor_is_lava.gpu.config.AntiAliasingMode;
import com.github.ahooder.the_floor_is_lava.gpu.config.ColorBlindMode;
import com.github.ahooder.the_floor_is_lava.gpu.config.UIScalingMode;
import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(Config.GROUP)
public interface Config extends net.runelite.client.config.Config
{
	String GROUP = "theFloorIsLava";

	@ConfigSection(
		name = "Settings",
		description = "Settings'",
		position = 1
	)
	String settingsSection = "settings";

	@ConfigSection(
		name = "GPU Settings",
		description = "Create a custom TheFloorIsLava game mode. Be sure to 'Enable Custom Game Mode'",
		position = 2
	)
	String gpuSettingsSection = "gpuSettings";

	@ConfigItem(
		keyName = "tileCounterOverlay",
		name = "Show tile counter overlay",
		section = settingsSection,
		description = "Configures whether to show an overlay with the total number of lava tiles.",
		position = 1
	)
	default boolean tileCounterOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "automarkTiles",
		name = "Auto-mark tiles",
		section = settingsSection,
		description = "Automatically mark tiles as you walk.",
		position = 2
	)
	default boolean automarkTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "drawOnMinimap",
		name = "Draw tiles on minimap",
		section = settingsSection,
		description = "Configures whether marked tiles should be drawn on minimap",
		position = 3
	)
	default boolean drawTilesOnMinimap()
	{
		return false;
	}

	@ConfigItem(
		keyName = "drawTilesOnWorldMap",
		name = "Draw tiles on world map",
		section = settingsSection,
		description = "Configures whether marked tiles should be drawn on world map",
		position = 4
	)
	default boolean drawTilesOnWorldMap()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showResetAllOption",
		name = "Enable reset-all option",
		section = settingsSection,
		description = "Adds a new right-click option on the inventory button for discarding all lava tiles",
		position = 5
	)
	default boolean showResetAllOption()
	{
		return false;
	}

	@Range(
		max = MAX_DISTANCE
	)
	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Draw distance",
		position = 1
	)
	default int drawDistance()
	{
		return 90;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "smoothBanding",
		name = "Remove Color Banding",
		description = "Smooths out the color banding that is present in the CPU renderer",
		position = 2
	)
	default boolean smoothBanding()
	{
		return false;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "antiAliasingMode",
		name = "Anti Aliasing",
		description = "Configures the anti-aliasing mode",
		position = 3
	)
	default AntiAliasingMode antiAliasingMode()
	{
		return AntiAliasingMode.DISABLED;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "uiScalingMode",
		name = "UI scaling mode",
		description = "Sampling function to use for the UI in stretched mode",
		position = 4
	)
	default UIScalingMode uiScalingMode()
	{
		return UIScalingMode.LINEAR;
	}

	@Range(
		max = MAX_FOG_DEPTH
	)
	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "fogDepth",
		name = "Fog depth",
		description = "Distance from the scene edge the fog starts",
		position = 5
	)
	default int fogDepth()
	{
		return 0;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "useComputeShaders",
		name = "Compute Shaders",
		description = "Offloads face sorting to GPU, enabling extended draw distance. Requires plugin restart.",
		warning = "This feature requires OpenGL 4.3 to use. Please check that your GPU supports this.\nRestart the plugin for changes to take effect.",
		position = 6,
		hidden = true
	)
	default boolean useComputeShaders()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 16
	)
	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "anisotropicFilteringLevel",
		name = "Anisotropic Filtering",
		description = "Configures the anisotropic filtering level.",
		position = 7
	)
	default int anisotropicFilteringLevel()
	{
		return 0;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "colorBlindMode",
		name = "Colorblindness Correction",
		description = "Adjusts colors to account for colorblindness",
		position = 8
	)
	default ColorBlindMode colorBlindMode()
	{
		return ColorBlindMode.NONE;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "brightTextures",
		name = "Bright Textures",
		description = "Use old texture lighting method which results in brighter game textures",
		position = 9
	)
	default boolean brightTextures()
	{
		return false;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "unlockFps",
		name = "Unlock FPS",
		description = "Removes the 50 FPS cap for camera movement",
		position = 10
	)
	default boolean unlockFps()
	{
		return false;
	}

	enum SyncMode
	{
		OFF,
		ON,
		ADAPTIVE
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "vsyncMode",
		name = "Vsync Mode",
		description = "Method to synchronize frame rate with refresh rate",
		position = 11
	)
	default SyncMode syncMode()
	{
		return SyncMode.ADAPTIVE;
	}

	@ConfigItem(
		section = gpuSettingsSection,
		keyName = "fpsTarget",
		name = "FPS Target",
		description = "Target FPS when unlock FPS is enabled and Vsync mode is OFF",
		position = 12
	)
	@Range(
		min = 1,
		max = 999
	)
	default int fpsTarget()
	{
		return 60;
	}
}
