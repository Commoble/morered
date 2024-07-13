package commoble.morered.datagen;

import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.LanguageProvider;

public record BlockDataHelper(Block block)
{
	public static BlockDataHelper create(Block block, Map<ResourceLocation, BlockStateFile> blockstates, BlockStateFile blockstate)
	{
		BlockDataHelper helper = new BlockDataHelper(block);
		blockstates.put(helper.id(), blockstate);
		return helper;
	}
	
	public static BlockDataHelper create(Block block,
		Map<ResourceLocation, BlockStateFile> blockstates, BlockStateFile blockstate,
		Map<ResourceLocation, LootTable> lootTables, LootTable lootTable)
	{
		BlockDataHelper helper = new BlockDataHelper(block);
		blockstates.put(helper.id(), blockstate);
		lootTables.put(ResourceLocation.fromNamespaceAndPath(helper.id().getNamespace(), String.format("blocks/%s", helper.id().getPath())), lootTable);
		return helper;
	}
	
	public BlockDataHelper localize(LanguageProvider langProvider, String localizedName)
	{
		langProvider.add(this.block, localizedName);
		return this;
	}
	
	@SafeVarargs
	public final BlockDataHelper tags(TagProvider<Block> tagProvider, TagKey<Block>... tags)
	{
		for (TagKey<Block> tag : tags)
		{
			tagProvider.tag(tag).add(BuiltInRegistries.BLOCK.getResourceKey(block).get());
		}
		return this;
	}
	
	/**
	 * Adds a block model with the same name as this block (in the block models folder, e.g. modid:block/blockname)
	 * @param models models
	 * @param model model
	 * @return this
	 */
	public BlockDataHelper baseModel(Map<ResourceLocation, SimpleModel> models, SimpleModel model)
	{
		models.put(blockModel(this.block), model);
		return this;
	}
	
	/**
	 * Adds a model to a model map using a generic string formatted with this block's id
	 * @param models model map
	 * @param formatString e.g. "%s_model" where "%s" will be replaced with the block id's location
	 * @param model SimpleModel to add
	 * @return this
	 */
	public BlockDataHelper model(Map<ResourceLocation, SimpleModel> models, String formatString, SimpleModel model)
	{
		ResourceLocation id = this.id();
		models.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), String.format(formatString, id.getPath())), model);
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
	
	public ItemDataHelper blockItem()
	{
		return ItemDataHelper.create(this.block.asItem());
	}
	
	public ItemDataHelper simpleBlockItem(Map<ResourceLocation, SimpleModel> modelMap)
	{
		return blockItem(modelMap, SimpleModel.createWithoutRenderType(blockModel(this.id())));
	}
	
	public ItemDataHelper blockItem(Map<ResourceLocation, SimpleModel> modelMap, SimpleModel model)
	{
		return ItemDataHelper.create(this.block.asItem(), modelMap, model);
	}
}
