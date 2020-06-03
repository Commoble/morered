package com.github.commoble.morered;

import com.github.commoble.morered.client.ClientEvents;
import com.github.commoble.morered.plate_blocks.LogicGateType;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	
	// the network channel we'll use for sending packets associated with this mod
	public static final String CHANNEL_PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MoreRed.MODID, "main"),
		() -> CHANNEL_PROTOCOL_VERSION,
		CHANNEL_PROTOCOL_VERSION::equals,
		CHANNEL_PROTOCOL_VERSION::equals);
	
	public MoreRed()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		CommonModEvents.addListeners(modBus);
		
		// add layer of separation to client stuff so we don't break servers
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ClientEvents.addClientListeners(modBus, forgeBus));
		
		// blocks and blockitems for the logic gates are registered here
		LogicGateType.registerLogicGateTypes(BlockRegistrar.BLOCKS, ItemRegistrar.ITEMS);
	}
	
	public static ResourceLocation getRL(String name)
	{
		return new ResourceLocation(MODID, name);
	}
}
