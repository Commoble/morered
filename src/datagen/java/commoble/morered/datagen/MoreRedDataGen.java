package commoble.morered.datagen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.JsonOps;

import commoble.morered.MoreRed;
import commoble.morered.datagen.BlockStateFile.Case;
import commoble.morered.datagen.BlockStateFile.Model;
import commoble.morered.datagen.BlockStateFile.Multipart;
import commoble.morered.datagen.BlockStateFile.WhenApply;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.WireCountLootFunction;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegistryObject;

@Mod("morered_datagen")
@EventBusSubscriber(modid="morered_datagen", bus=Bus.MOD)
public class MoreRedDataGen
{
	public static final List<TagKey<Item>> WOOL_TAGS = IntStream.range(0, 16)
		.<TagKey<Item>>mapToObj(i -> TagKey.<Item>create(Registries.ITEM, new ResourceLocation("forge", "wools/" + DyeColor.values()[i].getSerializedName())))
		.toList();
	
	
	@SubscribeEvent
	static void onGatherData(GatherDataEvent event)
	{		
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper efh = event.getExistingFileHelper();
		CompletableFuture<HolderLookup.Provider> holders = event.getLookupProvider();
		PackOutput output = generator.getPackOutput();
		Map<ResourceLocation, BlockStateFile> blockStates = new HashMap<>();
		Map<ResourceLocation, SimpleModel> models = new HashMap<>();
		Map<ResourceLocation, WirePartModelDefinition> wirePartModels = new HashMap<>();
		Provider<LootTable> lootTables = Provider.create(event, Target.DATA_PACK, "loot_tables", LootDataType.TABLE.parser()::toJsonTree);
		Provider<FinishedRecipe> recipes = Provider.create(event, Target.DATA_PACK, "recipes", FinishedRecipe::serializeRecipe);
		TagProvider<Block> blockTags = TagProvider.create(event, Registries.BLOCK, holders);
		TagProvider<Item> itemTags = TagProvider.create(event, Registries.ITEM, holders);
		LanguageProvider lang = new LanguageProvider(output, MoreRed.MODID, "en_us")
		{
			@Override
			protected void addTranslations()
			{} // no
		};
		DataGenContext dataGenContext = new DataGenContext(event, blockStates, models, wirePartModels, lootTables, recipes, blockTags, itemTags, lang);

		// do stuff that has files for each color
		for (int i=0; i<16; i++)
		{
			buildColorData(dataGenContext, i);
		}

		generator.addProvider(event.includeClient(), new JsonCodecProvider<>(output, efh, MoreRed.MODID, JsonOps.INSTANCE, PackType.CLIENT_RESOURCES, "blockstates", BlockStateFile.CODEC, blockStates));
		generator.addProvider(event.includeClient(), new JsonCodecProvider<>(output, efh, MoreRed.MODID, JsonOps.INSTANCE, PackType.CLIENT_RESOURCES, "models", SimpleModel.CODEC, models));
		generator.addProvider(event.includeClient(), new JsonCodecProvider<>(output, efh, MoreRed.MODID, JsonOps.INSTANCE, PackType.CLIENT_RESOURCES, "models", WirePartModelDefinition.CODEC, wirePartModels));
		generator.addProvider(event.includeServer(), lootTables);
		generator.addProvider(event.includeServer(), recipes);
		generator.addProvider(event.includeServer(), blockTags);
		generator.addProvider(event.includeServer(), itemTags);
	}
	

	
	static void buildColorData(DataGenContext context, int i)
	{
		final RegistryObject<ColoredCableBlock> blockHolder = MoreRed.get().networkCableBlocks[i];
		final ResourceLocation blockId = blockHolder.getId();
		final String modid = blockId.getNamespace();
		final String blockPath = blockId.getPath();
		final ColoredCableBlock block = blockHolder.get();
		final Item item = block.asItem();
		final ResourceLocation blockTexture = new ResourceLocation(modid, String.format("block/%s", blockId.getPath()));
		
		// generate blockstate json
		ResourceLocation partsModel = new ResourceLocation(modid, String.format("block/%s_parts", blockPath));
		ResourceLocation nodeModel = new ResourceLocation(modid, String.format("block/%s_node", blockPath));
		ResourceLocation elbowModel = new ResourceLocation(modid, String.format("block/%s_elbow", blockPath));
		context.blockStates().put(blockId, BlockStateFile.multipart(Multipart.builder()
			.addWhenApply(WhenApply.always(Model.create(partsModel)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.DOWN, true),
				Model.create(nodeModel)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.UP, true),
				Model.create(nodeModel, BlockModelRotation.X180_Y0)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.SOUTH, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y0)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.WEST, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y90)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.NORTH, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y180)))
			.addWhenApply(WhenApply.when(
				Case.create(AbstractWireBlock.EAST, true),
				Model.create(nodeModel, BlockModelRotation.X90_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X0_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.SOUTH, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y0)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.WEST, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.NORTH, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.EAST, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X90_Y270)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.WEST, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y0)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.NORTH, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y90)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.EAST, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y180)))
			.addWhenApply(WhenApply.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.SOUTH, true),
				Model.create(elbowModel, BlockModelRotation.X180_Y270)))));
		
		
		// generate block models
		// generate the simple models for the block
		List<String> blockModelTypes = ImmutableList.of("edge","elbow","line","node");
		blockModelTypes.forEach(modelType->
			context.models().put(new ResourceLocation(modid, String.format("block/%s_%s", blockPath, modelType)),
				SimpleModel.create(new ResourceLocation(modid, String.format("block/colored_network_cable_%s_template", modelType)))
					.addTexture("wire",blockTexture)));
		
		// generate the parts model for the block
		context.wirePartModels().put(
			new ResourceLocation(modid, String.format("block/%s_parts", blockPath)),
			new WirePartModelDefinition(
				SimpleModel.create(new ResourceLocation(modid, String.format("block/%s_line", blockPath))),
				SimpleModel.create(new ResourceLocation(modid, String.format("block/%s_edge", blockPath)))));
		
		// generate item models
		context.models().put(
			new ResourceLocation(blockId.getNamespace(), String.format("item/%s", blockId.getPath())),
			SimpleModel.create(new ResourceLocation(modid, "item/colored_network_cable_template"))
				.addTexture("wire", blockTexture));

		// generate loot table
		context.lootTables().put(
			new ResourceLocation(modid, String.format("blocks/%s", blockPath)),
			LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
					.add(LootItem.lootTableItem(item)
						.apply(() -> WireCountLootFunction.INSTANCE))
					.when(ExplosionCondition.survivesExplosion()))
				.build());
		
		// generate recipe
		context.recipes().put(blockId,
			RecipeHelpers.shaped(blockId, item, 8, CraftingBookCategory.REDSTONE,
				List.of("www", "w#w", "www"),
				Map.<Character,Ingredient>of(
					'w', Ingredient.of(MoreRed.Tags.Items.RED_ALLOY_WIRES),
					'#', Ingredient.of(MoreRedDataGen.WOOL_TAGS.get(i)))));
		
		// tags
		context.itemTags()
			.getOrCreateRawBuilder(MoreRed.Tags.Items.COLORED_NETWORK_CABLES)
			.addElement(blockId);

		// generate tags for all the wool blocks
		context.itemTags().getOrCreateRawBuilder(WOOL_TAGS.get(i))
			.addElement(new ResourceLocation("minecraft", String.format("%s_wool", DyeColor.values()[i].getSerializedName())));
	}
}
