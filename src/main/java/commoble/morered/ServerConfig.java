package commoble.morered;

//import com.mojang.serialization.Codec;
//import commoble.databuddy.config.ConfigHelper;
//import net.minecraftforge.common.ForgeConfigSpec;
//import net.minecraftforge.fml.ModLoadingContext;
//import net.minecraftforge.fml.config.ModConfig;
//import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//
//public class ServerConfig
//{
//	public static ServerConfig INSTANCE;
//
//	// called during mod object construction
//	public static void initServerConfig(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext)
//	{
////		INSTANCE = ConfigHelper.register(modContext, fmlContext, ModConfig.Type.SERVER, ServerConfig::new);
//		INSTANCE = ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::new);
//	}
//
//	public ForgeConfigSpec.ConfigValue<Double> max_wire_post_connection_range;
//
//	public ServerConfig(ForgeConfigSpec.Builder builder, ConfigHelper helper)
//	{
//		builder.push("General Settings");
////		this.max_wire_post_connection_range = builder
////				.comment("Maximum Plinth Connection Range")
////				.translation("morered.config.max_wire_plinth_connection_range")
////				.defineInRange("max_wire_plinth_connection_range", 32D, 0D, Double.MAX_VALUE);
//		builder.pop();
//	}
//}

