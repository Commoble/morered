package com.github.commoble.morered;

import com.github.commoble.morered.client.ClientEvents;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	
	public MoreRed()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		CommonModEvents.addListeners(modBus);
		CommonForgeEvents.addListeners(forgeBus);
		
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
