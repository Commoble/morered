package commoble.morered.datagen;

import java.util.Map;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.LanguageProvider;

public record DataGenContext(Map<ResourceLocation, BlockStateFile> blockStates,
	Map<ResourceLocation, ClientItem> clientItems,
	Map<ResourceLocation, SimpleModel> models,
	Map<ResourceLocation, WirePartModelDefinition> wirePartModels,
	Map<ResourceLocation, Recipe<?>> recipes,
	Map<ResourceLocation, LootTable> lootTables,
	TagProvider<Block> blockTags,
	TagProvider<Item> itemTags,
	LanguageProvider lang)
{
}
