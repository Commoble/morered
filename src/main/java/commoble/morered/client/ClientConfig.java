package commoble.morered.client;

import commoble.databuddy.config.ConfigHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

//public class ClientConfig
//{
//	public static ClientConfig INSTANCE;
//
//	// called during mod object construction on client side
//	public static void initClientConfig(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext) {
//		INSTANCE = ConfigHelper.register(modContext, fmlContext, ModConfig.Type.CLIENT, ClientConfig::new);
//	}
//
//
//	public ForgeConfigSpec.ConfigValue<Boolean> showPlacementPreview;
//	public ForgeConfigSpec.ConfigValue<Double> previewPlacementOpacity;
//
//	public ClientConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
//	{
//		builder.push("Rendering");
////		this.showPlacementPreview = subscriber.subscribe(builder
////			.comment("Render preview of plate blocks before placing them")
////			.translation("morered.showPlacementPreview")
////			.define("showPlacementPreview", true));
////		this.previewPlacementOpacity = subscriber.subscribe(builder
////			.comment("Opacity of the render preview. Higher value = less transparent, lower = more transparent")
////			.translation("morered.showPlacementPreview")
////			.defineInRange("previewPlacementOpacity", 0.4D, 0D, 1D));
//		builder.pop();
//	}
//}
