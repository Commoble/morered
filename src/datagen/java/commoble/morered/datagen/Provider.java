package commoble.morered.datagen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator.PathProvider;
import net.minecraft.data.DataGenerator.Target;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;

public record Provider<T>(String directory, Function<ResourceLocation,Path> pathFunction, Function<T, JsonElement> serializer, Map<ResourceLocation,T> map) implements DataProvider
{
	public static <T> Provider<T> create(GatherDataEvent event, Target target, String directory, Function<T, JsonElement> serializer)
	{
		PathProvider pathProvider = event.getGenerator().createPathProvider(target, directory);
		return new Provider<>(directory, pathProvider::json, serializer, new HashMap<>());
	}
	
	public Provider<T> put(ResourceLocation id, T val)
	{
		this.map.put(id, val);
		return this;
	}

	@Override
	public void run(CachedOutput cache) throws IOException
	{
        this.map.forEach(LamdbaExceptionUtils.rethrowBiConsumer((id, val) -> 
    		DataProvider.saveStable(cache, this.serializer.apply(val), this.pathFunction.apply(id))));
	}

	@Override
	public String getName()
	{
		return directory;
	}

}