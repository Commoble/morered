package commoble.morered.datagen;

import java.util.Map;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

public record DataGenContext(GatherDataEvent event, Map<ResourceLocation, BlockStateFile> blockStates, Map<ResourceLocation, SimpleModel> models, Map<ResourceLocation, WirePartModelDefinition> wirePartModels, Provider<LootTable> lootTables, Provider<FinishedRecipe> recipes, TagProvider<Block> blockTags, TagProvider<Item> itemTags, LanguageProvider lang)
{

}
