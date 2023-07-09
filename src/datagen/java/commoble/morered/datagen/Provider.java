package commoble.morered.datagen;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.gson.JsonElement;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;

public record Provider<T>(String directory, Function<ResourceLocation,Path> pathFunction, Function<T, JsonElement> serializer, Map<ResourceLocation,T> map) implements DataProvider
{
	public static <T> Provider<T> create(GatherDataEvent event, Target target, String directory, Function<T, JsonElement> serializer)
	{
		return new Provider<>(directory, event.getGenerator().getPackOutput().createPathProvider(target, directory)::json, serializer, new HashMap<>());
	}
	
	public Provider<T> put(ResourceLocation id, T val)
	{
		this.map.put(id, val);
		return this;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache)
	{        
        return CompletableFuture.allOf(this.map.entrySet()
        	.stream()
        	.map(entry -> DataProvider.saveStable(cache, this.serializer.apply(entry.getValue()), this.pathFunction.apply(entry.getKey())))
        	.toList()
        	.toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName()
	{
		return directory;
	}

}