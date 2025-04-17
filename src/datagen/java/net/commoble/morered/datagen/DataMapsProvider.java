package net.commoble.morered.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;

public class DataMapsProvider extends DataMapProvider
{

	protected DataMapsProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider)
	{
		super(packOutput, lookupProvider);
	}

	@Override
	protected void gather(Provider provider)
	{
		// no
	}

}
