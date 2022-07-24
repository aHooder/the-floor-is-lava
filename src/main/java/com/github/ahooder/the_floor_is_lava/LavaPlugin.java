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

import com.github.ahooder.the_floor_is_lava.gpu.GpuPlugin;
import com.github.ahooder.the_floor_is_lava.overlays.MinimapOverlay;
import com.github.ahooder.the_floor_is_lava.overlays.TileCounterOverlay;
import com.github.ahooder.the_floor_is_lava.overlays.WorldMapOverlay;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import static net.runelite.api.Constants.TILE_FLAG_BRIDGE;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

@Slf4j
@PluginDescriptor(
	name = "The Floor is Lava",
	description = "Automatically place lava tiles where you walk",
	tags = {"lava", "tiles", "gpu"},
	loadInSafeMode = false,
	conflicts = "GPU"
)
public class LavaPlugin extends Plugin
{
	private static final String DOUSE_TILE = "Douse lava tile";
	private static final String PLACE_TILE = "Place lava tile";
	private static final String FORCE_DOUSE_TILE = "Force clear lava tile";
	private static final String CLEAR_ALL_TILES = "Clear all lava tiles";
	private static final String WALK_HERE = "Walk here";
	private static final String REGION_PREFIX = "region_";

	private static final WidgetMenuOption clearAllOptionFixed = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption clearAllOptionResizable = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption clearAllOptionResizable2 = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final Gson GSON = new Gson();

	@Getter
	private final List<WorldPoint> points = new ArrayList<>();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MenuManager menuManager;

	@Inject
	private MinimapOverlay minimapOverlay;

	@Inject
	private WorldMapOverlay worldMapOverlay;

	@Inject
	private TileCounterOverlay tileCounterOverlay;

	@Inject
	private GpuPlugin gpuPlugin;

	@Inject
	private Config config;

