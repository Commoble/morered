package net.commoble.morered;

import net.commoble.morered.util.ConfigHelper;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record ServerConfig(ConfigValue<Double> maxWirePostConnectionRange)
{
	// TODO move server config to main class
	public static ServerConfig INSTANCE;
	
	// called during mod object construction
	public static void initServerConfig()
	{
		INSTANCE = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.SERVER, ServerConfig::create);
	}
	
	public static ServerConfig create(ModConfigSpec.Builder builder)
	{
		builder.push("General Settings");
		ConfigValue<Double> maxWirePostConnectionRange = builder
			.comment("Range (in blocks) that wire posts can remotely connect to each other.")
			.translation("morered.config.max_wire_plinth_connection_range")
			.defineInRange("max_wire_plinth_connection_range", 32D, 0D, Double.MAX_VALUE);
		
		return new ServerConfig(maxWirePostConnectionRange);
	}
}

