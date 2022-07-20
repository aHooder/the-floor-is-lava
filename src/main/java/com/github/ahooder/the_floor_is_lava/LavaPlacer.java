package com.github.ahooder.the_floor_is_lava;

import java.util.Collection;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

@Slf4j
public class LavaPlacer
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private LavaPlugin lavaPlugin;

	public void update() {
		clientThread.invoke(() -> {
			Tile[][][] tiles = client.getScene().getTiles();
			final Collection<WorldPoint> points = lavaPlugin.getPoints();
			for (final WorldPoint worldPoint : points) {
				LocalPoint point = LocalPoint.fromWorld(client, worldPoint);
				if (worldPoint.getPlane() < 0 || worldPoint.getPlane() >= Constants.MAX_Z ||
					point == null ||
					point.getSceneX() < 0 || point.getSceneX() >= Constants.SCENE_SIZE ||
					point.getSceneY() < 0 || point.getSceneY() >= Constants.SCENE_SIZE) {
					log.debug("Skipping out of bounds point: {}", point);
					continue;
				}

				Tile tile = tiles[worldPoint.getPlane()][point.getSceneX()][point.getSceneY()];
				if (tile == null)
					continue;
			}
		});
	}
}
