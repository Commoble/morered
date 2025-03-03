package net.commoble.morered;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.morered.bitwise_logic.BitwiseGateBlock;
import net.commoble.morered.bitwise_logic.BitwiseGateSignalComponent;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlockEntity;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlockEntity;
import net.commoble.morered.client.ClientProxy;
import net.commoble.morered.plate_blocks.BitwiseLogicFunction;
import net.commoble.morered.plate_blocks.BitwiseLogicFunctions;
import net.commoble.morered.plate_blocks.LatchBlock;
import net.commoble.morered.plate_blocks.LogicFunction;
import net.commoble.morered.plate_blocks.LogicFunctionPlateBlock;
import net.commoble.morered.plate_blocks.LogicFunctionPlateBlock.LogicFunctionPlateBlockFactory;
import net.commoble.morered.plate_blocks.LogicFunctions;
import net.commoble.morered.plate_blocks.PlateBlock;
import net.commoble.morered.plate_blocks.PulseGateBlock;
import net.commoble.morered.soldering.SolderingMenu;
import net.commoble.morered.soldering.SolderingRecipe;
import net.commoble.morered.soldering.SolderingRecipeButtonPacket;
import net.commoble.morered.soldering.SolderingRecipesPacket;
import net.commoble.morered.soldering.SolderingTableBlock;
import net.commoble.morered.transportation.ColoredTubeBlock;
import net.commoble.morered.transportation.DistributorBlock;
import net.commoble.morered.transportation.DistributorBlockEntity;
import net.commoble.morered.transportation.ExtractorBlock;
import net.commoble.morered.transportation.FilterBlock;
import net.commoble.morered.transportation.FilterBlockEntity;
import net.commoble.morered.transportation.FilterMenu;
import net.commoble.morered.transportation.LoaderBlock;
import net.commoble.morered.transportation.LoaderMenu;
import net.commoble.morered.transportation.MultiFilterBlock;
import net.commoble.morered.transportation.MultiFilterBlockEntity;
import net.commoble.morered.transportation.MultiFilterMenu;
import net.commoble.morered.transportation.OsmosisFilterBlock;
import net.commoble.morered.transportation.OsmosisFilterBlockEntity;
import net.commoble.morered.transportation.OsmosisSlimeBlock;
import net.commoble.morered.transportation.PliersItem;
import net.commoble.morered.transportation.RaytraceHelper;
import net.commoble.morered.transportation.RedstoneTubeBlock;
import net.commoble.morered.transportation.RedstoneTubeBlockEntity;
import net.commoble.morered.transportation.ShuntBlock;
import net.commoble.morered.transportation.ShuntBlockEntity;
import net.commoble.morered.transportation.SyncTubesInChunkPacket;
import net.commoble.morered.transportation.TubeBlock;
import net.commoble.morered.transportation.TubeBlockEntity;
import net.commoble.morered.transportation.TubeBreakPacket;
import net.commoble.morered.transportation.TubesInChunk;
import net.commoble.morered.util.BlockSide;
import net.commoble.morered.util.ConfigHelper;
import net.commoble.morered.util.FakeStateLevel;
import net.commoble.morered.wire_post.CableJunctionBlock;
import net.commoble.morered.wire_post.CablePostBlockEntity;
import net.commoble.morered.wire_post.CableRelayBlock;
import net.commoble.morered.wire_post.SlackInterpolator;
import net.commoble.morered.wire_post.SyncPostsInChunkPacket;
import net.commoble.morered.wire_post.WireBreakPacket;
import net.commoble.morered.wire_post.WirePostBlock;
import net.commoble.morered.wire_post.WirePostBlockEntity;
import net.commoble.morered.wire_post.WirePostPlateBlock;
import net.commoble.morered.wire_post.WireSpoolItem;
import net.commoble.morered.wires.AbstractWireBlock;
import net.commoble.morered.wires.BundledCableBlock;
import net.commoble.morered.wires.PoweredWireBlock;
import net.commoble.morered.wires.PoweredWireBlockEntity;
import net.commoble.morered.wires.WireBlockEntity;
import net.commoble.morered.wires.WireBlockItem;
import net.commoble.morered.wires.WireCountLootFunction;
import net.commoble.morered.wires.WirePostSignalComponent;
import net.commoble.morered.wires.WireSignalComponent;
import net.commoble.morered.wires.WireUpdatePacket;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	private static MoreRed instance;
	public static MoreRed get() { return instance; }
	
	public static ResourceLocation id(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, name);
	}

	public static final ServerConfig SERVERCONFIG = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.SERVER, ServerConfig::create);
	public static final ToIntFunction<LevelReader> NO_SOURCE = reader -> 0;

	public final Map<ResourceLocation, DeferredHolder<Block, ? extends LogicFunctionPlateBlock>> logicPlates = new HashMap<>();
	public final Map<ResourceLocation, DeferredHolder<Block, ? extends BitwiseGateBlock>> bitwiseLogicPlates = new HashMap<>();
	
	public final DeferredHolder<Block, SolderingTableBlock> solderingTableBlock;
	public final DeferredHolder<Block, PlateBlock> stonePlateBlock;
	public final DeferredHolder<Block, LatchBlock> latchBlock;
	public final DeferredHolder<Block, PulseGateBlock> pulseGateBlock;
	public final DeferredHolder<Block, WirePostBlock> redwirePostBlock;
	public final DeferredHolder<Block, WirePostPlateBlock> redwireRelayBlock;
	public final DeferredHolder<Block, WirePostPlateBlock> redwireJunctionBlock;
	public final DeferredHolder<Block, HexidecrubrometerBlock> hexidecrubrometerBlock;
	public final DeferredHolder<Block, PoweredWireBlock> redAlloyWireBlock;
	public final DeferredHolder<Block, PoweredWireBlock>[] coloredCableBlocks;
	public final DeferredHolder<Block, BundledCableBlock> bundledCableBlock;
	public final DeferredHolder<Block, CableRelayBlock> cableRelayBlock;
	public final DeferredHolder<Block, CableJunctionBlock> cableJunctionBlock;

	public final DeferredHolder<Block, ColoredTubeBlock>[] coloredTubeBlocks;
	public final DeferredHolder<Block, DistributorBlock> distributorBlock;
	public final DeferredHolder<Block, ExtractorBlock> extractorBlock;
	public final DeferredHolder<Block, FilterBlock> filterBlock;
	public final DeferredHolder<Block, MultiFilterBlock> multiFilterBlock;
	public final DeferredHolder<Block, LoaderBlock> loaderBlock;
	public final DeferredHolder<Block, OsmosisFilterBlock> osmosisFilterBlock;
	public final DeferredHolder<Block, OsmosisSlimeBlock> osmosisSlimeBlock;
	public final DeferredHolder<Block, RedstoneTubeBlock> redstoneTubeBlock;
	public final DeferredHolder<Block, ShuntBlock> shuntBlock;
	public final DeferredHolder<Block, TubeBlock> tubeBlock;

	public final DeferredHolder<Item, WireSpoolItem> redwireSpoolItem;
	public final DeferredHolder<Item, Item> bundledCableSpoolItem;
	public final DeferredHolder<Item, Item> redAlloyIngotItem;
	public final DeferredHolder<Item, PliersItem> tubingPliers;
	
	public final DeferredHolder<CreativeModeTab, CreativeModeTab> tab;

	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirePostBlockEntity>> wirePostBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<CablePostBlockEntity>> cablePostBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireBlockEntity>> wireBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredWireBlockEntity>> poweredWireBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleInputBitwiseGateBlockEntity>> singleInputBitwiseGateBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputBitwiseGateBlockEntity>> twoInputBitwiseGateBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistributorBlockEntity>> distributorEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilterBlockEntity>> filterEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiFilterBlockEntity>> multiFilterEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<OsmosisFilterBlockEntity>> osmosisFilterEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneTubeBlockEntity>> redstoneTubeEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuntBlockEntity>> shuntEntity;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> tubeEntity;

	public final DeferredHolder<MenuType<?>, MenuType<SolderingMenu>> solderingMenuType;
	public final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolderingRecipe>> solderingSerializer;
	public final DeferredHolder<RecipeType<?>, RecipeType<SolderingRecipe>> solderingRecipeType;

	public final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> filterMenu;
	public final DeferredHolder<MenuType<?>, MenuType<MultiFilterMenu>> multiFilterMenu;
	public final DeferredHolder<MenuType<?>, MenuType<LoaderMenu>> loaderMenu;
	
	public final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<WireCountLootFunction>> wireCountLootFunction;
	
	public final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> postsInChunkAttachment;
	public final DeferredHolder<AttachmentType<?>, AttachmentType<Map<BlockPos, VoxelShape>>> voxelCacheAttachment;
	public final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> tubesInChunkAttachment;
	
	public final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> spooledPostComponent;
	public final DeferredHolder<DataComponentType<?>, DataComponentType<BlockSide>> plieredTubeDataComponent;
	
	
	
	
	@SuppressWarnings("unchecked")
	public MoreRed(IEventBus modBus)
	{
		instance = this;
		
		IEventBus forgeBus = NeoForge.EVENT_BUS;

		DeferredRegister<Block> blocks = defreg(modBus, Registries.BLOCK);
		DeferredRegister<Item> items = defreg(modBus, Registries.ITEM);
		DeferredRegister<CreativeModeTab> tabs = defreg(modBus, Registries.CREATIVE_MODE_TAB);
		DeferredRegister<BlockEntityType<?>> blockEntityTypes = defreg(modBus, Registries.BLOCK_ENTITY_TYPE);
		DeferredRegister<MenuType<?>> menuTypes = defreg(modBus, Registries.MENU);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = defreg(modBus, Registries.RECIPE_SERIALIZER);
		DeferredRegister<RecipeType<?>> recipeTypes = defreg(modBus, Registries.RECIPE_TYPE);
		DeferredRegister<LootItemFunctionType<?>> lootFunctions = defreg(modBus, Registries.LOOT_FUNCTION_TYPE);
		DeferredRegister<AttachmentType<?>> attachmentTypes = defreg(modBus, NeoForgeRegistries.Keys.ATTACHMENT_TYPES);
		DeferredRegister<DataComponentType<?>> dataComponentTypes = defreg(modBus, Registries.DATA_COMPONENT_TYPE);
		var signalComponentTypes = defreg(modBus, ExMachinaRegistries.SIGNAL_COMPONENT_TYPE);
		
		solderingTableBlock = registerBlockItem(blocks, items, Names.SOLDERING_TABLE,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.5F).noOcclusion(),
			SolderingTableBlock::new);
		stonePlateBlock = registerBlockItem(blocks, items, Names.STONE_PLATE,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD),
			PlateBlock::new);
		latchBlock = registerBlockItem(blocks, items, Names.LATCH,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD),
			LatchBlock::new);
		pulseGateBlock = registerBlockItem(blocks, items, Names.PULSE_GATE,
			() ->BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD),
			PulseGateBlock::new);
		redwirePostBlock = registerBlockItem(blocks, items, Names.REDWIRE_POST,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			WirePostBlock::new);
		redwireRelayBlock = registerBlockItem(blocks, items, Names.REDWIRE_RELAY,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			properties -> new WirePostPlateBlock(properties, WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
		redwireJunctionBlock = registerBlockItem(blocks, items, Names.REDWIRE_JUNCTION,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			properties -> new WirePostPlateBlock(properties, WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
		hexidecrubrometerBlock = registerBlockItem(blocks, items, Names.HEXIDECRUBROMETER,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			HexidecrubrometerBlock::new);
		cableRelayBlock = registerBlockItem(blocks, items, Names.CABLE_RELAY,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			CableRelayBlock::new);
		cableJunctionBlock = registerBlockItem(blocks, items, Names.CABLE_JUNCTION,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
			CableJunctionBlock::new);
		
		redAlloyWireBlock = registerBlockItem(blocks, items, Names.RED_ALLOY_WIRE, 
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).noCollission().instabreak(),
			PoweredWireBlock::createRedAlloyWireBlock,
			Item.Properties::new,
			WireBlockItem::new);
		coloredCableBlocks = Util.make((DeferredHolder<Block, PoweredWireBlock>[])new DeferredHolder[16], array ->
			Arrays.setAll(array, i -> registerBlockItem(blocks, items, Names.COLORED_CABLES_BY_COLOR[i],
				() -> BlockBehaviour.Properties.of().mapColor(DyeColor.values()[i]).pushReaction(PushReaction.DESTROY).noCollission().instabreak(),
				properties -> PoweredWireBlock.createColoredCableBlock(properties, DyeColor.values()[i]),
				Item.Properties::new,
				WireBlockItem::new)));
		bundledCableBlock = registerBlockItem(blocks, items, Names.BUNDLED_CABLE,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).pushReaction(PushReaction.DESTROY).noCollission().instabreak(),
			BundledCableBlock::new,
			Item.Properties::new,
			WireBlockItem::new);
		
		registerLogicGateType(blocks, items, Names.DIODE, LogicFunctions.INPUT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(blocks, items, Names.NOT_GATE, LogicFunctions.NOT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(blocks, items, Names.NOR_GATE, LogicFunctions.NOR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, Names.NAND_GATE, LogicFunctions.NAND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, Names.OR_GATE, LogicFunctions.OR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, Names.AND_GATE, LogicFunctions.AND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, Names.XOR_GATE, LogicFunctions.XOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, Names.XNOR_GATE, LogicFunctions.XNOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, Names.MULTIPLEXER, LogicFunctions.MULTIPLEX, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, Names.TWO_INPUT_AND_GATE, LogicFunctions.AND_2, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, Names.TWO_INPUT_NAND_GATE, LogicFunctions.NAND_2, LogicFunctionPlateBlock.T_INPUTS);
		
		// bitwise logic gates store state in a TE instead of block properties
		// they don't need to have their properties defined on construction but they do need to be registered to the TE type they use
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, SingleInputBitwiseGateBlock> singleInput = SingleInputBitwiseGateBlock::new;
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, TwoInputBitwiseGateBlock> twoInputs = TwoInputBitwiseGateBlock::new;
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_DIODE, BitwiseLogicFunctions.INPUT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_NOT_GATE, BitwiseLogicFunctions.NOT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_OR_GATE, BitwiseLogicFunctions.OR, twoInputs);
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_AND_GATE, BitwiseLogicFunctions.AND_2, twoInputs);
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_XOR_GATE, BitwiseLogicFunctions.XOR_AC, twoInputs);
		registerBitwiseLogicGateType(blocks, items, Names.BITWISE_XNOR_GATE, BitwiseLogicFunctions.XNOR_AC, twoInputs);
		List<Supplier<? extends TubeBlock>> tubeBlocksWithTubeBlockEntity = new ArrayList<>();
		
		this.tubeBlock = registerBlockItem(blocks, items, Names.TUBE,
			() -> BlockBehaviour.Properties.of()
				.instrument(NoteBlockInstrument.DIDGERIDOO)
				.mapColor(MapColor.TERRACOTTA_YELLOW)
				.strength(0.4F)
				.sound(SoundType.METAL),
			properties -> new TubeBlock(id("block/tube"), properties));
		this.shuntBlock = registerBlockItem(blocks, items, Names.SHUNT,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.TERRACOTTA_YELLOW)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				ShuntBlock::new);
		this.loaderBlock = registerBlockItem(blocks, items, Names.LOADER,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				LoaderBlock::new);
		this.redstoneTubeBlock = registerBlockItem(blocks, items, Names.REDSTONE_TUBE,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.GOLD)
				.strength(0.4F)
				.sound(SoundType.METAL),
				properties -> new RedstoneTubeBlock(id("block/tube"), properties));
		this.extractorBlock = registerBlockItem(blocks, items, Names.EXTRACTOR,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.TERRACOTTA_YELLOW)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				ExtractorBlock::new);
		this.filterBlock = registerBlockItem(blocks, items, Names.FILTER,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.TERRACOTTA_YELLOW)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				FilterBlock::new);
		this.multiFilterBlock = registerBlockItem(blocks, items, Names.MULTIFILTER,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				MultiFilterBlock::new);
		this.osmosisFilterBlock = registerBlockItem(blocks, items, Names.OSMOSIS_FILTER,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.GRASS)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				OsmosisFilterBlock::new);
		this.osmosisSlimeBlock = registerBlock(blocks, Names.OSMOSIS_SLIME, BlockBehaviour.Properties::of, OsmosisSlimeBlock::new);
		this.distributorBlock = registerBlockItem(blocks, items, Names.DISTRIBUTOR,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD)
				.strength(2F, 6F)
				.sound(SoundType.METAL),
				DistributorBlock::new);
		this.coloredTubeBlocks = Util.make((DeferredHolder<Block, ColoredTubeBlock>[])new DeferredHolder[16], array -> Arrays.setAll(array, i -> {
			DyeColor color = DyeColor.values()[i];
			String name = Names.COLORED_TUBE_NAMES[i];
			DeferredHolder<Block, ColoredTubeBlock> block = registerBlockItem(blocks, items, name,
				() -> BlockBehaviour.Properties.of()
				.mapColor(color)
				.instrument(NoteBlockInstrument.DIDGERIDOO)
				.strength(0.4F)
				.sound(SoundType.METAL),
				properties -> new ColoredTubeBlock(id("block/" + name),	color, properties));
			tubeBlocksWithTubeBlockEntity.add(block);
			return block;
		}));
		tubeBlocksWithTubeBlockEntity.add(this.tubeBlock);

		// notblock items
		redwireSpoolItem = registerItem(items, Names.REDWIRE_SPOOL, () -> new Item.Properties().durability(64), properties -> new WireSpoolItem(properties, MoreRed.Tags.Blocks.REDWIRE_POSTS));
		bundledCableSpoolItem = registerItem(items, Names.BUNDLED_CABLE_SPOOL, () -> new Item.Properties().durability(64), properties -> new WireSpoolItem(properties, MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS));
		redAlloyIngotItem = registerItem(items, Names.RED_ALLOY_INGOT, Item.Properties::new, Item::new);
		tubingPliers = registerItem(items, Names.PLIERS, () -> new Item.Properties().durability(128), PliersItem::new);

		ResourceLocation tabIconId = id(Names.NOR_GATE);
		this.tab = tabs.register(MoreRed.MODID, () -> CreativeModeTab.builder()
			.icon(() -> new ItemStack(this.logicPlates.get(tabIconId).get()))
			.title(Component.translatable("itemGroup.morered"))
			.displayItems((params, output) -> output.acceptAll(items.getEntries().stream().map(rob -> new ItemStack(rob.get())).toList()))
			.build()
			);
		
		wirePostBeType = blockEntityTypes.register(Names.WIRE_POST,
			() -> new BlockEntityType<>(WirePostBlockEntity::new,
				redwirePostBlock.get(),
				redwireRelayBlock.get(),
				redwireJunctionBlock.get()));
		cablePostBeType = blockEntityTypes.register(Names.CABLE_RELAY,
			() -> new BlockEntityType<>(CablePostBlockEntity::new,
				cableRelayBlock.get(),
				cableJunctionBlock.get())
			);
		wireBeType = blockEntityTypes.register(Names.WIRE,
			() -> new BlockEntityType<>(WireBlockEntity::new,
				bundledCableBlock.get())
			);
		poweredWireBeType = blockEntityTypes.register(Names.POWERED_WIRE,
			() -> new BlockEntityType<>(PoweredWireBlockEntity::new,
				Stream.concat(
					Stream.of(redAlloyWireBlock),
					Arrays.stream(coloredCableBlocks))
				.map(DeferredHolder::get)
				.toArray(Block[]::new))
			);
		singleInputBitwiseGateBeType = blockEntityTypes.register(Names.SINGLE_INPUT_BITWISE_GATE,
			() -> new BlockEntityType<>(SingleInputBitwiseGateBlockEntity::create,
				Util.make(() ->
				{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
					return this.bitwiseLogicPlates.values().stream()
						.map(rob -> rob.get())
						.toArray(Block[]::new);
				}))
			);
		twoInputBitwiseGateBeType = blockEntityTypes.register(Names.TWO_INPUT_BITWISE_GATE,
			() -> new BlockEntityType<>(TwoInputBitwiseGateBlockEntity::create,
				Util.make(() ->
				{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
					return this.bitwiseLogicPlates.values().stream()
						.map(rob -> rob.get())
						.toArray(Block[]::new);
				}))
			);
		this.tubeEntity = blockEntityTypes.register(Names.TUBE,
			() -> new BlockEntityType<>(
					TubeBlockEntity::new,
					tubeBlocksWithTubeBlockEntity.stream()
						.map(Supplier::get)
						.toArray(TubeBlock[]::new))
				);
		this.shuntEntity = blockEntityTypes.register(Names.SHUNT,
			() -> new BlockEntityType<>(ShuntBlockEntity::new, shuntBlock.get()));
		this.redstoneTubeEntity = blockEntityTypes.register(Names.REDSTONE_TUBE,
			() -> new BlockEntityType<>(RedstoneTubeBlockEntity::new, redstoneTubeBlock.get()));
		this.filterEntity = blockEntityTypes.register(Names.FILTER,
			() -> new BlockEntityType<>(FilterBlockEntity::new, filterBlock.get()));
		this.multiFilterEntity = blockEntityTypes.register(Names.MULTIFILTER,
			() -> new BlockEntityType<>(MultiFilterBlockEntity::new, multiFilterBlock.get()));
		this.osmosisFilterEntity = blockEntityTypes.register(Names.OSMOSIS_FILTER,
			() -> new BlockEntityType<>(OsmosisFilterBlockEntity::new, osmosisFilterBlock.get()));
		this.distributorEntity = blockEntityTypes.register(Names.DISTRIBUTOR,
			() -> new BlockEntityType<>(DistributorBlockEntity::new, distributorBlock.get()));

		solderingSerializer = recipeSerializers.register(Names.SOLDERING_RECIPE,
			() -> new SimpleRecipeSerializer<SolderingRecipe>(SolderingRecipe.CODEC, SolderingRecipe.STREAM_CODEC));
		solderingRecipeType = recipeTypes.register(Names.SOLDERING_RECIPE, () -> RecipeType.simple(id(Names.SOLDERING_RECIPE)));
		solderingMenuType = menuTypes.register(Names.SOLDERING_TABLE,
			() -> new MenuType<>(SolderingMenu::getClientContainer, FeatureFlags.VANILLA_SET));
		this.loaderMenu = menuTypes.register(Names.LOADER, () -> new MenuType<>(LoaderMenu::new, FeatureFlags.VANILLA_SET));
		this.filterMenu = menuTypes.register(Names.FILTER, () -> new MenuType<>(FilterMenu::createClientMenu, FeatureFlags.VANILLA_SET));
		this.multiFilterMenu = menuTypes.register(Names.MULTIFILTER, () -> new MenuType<>(MultiFilterMenu::clientMenu, FeatureFlags.VANILLA_SET));

		postsInChunkAttachment = attachmentTypes.register(Names.POSTS_IN_CHUNK, () -> AttachmentType.<Set<BlockPos>>builder(() -> new HashSet<>())
			.serialize(BlockPos.CODEC.listOf().xmap(HashSet::new, List::copyOf))
			.build());
		voxelCacheAttachment = attachmentTypes.register(Names.VOXEL_CACHE, () -> AttachmentType.<Map<BlockPos,VoxelShape>>builder(() -> new HashMap<>())
			.build());
		this.tubesInChunkAttachment = attachmentTypes.register(Names.TUBES_IN_CHUNK, () -> AttachmentType.<Set<BlockPos>>builder(() -> new HashSet<>())
			.serialize(TubesInChunk.TUBE_SET_CODEC)
			.build());

		spooledPostComponent = dataComponentTypes.register(Names.SPOOLED_POST, () -> DataComponentType.<BlockPos>builder()
			.networkSynchronized(BlockPos.STREAM_CODEC)
			.build());
		this.plieredTubeDataComponent = dataComponentTypes.register(Names.PLIERED_TUBE, () -> DataComponentType.<BlockSide>builder()
			.networkSynchronized(BlockSide.STREAM_CODEC)
			.build());
		
		wireCountLootFunction = lootFunctions.register(Names.WIRE_COUNT, () -> new LootItemFunctionType<>(WireCountLootFunction.CODEC));

		BiConsumer<ResourceKey<MapCodec<? extends SignalComponent>>, MapCodec<? extends SignalComponent>> registerSignalComponent = (key,codec) -> signalComponentTypes.register(key.location().getPath(), () -> codec);

		registerSignalComponent.accept(WireSignalComponent.RESOURCE_KEY, WireSignalComponent.CODEC);
		registerSignalComponent.accept(WirePostSignalComponent.RESOURCE_KEY, WirePostSignalComponent.CODEC);
		registerSignalComponent.accept(BitwiseGateSignalComponent.RESOURCE_KEY, BitwiseGateSignalComponent.CODEC);

		// menu types
		
		modBus.addListener(this::onRegisterPackets);
		modBus.addListener(this::onRegisterCapabilities);
		
		forgeBus.addListener(EventPriority.LOW, this::onUseItemOnBlock);
		forgeBus.addListener(EventPriority.LOW, this::onLeftClickBlock);
		forgeBus.addListener(this::onUseItemOnBlock);
		forgeBus.addListener(this::onChunkWatch);
		forgeBus.addListener(this::onDataPackSyncEvent);
		
		// add layer of separation to client stuff so we don't break servers
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientProxy.addClientListeners(modBus, forgeBus);
		}
	}
	
	public static class Tags
	{
		public static class Blocks
		{
			private static TagKey<Block> tag(String name) { return TagKey.create(Registries.BLOCK, id(name)); }
			
			public static final TagKey<Block> BUNDLED_CABLE_POSTS = tag(Names.CABLE_POSTS);
			public static final TagKey<Block> COLORED_CABLES = tag(Names.COLORED_CABLES);
			public static final TagKey<Block> REDWIRE_POSTS = tag(Names.REDWIRE_POSTS);
			public static final TagKey<Block> BITWISE_GATES = tag(Names.BITWISE_GATES);
			
			public static final TagKey<Block> WIRABLE_CUBE = tag("wirable/cube");
			public static final TagKey<Block> WIRABLE_FLOOR = tag("wirable/floor");
			public static final TagKey<Block> WIRABLE_INVERTED_WALL_FLOOR_CEILING = tag("wirable/inverted_wall_floor_ceiling");
			public static final TagKey<Block> WIRABLE_WIRE_POSTS = tag("wirable/wire_posts");
			public static final TagKey<Block> WIRABLE = tag("wirable/wires");
			public static final TagKey<Block> WIRABLE_BITWISE_GATES = tag("wirable/bitwise_gates");
			
			public static final TagKey<Block> COLORED_TUBES = tag(Names.COLORED_TUBES);
			public static final TagKey<Block> TUBES = tag(Names.TUBES);
		}
		
		public static class Items
		{
			private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, id(name)); }
			
			public static final TagKey<Item> BUNDLED_CABLES = tag(Names.BUNDLED_CABLES);
			public static final TagKey<Item> COLORED_CABLES = tag(Names.COLORED_CABLES);
			public static final TagKey<Item> COLORED_TUBES = tag(Names.COLORED_TUBES);
			public static final TagKey<Item> CABLES = tag(Names.CABLES);
			public static final TagKey<Item> RED_ALLOY_WIRES = tag(Names.RED_ALLOY_WIRES);
			public static final TagKey<Item> RED_ALLOYABLE_INGOTS = tag(Names.RED_ALLOYABLE_INGOTS);
			public static final TagKey<Item> TUBES = tag(Names.TUBES);
		}
	}
	
	private void onRegisterPackets(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar r = event.registrar("6.0.0");
		r.playToClient(SolderingRecipesPacket.TYPE, SolderingRecipesPacket.STREAM_CODEC, SolderingRecipesPacket::handle);
		r.playToServer(SolderingRecipeButtonPacket.TYPE, SolderingRecipeButtonPacket.STREAM_CODEC, SolderingRecipeButtonPacket::handle);
		r.playToClient(WireBreakPacket.TYPE, WireBreakPacket.STREAM_CODEC, WireBreakPacket::handle);
		r.playToClient(SyncPostsInChunkPacket.TYPE, SyncPostsInChunkPacket.STREAM_CODEC, SyncPostsInChunkPacket::handle);
		r.playToClient(WireUpdatePacket.TYPE, WireUpdatePacket.STREAM_CODEC, WireUpdatePacket::handle);
		r.playToServer(IsWasSprintPacket.TYPE, IsWasSprintPacket.STREAM_CODEC, IsWasSprintPacket::handle);
		r.playToClient(TubeBreakPacket.TYPE, TubeBreakPacket.STREAM_CODEC, TubeBreakPacket::handle);
		r.playToClient(SyncTubesInChunkPacket.TYPE, SyncTubesInChunkPacket.STREAM_CODEC, SyncTubesInChunkPacket::handle);
	}
	
	private void onRegisterCapabilities(RegisterCapabilitiesEvent event)
	{
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.distributorEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.filterEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.multiFilterEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.osmosisFilterEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.redstoneTubeEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.shuntEntity.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, this.tubeEntity.get(), (be,side) -> be.getItemHandler(side));
	}
	
	@SuppressWarnings("deprecation")
	private void onUseItemOnBlock(UseItemOnBlockEvent event)
	{
		UseOnContext useContext = event.getUseOnContext();
		ItemStack stack = useContext.getItemInHand();
		if (event.getUsePhase() == UsePhase.ITEM_AFTER_BLOCK && stack.getItem() instanceof BlockItem blockItem)
		{
			Level level = useContext.getLevel();
			BlockPlaceContext placeContext = new BlockPlaceContext(useContext);
			BlockPos placePos = placeContext.getClickedPos(); // getClickedPos is a misnomer, this is the position the block is placed at
			BlockState placementState = blockItem.getPlacementState(placeContext);
			if (placementState == null)
			{
				return; // placement state is null when the block couldn't be placed there anyway
			}
			
			// get relevant chunk positions near pos
			double range = Mth.absMax(SERVERCONFIG.maxWirePostConnectionRange().getAsDouble(), SERVERCONFIG.maxTubeConnectionRange().getAsDouble());
			ChunkPos nearbyChunkPos = new ChunkPos(placePos);
			int chunkRange = (int) Math.ceil(range/16D);
			Set<ChunkPos> chunkPositions = new HashSet<>();
			for (int xOff = -chunkRange; xOff <= chunkRange; xOff++)
			{
				for (int zOff = -chunkRange; zOff <= chunkRange; zOff++)
				{
					chunkPositions.add(new ChunkPos(nearbyChunkPos.x + xOff, nearbyChunkPos.z + zOff));
				}
			}
			
			for (ChunkPos chunkPos : chunkPositions)
			{
				if (level.hasChunkAt(chunkPos.getWorldPosition()))
				{
					LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
					
					// check wires blocking placement
					Set<BlockPos> posts = chunk.getData(this.postsInChunkAttachment.get());
					if (posts != null)
					{
						Set<BlockPos> checkedPostPositions = new HashSet<BlockPos>();
						for (BlockPos postPos : posts)
						{
							BlockEntity be = level.getBlockEntity(postPos);
							if (be instanceof WirePostBlockEntity wire)
							{
								Vec3 hit = SlackInterpolator.doesBlockStateIntersectAnyWireOfPost(new FakeStateLevel(level, placePos, placementState), postPos, placePos, placementState, wire.getRemoteConnectionBoxes(), checkedPostPositions);
								if (hit != null)
								{
									Player player = placeContext.getPlayer();
									if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel)
									{
										serverPlayer.connection.send(new ClientboundSetEquipmentPacket(serverPlayer.getId(), ImmutableList.of(Pair.of(EquipmentSlot.MAINHAND, serverPlayer.getItemInHand(InteractionHand.MAIN_HAND)))));
										serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
									}
									else if (level.isClientSide)
									{
										level.addParticle(DustParticleOptions.REDSTONE, hit.x, hit.y, hit.z, 0.05D, 0.05D, 0.05D);
									}
									
									if (player != null)
									{
										player.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
									}
									event.cancelWithResult(InteractionResult.SUCCESS);
									return;
								}
								else
								{
									checkedPostPositions.add(postPos.immutable());
								}
							}
						}
					}
					
					// check tubes blocking placement
					Set<BlockPos> checkedTubePositions = new HashSet<BlockPos>();
					for (BlockPos tubePos : TubesInChunk.getTubesInChunkIfLoaded(level, chunkPos))
					{
						if (level.getBlockEntity(tubePos) instanceof TubeBlockEntity tube)
						{
							Vec3 hit = RaytraceHelper.doesBlockStateIntersectTubeConnections(tube.getBlockPos(), placePos, new FakeStateLevel(level, placePos, placementState), placementState, checkedTubePositions, tube.getRemoteConnections());
//							Vec3 hit = RaytraceHelper.doesBlockStateIntersectTubeConnections(tube.getBlockPos(), placePos, level, placementState, checkedTubePositions, tube.getRemoteConnections());
							if (hit != null)
							{
								Player player = placeContext.getPlayer();
								if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel)
								{
									serverPlayer.connection.send(new ClientboundSetEquipmentPacket(serverPlayer.getId(), ImmutableList.of(Pair.of(EquipmentSlot.MAINHAND, serverPlayer.getItemInHand(InteractionHand.MAIN_HAND)))));
									serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
								}
								else if (level.isClientSide)
								{
									level.addParticle(DustParticleOptions.REDSTONE, hit.x, hit.y, hit.z, 0.05D, 0.05D, 0.05D);
								}
								
								if (player != null)
								{
									player.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
								}
								event.cancelWithResult(InteractionResult.SUCCESS);
								return;
							}
							else
							{
								checkedTubePositions.add(tubePos);
							}
						}
					}
				}
			}
		}
	}
	
	private void onLeftClickBlock(LeftClickBlock event)
	{
		if (event.getAction() != LeftClickBlock.Action.START)
			return;
		
		Level level = event.getLevel();
		if (!(level instanceof ServerLevel serverLevel))
			return;
		
		Player player = event.getEntity();
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (!(block instanceof AbstractWireBlock wireBlock))
			return;
		
		// override event from here
		event.setCanceled(true);
		
		// we still have to redo a few of the existing checks
		if (!serverPlayer.canInteractWithBlock(pos, 1.0)
			|| (pos.getY() >= serverLevel.getMaxY() || pos.getY() < serverLevel.getMinY())
			|| !serverLevel.mayInteract(serverPlayer, pos) // checks spawn protection and world border
			|| CommonHooks.fireBlockBreak(serverLevel, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos, state).isCanceled()
			|| serverPlayer.blockActionRestricted(serverLevel, pos, serverPlayer.gameMode.getGameModeForPlayer()))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		Direction hitNormal = event.getFace();
		Direction destroySide = hitNormal.getOpposite();
		if (serverPlayer.isCreative())
		{
			wireBlock.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, false);
			return;
		}
		if (!state.canHarvestBlock(serverLevel, pos, serverPlayer))
		{
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			return;
		}
		wireBlock.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, true);
	}
	
	// fired on the server just after vanilla chunk data is sent to the client
	private void onChunkWatch(ChunkWatchEvent.Watch event)
	{
		ServerPlayer player = event.getPlayer();
		LevelChunk chunk = event.getChunk();
		ChunkPos pos = chunk.getPos();
		Set<BlockPos> postsInChunk = chunk.getData(postsInChunkAttachment.get());
		if (postsInChunk != null && !postsInChunk.isEmpty())
		{
			PacketDistributor.sendToPlayer(player, new SyncPostsInChunkPacket(pos, postsInChunk));
		}
		PacketDistributor.sendToPlayer(player, new SyncTubesInChunkPacket(pos, TubesInChunk.getTubesInChunk(chunk)));
	}
	
	private void onDataPackSyncEvent(OnDatapackSyncEvent event)
	{
		// we need to sync our recipes since vanilla doesn't anymore
		// on player login, event fires after tag sync
		// on reload, event fires BEFORE tag sync
		// until neoforge fixes that we'll use a mixin to sync recipes on reload
		ServerPlayer player = event.getPlayer();
		if (player != null)
		{
			PacketDistributor.sendToPlayer(player, SolderingRecipesPacket.create(player.getServer().getRecipeManager()));
		}
	}
	
	private static <T> DeferredRegister<T> defreg(IEventBus modBus, ResourceKey<Registry<T>> registryKey)
	{
		var reg = DeferredRegister.create(registryKey, MODID);
		reg.register(modBus);
		return reg;
	}
	
	private static <BLOCK extends Block> DeferredHolder<Block, BLOCK> registerBlock(
		DeferredRegister<Block> blocks,
		String name,
		Supplier<BlockBehaviour.Properties> blockProperties,
		Function<BlockBehaviour.Properties, ? extends BLOCK> blockFactory)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blocks.getNamespace(), name);
		return blocks.register(name, () -> blockFactory.apply(blockProperties.get().setId(ResourceKey.create(Registries.BLOCK, id))));
	}
	
	private static <ITEM extends Item> DeferredHolder<Item, ITEM> registerItem(
		DeferredRegister<Item> items,
		String name,
		Supplier<Item.Properties> itemProperties,
		Function<Item.Properties, ? extends ITEM> itemFactory)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(items.getNamespace(), name);
		return items.register(name,  () -> itemFactory.apply(itemProperties.get().setId(ResourceKey.create(Registries.ITEM, id))));
	}
	
	private static <BLOCK extends Block, ITEM extends BlockItem> DeferredHolder<Block, BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<BlockBehaviour.Properties> blockProperties,
		Function<BlockBehaviour.Properties, ? extends BLOCK> blockFactory,
		Supplier<Item.Properties> itemProperties,
		BiFunction<? super BLOCK, Item.Properties, ? extends ITEM> itemFactory)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blocks.getNamespace(), name);
		DeferredHolder<Block, BLOCK> block = blocks.register(name, () -> blockFactory.apply(blockProperties.get().setId(ResourceKey.create(Registries.BLOCK, id))));;
		items.register(name, () -> itemFactory.apply(block.get(), itemProperties.get().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
		return block;
	}
	
	private static <BLOCK extends Block> DeferredHolder<Block, BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<BlockBehaviour.Properties> blockProperties,
		Function<BlockBehaviour.Properties, ? extends BLOCK> blockFactory)
	{
		return registerBlockItem(blocks, items, name, blockProperties, blockFactory, () -> new Item.Properties(), BlockItem::new);
	}
	
	public DeferredHolder<Block, LogicFunctionPlateBlock> registerLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		LogicFunctionPlateBlockFactory factory)
	{
		DeferredHolder<Block, LogicFunctionPlateBlock> blockGetter = registerBlockItem(blocks, items, name,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD),
			properties -> factory.makeBlock(function, properties));
		logicPlates.put(blockGetter.getId(), blockGetter);
		return blockGetter;
	}
	
	public <B extends BitwiseGateBlock> DeferredHolder<Block, B> registerBitwiseLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		BitwiseLogicFunction function,
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, B> blockFactory)
	{
		DeferredHolder<Block, B> rob = registerBlockItem(blocks, items, name,
			() -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD),
			properties -> blockFactory.apply(properties, function));
		bitwiseLogicPlates.put(rob.getId(), rob);
		return rob;
	}
}
