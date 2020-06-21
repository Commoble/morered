package com.github.commoble.morered;

import com.github.commoble.morered.util.ConfigHelper;
import com.github.commoble.morered.util.ConfigHelper.ConfigValueListener;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class ServerConfig
{
	public static ServerConfig INSTANCE;
	
	// called during mod object construction
	public static void initServerConfig()
	{
		INSTANCE = ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::new);
	}
	
	public ConfigValueListener<Double> max_wire_post_connection_range;
	
	public ServerConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		builder.push("General Settings");
		this.max_wire_post_connection_range = subscriber.subscribe(builder
			.comment("Maximum Plinth Connection Range")
			.translation("morered.config.max_wire_plinth_connection_range")
			.defineInRange("max_wire_plinth_connection_range", 32D, 0D, Double.MAX_VALUE));
	}
}

