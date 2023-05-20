package commoble.morered;

import commoble.morered.util.ConfigHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public record ServerConfig(ConfigValue<Double> maxWirePostConnectionRange)
{
	// TODO move server config to main class
	public static ServerConfig INSTANCE;
	
	// called during mod object construction
	public static void initServerConfig(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext)
	{
		INSTANCE = ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::create);
	}
	
	public static ServerConfig create(ForgeConfigSpec.Builder builder)
	{
		builder.push("General Settings");
		ConfigValue<Double> maxWirePostConnectionRange = builder
			.comment("Maximum Plinth Connection Range")
			.translation("morered.config.max_wire_plinth_connection_range")
			.defineInRange("max_wire_plinth_connection_range", 32D, 0D, Double.MAX_VALUE);
		
		return new ServerConfig(maxWirePostConnectionRange);
	}
}

