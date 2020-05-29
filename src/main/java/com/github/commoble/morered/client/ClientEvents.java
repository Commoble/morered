package com.github.commoble.morered.client;

import com.github.commoble.morered.LogicGateType;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents
{
	public static void addClientListeners(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientEvents::onClientSetup);
		modBus.addListener(ClientEvents::onRegisterBlockColors);
		modBus.addListener(ClientEvents::onRegisterItemColors);
		
	}
	
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		LogicGateType.TYPES.values().forEach(ClientEvents::setLogicGateRenderLayer);
	}
	
	public static void setLogicGateRenderLayer(LogicGateType type)
	{
		RenderTypeLookup.setRenderLayer(type.blockGetter.get(), RenderType.getTranslucent());
	}
	
	public static void onRegisterBlockColors(ColorHandlerEvent.Block event)
	{
		BlockColors colors = event.getBlockColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockTint, type.blockGetter.get()));
	}
	
	public static void onRegisterItemColors(ColorHandlerEvent.Item event)
	{
		ItemColors colors = event.getItemColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockItemTint, type.itemGetter.get()));
	}
	
	
}
