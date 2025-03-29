package net.commoble.morered.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record ClientConfig(ConfigValue<Boolean> showPlacementPreview, ConfigValue<Double> previewPlacementOpacity, ConfigValue<Integer> machineRenderCycleTicks)
{	
	public static ClientConfig create(ModConfigSpec.Builder builder)
	{
		builder.push("Rendering");
		ConfigValue<Boolean> showPlacementPreview = builder
			.comment("Render preview of plate blocks before placing them")
			.translation("morered.showPlacementPreview")
			.define("showPlacementPreview", true);
		ConfigValue<Double> previewPlacementOpacity = builder
			.comment("Opacity of the render preview. Higher value = less transparent, lower = more transparent")
			.translation("morered.showPlacementPreview")
			.defineInRange("previewPlacementOpacity", 0.4D, 0D, 1D);
		ConfigValue<Integer> machineRenderCycleTicks = builder
			.comment("Some machine animations modulate the game ticks by this number (i.e. effectiveTicks = gameTime % n) to avoid floating-point rounding errors at high game times",
				"This value should be a value such that (value+1) / (40pi) is almost, but not quite, a whole number",
				"The default value of 6030 causes a render cycle every five minutes with <0.1% discontinuity after the final tick",
				"Higher values increase the time between discontinuities but also increase potential for rounding errors")
			.translation("morered.machineRenderCycleTicks")
			.defineInRange("machineRenderCycleTicks", 6030, 1, Integer.MAX_VALUE);
		builder.pop();
		
		return new ClientConfig(showPlacementPreview, previewPlacementOpacity, machineRenderCycleTicks);
	}
}
