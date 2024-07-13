/*
The MIT License (MIT)
Copyright (c) 2021 Joseph Bettendorff aka "Commoble"
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package commoble.morered.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Generic data provider that uses DataFixerUpper Codecs to generate jsons from
 * java objects.
 * 
 * @param packOutput
 * 			PackOutput from DataGenerator#addProvider
 * @param generator
 *			The generator instance from the GatherDataEvent
 * @param target
 *			Whether to generate data in the assets or data folder
 * @param folder
 *			The root folder of this data type in a given data domain e.g. to
 *			generate data in resources/data/modid/foods/cheeses/, use
 *			DATA for the resource type, and "foods/cheeses" for
 *			the folder name.
 * @param codec
 *			The codec that will be used to convert objects to jsons
 * @param objects
 *			An ID-to-object map that defines the objects to generate jsons
 *			from and where the jsons will be generated.
 */
public record JsonDataProvider<T>(PackOutput packOutput, DataGenerator generator, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects, String uniqueName) implements DataProvider
{
	private static final Logger LOGGER = LogUtils.getLogger();
	
	public static <T> JsonDataProvider<T> create(PackOutput packOutput, DataGenerator generator, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects)
	{
		return named(packOutput, generator, target, folder, codec, objects, folder);
	}
	
	public static <T> JsonDataProvider<T> named(PackOutput packOutput, DataGenerator generator, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects, String uniqueName)
	{
		return new JsonDataProvider<>(packOutput, generator, target, folder, codec, objects, uniqueName);
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput cache)
	{
		PathProvider pathProvider = packOutput.createPathProvider(target, folder);
		List<CompletableFuture<?>> results = new ArrayList<>();
		for (var entry : this.objects.entrySet())
		{
			var id = entry.getKey();
			this.codec.encodeStart(JsonOps.INSTANCE, entry.getValue())
				.resultOrPartial(s -> LOGGER.error("{} failed to encode {}: {}", this.getName(), id, s))
				.ifPresent(json -> {
					results.add(DataProvider.saveStable(cache, json, pathProvider.json(id)));
				});
		}
		return CompletableFuture.allOf(results.toArray(CompletableFuture[]::new));
	}

	/**
	 * Gets the name of this data provider. Used by the data generator to log its root data providers.
	 */
	@Override
	public String getName()
	{
		return this.uniqueName();
	}

}