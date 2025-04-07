package commoble.morered.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.text.WordUtils;

import com.google.common.collect.ImmutableList;
import com.mojang.math.Quadrant;

import commoble.morered.datagen.BlockStateBuilder.Case;
import commoble.morered.datagen.BlockStateBuilder.Multipart;
import commoble.morered.datagen.BlockStateBuilder.PropertyValue;
import commoble.morered.datagen.BlockStateBuilder.Variants;
import commoble.morered.datagen.BlockStateBuilder.When;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.Parity;
import net.commoble.exmachina.api.content.MultipartMechanicalComponent;
import net.commoble.exmachina.api.content.MultipartMechanicalComponent.ApplyWhen;
import net.commoble.exmachina.api.content.RawNode;
import net.commoble.exmachina.api.content.RawNode.RawConnection;
import net.commoble.exmachina.api.content.VariantsMechanicalComponent;
import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.HexidecrubrometerBlock;
import net.commoble.morered.MoreRed;
import net.commoble.morered.Names;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlock;
import net.commoble.morered.client.ColorHandlers;
import net.commoble.morered.client.UnbakedLogicGateModel;
import net.commoble.morered.client.UnbakedWindcatcherModel;
import net.commoble.morered.client.UnbakedWirePartBlockStateModel;
import net.commoble.morered.mechanisms.AxleBlock;
import net.commoble.morered.mechanisms.GearBlock;
import net.commoble.morered.mechanisms.GearsLootEntry;
import net.commoble.morered.mechanisms.GearshifterBlock;
import net.commoble.morered.mechanisms.WindcatcherBlock;
import net.commoble.morered.mechanisms.WindcatcherDyeRecipe;
import net.commoble.morered.mechanisms.WindcatcherRecipe;
import net.commoble.morered.mechanisms.WoodSets;
import net.commoble.morered.mechanisms.WoodSets.WoodSet;
import net.commoble.morered.plate_blocks.PlateBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.commoble.morered.soldering.SolderingRecipe;
import net.commoble.morered.transportation.ColoredTubeBlock;
import net.commoble.morered.transportation.ExtractorBlock;
import net.commoble.morered.transportation.RedstoneTubeBlock;
import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.wires.AbstractWireBlock;
import net.commoble.morered.wires.PoweredWireBlock;
import net.commoble.morered.wires.WireCountLootFunction;
import net.minecraft.Util;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

