package com.github.commoble.morered;

import java.util.function.Supplier;

import com.github.commoble.morered.client.ClientEvents;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistryEntry;

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
	
	public static final <T extends IForgeRegistryEntry<T>, SubType extends T> RegistryObject<SubType> register(DeferredRegister<T> register, String name, Supplier<SubType> thingMaker)
	{
		return register.register(name, thingMaker);
	}
}
