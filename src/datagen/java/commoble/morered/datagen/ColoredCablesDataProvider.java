package commoble.morered.datagen;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import commoble.morered.BlockRegistrar;
import commoble.morered.ItemRegistrar;
import commoble.morered.datagen.BlockStateDefinitions.MultipartBlockStateDefinition;
import commoble.morered.datagen.BlockStateDefinitions.MultipartBlockStateDefinition.CaseDefinition;
import commoble.morered.datagen.JsonDataProvider.ResourceType;
import commoble.morered.wires.WireCountLootFunction;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.LootTableProvider;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.ConstantRange;
import net.minecraft.loot.ItemLootEntry;
import net.minecraft.loot.LootParameterSet;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.ValidationTracker;
import net.minecraft.loot.conditions.SurvivesExplosion;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ColoredCablesDataProvider implements IDataProvider
{
	public static final INamedTag<Item> RED_ALLOY_WIRE_TAG = ItemTags.bind("morered:red_alloy_wires");
	
	protected final DataGenerator generator;
	protected final ExistingFileHelper fileHelper;
	
	public ColoredCablesDataProvider(DataGenerator generator, ExistingFileHelper fileHelper)
	{
		this.generator = generator;
		this.fileHelper = fileHelper;
	}

	@Override
	public void run(DirectoryCache cache) throws IOException
	{
		for (int i=0; i<16; i++)
		{
			new ColoredCableDataProvider(i).act(cache);
		}
	}

	@Override
	public String getName()
	{
		return "More Red Colored Network Cables";
	}
	
	class ColoredCableDataProvider
	{
		final int colorIndex;
		
		ColoredCableDataProvider(int colorIndex)
		{
			this.colorIndex = colorIndex;
		}
		
		void act(DirectoryCache cache) throws IOException
		{
			final Block block = BlockRegistrar.NETWORK_CABLES[this.colorIndex].get();
			final ResourceLocation blockid = block.getRegistryName();
			final String modid = blockid.getNamespace();
			final String blockLocation = blockid.getPath();
			final BlockItem item = ItemRegistrar.NETWORK_CABLES[this.colorIndex].get();
			final String blockTexture = String.format("morered:block/%s", block.getRegistryName().getPath());
			
			DataGenerator generator = ColoredCablesDataProvider.this.generator;
			// generate blockstate json
			ResourceLocation partsModel = new ResourceLocation(modid, String.format("block/%s_parts", blockLocation));
			ResourceLocation nodeModel = new ResourceLocation(modid, String.format("block/%s_node", blockLocation));
			ResourceLocation elbowModel = new ResourceLocation(modid, String.format("block/%s_elbow", blockLocation));
			new JsonDataProvider<MultipartBlockStateDefinition>(generator, ResourceType.ASSETS, "blockstates", MultipartBlockStateDefinition.CODEC)
				.with(blockid, BlockStateDefinitions.multipartBuilder()
					.withCase(CaseDefinition.builder(partsModel,0,0))
					.withCase(CaseDefinition.builder(nodeModel,0,0)
						.withCondition("down", "true"))
					.withCase(CaseDefinition.builder(nodeModel,180,0)
						.withCondition("up", "true"))
					.withCase(CaseDefinition.builder(nodeModel,90,0)
						.withCondition("south", "true"))
					.withCase(CaseDefinition.builder(nodeModel,90,90)
						.withCondition("west", "true"))
					.withCase(CaseDefinition.builder(nodeModel,90,180)
						.withCondition("north", "true"))
					.withCase(CaseDefinition.builder(nodeModel,90,270)
						.withCondition("east", "true"))
					.withCase(CaseDefinition.builder(elbowModel,0,0)
						.withCondition("down", "true")
						.withCondition("west", "true"))
					.withCase(CaseDefinition.builder(elbowModel,0,90)
						.withCondition("down", "true")
						.withCondition("north", "true"))
					.withCase(CaseDefinition.builder(elbowModel,0,180)
						.withCondition("down", "true")
						.withCondition("east", "true"))
					.withCase(CaseDefinition.builder(elbowModel,0,270)
						.withCondition("down", "true")
						.withCondition("south", "true"))
					.withCase(CaseDefinition.builder(elbowModel,90,0)
						.withCondition("south", "true")
						.withCondition("west", "true"))
					.withCase(CaseDefinition.builder(elbowModel,90,90)
						.withCondition("west", "true")
						.withCondition("north", "true"))
					.withCase(CaseDefinition.builder(elbowModel,90,180)
						.withCondition("north", "true")
						.withCondition("east", "true"))
					.withCase(CaseDefinition.builder(elbowModel,90,270)
						.withCondition("east", "true")
						.withCondition("south", "true"))
					.withCase(CaseDefinition.builder(elbowModel,180,0)
						.withCondition("up", "true")
						.withCondition("west", "true"))
					.withCase(CaseDefinition.builder(elbowModel,180,90)
						.withCondition("up", "true")
						.withCondition("north", "true"))
					.withCase(CaseDefinition.builder(elbowModel,180,180)
						.withCondition("up", "true")
						.withCondition("east", "true"))
					.withCase(CaseDefinition.builder(elbowModel,180,270)
						.withCondition("up", "true")
						.withCondition("south", "true")))
				.run(cache);
			
			// generate block models
			// generate the simple models for the block
			List<String> blockModelTypes = ImmutableList.of("edge","elbow","line","node");
			Util.make(new JsonDataProvider<SimpleModel>(generator, ResourceType.ASSETS, "models/block", SimpleModel.CODEC), provider->
				blockModelTypes.forEach(modelType->
					provider.with(new ResourceLocation(modid, String.format("%s_%s", blockLocation, modelType)),
						SimpleModel.builder(String.format("morered:block/colored_network_cable_%s_template", modelType))
							.withTexture("wire",blockTexture))))
			.run(cache);
			// generate the parts model for the block
			new JsonDataProvider<WirePartModelDefinition>(generator, ResourceType.ASSETS, "models/block", WirePartModelDefinition.CODEC)
				.with(new ResourceLocation(modid, String.format("%s_parts", blockLocation)), new WirePartModelDefinition(
					SimpleModel.builder(String.format("morered:block/%s_line", blockLocation)),
					SimpleModel.builder(String.format("morered:block/%s_edge", blockLocation))))
				.run(cache);
			
			// generate item models
			new JsonDataProvider<SimpleModel>(generator, ResourceType.ASSETS, "models/item", SimpleModel.CODEC)
				.with(item.getRegistryName(), SimpleModel.builder("morered:item/colored_network_cable_template")
					.withTexture("wire", String.format("morered:block/%s", block.getRegistryName().getPath())))
				.run(cache);
			
			// generate loot table
			new LootTableProvider(generator)
			{

				@Override
				protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables()
				{
					return ImmutableList.of(Pair.of(
						() -> context -> context.accept(new ResourceLocation(modid, "blocks/"+blockLocation), LootTable.lootTable()
							.withPool(LootPool.lootPool().setRolls(ConstantRange.exactly(1))
								.add(ItemLootEntry.lootTableItem(item)
									.apply(() -> WireCountLootFunction.INSTANCE))
								.when(SurvivesExplosion.survivesExplosion()))),
						LootParameterSets.BLOCK
						));
				}

				@Override
				protected void validate(Map<ResourceLocation, LootTable> map, ValidationTracker validationtracker)
				{
					// noop
				}
				
			}
			.run(cache);
			
			// generate recipe
			new RecipeProvider(generator)
			{
				@Override
				protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer)
				{
					// override the recipe builder to not validate advancements
					ShapedRecipeBuilder builder = new ShapedRecipeBuilder(item,8);
					consumer.accept(builder.new Result(
						item.getRegistryName(),
						item,
						8,
						"",
						ImmutableList.of("www", "w#w", "www"),
						ImmutableMap.<Character,Ingredient>builder()
							.put('w', Ingredient.of(RED_ALLOY_WIRE_TAG))
							.put('#', Ingredient.of(MoreRedDataGen.WOOL_TAGS[ColoredCableDataProvider.this.colorIndex]))
							.build(),
						null,
						null
						)
						{
							@Override
							public JsonObject serializeAdvancement()
							{
								return null;
							}
						});
				}
			}
			.run(cache);
		}
	}
	
}