@SuppressWarnings("deprecation")
@EventBusSubscriber(modid=MoreRed.MODID, bus=Bus.MOD)
public class MoreRedDataGen
{
	static final TagKey<Item> SMOOTH_STONE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "smooth_stone"));
	static final TagKey<Item> SMOOTH_STONE_SLABS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "slabs/smooth_stone"));
	static final TagKey<Item> SMOOTH_STONE_QUARTER_SLABS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "quarter_slabs/smooth_stone"));
	static final TagKey<Item> REDSTONE_ALLOY_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/redstone_alloy"));

	@SuppressWarnings("unchecked")
	@SubscribeEvent
	static void onGatherData(GatherDataEvent.Client event)
	{
		DataGenerator generator = event.getGenerator();
		CompletableFuture<HolderLookup.Provider> holders = event.getLookupProvider();
		PackOutput output = generator.getPackOutput();
		
		RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder();
		Map<ResourceLocation, BlockModelDefinition> blockStates = new HashMap<>();
		Map<ResourceLocation, ClientItem> clientItems = new HashMap<>();
		Map<ResourceLocation, SimpleModel> models = new HashMap<>();
		Map<ResourceLocation, Recipe<?>> recipes = new HashMap<>();
		Map<ResourceLocation, LootTable> lootTables = new HashMap<>();
		DataMapsProvider dataMaps = new DataMapsProvider(output, holders);
		TagProvider<Block> blockTags = TagProvider.create(event, Registries.BLOCK, holders);
		TagProvider<Item> itemTags = TagProvider.create(event, Registries.ITEM, holders);
		LanguageProvider lang = new LanguageProvider(output, MoreRed.MODID, "en_us")
		{
			@Override
			protected void addTranslations()
			{} // no
		};
		DataGenContext context = new DataGenContext(new HashMap<>(), blockStates, clientItems, models, recipes, lootTables, dataMaps, blockTags, itemTags, lang);
		
		ResourceLocation blockBlock = ResourceLocation.withDefaultNamespace("block/block");
		
		String fromSoldering = "%s_from_soldering";
		
		// blocks
		redstonePlateBlock(Names.TWO_INPUT_AND_GATE, "Two Input AND Gate", context, 4,
			"#t#",
			"trt",
			"###");
		redstonePlateBlock(Names.AND_GATE, "AND Gate", context, 5,
			"#t#",
			"trt",
			"#t#");
		bitwisePlateBlock(Names.BITWISE_AND_GATE, "Bitwise AND Gate", "and_gate_symbol", context);
		bitwisePlateBlock(Names.BITWISE_DIODE, "Bitwise Diode", "diode_symbol", context);
		bitwisePlateBlock(Names.BITWISE_NOT_GATE, "Bitwise NOT Gate", "not_gate_symbol", context);
		bitwisePlateBlock(Names.BITWISE_OR_GATE, "Bitwise OR Gate", "or_gate_symbol", context);
		bitwisePlateBlock(Names.BITWISE_XNOR_GATE, "Bitwise XNOR Gate", "xnor_gate_symbol", context);
		bitwisePlateBlock(Names.BITWISE_XOR_GATE, "Bitwise XOR Gate", "xor_gate_symbol", context);
		postBlock(Names.CABLE_RELAY, "Cable Relay", context)
			.tags(MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS)
			.simpleBlockItem()
			.help(helper -> helper.recipe(RecipeHelpers.shapeless(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					ingredient(MoreRed.Tags.Items.BUNDLED_CABLES),
					ingredient(Tags.Items.INGOTS_IRON))))
				.recipe(mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					sizedIngredient(MoreRed.Tags.Items.BUNDLED_CABLES,1),
					sizedIngredient(Tags.Items.INGOTS_IRON,1)))));
		postBlock(Names.CABLE_JUNCTION, "Cable Junction", context)
			.tags(MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS)
			.simpleBlockItem()
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" b ",
					"bFb",
					"###"), Map.of(
					'#', ingredient(SMOOTH_STONE_QUARTER_SLABS),
					'b', ingredient(MoreRed.Tags.Items.BUNDLED_CABLES),
					'F', ingredient(Tags.Items.INGOTS_IRON))))
				.recipe(mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					sizedIngredient(SMOOTH_STONE_QUARTER_SLABS, 2),
					sizedIngredient(Tags.Items.INGOTS_IRON, 1),
					sizedIngredient(MoreRed.Tags.Items.BUNDLED_CABLES, 1)))));
		wireBlock(Names.BUNDLED_CABLE, "Bundled Cable", context)
			.blockItemWithoutItemModel()
			.tags(MoreRed.Tags.Items.BUNDLED_CABLES)
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 3, CraftingBookCategory.REDSTONE, List.of(
					"#",
					"#",
					"#"), Map.of('#', ingredient(MoreRed.Tags.Items.CABLES)))));
		redstonePlateBlock(Names.DIODE, "Diode", context, 3,
			"trt",
			"###");
		hexidecrubrometerBlock(context);
		redstonePlateBlock(Names.LATCH, "Latch", context, 3,
			"#t#",
			"r r",
			"#t#");
		switchedPlateBlock(Names.MULTIPLEXER, "Multiplexer", context, 1,
			"#r#",
			"rir",
			"#r#");
		redstonePlateBlock(Names.TWO_INPUT_NAND_GATE, "Two Input NAND Gate", context, 3,
			"#r#",
			"trt",
			"###");
		redstonePlateBlock(Names.NAND_GATE, "NAND Gate", context, 4,
			"#r#",
			"trt",
			"#t#");
		redstonePlateBlock(Names.NOR_GATE, "NOR Gate", context, 2,
			"#r#",
			"rtr",
			"#r#");
		redstonePlateBlock(Names.NOT_GATE, "NOT Gate", context, 2,
			"rtr",
			"###");
		redstonePlateBlock(Names.OR_GATE, "OR Gate", context, 3,
			"#t#",
			"rtr",
			"#r#");
		switchedPlateBlock(Names.PULSE_GATE, "Pulse Gate", context, 1,
			"rir",
			"###");
		wireBlock(Names.RED_ALLOY_WIRE, "Red Alloy Wire", context)
			.blockItemWithoutItemModel(id -> new ClientItem(new BlockModelWrapper.Unbaked(id, List.of(new Constant(ColorHandlers.UNLIT))), ClientItem.Properties.DEFAULT))
			.tags(MoreRed.Tags.Items.RED_ALLOY_WIRES)
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 12, CraftingBookCategory.REDSTONE, List.of("###"), Map.of('#', ingredient(REDSTONE_ALLOY_INGOTS)))));
		postBlock(Names.REDWIRE_POST, "Redwire Post", context)
			.tags(MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.simpleBlockItem(id -> new ClientItem(new BlockModelWrapper.Unbaked(id, List.of(new Constant(ColorHandlers.NO_TINT), new Constant(ColorHandlers.UNLIT))), ClientItem.Properties.DEFAULT))
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					"F",
					"R"), Map.of(
					'R', ingredient(REDSTONE_ALLOY_INGOTS),
					'F', ingredient(Tags.Items.INGOTS_IRON))))
				.recipe(mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					sizedIngredient(Tags.Items.INGOTS_IRON,1),
					sizedIngredient(REDSTONE_ALLOY_INGOTS,1)))));
				
		postBlock(Names.REDWIRE_RELAY, "Redwire Relay", context)
			.tags(MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.simpleBlockItem(id -> new ClientItem(new BlockModelWrapper.Unbaked(id, List.of(new Constant(ColorHandlers.NO_TINT), new Constant(ColorHandlers.UNLIT))), ClientItem.Properties.DEFAULT))
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" F ",
					" R ",
					"###"), Map.of(
					'#', ingredient(SMOOTH_STONE_QUARTER_SLABS),
					'R', ingredient(REDSTONE_ALLOY_INGOTS),
					'F', ingredient(Tags.Items.INGOTS_IRON))))
				.recipe(mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					sizedIngredient(SMOOTH_STONE_QUARTER_SLABS,1),
					sizedIngredient(REDSTONE_ALLOY_INGOTS,1),
					sizedIngredient(Tags.Items.INGOTS_IRON,1)))));
				
		postBlock(Names.REDWIRE_JUNCTION, "Redwire Junction", context)
			.tags(MoreRed.Tags.Blocks.REDWIRE_POSTS)
			.simpleBlockItem(id -> new ClientItem(new BlockModelWrapper.Unbaked(id, List.of(new Constant(ColorHandlers.NO_TINT), new Constant(ColorHandlers.UNLIT))), ClientItem.Properties.DEFAULT))
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
					" F ",
					"rRr",
					"###"), Map.of(
					'#', ingredient(SMOOTH_STONE_QUARTER_SLABS),
					'r', ingredient(Tags.Items.DUSTS_REDSTONE),
					'R', ingredient(REDSTONE_ALLOY_INGOTS),
					'F', ingredient(Tags.Items.INGOTS_IRON))))
				.recipe(mangle(helper.id(), fromSoldering), new SolderingRecipe(new ItemStack(helper.item()), List.of(
					sizedIngredient(SMOOTH_STONE_QUARTER_SLABS,1),
					sizedIngredient(Tags.Items.DUSTS_REDSTONE,1),
					sizedIngredient(REDSTONE_ALLOY_INGOTS,1),
					sizedIngredient(Tags.Items.INGOTS_IRON,1)))));
		simpleBlock(Names.SOLDERING_TABLE, "Soldering Table", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
				"sss",
				"#b#",
				"#b#"), Map.of(
				'#', Ingredient.of(Items.RED_NETHER_BRICKS),
				's', ingredient(SMOOTH_STONE_QUARTER_SLABS),
				'b', ingredient(Tags.Items.RODS_BLAZE)))));
		plateBlock(Names.STONE_PLATE, "Stone Plate", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.blockItemWithoutItemModel()
			.tags(SMOOTH_STONE_QUARTER_SLABS)
			.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 12, CraftingBookCategory.BUILDING,
					List.of("###"),
					Map.of('#', ingredient(SMOOTH_STONE_SLABS))))
				.recipe(mangle(helper.id(), "%s_from_smooth_stone_slab_stonecutting"),
					new StonecutterRecipe("", ingredient(SMOOTH_STONE_SLABS), new ItemStack(helper.item(), 4)))
				.recipe(mangle(helper.id(), "%s_from_smooth_stone_stonecutting"),
					new StonecutterRecipe("", ingredient(SMOOTH_STONE), new ItemStack(helper.item(), 8))));				
		redstonePlateBlock(Names.XNOR_GATE, "XNOR Gate", context, 4,
		    "#t#",
		    "ttt",
		    "###");
		redstonePlateBlock(Names.XOR_GATE, "XOR Gate", context, 4,
		    "#r#",
		    "ttt",
		    "###");
		
		BlockDataHelper.create(MoreRed.get().tubeBlock.get(), context, MoreRedDataGen::tubeBlockState, MoreRedDataGen::simpleLoot)
			.localize("Tube")
			.tags(MoreRed.Tags.Blocks.TUBES, BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(mangle(helper.id(), "%s_from_gold"), RecipeHelpers.shaped(helper.item(), 8, CraftingBookCategory.BUILDING, List.of("iGi"), Map.of(
					'i', ingredient(Tags.Items.INGOTS_GOLD),
					'G', ingredient(Tags.Items.GLASS_BLOCKS_COLORLESS))))
				.recipe(mangle(helper.id(), "%s_from_copper"), RecipeHelpers.shaped(helper.item(), 2, CraftingBookCategory.BUILDING, List.of("iGi"), Map.of(
					'i', ingredient(Tags.Items.INGOTS_COPPER),
					'G', ingredient(Tags.Items.GLASS_BLOCKS_COLORLESS))))
				.tags(MoreRed.Tags.Items.TUBES));
		tubeBlock(Names.REDSTONE_TUBE, "Redstone Tube", context, redstoneTubeBlockState(MoreRed.get().redstoneTubeBlock.get()))
			.model("block/%s_on", SimpleModel.createWithoutRenderType(MoreRed.id("block/tube"))
				.addTexture("all", ResourceLocation.fromNamespaceAndPath(MoreRed.MODID, "block/redstone_tube_on")))
			.tags(MoreRed.Tags.Blocks.TUBES, BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of(" r ", "rtr", " r "),
					Map.of(
						'r', ingredient(Tags.Items.DUSTS_REDSTONE),
						't', ingredient(MoreRed.Tags.Items.TUBES)))));
		simpleBlock(Names.DISTRIBUTOR, "Distributor", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of("csc", "sGs", "csc"),
					Map.of(
						'c', ingredient(Tags.Items.COBBLESTONES),
						's', Ingredient.of(MoreRed.get().shuntBlock.get().asItem()),
						'G', ingredient(Tags.Items.FENCE_GATES)))));
		BlockDataHelper.create(MoreRed.get().extractorBlock.get(), context, block -> {
			ResourceLocation blockModel = BlockDataHelper.blockModel(block);
			ResourceLocation poweredBlockModel = mangle(blockModel, "%s_powered");
			var builder = Variants.builder();
			for (boolean powered : new boolean[]{false, true})
			{
				for (Direction direction : Direction.values())
				{
					ResourceLocation model = powered ? poweredBlockModel : blockModel;
					Quadrant y = switch(direction) {
						case WEST -> Quadrant.R90;
						case EAST -> Quadrant.R270;
						default -> Quadrant.R0;
					};
					Quadrant x = switch(direction) {
						case DOWN -> Quadrant.R0;
						case UP -> Quadrant.R180;
						case NORTH -> Quadrant.R270;
						default -> Quadrant.R90;
					};
					builder.addVariant(
						List.of(
							PropertyValue.create(ExtractorBlock.FACING, direction),
							PropertyValue.create(ExtractorBlock.POWERED, powered)),
						BlockStateBuilder.model(model, x, y, true));
				}
			}
			return BlockStateBuilder.variants(builder);
		}, block -> simpleLoot(block))
			.localize("Extractor")
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of("h", "p", "s"),
					Map.of(
						'h', Ingredient.of(Items.HOPPER),
						'p', Ingredient.of(Items.PISTON),
						's', Ingredient.of(MoreRed.get().shuntBlock.get().asItem())))));
		sixWayBlock(Names.SHUNT, "Shunt", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of(" t ", "tst", " t "),
					Map.of(
						's', ingredient(Tags.Items.COBBLESTONES),
						't', ingredient(MoreRed.Tags.Items.TUBES)))));
		sixWayBlock(Names.FILTER, "Filter", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shapeless(helper.item(), 1, CraftingBookCategory.BUILDING, List.of(
					Ingredient.of(MoreRed.get().shuntBlock.get().asItem()),
					Ingredient.of(Items.ITEM_FRAME)))));
		sixWayBlock(Names.MULTIFILTER, "Multifilter", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of("ifi", "fCf", "ifi"),
					Map.of(
						'i', ingredient(Tags.Items.INGOTS_IRON),
						'f', Ingredient.of(MoreRed.get().filterBlock.get().asItem()),
						'C', ingredient(Tags.Items.CHESTS)))));
		sixWayBlock(Names.LOADER, "Loader", context)
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.simpleBlockItem()
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of("sss", "P S", "sss"),
					Map.of(
						's', ingredient(Tags.Items.COBBLESTONES),
						'S', Ingredient.of(MoreRed.get().shuntBlock.get().asItem()),
						'P', Ingredient.of(Items.PISTON)))));
		BlockDataHelper.create(MoreRed.get().osmosisFilterBlock.get(), context, MoreRedDataGen::sixWayBlockState, MoreRedDataGen::simpleLoot)
			.localize("Osmosis Filter")
			.tags(BlockTags.MINEABLE_WITH_PICKAXE)
			.blockItemWithoutItemModel() // model isn't generated
			.help(helper -> helper
				.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING,
					List.of("f", "s", "h"),
					Map.of(
						'f', Ingredient.of(MoreRed.get().filterBlock.get().asItem()),
						's', ingredient(Tags.Items.SLIME_BALLS),
						'h', Ingredient.of(Items.HOPPER)))));
		
		BlockDataHelper.create(MoreRed.get().osmosisSlimeBlock.get(), context, sixWayBlockState(MoreRed.get().osmosisSlimeBlock.get()));
		
		BlockDataHelper.createWithoutLoot(MoreRed.get().airFoilBlock.get(), context,
			(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(blockId(Blocks.AIR))))))
			.tags(MoreRed.Tags.Blocks.AIRFOILS);
			
		
		// other items
		spool(Names.BUNDLED_CABLE_SPOOL, "Bundled Cable Spool", context, MoreRed.Tags.Items.BUNDLED_CABLES);
		spool(Names.REDWIRE_SPOOL, "Redwire Spool", context, MoreRed.Tags.Items.RED_ALLOY_WIRES);
		simpleItem(Names.RED_ALLOY_INGOT, "Red Alloy Ingot", context)
			.tags(REDSTONE_ALLOY_INGOTS);
		Util.make(MoreRed.get().tubingPliers.get(), pliers -> ItemDataHelper.create(pliers, context, SimpleModel.create(ResourceLocation.withDefaultNamespace("item/handheld"), SimpleModel.RenderTypes.CUTOUT)
			.addTexture("layer0", mangle(MoreRed.id(Names.PLIERS), "item/%s")))
			.tags(Tags.Items.TOOLS_WRENCH)
			.recipe(RecipeHelpers.shaped(pliers, 1, CraftingBookCategory.MISC,
				List.of("  I", "It ", " I "),
				Map.of(
					'I', ingredient(Tags.Items.INGOTS_IRON),
					't', ingredient(MoreRed.Tags.Items.TUBES))))
			.localize("Pliers"));
		
		// data for vanilla things
		recipes.put(MoreRed.id("smooth_stone_slab_from_stone_plate"), RecipeHelpers.shaped(Items.SMOOTH_STONE_SLAB, 1, CraftingBookCategory.BUILDING, List.of(
				"###"), Map.of(
				'#', Ingredient.of(MoreRed.get().stonePlateBlock.get()))));
		
		itemTags.tag(SMOOTH_STONE)
			.add(Items.SMOOTH_STONE.builtInRegistryHolder().key());
		itemTags.tag(SMOOTH_STONE_SLABS)
			.add(Items.SMOOTH_STONE_SLAB.builtInRegistryHolder().key());
		itemTags.tag(MoreRed.Tags.Items.RED_ALLOYABLE_INGOTS)
			.addTag(Tags.Items.INGOTS_COPPER);
		
		// tags in tags
		itemTags.tag(MoreRed.Tags.Items.CABLES)
			.addTag(MoreRed.Tags.Items.BUNDLED_CABLES)
			.addTag(MoreRed.Tags.Items.COLORED_CABLES);
		blockTags.tag(MoreRed.Tags.Blocks.TUBES).addTag(MoreRed.Tags.Blocks.COLORED_TUBES);
		blockTags.tag(BlockTags.MINEABLE_WITH_PICKAXE).addTag(MoreRed.Tags.Blocks.COLORED_TUBES);
		blockTags.tag(BlockTags.MINEABLE_WITH_AXE).addTags(
			MoreRed.Tags.Blocks.AXLES,
			MoreRed.Tags.Blocks.GEARS,
			MoreRed.Tags.Blocks.GEARSHIFTERS,
			MoreRed.Tags.Blocks.WINDCATCHERS);
		itemTags.tag(MoreRed.Tags.Items.TUBES).addTag(MoreRed.Tags.Items.COLORED_TUBES);
		
		// misc. translations
		lang.add("itemGroup.morered", "More Red");
		lang.add("emi.category.morered.soldering", "Soldering");
		lang.add("gui.morered.category.soldering", "Soldering");
		
		Map<DyeColor,ResourceLocation> airfoilSailDummyModels = Util.makeEnumMap(DyeColor.class,
			color -> MoreRed.id(String.format("block/%s_airfoil_sail", color)));

		// do stuff that has files for each color
		for (int i=0; i<16; i++)
		{
			final DyeColor color = DyeColor.values()[i];
			final DeferredHolder<Block, PoweredWireBlock> wireBlockHolder = MoreRed.get().coloredCableBlocks[i];
			final ResourceLocation wireBlockId = wireBlockHolder.getId();
			final String modid = wireBlockId.getNamespace();
			final String wireBlockPath = wireBlockId.getPath();
			final PoweredWireBlock wireBlock = wireBlockHolder.get();
			final Item wireBlockItem = wireBlock.asItem();
			
			String wireBlockName = WordUtils.capitalize(wireBlockPath.replace("_", " ")); 
			
			wireBlock(Names.COLORED_CABLES_BY_COLOR[i], wireBlockName, context)
				.tags(MoreRed.Tags.Blocks.COLORED_CABLES)
				.blockItemWithoutItemModel()
				.tags(MoreRed.Tags.Items.COLORED_CABLES, color.getDyedTag());
			
			ResourceLocation wireBlockTexture = mangle(wireBlockId, "block/%s");
			
			// generate block models
			// generate the simple models for the block
			List<String> blockModelTypes = ImmutableList.of("edge","elbow","line","node");
			blockModelTypes.forEach(modelType->
				context.models().put(MoreRed.id(String.format("block/%s_%s", wireBlockPath, modelType)),
					SimpleModel.createWithoutRenderType(MoreRed.id(String.format("block/colored_cable_%s_template", modelType)))
						.addTexture("wire",wireBlockTexture)));

			// generate item models
			models.put(
				ResourceLocation.fromNamespaceAndPath(wireBlockId.getNamespace(), String.format("item/%s", wireBlockId.getPath())),
				SimpleModel.createWithoutRenderType(ResourceLocation.fromNamespaceAndPath(modid, "item/colored_cable_template"))
					.addTexture("wire", wireBlockTexture));	

			// generate recipe
			recipes.put(wireBlockId,
				RecipeHelpers.shaped(wireBlockItem, 8, CraftingBookCategory.REDSTONE,
					List.of("www", "w#w", "www"),
					Map.<Character,Ingredient>of(
						'w', ingredient(MoreRed.Tags.Items.RED_ALLOY_WIRES),
						'#', IntersectionIngredient.of(
							ingredient(ItemTags.WOOL),
							ingredient(color.getDyedTag())))));
			
			// color tubes
			// capitalize each letter of each word in color name
			final DeferredHolder<Block, ColoredTubeBlock> tubeBlockHolder = MoreRed.get().coloredTubeBlocks[i];
			final ResourceLocation tubeBlockId = tubeBlockHolder.getId();
			final String tubeBlockPath = tubeBlockId.getPath();
			
			String tubeBlockName = WordUtils.capitalize(tubeBlockPath.replace("_", " "));
			tubeBlock(Names.COLORED_TUBE_NAMES[i], tubeBlockName, context, tubeBlockState(tubeBlockHolder.get()))
				.tags(MoreRed.Tags.Blocks.COLORED_TUBES)
				.simpleBlockItem()
				.help(helper -> helper
					.recipe(mangle(helper.id(), "%s_from_gold"), RecipeHelpers.shaped(helper.item(), 8, CraftingBookCategory.BUILDING, List.of("iGi"), Map.of(
						'i', ingredient(Tags.Items.INGOTS_GOLD),
						'G', IntersectionIngredient.of(
							ingredient(Tags.Items.GLASS_BLOCKS),
							ingredient(color.getDyedTag())))))
					.recipe(mangle(helper.id(), "%s_from_copper"), RecipeHelpers.shaped(helper.item(), 2, CraftingBookCategory.BUILDING, List.of("iGi"), Map.of(
						'i', ingredient(Tags.Items.INGOTS_COPPER),
						'G', IntersectionIngredient.of(
							ingredient(Tags.Items.GLASS_BLOCKS),
							ingredient(color.getDyedTag())))))
					.tags(MoreRed.Tags.Items.COLORED_TUBES, color.getDyedTag()));
			
			// dummy models for rendering airfoil sails (by wool color)
			ResourceLocation airfoilSailModelId = airfoilSailDummyModels.get(color);
			ResourceLocation woolBlockId = ResourceLocation.withDefaultNamespace(String.format("block/%s_wool", color));
			models.put(airfoilSailModelId, SimpleModel.createWithoutRenderType(MoreRed.id("block/airfoil_sail_template"))
				.addTexture("foil", woolBlockId));
		}
		
		// stuff for each wood type
		for (var entry : WoodSets.LOOKUP.entrySet())
		{
			String woodName = entry.getKey();
			WoodSet woodSet = entry.getValue();
			Block strippedLog = woodSet.strippedLog();
			ResourceLocation strippedLogId = BuiltInRegistries.BLOCK.getKey(strippedLog);
			ResourceLocation strippedLogBlockModel = blockModel(strippedLogId);
			ResourceLocation strippedLogTopTexture = mangle(strippedLogBlockModel, "%s_top");
			boolean isWoodSupported = !woodName.equals("oak");
			if (isWoodSupported)
			{
				// unsupported modded logs are craftable into oak parts
				// but, we want to make sure the relevant ingredient is never empty
				// we can cheese it by just not declaring oak a "supported" wood type
				itemTags.tag(MoreRed.Tags.Items.SUPPORTED_STRIPPED_LOGS).add(ResourceKey.create(Registries.ITEM, strippedLogId));
			}
			
			VariantsMechanicalComponent axleMechanicalComponent = VariantsMechanicalComponent.builder(true);
			for (Direction.Axis axis : Direction.Axis.values())
			{
				axleMechanicalComponent.addVariant(AxleBlock.AXIS, axis, new RawNode(NodeShape.ofCube(), 0D,0D,0D, 0.02D, List.of(
					new RawConnection(Optional.of(axis.getPositive()), NodeShape.ofSide(axis.getNegative()), Parity.POSITIVE, 0),
					new RawConnection(Optional.of(axis.getNegative()), NodeShape.ofSide(axis.getPositive()), Parity.POSITIVE, 0))));
			}
			
			BlockDataHelper.create(MoreRed.get().axleBlocks.get(woodName).get(), context,
				(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(id)))),
				(id,block) -> simpleLoot(block))
				.baseModel(SimpleModel.createWithoutRenderType(blockBlock)
					.addTexture("particle", strippedLogBlockModel))
				.localize()
				.mechanicalComponent(axleMechanicalComponent)
				.tags(MoreRed.Tags.Blocks.AXLES)
				.blockItem(SimpleModel.createWithoutRenderType(MoreRed.id("item/axle_template"))
					.addTexture("side", strippedLogBlockModel)
					.addTexture("top", strippedLogTopTexture))
				.tags(MoreRed.Tags.Items.AXLES)
				.help(helper -> {
					if (isWoodSupported)
					{
						helper.recipe(RecipeHelpers.shaped(helper.item(), 16, CraftingBookCategory.BUILDING, List.of(
							"#",
							"#",
							"#"),
							Map.of('#', Ingredient.of(strippedLog))));	
					}
				});

			VariantsMechanicalComponent gearMechanicalComponent = VariantsMechanicalComponent.builder(true);
			for (Direction directionToNeighbor : Direction.values())
			{
				List<RawConnection> connections = new ArrayList<>();
				// connect to attached neighbor
				connections.add(new RawConnection(
					Optional.of(directionToNeighbor),
					NodeShape.ofSide(directionToNeighbor.getOpposite()),
					Parity.POSITIVE,
					0));
				// connect to four parallel neighbors
				for (Direction parallelDirection : Direction.values())
				{
					if (parallelDirection.getAxis() == directionToNeighbor.getAxis())
						continue;
					connections.add(new RawConnection(
						Optional.of(parallelDirection),
						NodeShape.ofSideSide(directionToNeighbor, parallelDirection.getOpposite()),
						Parity.NEGATIVE,
						16));
				}
				gearMechanicalComponent.addVariant(GearBlock.FACING, directionToNeighbor, new RawNode(NodeShape.ofSide(directionToNeighbor), 0D,0D,0D, 2D, connections));
			}
			
			BlockDataHelper.create(MoreRed.get().gearBlocks.get(woodName).get(), context,
				(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(id)))),
				(id,block) -> simpleLoot(block))
				.baseModel(SimpleModel.createWithoutRenderType(blockBlock)
					.addTexture("particle", strippedLogTopTexture))
				.localize()
				.mechanicalComponent(gearMechanicalComponent)
				.tags(MoreRed.Tags.Blocks.GEARS)
				.blockItem(SimpleModel.createWithoutRenderType(MoreRed.id("item/gear_template"))
					.addTexture("side", strippedLogBlockModel)
					.addTexture("top", strippedLogTopTexture))
				.tags(MoreRed.Tags.Items.GEARS)
				.help(helper -> {
					if (isWoodSupported)
					{
						helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING, List.of(
							"///",
							"/O/",
							"///"),
							Map.of('/', ingredient(Tags.Items.RODS_WOODEN),
								'O', Ingredient.of(strippedLog))));
					}
				});
			
			VariantsMechanicalComponent gearshifterMechanicalComponent = VariantsMechanicalComponent.builder(true);
			BlockState defaultGearshifterState = MoreRed.get().gearshifterBlocks.get(woodName).get().defaultBlockState();
			for (Direction bigDir : Direction.values())
			{
				for (int i=0; i<4; i++)
				{
					final int rotation = i;
					BlockState state = defaultGearshifterState
						.setValue(GearshifterBlock.ATTACHMENT_DIRECTION, bigDir)
						.setValue(GearshifterBlock.ROTATION, rotation);
					Direction smallDir = PlateBlockStateProperties.getOutputDirection(state);
					Direction axleDir = smallDir.getOpposite();
					gearshifterMechanicalComponent.addMultiPropertyVariant(builder -> builder
						.add(GearshifterBlock.ATTACHMENT_DIRECTION, bigDir)
						.add(GearshifterBlock.ROTATION, rotation),
						new RawNode(NodeShape.ofSide(bigDir), 0D,0D,0D, 2D, List.of(
							new RawConnection(Optional.of(bigDir), NodeShape.ofSide(bigDir.getOpposite()), Parity.POSITIVE, 0),
							new RawConnection(Optional.empty(), NodeShape.ofSide(smallDir), Parity.inversion(bigDir, smallDir), 16))),
						new RawNode(NodeShape.ofSide(smallDir), 0D,0D,0D, 0.5D, List.of(
							new RawConnection(Optional.of(smallDir), NodeShape.ofSide(smallDir.getOpposite()), Parity.POSITIVE,0),
							new RawConnection(Optional.empty(), NodeShape.ofSide(bigDir), Parity.inversion(bigDir, smallDir), 8),
							new RawConnection(Optional.empty(), NodeShape.ofSide(axleDir), Parity.POSITIVE, 0))),
						new RawNode(NodeShape.ofSide(axleDir), 0D,0D,0D, 0.02D, List.of(
							new RawConnection(Optional.of(axleDir), NodeShape.ofSide(axleDir.getOpposite()), Parity.POSITIVE,0),
							new RawConnection(Optional.empty(), NodeShape.ofSide(smallDir), Parity.POSITIVE, 0))));
				}
			}
			BlockDataHelper.create(MoreRed.get().gearshifterBlocks.get(woodName).get(), context,
				(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(id)))),
				(id,block) -> simpleLoot(block))
				.baseModel(SimpleModel.createWithoutRenderType(blockBlock)
					.addTexture("particle", strippedLogTopTexture))
				.localize()
				.mechanicalComponent(gearshifterMechanicalComponent)
				.tags(MoreRed.Tags.Blocks.GEARSHIFTERS)
				.blockItemWithoutItemModel(itemModelId -> new ClientItem(
					new CompositeModel.Unbaked(List.of(
						new BlockModelWrapper.Unbaked(mangle(itemModelId, "%s_gear"), List.of()),
						new BlockModelWrapper.Unbaked(mangle(itemModelId, "%s_axle"), List.of()))),
					ClientItem.Properties.DEFAULT))
				.tags(MoreRed.Tags.Items.GEARSHIFTERS)
				.help(helper -> {
					ResourceLocation gearId = mangle(helper.id(), "%s_gear");
					ResourceLocation axleId = mangle(helper.id(), "%s_axle");
					ResourceLocation gearItemModelId = mangle(itemModel(helper.id()), "%s_gear");
					ResourceLocation axleItemModelId = mangle(itemModel(helper.id()), "%s_axle");
					models.put(gearItemModelId, SimpleModel.createWithoutRenderType(MoreRed.id("item/gearshifter_gear_template"))
						.addTexture("side", strippedLogBlockModel)
						.addTexture("top", strippedLogTopTexture));
					models.put(axleItemModelId, SimpleModel.createWithoutRenderType(MoreRed.id("item/gearshifter_axle_template"))
						.addTexture("side", strippedLogBlockModel)
						.addTexture("top", strippedLogTopTexture));
					clientItems.put(gearId, new ClientItem(new BlockModelWrapper.Unbaked(gearItemModelId, List.of()), ClientItem.Properties.DEFAULT));
					clientItems.put(axleId, new ClientItem(new BlockModelWrapper.Unbaked(axleItemModelId, List.of()), ClientItem.Properties.DEFAULT));

					helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.BUILDING, List.of(
						"-G",
						"G "), Map.of(
						'-', Ingredient.of(MoreRed.get().axleBlocks.get(woodName).get()),
						'G', Ingredient.of(MoreRed.get().gearBlocks.get(woodName).get()))));
				});
			
			// make some dummy models for the windcatcher
			ResourceLocation windcatcherAxleDummyModel = blockModel(MoreRed.id(woodName + "_windcatcher_axle"));
			ResourceLocation airfoilDummyModel = blockModel(MoreRed.id(woodName + "_" + Names.AIRFOIL));
			models.put(windcatcherAxleDummyModel, SimpleModel.createWithoutRenderType(MoreRed.id("block/windcatcher_axle_template"))
				.addTexture("side", strippedLogBlockModel)
				.addTexture("top", strippedLogTopTexture));
			models.put(airfoilDummyModel, SimpleModel.createWithoutRenderType(MoreRed.id("block/airfoil_template"))
				.addTexture("side", strippedLogBlockModel)
				.addTexture("top", strippedLogTopTexture));
			
			VariantsMechanicalComponent windcatcherMechanicalComponent = VariantsMechanicalComponent.builder(true);
			for (int i=0; i<=8; i++)
			{
				windcatcherMechanicalComponent.addVariant(WindcatcherBlock.WIND, i, new RawNode(NodeShape.ofCube(), 3*i*i, 0, 0, 25D, List.of(
					new RawConnection(Optional.of(Direction.UP), NodeShape.ofSide(Direction.DOWN), Parity.POSITIVE, 0),
					new RawConnection(Optional.of(Direction.DOWN), NodeShape.ofSide(Direction.UP), Parity.POSITIVE, 0))));
			}
			BlockDataHelper.create(MoreRed.get().windcatcherBlocks.get(woodName).get(), context,
				(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(id)))),
				(id,block) -> LootTable.lootTable()
					.withPool(LootPool.lootPool()
						.add(LootItem.lootTableItem(block)
							.apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
								.include(MoreRed.get().windcatcherColorsDataComponent.get())))
						.when(ExplosionCondition.survivesExplosion()))
					.build())
				.localize()
				.tags(MoreRed.Tags.Blocks.WINDCATCHERS)
				.baseModel(SimpleModel.createWithoutRenderType(blockBlock).addTexture("particle", mangle(BuiltInRegistries.BLOCK.getKey(strippedLog), "block/%s")))
				.mechanicalComponent(windcatcherMechanicalComponent)
				.blockItemWithoutItemModel(modelId -> new ClientItem(
					new UnbakedWindcatcherModel(
						windcatcherAxleDummyModel,
						airfoilDummyModel,
						airfoilSailDummyModels),
					ClientItem.Properties.DEFAULT))
				.tags(MoreRed.Tags.Items.WINDCATCHERS)
				.help(helper -> helper.recipe(WindcatcherRecipe.of(helper.item(), CraftingBookCategory.BUILDING, List.of(
					// putting the sticks on the cardinal directions would look nicer
					// but having the top wool be north is important
					"-W-",
					"WoW",
					"-W-"), Map.of(
					'W', ingredient(ItemTags.WOOL),
					'-', ingredient(Tags.Items.RODS_WOODEN),
					'o', Ingredient.of(MoreRed.get().axleBlocks.get(woodName).get())))));			
		}
		
		MultipartMechanicalComponent gearsMechanicalComponent = MultipartMechanicalComponent.builder(true);
		for (Direction directionToNeighbor : Direction.values())
		{
			List<RawConnection> connections = new ArrayList<>();
			
			// connect to the neighbor block this face is touching
			connections.add(new RawConnection(
				Optional.of(directionToNeighbor),
				NodeShape.ofSide(directionToNeighbor.getOpposite()),
				Parity.POSITIVE,
				0));
			
			// connect to the four internal nodes sharing an edge with this one
			for (Direction internalSide : Direction.values())
			{
				if (internalSide.getAxis() == directionToNeighbor.getAxis())
					continue;
				connections.add(new RawConnection(
					Optional.empty(),
					NodeShape.ofSide(internalSide),
					Parity.inversion(directionToNeighbor, internalSide),
					16));
				// also connect to the four parallel neighbors
				Direction parallelDirection = internalSide;
				connections.add(new RawConnection(
					Optional.of(parallelDirection),
					NodeShape.ofSideSide(directionToNeighbor, parallelDirection.getOpposite()),
					Parity.NEGATIVE,
					16));
			}
			
			gearsMechanicalComponent.addApplyWhen(
				ApplyWhen.when(
					MultipartMechanicalComponent.Case.create(FaceSegmentBlock.getProperty(directionToNeighbor),true),
					new RawNode(NodeShape.ofSide(directionToNeighbor), 0,0,0,2D, connections)));
		}
		BlockDataHelper.create(MoreRed.get().gearsBlock.get(), context,
			(id,block) -> BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(blockModel(id)))),
			(id,block) -> LootTable.lootTable()
				.withPool(LootPool.lootPool()
					.add(GearsLootEntry.gearsLoot())
					.when(ExplosionCondition.survivesExplosion()))
				.build())
			.localize()
			.tags(BlockTags.MINEABLE_WITH_AXE)
			.baseModel(SimpleModel.createWithoutRenderType(blockBlock).addTexture("particle", ResourceLocation.withDefaultNamespace("block/stripped_oak_log_top")))
			.mechanicalComponent(gearsMechanicalComponent);
			
		
		// special recipes
		recipes.put(MoreRed.get().windcatcherDyeRecipeSerializer.getId(), WindcatcherDyeRecipe.of(ingredient(MoreRed.Tags.Items.WINDCATCHERS)));
		
		// add recipes for unsupported wood types so they can default to oak
		Ingredient unsupportedStrippedLogsIngredient = new Ingredient(new DifferenceIngredient(
			ingredient(Tags.Items.STRIPPED_LOGS),
			ingredient(MoreRed.Tags.Items.SUPPORTED_STRIPPED_LOGS)));
		
		recipes.put(MoreRed.id("oak_" + Names.AXLE), RecipeHelpers.shaped(MoreRed.get().axleBlocks.get("oak").get().asItem(), 16, CraftingBookCategory.BUILDING, List.of(
			"#",
			"#",
			"#"),
			Map.of('#', unsupportedStrippedLogsIngredient)));
		recipes.put(MoreRed.id("oak_" + Names.GEAR), RecipeHelpers.shaped(MoreRed.get().gearBlocks.get("oak").get().asItem(), 1, CraftingBookCategory.BUILDING, List.of(
			"///",
			"/O/",
			"///"),
			Map.of('/', ingredient(Tags.Items.RODS_WOODEN),
				'O', unsupportedStrippedLogsIngredient)));
		
		// add fuels
		dataMaps.builder(NeoForgeDataMaps.FURNACE_FUELS)
			.add(MoreRed.Tags.Items.AXLES, new FurnaceFuel(100), false)
			.add(MoreRed.Tags.Items.GEARS, new FurnaceFuel(300), false)
			.add(MoreRed.Tags.Items.GEARSHIFTERS, new FurnaceFuel(300), false)
			.add(MoreRed.Tags.Items.WINDCATCHERS, new FurnaceFuel(300), false);
		
		// finalize datapack registries (we have to do these all in one go)
		registrySetBuilder.add(ExMachinaRegistries.MECHANICAL_COMPONENT, bootstap ->
			context.mechanicalComponents()
				.forEach((id,component) -> bootstap.register(
					ResourceKey.create(ExMachinaRegistries.MECHANICAL_COMPONENT, id),
					component)));
		
		generator.addProvider(true, JsonDataProvider.create(holders, output, generator, PackOutput.Target.RESOURCE_PACK, "blockstates", BlockModelDefinition.CODEC, blockStates));
		generator.addProvider(true, JsonDataProvider.create(holders, output, generator, PackOutput.Target.RESOURCE_PACK, "items", ClientItem.CODEC, clientItems));
		generator.addProvider(true, JsonDataProvider.create(holders, output, generator, PackOutput.Target.RESOURCE_PACK, "models", SimpleModel.CODEC, models));
		generator.addProvider(true, lang);

		generator.addProvider(true, JsonDataProvider.create(holders, output, generator, PackOutput.Target.DATA_PACK, "loot_table", LootTable.DIRECT_CODEC, lootTables));
		generator.addProvider(true, JsonDataProvider.create(holders, output, generator, PackOutput.Target.DATA_PACK, "recipe", Recipe.CODEC, recipes));
		generator.addProvider(true, dataMaps);
		generator.addProvider(true, blockTags);
		generator.addProvider(true, itemTags);

		event.createDatapackRegistryObjects(registrySetBuilder);
	}
	
	static BlockDataHelper plateBlock(String blockPath, String name, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		ResourceLocation model = mangle(blockId, "block/%s");
		ResourceLocation modelAlt = mangle(blockId, "block/%s_alt");
		
		var variantBuilder = Variants.builder();
		for (Direction dir : Direction.values())
		{
			for (int rotationIndex = 0; rotationIndex < 4; rotationIndex++)
			{
				ResourceLocation stateModel =
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? model
					: modelAlt;
				int x = dir == Direction.DOWN ? 0
					: dir == Direction.UP ? 180
					: 270 - 90*rotationIndex;
				// don't look too closely at the magic numbers
				int y = switch(dir) {
					case DOWN -> 90 * rotationIndex;
					case UP -> new int[] {180,90,0,270}[rotationIndex];
					case NORTH -> new int[] {0,270,180,270}[rotationIndex];
					case SOUTH -> new int[] {180,90,0,90}[rotationIndex];
					case WEST -> new int[] {270,180,90,180}[rotationIndex];
					case EAST -> new int[] {90,0,270,0}[rotationIndex];
				};
				Quadrant qx = Quadrant.parseJson(x);
				Quadrant qy = Quadrant.parseJson(y);
				variantBuilder.addVariant(List.of(
						PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
						PropertyValue.create(PlateBlock.ROTATION, rotationIndex)),
					BlockStateBuilder.model(stateModel, qx, qy));
			}
		}
		BlockModelDefinition blockState = BlockStateBuilder.variants(variantBuilder);
		LootTable lootTable = simpleLoot(block);
		var blockHelper = BlockDataHelper.create(block, context, blockState, lootTable).localize(name);
		blockHelper.simpleBlockItem();
		return blockHelper;
	}
	
	static BlockDataHelper redstonePlateBlock(String blockPath, String name, DataGenContext context, int redstone, String... recipePattern)
	{
		var blockHelper = plateBlock(blockPath, name, context);
		blockHelper.blockItemWithoutItemModel(id -> new ClientItem(new UnbakedLogicGateModel(id), ClientItem.Properties.DEFAULT)).help(helper -> plateRecipes(helper, context, redstone, recipePattern));
		return blockHelper;
	}
	
	static BlockDataHelper bitwisePlateBlock(String blockPath, String name, String symbolTexture, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		ResourceLocation parent = block instanceof TwoInputBitwiseGateBlock
			? MoreRed.id("block/two_input_bitwise_logic_plate_template")
			: MoreRed.id("block/single_input_bitwise_logic_plate_template");
		ResourceLocation symbolLocation = MoreRed.id("block/" + symbolTexture);
		
		context.models().put(mangle(blockId, "block/%s"), SimpleModel.createWithoutRenderType(parent).addTexture("symbol", symbolLocation));
		context.models().put(mangle(blockId, "block/%s_alt"), SimpleModel.createWithoutRenderType(mangle(parent, "%s_alt")).addTexture("symbol", symbolLocation));
		BlockDataHelper helper = plateBlock(blockPath, name, context);
		helper.tags(MoreRed.Tags.Blocks.BITWISE_GATES);
		helper.blockItemWithoutItemModel().help(h -> h.recipe(mangle(h.id(), "%s_from_soldering"), new SolderingRecipe(new ItemStack(h.item()), List.of(
			sizedIngredient(SMOOTH_STONE_QUARTER_SLABS, 2),
			sizedIngredient(Tags.Items.GEMS_QUARTZ, 1),
			sizedIngredient(Tags.Items.DUSTS_REDSTONE, 1),
			sizedIngredient(MoreRed.Tags.Items.BUNDLED_CABLES, 1)))));
		return helper;
	}
	
	static BlockDataHelper postBlock(String blockPath, String name, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		ResourceLocation blockModel = blockModel(blockId);
		var variants = Variants.builder();
		int[] xs = {0,180,270,90,90,90};
		int[] ys = {0,0,0,0,90,270};
		for (Direction dir : Direction.values())
		{
			int x = xs[dir.ordinal()];
			int y = ys[dir.ordinal()];
			Quadrant qx = Quadrant.parseJson(x);
			Quadrant qy = Quadrant.parseJson(y);
			variants.addVariant(PropertyValue.create(BlockStateProperties.FACING, dir),
				BlockStateBuilder.model(blockModel, qx, qy));
		}
		BlockModelDefinition blockState = BlockStateBuilder.variants(variants);
		BlockDataHelper blockHelper = BlockDataHelper.create(block, context, blockState, simpleLoot(block))
			.localize(name);
		blockHelper.tags(BlockTags.MINEABLE_WITH_PICKAXE);
		return blockHelper;
	}
	
	static LootTable simpleLoot(Block block)
	{
		return LootTable.lootTable()
			.withPool(LootPool.lootPool()
				.add(LootItem.lootTableItem(block))
				.when(ExplosionCondition.survivesExplosion()))
			.build();
	}
	
	static ResourceLocation mangle(ResourceLocation id, String pathFormatter)
	{
		return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), String.format(pathFormatter, id.getPath()));
	}
	
	static ResourceLocation blockId(Block block)
	{
		return block.builtInRegistryHolder().key().location();
	}
	
	static ResourceLocation blockModel(ResourceLocation blockId)
	{
		return mangle(blockId, "block/%s");
	}
	
	static ResourceLocation itemModel(ResourceLocation itemId)
	{
		return mangle(itemId, "item/%s");
	}
	
	static BlockDataHelper hexidecrubrometerBlock(DataGenContext context)
	{
		Block block = MoreRed.get().hexidecrubrometerBlock.get();
		ResourceLocation blockId = MoreRed.get().hexidecrubrometerBlock.getId();
		var variants = Variants.builder();
		for (int power=0; power<16; power++)
		{
			ResourceLocation modelId = mangle(blockModel(blockId), "%s_" + String.valueOf(power));
			SimpleModel simpleModel = SimpleModel.createWithoutRenderType(MoreRed.id("block/hexidecrubrometer_template"))
				.addTexture("display", modelId);
			context.models().put(modelId, simpleModel);
			for (Direction facing : HexidecrubrometerBlock.ROTATION.getPossibleValues())
			{
				int y = switch(facing) {
					case EAST -> 90;
					case SOUTH -> 180;
					case WEST -> 270;
					default -> 0;
				};
				Quadrant qy = Quadrant.parseJson(y);
				for (AttachFace face : AttachFace.values())
				{
					int x = switch(face) {
						case CEILING -> 90;
						case FLOOR -> 270;
						case WALL -> 0;
					};
					Quadrant qx = Quadrant.parseJson(x);
					
					variants.addVariant(List.of(
						PropertyValue.create(HexidecrubrometerBlock.READING_FACE, face),
						PropertyValue.create(HexidecrubrometerBlock.ROTATION, facing),
						PropertyValue.create(HexidecrubrometerBlock.POWER, power)),
						BlockStateBuilder.model(modelId, qx, qy));
				}
			}
		}
		
		var helper = BlockDataHelper.create(block, context, BlockStateBuilder.variants(variants), simpleLoot(block));

		helper.tags(BlockTags.MINEABLE_WITH_PICKAXE);
		helper.blockItem(SimpleModel.createWithoutRenderType(mangle(blockId, "block/%s_0")))
			.recipe(mangle(blockId, "%s_from_soldering"), new SolderingRecipe(new ItemStack(block.asItem()), List.of(
				sizedIngredient(SMOOTH_STONE_QUARTER_SLABS, 8),
				sizedIngredient(Tags.Items.DUSTS_REDSTONE, 9),
				sizedIngredient(Tags.Items.GEMS_QUARTZ, 4))));
		helper.localize("Hexidecrubrometer");
		
		return helper;
	}
	
	static BlockDataHelper switchedPlateBlock(String blockPath, String blockName, DataGenContext context, int redstone, String... recipePattern)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		ResourceLocation model = mangle(blockId, "block/%s");
		ResourceLocation modelAlt = mangle(blockId, "block/%s_alt");
		ResourceLocation switchedModel = mangle(blockId, "block/%s_switched");
		ResourceLocation switchedModelAlt = mangle(blockId, "block/%s_switched_alt");
		
		var variantBuilder = Variants.builder();
		for (Direction dir : Direction.values())
		{
			for (int rotationIndex = 0; rotationIndex < 4; rotationIndex++)
			{
				ResourceLocation stateModel =
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? model
					: modelAlt;
				ResourceLocation stateModelSwitched = 
					dir.getAxis() == Direction.Axis.Y || rotationIndex % 2 == 0
					? switchedModel
					: switchedModelAlt;
				
				int x = dir == Direction.DOWN ? 0
					: dir == Direction.UP ? 180
					: 270 - 90*rotationIndex;
				// don't look too closely at the magic numbers
				int y = switch(dir) {
					case DOWN -> 90 * rotationIndex;
					case UP -> new int[] {180,90,0,270}[rotationIndex];
					case NORTH -> new int[] {0,270,180,270}[rotationIndex];
					case SOUTH -> new int[] {180,90,0,90}[rotationIndex];
					case WEST -> new int[] {270,180,90,180}[rotationIndex];
					case EAST -> new int[] {90,0,270,0}[rotationIndex];
				};
				Quadrant qx = Quadrant.parseJson(x);
				Quadrant qy = Quadrant.parseJson(y);
				variantBuilder.addVariant(List.of(
						PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
						PropertyValue.create(PlateBlock.ROTATION, rotationIndex),
						PropertyValue.create(PlateBlockStateProperties.INPUT_B, false)),
					BlockStateBuilder.model(stateModel, qx, qy));
				variantBuilder.addVariant(List.of(
						PropertyValue.create(PlateBlock.ATTACHMENT_DIRECTION, dir),
						PropertyValue.create(PlateBlock.ROTATION, rotationIndex),
						PropertyValue.create(PlateBlockStateProperties.INPUT_B, true)),
					BlockStateBuilder.model(stateModelSwitched, qx, qy));
			}
		}
		BlockModelDefinition blockState = BlockStateBuilder.variants(variantBuilder);
		
		var blockHelper = BlockDataHelper.create(block, context, blockState, simpleLoot(block))
			.localize(blockName);
		blockHelper.simpleBlockItem(id -> new ClientItem(new UnbakedLogicGateModel(id), ClientItem.Properties.DEFAULT)).help(helper -> switchedPlateRecipes(helper, context, redstone, recipePattern));
		return blockHelper;
	}
	
	static BlockDataHelper wireBlock(String blockPath, String blockName, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		
		Quadrant r0 = Quadrant.R0;
		Quadrant r90 = Quadrant.R90;
		Quadrant r180 = Quadrant.R180;
		Quadrant r270 = Quadrant.R270;

		// generate blockstate json
		ResourceLocation nodeModel = mangle(blockId, "block/%s_node");
		ResourceLocation elbowModel = mangle(blockId, "block/%s_elbow");
		ResourceLocation lineModel = mangle(blockId, "block/%s_line");
		ResourceLocation edgeModel = mangle(blockId, "block/%s_edge");
		BlockModelDefinition blockState = BlockStateBuilder.multipart(Multipart.builder()
			.addWhenApply(When.always().apply(new UnbakedWirePartBlockStateModel(lineModel, edgeModel)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.DOWN, true))
				.apply(BlockStateBuilder.model(nodeModel)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.UP, true))
				.apply(BlockStateBuilder.model(nodeModel, r180, r0)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(nodeModel, r90, r0)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.WEST, true))
				.apply(BlockStateBuilder.model(nodeModel, r90, r90)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.NORTH, true))
				.apply(BlockStateBuilder.model(nodeModel, r90, r180)))
			.addWhenApply(When.when(Case.create(AbstractWireBlock.EAST, true))
				.apply(BlockStateBuilder.model(nodeModel, r90, r270)))
			.addWhenApply(When.when(Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.WEST, true))
				.apply(BlockStateBuilder.model(elbowModel)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.NORTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r0, r90)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.EAST, true))
				.apply(BlockStateBuilder.model(elbowModel, r0, r180)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.DOWN, true)
					.addCondition(AbstractWireBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r0, r270)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.SOUTH, true)
					.addCondition(AbstractWireBlock.WEST, true))
				.apply(BlockStateBuilder.model(elbowModel, r90, r0)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.WEST, true)
					.addCondition(AbstractWireBlock.NORTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r90, r90)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.NORTH, true)
					.addCondition(AbstractWireBlock.EAST, true))
				.apply(BlockStateBuilder.model(elbowModel, r90, r180)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.EAST, true)
					.addCondition(AbstractWireBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r90, r270)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.WEST, true))
				.apply(BlockStateBuilder.model(elbowModel, r180, r0)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.NORTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r180, r90)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.EAST, true))
				.apply(BlockStateBuilder.model(elbowModel, r180, r180)))
			.addWhenApply(When.when(
				Case.builder()
					.addCondition(AbstractWireBlock.UP, true)
					.addCondition(AbstractWireBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(elbowModel, r180, r270))));
		
		LootTable lootTable = LootTable.lootTable()
			.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
				.add(LootItem.lootTableItem(block)
					.apply(() -> WireCountLootFunction.INSTANCE))
				.when(ExplosionCondition.survivesExplosion()))
			.build();
		
		BlockDataHelper helper = BlockDataHelper.create(block, context, blockState, lootTable);
		
		return helper.localize(blockName);
	}

	static BlockDataHelper simpleBlock(String blockPath, String blockName, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		context.lang().add(block, blockName);
		return BlockDataHelper.create(block, context,
			BlockStateBuilder.variants(Variants.always(BlockStateBuilder.model(mangle(blockId, "block/%s")))),
			simpleLoot(block));
	}
	
	static BlockDataHelper tubeBlock(String blockPath, String blockName, DataGenContext context, BlockModelDefinition blockState)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		var blockHelper = BlockDataHelper.create(block, context, blockState, simpleLoot(block))
			.baseModel(SimpleModel.createWithoutRenderType(MoreRed.id("block/tube"))
				.addTexture("all", mangle(blockId, "block/%s")))
			.model("block/%s_extension", SimpleModel.createWithoutRenderType(MoreRed.id("block/tube_extension"))
				.addTexture("all", mangle(blockId, "block/%s")))
			.localize(blockName);
		
		return blockHelper;
	}
	
	static BlockDataHelper sixWayBlock(String blockPath, String blockName, DataGenContext context)
	{
		ResourceLocation blockId = MoreRed.id(blockPath);
		Block block = BuiltInRegistries.BLOCK.getValue(blockId);
		return BlockDataHelper.create(block, context, sixWayBlockState(block), simpleLoot(block))
			.localize(blockName);
	}
	
	static ItemDataHelper plateRecipes(ItemDataHelper helper, DataGenContext context, int redstone, String... pattern)
	{
		Map<Character, Ingredient> patternKey = new HashMap<>();
		for (String line : pattern)
		{
			if (line.contains("#"))
			{
				patternKey.put('#', ingredient(SMOOTH_STONE_QUARTER_SLABS));
			}
			if (line.contains("r"))
			{
				patternKey.put('r', ingredient(Tags.Items.DUSTS_REDSTONE));
			}
			if (line.contains("t"))
			{
				patternKey.put('t', Ingredient.of(Items.REDSTONE_TORCH));
			}
		}
		helper.recipe(mangle(helper.id(), "%s_from_soldering"), 
			new SolderingRecipe(new ItemStack(helper.item()), List.of(
				new SizedIngredient(ingredient(SMOOTH_STONE_QUARTER_SLABS), 1),
				new SizedIngredient(ingredient(Tags.Items.DUSTS_REDSTONE), redstone))))
		.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(pattern), patternKey));
		
		return helper;
	}
	
	static ItemDataHelper switchedPlateRecipes(ItemDataHelper helper, DataGenContext context, int redstone, String... recipePattern)
	{
		helper.recipe(mangle(helper.id(), "%s_from_soldering"), 
			new SolderingRecipe(new ItemStack(helper.item()), List.of(
				sizedIngredient(SMOOTH_STONE_QUARTER_SLABS, 1),
				sizedIngredient(Tags.Items.DUSTS_REDSTONE, redstone),
				sizedIngredient(Tags.Items.INGOTS_IRON, 1))))
		.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(recipePattern), Map.of(
		    '#', ingredient(SMOOTH_STONE_QUARTER_SLABS),
		    'r', ingredient(Tags.Items.DUSTS_REDSTONE),
		    'i', ingredient(Tags.Items.INGOTS_IRON))));
		
		return helper;
	}
	
	static ItemDataHelper spool(String itemPath, String name, DataGenContext context, TagKey<Item> wireIngredientTag)
	{
		ResourceLocation itemId = MoreRed.id(itemPath);
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		return ItemDataHelper.create(item, context, SimpleModel.createWithoutRenderType(ResourceLocation.withDefaultNamespace("item/handheld"))
				.addTexture("layer0", ResourceLocation.withDefaultNamespace("item/stick"))
				.addTexture("layer1", mangle(itemId, "item/%s")))
		.localize(name)
		.help(helper -> helper.recipe(RecipeHelpers.shaped(helper.item(), 1, CraftingBookCategory.REDSTONE, List.of(
		    "rfs",
		    "frf",
		    "sfr"), Map.of(
		    's', ingredient(Tags.Items.RODS_WOODEN),
		    'f', ingredient(Tags.Items.INGOTS_IRON),
		    'r', ingredient(wireIngredientTag)))));
	}
	
	static ItemDataHelper simpleItem(String itemPath, String name, DataGenContext context)
	{
		ResourceLocation itemId = MoreRed.id(itemPath);
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		return ItemDataHelper.create(item, context, SimpleModel.createWithoutRenderType(ResourceLocation.withDefaultNamespace("item/generated"))
			.addTexture("layer0", mangle(itemId, "item/%s")))
			.localize(name);
	}
	
	private static BlockModelDefinition tubeBlockState(Block block)
	{
		ResourceLocation blockModel = BlockDataHelper.blockModel(block);
		ResourceLocation extension = mangle(blockModel, "%s_extension");
		return BlockStateBuilder.multipart(Multipart.builder()
			.addWhenApply(When.always().apply(BlockStateBuilder.model(blockModel)))
			.addWhenApply(When.when(Case.create(TubeBlock.DOWN, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R90, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.UP, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R270, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.NORTH, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.EAST, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R90, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R180, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.WEST, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R270, true)))
			);
	}
	
	private static BlockModelDefinition redstoneTubeBlockState(Block block)
	{
		ResourceLocation blockModel = BlockDataHelper.blockModel(block);
		ResourceLocation offModel = blockModel;
		ResourceLocation onModel = mangle(blockModel, "%s_on");
		ResourceLocation extension = MoreRed.id("block/tube_extension");
		return BlockStateBuilder.multipart(Multipart.builder()
			.addWhenApply(When.when(Case.create(RedstoneTubeBlock.POWERED, false))
				.apply(BlockStateBuilder.model(offModel)))
			.addWhenApply(When.when(Case.create(RedstoneTubeBlock.POWERED, true))
				.apply(BlockStateBuilder.model(onModel)))
			.addWhenApply(When.when(Case.create(TubeBlock.DOWN, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R90, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.UP, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R270, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.NORTH, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R0, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.EAST, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R90, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.SOUTH, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R180, true)))
			.addWhenApply(When.when(Case.create(TubeBlock.WEST, true))
				.apply(BlockStateBuilder.model(extension, Quadrant.R0, Quadrant.R270, true)))
			);
	}
	
	private static BlockModelDefinition sixWayBlockState(Block block)
	{
		var builder = Variants.builder();
		ResourceLocation model = BlockDataHelper.blockModel(block);
		for (Direction facing : Direction.values())
		{
			Quadrant x = switch(facing) {
				case DOWN -> Quadrant.R90;
				case UP -> Quadrant.R270;
				default -> Quadrant.R0;
			};
			Quadrant y = switch(facing) {
				case EAST -> Quadrant.R90;
				case SOUTH -> Quadrant.R180;
				case WEST -> Quadrant.R270;
				default -> Quadrant.R0;
			};
			builder.addVariant(
				PropertyValue.create(BlockStateProperties.FACING, facing),
				BlockStateBuilder.model(model, x, y, true));
		}
		return BlockStateBuilder.variants(builder);
	}
	
	private static Ingredient ingredient(TagKey<Item> tagKey)
	{
		return Ingredient.of(HolderSet.emptyNamed(BuiltInRegistries.ITEM, tagKey));
	}
	
	private static SizedIngredient sizedIngredient(TagKey<Item> tagKey, int count)
	{
		return new SizedIngredient(ingredient(tagKey), count);
	}
}
