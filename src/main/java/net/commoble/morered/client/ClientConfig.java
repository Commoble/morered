package net.commoble.morered.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record ClientConfig(
	ConfigValue<Boolean> showPlacementPreview,
	ConfigValue<Double> previewPlacementOpacity)
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
		builder.pop();
		
		return new ClientConfig(showPlacementPreview, previewPlacementOpacity);
	}
}
