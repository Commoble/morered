package commoble.morered.client;

import commoble.morered.util.ConfigHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig;

public record ClientConfig(ConfigValue<Boolean> showPlacementPreview, ConfigValue<Double> previewPlacementOpacity)
{
	public static ClientConfig INSTANCE;
	
	// TODO move to ClientProxy
	public static void initClientConfig()
	{
		INSTANCE = ConfigHelper.register(ModConfig.Type.CLIENT, ClientConfig::create);
	}
	
	public static ClientConfig create(ForgeConfigSpec.Builder builder)
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