	@Provides
	Config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(Config.class);
	}

	private final MovementFlag[] fullBlock = new MovementFlag[]
		{MovementFlag.BLOCK_MOVEMENT_FLOOR,
			MovementFlag.BLOCK_MOVEMENT_FLOOR_DECORATION,
			MovementFlag.BLOCK_MOVEMENT_OBJECT,
			MovementFlag.BLOCK_MOVEMENT_FULL};

	private final MovementFlag[] allDirections = new MovementFlag[]
		{
			MovementFlag.BLOCK_MOVEMENT_NORTH_WEST,
			MovementFlag.BLOCK_MOVEMENT_NORTH,
			MovementFlag.BLOCK_MOVEMENT_NORTH_EAST,
			MovementFlag.BLOCK_MOVEMENT_EAST,
			MovementFlag.BLOCK_MOVEMENT_SOUTH_EAST,
			MovementFlag.BLOCK_MOVEMENT_SOUTH,
			MovementFlag.BLOCK_MOVEMENT_SOUTH_WEST,
			MovementFlag.BLOCK_MOVEMENT_WEST
		};

	private final HashSet<Integer> tutorialIslandRegionIds = new HashSet<Integer>();

	private int totalTileCount;
	private WorldPoint lastTile;
	private int lastPlane;
	private boolean inHouse = false;
	private long totalXp;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction().getId() != MenuAction.RUNELITE.getId() ||
			!event.getMenuOption().startsWith(DOUSE_TILE) &&
			!event.getMenuOption().equals(PLACE_TILE) &&
			!event.getMenuOption().equals(FORCE_DOUSE_TILE))
			return;

		Tile target = client.getSelectedSceneTile();
		if (target == null)
			return;

		updateTileMark(target.getWorldLocation(),
			event.getMenuOption().equals(PLACE_TILE),
			event.getMenuOption().equals(FORCE_DOUSE_TILE));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getOption().equals(WALK_HERE))
		{
			final Tile selectedSceneTile = client.getSelectedSceneTile();

			if (selectedSceneTile == null)
			{
				return;
			}

			WorldPoint worldPoint = selectedSceneTile.getWorldLocation();
			final int regionId = worldPoint.getRegionID();
			final LavaTile point = new LavaTile(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(),
				getPlaneIncludingBridge(worldPoint));

			boolean isLava = getTiles(regionId).contains(point);
			boolean force = client.isKeyPressed(KeyCode.KC_SHIFT);
			if (isLava || force) {
				client.createMenuEntry(-1)
					.setOption(force ?
						(isLava ? FORCE_DOUSE_TILE : PLACE_TILE) :
						getDouseOptionString())
					.setTarget(event.getTarget())
					.setType(MenuAction.RUNELITE);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		autoMark();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
			return;
		loadPoints();
		updateTileCounter();
		inHouse = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(Config.GROUP)) {
			updateTileCounter();
			if (event.getKey().equals("showResetAllOption"))
				updateCustomOptions();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();

		if (gameObject.getId() == 4525)
		{
			inHouse = true;
		}
	}

	@Override
	protected void startUp()
	{
		clientThread.invoke(() -> {
			try
			{
				if (!gpuPlugin.initialize())
					return false;

				eventBus.register(gpuPlugin);

				tutorialIslandRegionIds.add(12079);
				tutorialIslandRegionIds.add(12080);
				tutorialIslandRegionIds.add(12335);
				tutorialIslandRegionIds.add(12336);
				tutorialIslandRegionIds.add(12592);
				overlayManager.add(minimapOverlay);
				overlayManager.add(worldMapOverlay);
				overlayManager.add(tileCounterOverlay);
				updateCustomOptions();
				loadPoints();
				updateTileCounter();
				log.debug("startup");
			}
			catch (Throwable e)
			{
				log.error("Error starting GPU plugin", e);

				SwingUtilities.invokeLater(() ->
				{
					try
					{
						pluginManager.setPluginEnabled(this, false);
						pluginManager.stopPlugin(this);
					}
					catch (PluginInstantiationException ex)
					{
						log.error("error stopping plugin", ex);
					}
				});

				gpuPlugin.destroy();
			}

			return true;
		});
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() -> {
			eventBus.unregister(gpuPlugin);
			gpuPlugin.destroy();

			tutorialIslandRegionIds.clear();
			overlayManager.remove(minimapOverlay);
			overlayManager.remove(worldMapOverlay);
			overlayManager.remove(tileCounterOverlay);
			menuManager.removeManagedCustomMenu(clearAllOptionFixed);
			points.clear();
		});
	}

	private void updateCustomOptions() {
		if (config.showResetAllOption()) {
			menuManager.addManagedCustomMenu(clearAllOptionFixed, e -> clearAllLavaTiles());
			menuManager.addManagedCustomMenu(clearAllOptionResizable, e -> clearAllLavaTiles());
			menuManager.addManagedCustomMenu(clearAllOptionResizable2, e -> clearAllLavaTiles());
		} else {
			removeCustomOptions();
		}
	}

	private void removeCustomOptions() {
		menuManager.removeManagedCustomMenu(clearAllOptionFixed);
		menuManager.removeManagedCustomMenu(clearAllOptionResizable);
		menuManager.removeManagedCustomMenu(clearAllOptionResizable2);
	}

	private void clearAllLavaTiles()
	{
		configManager.getConfigurationKeys(Config.GROUP)
			.stream()
			.filter(key -> key.startsWith(Config.GROUP + "." + REGION_PREFIX))
			.forEach(key -> {
				configManager.unsetConfiguration(Config.GROUP, key.substring(Config.GROUP.length() + 1));
			});
		loadPoints();
	}

	private int getPlaneIncludingBridge(WorldPoint wp) {
//		int plane = client.getPlane(); // TODO: check if this is now broken
		int plane = wp.getPlane();

		if (wp.getX() >= 3144 && wp.getY() >= 3472 &&
			wp.getX() <= 3183 && wp.getY() <= 3508)
			return plane;

		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		if (lp == null)
			return wp.getPlane();

		byte[][][] tileSettings = client.getTileSettings();
		if (plane == 0 && (tileSettings[1][lp.getSceneX()][lp.getSceneY()] & TILE_FLAG_BRIDGE) == TILE_FLAG_BRIDGE)
			plane++;
		return plane;
	}

	private void autoMark()
	{
		final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
		if (playerPos == null)
		{
			return;
		}

		int playerPlane = getPlaneIncludingBridge(playerPos);
		long currentTotalXp = client.getOverallExperience();

		// If we have no last tile, we probably just spawned in, so make sure we walk on our current tile
		if ((lastTile == null
			|| (lastTile.distanceTo(playerPos) != 0 && lastPlane == playerPlane)
			|| lastPlane != playerPlane))
		{
			// Player moved
			handleWalkedToTile(playerPos);
			lastTile = playerPos;
			lastPlane = getPlaneIncludingBridge(lastTile);
			updateTileCounter();
			log.debug("player moved");
			log.debug("last tile={}  distance={}", lastTile, lastTile == null ? "null" : lastTile.distanceTo(playerPos));
		}
		else if (totalXp != currentTotalXp)
		{
			updateTileCounter();
			totalXp = currentTotalXp;
		}
	}

	List<String> getAllRegionIds(String configGroup)
	{
		return removeRegionPrefixes(configManager.getConfigurationKeys(configGroup + ".region"));
	}

	private List<String> removeRegionPrefixes(List<String> regions)
	{
		List<String> trimmedRegions = new ArrayList<String>();
		for (String region : regions)
		{
			trimmedRegions.add(removeRegionPrefix(region));
		}
		return trimmedRegions;
	}

	private String removeRegionPrefix(String region)
	{
		return region.substring(region.indexOf('_') + 1);
	}

	public Collection<LavaTile> getTiles(int regionId)
	{
		return getConfiguration(Config.GROUP, REGION_PREFIX + regionId);
	}

	public Collection<LavaTile> getTiles(String regionId)
	{
		return getConfiguration(Config.GROUP, REGION_PREFIX + regionId);
	}

	private String getDouseOptionString() {
		return DOUSE_TILE + " " +
			ColorUtil.wrapWithColorTag(String.format("(%s remaining)",
				TileCounterOverlay.addCommasToNumber(getRemainingDousePoints())),
				Color.GRAY);
	}

	private void updateTileCounter()
	{
		List<String> regions = configManager.getConfigurationKeys(Config.GROUP + ".region");
		int totalTiles = 0;
		for (String region : regions)
		{
			Collection<LavaTile> regionTiles = getTiles(removeRegionPrefix(region));

			totalTiles += regionTiles.size();
		}

		log.debug("Updating tile counter");
		totalTileCount = totalTiles;
	}

	private Collection<LavaTile> getConfiguration(String configGroup, String key)
	{
		String json = configManager.getConfiguration(configGroup, key);

		if (Strings.isNullOrEmpty(json))
			return Collections.emptyList();

		return GSON.fromJson(json, new TypeToken<List<LavaTile>>() {}.getType());
	}

	private void loadPoints()
	{
		points.clear();

		int[] regions = client.getMapRegions();

		if (regions == null)
		{
			return;
		}

		for (int regionId : regions)
		{
			// load points for region
			log.debug("Loading points for region {}", regionId);
			Collection<WorldPoint> worldPoint = translateToWorldPoint(getTiles(regionId));
			points.addAll(worldPoint);
		}
		updateTileCounter();
	}

	private void savePoints(int regionId, Collection<LavaTile> points)
	{
		if (points == null || points.isEmpty())
		{
			configManager.unsetConfiguration(Config.GROUP, REGION_PREFIX + regionId);
			return;
		}

		String json = GSON.toJson(points);
		configManager.setConfiguration(Config.GROUP, REGION_PREFIX + regionId, json);
	}

	private Collection<WorldPoint> translateToWorldPoint(Collection<LavaTile> points)
	{
		if (points.isEmpty())
		{
			return Collections.emptyList();
		}

		return points.stream()
			.map(point -> WorldPoint.fromRegion(point.getRegionId(), point.getRegionX(), point.getRegionY(), point.getZ()))
			.flatMap(worldPoint ->
			{
				final Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(client, worldPoint);
				return localWorldPoints.stream();
			})
			.collect(Collectors.toList());
	}

	public int getTotalTiles()
	{
		return totalTileCount;
	}

	public boolean containsTile(int plane, int tileX, int tileY) {
		for (final WorldPoint wp : getPoints()) {
			int sceneX = wp.getX() - client.getBaseX();
			int sceneY = wp.getY() - client.getBaseY();
			if (wp.getPlane() == plane && sceneX == tileX && sceneY == tileY)
				return true;
		}
		return false;
	}

	private void handleWalkedToTile(WorldPoint currentPlayerPoint)
	{
		if (currentPlayerPoint == null ||
			inHouse ||
			!config.automarkTiles())
		{
			return;
		}

		// If player moves 2 tiles in a straight line, fill in the middle tile
		if (lastTile != null)
		{
			// Mark the tile they walked from
			updateTileMark(lastTile, true, false);

			int xDiff = currentPlayerPoint.getX() - lastTile.getX();
			int yDiff = currentPlayerPoint.getY() - lastTile.getY();
			int xModifier = xDiff / 2;
			int yModifier = yDiff / 2;

			int distance;
			{
				int x = xDiff * Perspective.LOCAL_TILE_SIZE;
				int y = yDiff * Perspective.LOCAL_TILE_SIZE;
				distance = (int) Math.sqrt(x * x + y * y);
			}
			log.info("diff: {},{}, mod: {},{}, distance: {}", xDiff, yDiff, xModifier, yModifier, distance);

			switch (distance)
			{
				case 0: // Haven't moved
				case 128: // Moved 1 tile
					return;
				case 181: // Moved 1 tile diagonally
					handleCornerMovement(xDiff, yDiff);
					break;
				case 256: // Moved 2 tiles straight
				case 362: // Moved 2 tiles diagonally
					log.info("Moved straight or diagonally");
					fillTile(new WorldPoint(lastTile.getX() + xModifier, lastTile.getY() + yModifier, lastTile.getPlane()));
					break;
				case 286: // Moved in an 'L' shape
					handleLMovement(xDiff, yDiff);
					break;
			}
		}
	}

	private void handleLMovement(int xDiff, int yDiff)
	{
		int xModifier = xDiff / 2;
		int yModifier = yDiff / 2;
		int tileBesideXDiff, tileBesideYDiff;

		// Whichever direction has moved only one, keep it 0. This is the translation to the potential 'problem' gameObject
		if (Math.abs(yDiff) == 128)
		{
			tileBesideXDiff = xDiff;
			tileBesideYDiff = 0;
		}
		else
		{
			tileBesideXDiff = 0;
			tileBesideYDiff = yDiff;
		}

		MovementFlag[] tileBesideFlagsArray = getTileMovementFlags(lastTile.getX() + tileBesideXDiff, lastTile.getY() + tileBesideYDiff);

		if (tileBesideFlagsArray.length == 0)
		{
			fillTile(new WorldPoint(lastTile.getX() + tileBesideXDiff / 2, lastTile.getY() + tileBesideYDiff / 2, lastTile.getPlane()));
		}
		else if (containsAnyOf(fullBlock, tileBesideFlagsArray))
		{
			if (yModifier == 64)
			{
				yModifier = 128;
			}
			else if (xModifier == 64)
			{
				xModifier = 128;
			}
			fillTile(new WorldPoint(lastTile.getX() + xModifier, lastTile.getY() + yModifier, lastTile.getPlane()));
		}
		else if (containsAnyOf(allDirections, tileBesideFlagsArray))
		{
			MovementFlag direction1, direction2;
			if (yDiff == 256 || yDiff == -128)
			{
				// Moving 2 North or 1 South
				direction1 = MovementFlag.BLOCK_MOVEMENT_SOUTH;
			}
			else
			{
				// Moving 2 South or 1 North
				direction1 = MovementFlag.BLOCK_MOVEMENT_NORTH;
			}
			if (xDiff == 256 || xDiff == -128)
			{
				// Moving 2 East or 1 West
				direction2 = MovementFlag.BLOCK_MOVEMENT_WEST;
			}
			else
			{
				// Moving 2 West or 1 East
				direction2 = MovementFlag.BLOCK_MOVEMENT_EAST;
			}

			if (containsAnyOf(tileBesideFlagsArray, new MovementFlag[]{direction1, direction2}))
			{
				// Interrupted
				if (yModifier == 64)
				{
					yModifier = 128;
				}
				else if (xModifier == 64)
				{
					xModifier = 128;
				}
				fillTile(new WorldPoint(lastTile.getX() + xModifier, lastTile.getY() + yModifier, lastTile.getPlane()));
			}
			else
			{
				// Normal Pathing
				fillTile(new WorldPoint(lastTile.getX() + tileBesideXDiff / 2, lastTile.getY() + tileBesideYDiff / 2, lastTile.getPlane()));
			}
		}
	}

	private void handleCornerMovement(int xDiff, int yDiff)
	{
		LocalPoint northPoint;
		LocalPoint southPoint;
		if (yDiff > 0)
		{
			northPoint = new LocalPoint(lastTile.getX(), lastTile.getY() + yDiff);
			southPoint = new LocalPoint(lastTile.getX() + xDiff, lastTile.getY());
		}
		else
		{
			northPoint = new LocalPoint(lastTile.getX() + xDiff, lastTile.getY());
			southPoint = new LocalPoint(lastTile.getX(), lastTile.getY() + yDiff);
		}

		MovementFlag[] northTile = getTileMovementFlags(northPoint);
		MovementFlag[] southTile = getTileMovementFlags(southPoint);

		if (xDiff + yDiff == 0)
		{
			// Diagonal tilts north west
			if (containsAnyOf(fullBlock, northTile)
				|| containsAnyOf(northTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_WEST}))
			{
				fillTile(WorldPoint.fromLocal(client, southPoint));
			}
			else if (containsAnyOf(fullBlock, southTile)
				|| containsAnyOf(southTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_EAST}))
			{
				fillTile(WorldPoint.fromLocal(client, northPoint));
			}
		}
		else
		{
			// Diagonal tilts north east
			if (containsAnyOf(fullBlock, northTile)
				|| containsAnyOf(northTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_EAST}))
			{
				fillTile(WorldPoint.fromLocal(client, southPoint));
			}
			else if (containsAnyOf(fullBlock, southTile)
				|| containsAnyOf(southTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_WEST}))
			{
				fillTile(WorldPoint.fromLocal(client, northPoint));
			}
		}
	}

	private MovementFlag[] getTileMovementFlags(int x, int y)
	{
		LocalPoint pointBeside = new LocalPoint(x, y);

		CollisionData[] collisionData = client.getCollisionMaps();
		assert collisionData != null;
		int[][] collisionDataFlags = collisionData[client.getPlane()].getFlags();

		Set<MovementFlag> tilesBesideFlagsSet = MovementFlag.getSetFlags(collisionDataFlags[pointBeside.getSceneX()][pointBeside.getSceneY()]);
		MovementFlag[] tileBesideFlagsArray = new MovementFlag[tilesBesideFlagsSet.size()];
		tilesBesideFlagsSet.toArray(tileBesideFlagsArray);

		return tileBesideFlagsArray;
	}

	private MovementFlag[] getTileMovementFlags(LocalPoint localPoint)
	{
		return getTileMovementFlags(localPoint.getX(), localPoint.getY());
	}

	private boolean containsAnyOf(MovementFlag[] comparisonFlags, MovementFlag[] flagsToCompare)
	{
		if (comparisonFlags.length == 0 || flagsToCompare.length == 0)
		{
			return false;
		}
		for (MovementFlag flag : flagsToCompare)
		{
			if (Arrays.asList(comparisonFlags).contains(flag))
			{
				return true;
			}
		}
		return false;
	}

	private boolean regionIsOnTutorialIsland(int regionId)
	{
		return tutorialIslandRegionIds.contains(regionId);
	}

	private void fillTile(WorldPoint point)
	{
		if (lastPlane != getPlaneIncludingBridge(point))
		{
			return;
		}
		updateTileMark(point, true, false);
	}

	public int getRemainingDousePoints() {
		return client.getTotalLevel() - config.getTilesDoused() - 32;
	}

	private void updateTileMark(@NonNull WorldPoint worldPoint, boolean markedValue, boolean force)
	{
		int plane = getPlaneIncludingBridge(worldPoint);
		int regionId = worldPoint.getRegionID();
		LavaTile point = new LavaTile(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), plane);
		log.debug("Updating point: {} - {}", point, worldPoint);

		List<LavaTile> lavaTiles = new ArrayList<>(getTiles(regionId));

		if (markedValue) {
			if (!lavaTiles.contains(point))
				lavaTiles.add(point);
		} else {
			if (!force) {
				// Check if the player has any unspent douse points
				if (getRemainingDousePoints() > 0) {
					config.setTilesDoused(config.getTilesDoused() + 1);
				} else {
					client.addChatMessage(ChatMessageType.GAMEMESSAGE,
						"[The Floor is Lava]", "You have no remaining douse points left.",
						"", false);
					return;
				}
			}
			lavaTiles.remove(point);
		}

		savePoints(regionId, lavaTiles);
		loadPoints();
	}

	@AllArgsConstructor
	enum MovementFlag
	{
		BLOCK_MOVEMENT_NORTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST),
		BLOCK_MOVEMENT_NORTH(CollisionDataFlag.BLOCK_MOVEMENT_NORTH),
		BLOCK_MOVEMENT_NORTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST),
		BLOCK_MOVEMENT_EAST(CollisionDataFlag.BLOCK_MOVEMENT_EAST),
		BLOCK_MOVEMENT_SOUTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST),
		BLOCK_MOVEMENT_SOUTH(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH),
		BLOCK_MOVEMENT_SOUTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST),
		BLOCK_MOVEMENT_WEST(CollisionDataFlag.BLOCK_MOVEMENT_WEST),

		BLOCK_MOVEMENT_OBJECT(CollisionDataFlag.BLOCK_MOVEMENT_OBJECT),
		BLOCK_MOVEMENT_FLOOR_DECORATION(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION),
		BLOCK_MOVEMENT_FLOOR(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR),
		BLOCK_MOVEMENT_FULL(CollisionDataFlag.BLOCK_MOVEMENT_FULL);

		@Getter
		private final int flag;

		/**
		 * @param collisionData The tile collision flags.
		 * @return The set of {@link MovementFlag}s that have been set.
		 */
		public static Set<MovementFlag> getSetFlags(int collisionData)
		{
			return Arrays.stream(values())
				.filter(movementFlag -> (movementFlag.flag & collisionData) != 0)
				.collect(Collectors.toSet());
		}
	}

}
