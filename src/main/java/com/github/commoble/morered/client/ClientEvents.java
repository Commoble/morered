package com.github.commoble.morered.client;

import com.github.commoble.morered.BlockRegistrar;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents
{
	public static void addClientListeners(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientEvents::onClientSetup);
		modBus.addListener(ClientEvents::onRegisterBlockColors);
		
	}
	
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		RenderTypeLookup.setRenderLayer(BlockRegistrar.NOT_GATE.get(), RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(BlockRegistrar.NOR_GATE.get(), RenderType.getTranslucent());
	}
	
	public static void onRegisterBlockColors(ColorHandlerEvent.Block event)
	{
		event.getBlockColors().register(BlockColorHandlers::getLogicGateTint,
			BlockRegistrar.NOT_GATE.get(),
			BlockRegistrar.NOR_GATE.get());
	}
}
