package commoble.morered.datagen;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.commoble.exmachina.api.MechanicalComponent;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

public record BlockDataHelper(Block block, DataGenContext context)
{
	public static BlockDataHelper create(Block block, DataGenContext context,
		BiFunction<ResourceLocation, Block, BlockStateFile> blockstate,
		BiFunction<ResourceLocation, Block, LootTable> lootTable)
	{
		BlockDataHelper helper = new BlockDataHelper(block, context);
		ResourceLocation id = helper.id();
		context.blockStates().put(id, blockstate.apply(id,block));
		context.lootTables().put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "blocks/" + id.getPath()), lootTable.apply(id,block));
		return helper;
	}
	
	public static BlockDataHelper createWithoutLoot(Block block, DataGenContext context,
		BiFunction<ResourceLocation, Block, BlockStateFile> blockstate)
	{
		BlockDataHelper helper = new BlockDataHelper(block, context);
		ResourceLocation id = helper.id();
		context.blockStates().put(id, blockstate.apply(id,block));
		return helper;
	}
	
	public static BlockDataHelper create(Block block, DataGenContext context, BlockStateFile blockstate)
	{
		BlockDataHelper helper = new BlockDataHelper(block, context);
		context.blockStates().put(helper.id(), blockstate);
		return helper;
	}
	
	public static BlockDataHelper create(Block block, DataGenContext context, BlockStateFile blockstate, LootTable lootTable)
	{
		BlockDataHelper helper = new BlockDataHelper(block, context);
		context.blockStates().put(helper.id(), blockstate);
		context.lootTables().put(ResourceLocation.fromNamespaceAndPath(helper.id().getNamespace(), String.format("blocks/%s", helper.id().getPath())), lootTable);
		return helper;
	}
	
	public static BlockDataHelper create(Block block, DataGenContext context, Function<Block, BlockStateFile> blockstate, Function<Block, LootTable> lootTable)
	{
		return create(block, context, blockstate.apply(block), lootTable.apply(block));
	}
	
	public BlockDataHelper localize(String localizedName)
	{
		context.lang().add(this.block, localizedName);
		return this;
	}
	
	@SafeVarargs
	public final BlockDataHelper tags(TagKey<Block>... tags)
	{
		for (TagKey<Block> tag : tags)
		{
			context.blockTags().tag(tag).add(BuiltInRegistries.BLOCK.getResourceKey(block).get());
		}
		return this;
	}
	
	/**
	 * Adds a block model with the same name as this block (in the block models folder, e.g. modid:block/blockname)
	 * @param models models
	 * @param model model
	 * @return this
	 */
	public BlockDataHelper baseModel(SimpleModel model)
	{
		context.models().put(blockModel(this.block), model);
		return this;
	}
	
	/**
	 * Adds a model to a model map using a generic string formatted with this block's id
	 * @param models model map
	 * @param formatString e.g. "%s_model" where "%s" will be replaced with the block id's location
	 * @param model SimpleModel to add
	 * @return this
	 */
	public BlockDataHelper model(String formatString, SimpleModel model)
	{
		ResourceLocation id = this.id();
		context.models().put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), String.format(formatString, id.getPath())), model);
		return this;
	}
	
	public BlockDataHelper mechanicalComponent(MechanicalComponent component)
	{
		this.context.mechanicalComponents().put(this.id(), component);
		return this;
	}
	
	public ResourceLocation id()
	{
		return BuiltInRegistries.BLOCK.getKey(this.block);
	}
	
	public static ResourceLocation blockModel(Block block)
	{
		return blockModel(BuiltInRegistries.BLOCK.getKey(block));
	}
	
	public static ResourceLocation blockModel(ResourceLocation location)
	{
		return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "block/" + location.getPath());
	}
	
	public ItemDataHelper blockItemWithoutItemModel()
	{
		return blockItemWithoutItemModel(id -> new ClientItem(new BlockModelWrapper.Unbaked(ItemDataHelper.itemModel(id()), List.of()), ClientItem.Properties.DEFAULT));
	}
	
	public ItemDataHelper blockItemWithoutItemModel(Function<ResourceLocation, ClientItem> modelFactory)
	{
		return ItemDataHelper.create(this.block.asItem(), context, modelFactory.apply(ItemDataHelper.itemModel(this.id())));
	}
	
	public ItemDataHelper simpleBlockItem()
	{
		return blockItem(SimpleModel.createWithoutRenderType(blockModel(this.id())));
	}
	
	public ItemDataHelper simpleBlockItem(Function<ResourceLocation, ClientItem> modelFactory)
	{
		return blockItem(modelFactory, SimpleModel.createWithoutRenderType(blockModel(this.id())));
	}
	
	public ItemDataHelper blockItem(SimpleModel model)
	{
		return blockItem(modelId -> new ClientItem(new BlockModelWrapper.Unbaked(modelId, List.of()), ClientItem.Properties.DEFAULT), model);
	}
	
	public ItemDataHelper blockItem(Function<ResourceLocation, ClientItem> modelFactory, SimpleModel model)
	{
		return ItemDataHelper.create(this.block.asItem(), context, modelFactory.apply(ItemDataHelper.itemModel(id())), model);
	}
}
