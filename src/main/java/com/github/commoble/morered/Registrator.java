package com.github.commoble.morered;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class Registrator<T extends IForgeRegistryEntry<T>>
{
	private final IForgeRegistry<T> registry;
	private final String modid;
	

	
	public Registrator(IForgeRegistry<T> registry, String modid)
	{
		this.registry = registry;
		this.modid = modid;
	}
	
	public void register(String name, T thing)
	{
		thing.setRegistryName(new ResourceLocation(this.modid, name));
		this.registry.register(thing);
	}
}
