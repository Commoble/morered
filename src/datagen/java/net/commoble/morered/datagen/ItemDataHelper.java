package net.commoble.morered.datagen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

public record ItemDataHelper(Item item, DataGenContext context)
{	
	public static ItemDataHelper create(Item item, DataGenContext context, ClientItem clientItem)
	{
		ItemDataHelper helper = new ItemDataHelper(item, context);
		context.clientItems().put(helper.id(), clientItem);
		return new ItemDataHelper(item, context);
	}
	public static ItemDataHelper create(Item item, DataGenContext context, ClientItem clientItem, SimpleModel model)
	{
		ItemDataHelper helper = create(item, context, clientItem);
		context.models().put(itemModel(helper.id()), model);
		return helper;
	}
	
	public static ItemDataHelper create(Item item, DataGenContext context, SimpleModel model)
	{
		ItemDataHelper helper = new ItemDataHelper(item, context);
		var itemModelId = itemModel(helper.id());
		context.clientItems().put(helper.id(), new ClientItem(new BlockModelWrapper.Unbaked(itemModelId, List.of()), ClientItem.Properties.DEFAULT));
		context.models().put(itemModelId, model);
		return helper;		
	}
	
	public ItemDataHelper recipe(Recipe<?> recipe)
	{
		return this.recipe(this.id(), recipe);
	}
	
	public ItemDataHelper recipe(Identifier recipeId, Recipe<?> recipe)
	{
		this.context.recipes().put(recipeId, recipe);
		return this;
	}
	
	public ItemDataHelper localize(String localizedName)
	{
		this.context.lang().addItem(this::item, localizedName);
		return this;
	}
	
	@SafeVarargs
	public final ItemDataHelper tags(TagKey<Item>... tags)
	{
		for (TagKey<Item> tagKey : tags)
		{
			context.itemTags().tag(tagKey).add(BuiltInRegistries.ITEM.getResourceKey(item).get());
		}
		return this;
	}
	
	public Identifier id()
	{
		return BuiltInRegistries.ITEM.getKey(this.item);
	}
	
	public static Identifier itemModel(Identifier id)
	{
		return Identifier.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath());
	}
	
	public ItemDataHelper help(Consumer<ItemDataHelper> helperer)
	{
		helperer.accept(this);
		return this;
	}
}