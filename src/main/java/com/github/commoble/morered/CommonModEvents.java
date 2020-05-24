package com.github.commoble.morered;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class CommonModEvents
{
	public static void addListeners(IEventBus modBus)
	{
		subscribedDeferredRegisters(modBus,
			BlockRegistrar.BLOCKS,
			ItemRegistrar.ITEMS);
	}
	
	public static void subscribedDeferredRegisters(IEventBus modBus, DeferredRegister<?>... registers)
	{
		for(DeferredRegister<?> register : registers)
		{
			register.register(modBus);
		}
	}
}
