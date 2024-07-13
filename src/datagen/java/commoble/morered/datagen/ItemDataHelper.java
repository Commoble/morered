package commoble.morered.datagen;

import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.data.LanguageProvider;

public record ItemDataHelper(Item item)
{
	public static ItemDataHelper create(Item item)
	{
		return new ItemDataHelper(item);
	}
	public static ItemDataHelper create(Item item, Map<ResourceLocation,SimpleModel> modelMap, SimpleModel model)
	{
		ItemDataHelper provider = new ItemDataHelper(item);
		modelMap.put(itemModel(provider.id()), model);
		return provider;
	}
	
	public ItemDataHelper recipe(Map<ResourceLocation,Recipe<?>> recipes, Recipe<?> recipe)
	{
		return this.recipe(recipes, this.id(), recipe);
	}
	
	public ItemDataHelper recipe(Map<ResourceLocation,Recipe<?>> recipes, ResourceLocation recipeId, Recipe<?> recipe)
	{
		recipes.put(recipeId, recipe);
		return this;
	}
	
	public ItemDataHelper localize(LanguageProvider langProvider, String localizedName)
	{
		langProvider.addItem(this::item, localizedName);
		return this;
	}
	
	@SafeVarargs
	public final ItemDataHelper tags(TagProvider<Item> tagProvider, TagKey<Item>... tags)
	{
		for (TagKey<Item> tagKey : tags)
		{
			tagProvider.tag(tagKey).add(BuiltInRegistries.ITEM.getResourceKey(item).get());
		}
		return this;
	}
	
	public ResourceLocation id()
	{
		return BuiltInRegistries.ITEM.getKey(this.item);
	}
	
	public static ResourceLocation itemModel(ResourceLocation id)
	{
		return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath());
	}
	
	public ItemDataHelper help(Consumer<ItemDataHelper> helperer)
	{
		helperer.accept(this);
		return this;
	}
}