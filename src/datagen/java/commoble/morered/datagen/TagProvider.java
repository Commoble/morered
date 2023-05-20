package commoble.morered.datagen;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Better TagsProvider that allows other things to add tags to it
 */
public class TagProvider<T> extends TagsProvider<T>
{	
	// mojang clears the map for some reason so we have to maintain our own
	protected final Map<ResourceLocation, TagBuilder> subclassBuilders = Maps.newLinkedHashMap();
	public static <T> TagProvider<T> create(GatherDataEvent event, Registry<T> registry)
	{
		return new TagProvider<>(event.getGenerator(), registry, ModLoadingContext.get().getActiveContainer().getModId(), event.getExistingFileHelper());
	}
	
	protected TagProvider(DataGenerator dataGenerator, Registry<T> registry, String modId, @Nullable ExistingFileHelper existingFileHelper)
	{
		super(dataGenerator, registry, modId, existingFileHelper);
	}

   public TagBuilder getOrCreateRawBuilder(TagKey<T> tagKey) {
	  super.getOrCreateRawBuilder(tagKey); //track file
      return this.subclassBuilders.computeIfAbsent(tagKey.location(), (key) -> TagBuilder.create());
   }
	
	public TagAppender<T> tag(TagKey<T> tagKey)
	{
		return super.tag(tagKey);
	}

	@Override
	protected void addTags()
	{
		this.builders.putAll(this.subclassBuilders);
	}

}
