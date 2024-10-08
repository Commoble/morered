package net.commoble.morered.api.internal;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record ExperimentalCommonConfig(
	IntValue maxWireNetworkSize)
{

	
	public static ExperimentalCommonConfig create(ModConfigSpec.Builder builder)
	{
		IntValue maxWireNetworkSize = builder
			.comment("Maximum size (in nodes) of wire networks (including wires, cables, and wire posts),",
				"where a node is a color channel on a given face (e.g. [0,0,0] + north + orange).",
				"Most blocks will consume one node per face.",
				"Bundled cables use one node for each channel needed (up to 16);",
				"connecting a red alloy wire directly to a bundled cable will cause all 16 nodes to be consumed.")
			.translation("morered.config.max_wire_network_size")
			.defineInRange("max_wire_network_size", 1024, 1, Integer.MAX_VALUE);
		
		return new ExperimentalCommonConfig(maxWireNetworkSize);
	}
}
