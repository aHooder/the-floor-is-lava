package com.github.ahooder.the_floor_is_lava;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginTest {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(LavaPlugin.class);
		RuneLite.main(args);
	}
}
