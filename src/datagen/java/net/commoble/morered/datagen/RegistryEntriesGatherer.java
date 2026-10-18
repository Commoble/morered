package net.commoble.morered.datagen;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.commoble.exmachina.api.ExMachinaRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

public record RegistryEntriesGatherer(Supplier<DataGenContext> memoizedContext) implements Consumer<GatherDataRegistryEntriesEvent>
{
	@Override
	public void accept(GatherDataRegistryEntriesEvent event)
	{
		DataGenContext context = this.memoizedContext.get();
		
		event.add(ExMachinaRegistries.MECHANICAL_COMPONENT, bootstap ->
			context.mechanicalComponents()
				.forEach((id,component) -> bootstap.register(
					ResourceKey.create(ExMachinaRegistries.MECHANICAL_COMPONENT, id),
					component)));
		
		addRegistry(event, Registries.RECIPE, context.recipes());
		addRegistry(event, Registries.LOOT_TABLE, context.lootTables());
		addRegistry(event, Registries.ADVANCEMENT, context.advancements());
	}
	
	private static <T> void addRegistry(GatherDataRegistryEntriesEvent event, ResourceKey<Registry<T>> registryKey, Map<Identifier, Function<BootstrapContext<T>, T>> entries) {
		event.add(registryKey, bootstrap -> {
			entries.forEach((key,value) -> {
				bootstrap.register(
					ResourceKey.create(registryKey, key),
					value.apply(bootstrap));
			});
		});
	}
}
