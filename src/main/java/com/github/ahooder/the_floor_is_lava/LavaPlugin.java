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
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
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
	private static final int MARK_DELAY = 250;
	private static final int MULTI_MARK_DELAY = MARK_DELAY + 300;

	private static final String DOUSE_TILE = "Douse lava tile";
	private static final String PLACE_TILE = "Place lava tile";
	private static final String FORCE_DOUSE_TILE = "Force clear lava tile";
	private static final String CLEAR_ALL_TILES = "Clear all lava tiles";
	private static final String RESET_DOUSE_COUNTER = "Reset douse counter";
	private static final String IMPORT_OLD_CONFIG = "Import old tiles";
	private static final String WALK_HERE = "Walk here";
	private static final String REGION_PREFIX = "region_";

	private static final WidgetMenuOption clearAllOptionFixed = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption clearAllOptionResizable = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption clearAllOptionResizable2 = new WidgetMenuOption(
		CLEAR_ALL_TILES, "", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final WidgetMenuOption resetDouseCounterOptionFixed = new WidgetMenuOption(
		RESET_DOUSE_COUNTER, "", WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption resetDouseCounterOptionResizable = new WidgetMenuOption(
		RESET_DOUSE_COUNTER, "", WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption resetDouseCounterOptionResizable2 = new WidgetMenuOption(
		RESET_DOUSE_COUNTER, "", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final WidgetMenuOption importOldOptionFixed = new WidgetMenuOption(
		IMPORT_OLD_CONFIG, "", WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption importOldOptionResizable = new WidgetMenuOption(
		IMPORT_OLD_CONFIG, "", WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption importOldOptionResizable2 = new WidgetMenuOption(
		IMPORT_OLD_CONFIG, "", WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

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
		{
			MovementFlag.BLOCK_MOVEMENT_FLOOR,
			MovementFlag.BLOCK_MOVEMENT_FLOOR_DECORATION,
			MovementFlag.BLOCK_MOVEMENT_OBJECT,
			MovementFlag.BLOCK_MOVEMENT_FULL
		};

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

	private final HashSet<Integer> tutorialIslandRegionIds = new HashSet<>();

	private int totalTileCount;
	private int tilesDoused;
	private WorldPoint lastTile;
	private int lastPlane;
	private boolean inHouse = false;
	private long totalXp;

	public static class MarkedTile {
		public WorldPoint point;
		public long millis = System.currentTimeMillis();

		public MarkedTile(WorldPoint point, long delay) {
			this.point = point;
			millis += delay;
		}
	}

	public ArrayDeque<MarkedTile> recentlyMarkedTiles = new ArrayDeque<>();

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
			event.getMenuOption().equals(FORCE_DOUSE_TILE),
			0);
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
			else if (event.getKey().equals("perAccountSave"))
				loadPoints();
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
				updateTilesDoused();

				menuManager.addManagedCustomMenu(importOldOptionFixed, e -> importOldConfig());
				menuManager.addManagedCustomMenu(importOldOptionResizable, e -> importOldConfig());
				menuManager.addManagedCustomMenu(importOldOptionResizable2, e -> importOldConfig());

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
			removeCustomOptions();
			points.clear();
		});
	}

	private void updateCustomOptions() {
		if (config.showResetAllOption()) {
			menuManager.addManagedCustomMenu(clearAllOptionFixed, e -> clearAllLavaTiles());
			menuManager.addManagedCustomMenu(clearAllOptionResizable, e -> clearAllLavaTiles());
			menuManager.addManagedCustomMenu(clearAllOptionResizable2, e -> clearAllLavaTiles());
			menuManager.addManagedCustomMenu(resetDouseCounterOptionFixed, e -> resetDouseCounter());
			menuManager.addManagedCustomMenu(resetDouseCounterOptionResizable, e -> resetDouseCounter());
			menuManager.addManagedCustomMenu(resetDouseCounterOptionResizable2, e -> resetDouseCounter());
		} else {
			removeCustomOptions();
		}
	}

	private void removeCustomOptions() {
		menuManager.removeManagedCustomMenu(clearAllOptionFixed);
		menuManager.removeManagedCustomMenu(clearAllOptionResizable);
		menuManager.removeManagedCustomMenu(clearAllOptionResizable2);
		menuManager.removeManagedCustomMenu(resetDouseCounterOptionFixed);
		menuManager.removeManagedCustomMenu(resetDouseCounterOptionResizable);
		menuManager.removeManagedCustomMenu(resetDouseCounterOptionResizable2);
		menuManager.removeManagedCustomMenu(importOldOptionFixed);
		menuManager.removeManagedCustomMenu(importOldOptionResizable);
		menuManager.removeManagedCustomMenu(importOldOptionResizable2);
	}

	private void clearAllLavaTiles()
	{
		configManager.getConfigurationKeys(Config.GROUP)
			.stream()
			.filter(key ->
				key.startsWith(String.join(".", Config.GROUP, getConfigUUID(), REGION_PREFIX)))
			.forEach(key -> configManager.unsetConfiguration(Config.GROUP,
				key.substring(Config.GROUP.length() + 1)));
		loadPoints();
	}

	private int getPlaneIncludingBridge(WorldPoint wp) {
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
		// Handle the previous movement event now, delayed by one movement action
		WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
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
		return getConfiguration(Config.GROUP, getConfigUUID() + "." + REGION_PREFIX + regionId);
	}

	public Collection<LavaTile> getTiles(String regionId)
	{
		return getConfiguration(Config.GROUP,  getConfigUUID() + "." + REGION_PREFIX + regionId);
	}

	private String getConfigUUID() {
		if (config.perAccountSave())
			return Long.toHexString(client.getAccountHash());
		return "global";
	}

	private String getDouseOptionString() {
		return DOUSE_TILE + " " +
			ColorUtil.wrapWithColorTag(String.format("(%s remaining)",
				TileCounterOverlay.addCommasToNumber(getRemainingDousePoints())),
				Color.GRAY);
	}

	private void resetDouseCounter() {
		setTilesDoused(0);
		tilesDoused = 0;
	}

	private void importOldConfig() {
		String name = client.getLocalPlayer().getName();
		if (name == null) {
			postMessage("Failed to get the current player name.");
			return;
		}
		String oldHash = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
		String newHash = Long.toHexString(client.getAccountHash());
		copyConfig(oldHash, newHash);
	}

	private void updateTileCounter()
	{
		List<String> regions = configManager.getConfigurationKeys(
			Config.GROUP + "." + getConfigUUID() + ".region");
		int totalTiles = 0;
		for (String region : regions)
		{
			Collection<LavaTile> regionTiles = getTiles(removeRegionPrefix(region));
			totalTiles += regionTiles.size();
		}

		log.debug("Updating tile counter");
		totalTileCount = totalTiles;
	}

	private void updateTilesDoused()
	{
		Integer number = configManager.getConfiguration(Config.GROUP,
			getConfigUUID() + "." + Config.TILES_DOUSED, Integer.class);
		tilesDoused = number == null ? 0 : number;
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
		updateTilesDoused();
	}

	private void savePoints(int regionId, Collection<LavaTile> points)
	{
		if (points == null || points.isEmpty())
		{
			configManager.unsetConfiguration(Config.GROUP, getConfigUUID() + "." + REGION_PREFIX + regionId);
			return;
		}

		String json = GSON.toJson(points);
		configManager.setConfiguration(Config.GROUP, getConfigUUID() + "." + REGION_PREFIX + regionId, json);
	}

	private void copyConfig(String fromPrefix, String toPrefix) {
		configManager.getConfigurationKeys(Config.GROUP)
			.stream()
			.filter(key ->
				key.startsWith(Config.GROUP + "." + fromPrefix + "."))
			.forEach(fullKey -> {
				String key = fullKey.substring(Config.GROUP.length() + 1);
				int sepIndex = key.indexOf(".");
				if (sepIndex == -1)
					return;
				String newKey = toPrefix + key.substring(sepIndex);
				configManager.setConfiguration(Config.GROUP, newKey,
					configManager.getConfiguration(Config.GROUP, key));
			});
		loadPoints();
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

	public MarkedTile getMarkedTile(int plane, int tileX, int tileY) {
		if (!containsTile(plane, tileX, tileY))
			return null;

		MarkedTile marked = recentlyMarkedTiles
			.stream()
			.filter(m -> {
				LocalPoint lp = LocalPoint.fromWorld(client, m.point);
				return lp != null && lp.getSceneX() == tileX && lp.getSceneY() == tileY;
			})
			.findFirst()
			.orElseGet(() -> {
				WorldPoint wp = WorldPoint.fromLocal(client,
					tileX * Perspective.LOCAL_TILE_SIZE,
					tileY * Perspective.LOCAL_TILE_SIZE,
					plane);
				return new MarkedTile(wp, -1_000_000);
			});

		long elapsedMillis = System.currentTimeMillis() - marked.millis;
		if (elapsedMillis < 0)
			return null;
		return marked;
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
			fillTile(lastTile, MARK_DELAY);

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
			log.debug("diff: {},{}, mod: {},{}, distance: {}", xDiff, yDiff, xModifier, yModifier, distance);

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
					log.debug("Moved straight or diagonally");
					fillTile(new WorldPoint(
						lastTile.getX() + xModifier,
						lastTile.getY() + yModifier,
						lastTile.getPlane()),
						MULTI_MARK_DELAY);
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
		if (Math.abs(yDiff) == 1)
		{
			tileBesideXDiff = xDiff;
			tileBesideYDiff = 0;
		}
		else
		{
			tileBesideXDiff = 0;
			tileBesideYDiff = yDiff;
		}

		MovementFlag[] tileBesideFlagsArray = getTileMovementFlags(
			new WorldPoint(
				lastTile.getX() + tileBesideXDiff,
				lastTile.getY() + tileBesideYDiff,
				lastTile.getPlane()));

		if (tileBesideFlagsArray.length == 0)
		{
			fillTile(new WorldPoint(
				lastTile.getX() + tileBesideXDiff / 2,
				lastTile.getY() + tileBesideYDiff / 2,
				lastTile.getPlane()),
				MULTI_MARK_DELAY);
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
			fillTile(new WorldPoint(
				lastTile.getX() + xModifier,
				lastTile.getY() + yModifier,
				lastTile.getPlane()),
				MULTI_MARK_DELAY);
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
				fillTile(new WorldPoint(
					lastTile.getX() + xModifier,
					lastTile.getY() + yModifier,
					lastTile.getPlane()),
					MULTI_MARK_DELAY);
			}
			else
			{
				// Normal Pathing
				fillTile(new WorldPoint(
					lastTile.getX() + tileBesideXDiff / 2,
					lastTile.getY() + tileBesideYDiff / 2,
					lastTile.getPlane()),
					MULTI_MARK_DELAY);
			}
		}
	}

	private void handleCornerMovement(int xDiff, int yDiff)
	{
		WorldPoint northPoint;
		WorldPoint southPoint;
		if (yDiff > 0)
		{
			northPoint = new WorldPoint(lastTile.getX(), lastTile.getY() + yDiff, lastTile.getPlane());
			southPoint = new WorldPoint(lastTile.getX() + xDiff, lastTile.getY(), lastTile.getPlane());
		}
		else
		{
			northPoint = new WorldPoint(lastTile.getX() + xDiff, lastTile.getY(), lastTile.getPlane());
			southPoint = new WorldPoint(lastTile.getX(), lastTile.getY() + yDiff, lastTile.getPlane());
		}

		MovementFlag[] northTile = getTileMovementFlags(northPoint);
		MovementFlag[] southTile = getTileMovementFlags(southPoint);

		if (xDiff + yDiff == 0)
		{
			// Diagonal tilts north west
			if (containsAnyOf(fullBlock, northTile)
				|| containsAnyOf(northTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_WEST}))
			{
				fillTile(southPoint, MULTI_MARK_DELAY);
			}
			else if (containsAnyOf(fullBlock, southTile)
				|| containsAnyOf(southTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_EAST}))
			{
				fillTile(northPoint, MULTI_MARK_DELAY);
			}
		}
		else
		{
			// Diagonal tilts north east
			if (containsAnyOf(fullBlock, northTile)
				|| containsAnyOf(northTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_SOUTH, MovementFlag.BLOCK_MOVEMENT_EAST}))
			{
				fillTile(southPoint, MULTI_MARK_DELAY);
			}
			else if (containsAnyOf(fullBlock, southTile)
				|| containsAnyOf(southTile, new MovementFlag[]{MovementFlag.BLOCK_MOVEMENT_NORTH, MovementFlag.BLOCK_MOVEMENT_WEST}))
			{
				fillTile(northPoint, MULTI_MARK_DELAY);
			}
		}
	}

	private MovementFlag[] getTileMovementFlags(WorldPoint pointBeside)
	{
		CollisionData[] collisionData = client.getCollisionMaps();
		assert collisionData != null;
		int[][] collisionDataFlags = collisionData[client.getPlane()].getFlags();

		LocalPoint lp = LocalPoint.fromWorld(client, pointBeside);
		if (lp == null)
			return new MovementFlag[0];

		Set<MovementFlag> tilesBesideFlagsSet = MovementFlag.getSetFlags(collisionDataFlags[lp.getSceneX()][lp.getSceneY()]);
		MovementFlag[] tileBesideFlagsArray = new MovementFlag[tilesBesideFlagsSet.size()];
		tilesBesideFlagsSet.toArray(tileBesideFlagsArray);

		return tileBesideFlagsArray;
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

	private void fillTile(WorldPoint point, long delay)
	{
		if (lastPlane != getPlaneIncludingBridge(point))
		{
			return;
		}

		updateTileMark(point, true, false, delay);
	}

	public int getTilesDoused() {
		return tilesDoused;
	}

	public void setTilesDoused(int tilesDoused) {
		configManager.setConfiguration(Config.GROUP, getConfigUUID() + "." + Config.TILES_DOUSED, tilesDoused);
	}

	public int getRemainingDousePoints() {
		return client.getTotalLevel() - getTilesDoused() - 32;
	}

	private void updateTileMark(@NonNull WorldPoint worldPoint, boolean markedValue, boolean force, long delay)
	{
		int plane = getPlaneIncludingBridge(worldPoint);
		int regionId = worldPoint.getRegionID();
		LavaTile point = new LavaTile(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), plane);
		log.debug("Updating point: {} - {}", point, worldPoint);

		List<LavaTile> lavaTiles = new ArrayList<>(getTiles(regionId));

		if (markedValue) {
			if (!lavaTiles.contains(point)) {
				lavaTiles.add(point);
				while (recentlyMarkedTiles.size() > 25)
					recentlyMarkedTiles.removeFirst();
				recentlyMarkedTiles.addLast(new MarkedTile(worldPoint, delay));
			}
		} else {
			if (!force) {
				// Check if the player has any unspent douse points
				if (getRemainingDousePoints() > 0) {
					setTilesDoused(getTilesDoused() + 1);
					tilesDoused++;
				} else {
					postMessage("You have no remaining douse points left.");
					return;
				}
			}
			lavaTiles.remove(point);
		}

		savePoints(regionId, lavaTiles);
		loadPoints();
	}

	private void postMessage(String message) {
		client.addChatMessage(ChatMessageType.GAMEMESSAGE,
			"[The Floor is Lava]", message, "", false);
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
