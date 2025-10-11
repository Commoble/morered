package net.commoble.morered;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.morered.bitwise_logic.BitwiseGateBlock;
import net.commoble.morered.bitwise_logic.BitwiseGateSignalComponent;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlockEntity;
import net.commoble.morered.bitwise_logic.ThreeInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.ThreeInputBitwiseGateBlockEntity;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlockEntity;
import net.commoble.morered.mechanisms.AirFoilBlock;
import net.commoble.morered.mechanisms.AxleBlock;
import net.commoble.morered.mechanisms.ClutchBlock;
import net.commoble.morered.mechanisms.GearBlock;
import net.commoble.morered.mechanisms.GearBlockItem;
import net.commoble.morered.mechanisms.GearsBlock;
import net.commoble.morered.mechanisms.GearsLootEntry;
import net.commoble.morered.mechanisms.GearshifterBlock;
import net.commoble.morered.mechanisms.StonemillBlock;
import net.commoble.morered.mechanisms.StonemillBlock.StonemillData;
import net.commoble.morered.mechanisms.StonemillMenu;
import net.commoble.morered.mechanisms.Wind;
import net.commoble.morered.mechanisms.WindCatcherBlockItem;
import net.commoble.morered.mechanisms.WindcatcherBlock;
import net.commoble.morered.mechanisms.WindcatcherColors;
import net.commoble.morered.mechanisms.WindcatcherDyeRecipe;
import net.commoble.morered.mechanisms.WindcatcherRecipe;
import net.commoble.morered.mechanisms.WoodSets;
import net.commoble.morered.mechanisms.WoodSets.WoodSet;
import net.commoble.morered.plate_blocks.AlternatorBlock;
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
import net.commoble.morered.util.EightGroup;
import net.commoble.morered.util.FakeStateLevel;
import net.commoble.morered.util.MoreCodecs;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	
	public static ResourceLocation id(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, name);
	}
	
	public static final ServerConfig SERVERCONFIG = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.SERVER, ServerConfig::create);
	public static final ToIntFunction<LevelReader> NO_SOURCE = reader -> 0;
	private static final BlockBehaviour.StatePredicate NEVER_STATE_PREDICATE = ($,$$,$$$) -> false;
	
	private static final DeferredRegister<Block> BLOCKS = defreg(Registries.BLOCK);
	private static final DeferredRegister<Item> ITEMS = defreg(Registries.ITEM);
	private static final DeferredRegister<CreativeModeTab> TABS = defreg(Registries.CREATIVE_MODE_TAB);
	private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = defreg(Registries.BLOCK_ENTITY_TYPE);
	private static final DeferredRegister<MenuType<?>> MENU_TYPES = defreg(Registries.MENU);
	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = defreg(Registries.RECIPE_SERIALIZER);
	private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = defreg(Registries.RECIPE_TYPE);
	private static final DeferredRegister<LootPoolEntryType> LOOT_ENTRIES = defreg(Registries.LOOT_POOL_ENTRY_TYPE);
	private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = defreg(Registries.LOOT_FUNCTION_TYPE);
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = defreg(NeoForgeRegistries.Keys.ATTACHMENT_TYPES);
	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = defreg(Registries.DATA_COMPONENT_TYPE);
	private static final DeferredRegister<MapCodec<? extends SignalComponent>> SIGNAL_COMPONENT_TYPES = defreg(ExMachinaRegistries.SIGNAL_COMPONENT_TYPE);
	
	public static final Map<ResourceLocation, DeferredHolder<Block, ? extends LogicFunctionPlateBlock>> LOGIC_PLATES = new HashMap<>();
	public static final Map<ResourceLocation, DeferredHolder<Block, ? extends BitwiseGateBlock>> BITWISE_LOGIC_PLATES = new HashMap<>();
		
	// data component types
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Map<Direction,ItemStack>>> GEARS_DATA_COMPONENT = DATA_COMPONENT_TYPES.register(Names.GEARS, () -> DataComponentType.<Map<Direction,ItemStack>>builder()
		.persistent(Codec.unboundedMap(Direction.CODEC, ItemStack.OPTIONAL_CODEC))
		.networkSynchronized(ByteBufCodecs.map(HashMap::new, Direction.STREAM_CODEC, ItemStack.OPTIONAL_STREAM_CODEC))
		.build());
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockSide>> PLIERED_TUBE_DATA_COMPONENT = DATA_COMPONENT_TYPES.register(Names.PLIERED_TUBE, () -> DataComponentType.<BlockSide>builder()
		.networkSynchronized(BlockSide.STREAM_CODEC)
		.build());
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> SPOOLED_POST_DATA_COMPONENT = DATA_COMPONENT_TYPES.register(Names.SPOOLED_POST, () -> DataComponentType.<BlockPos>builder()
		.networkSynchronized(BlockPos.STREAM_CODEC)
		.build());
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<StonemillData>> STONEMILL_DATA_COMPONENT = DATA_COMPONENT_TYPES.register(Names.STONEMILL, () -> DataComponentType.<StonemillData>builder()
		.persistent(StonemillData.CODEC)
		.build());
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<WindcatcherColors>> WINDCATCHER_COLORS_DATA_COMPONENT = DATA_COMPONENT_TYPES.register(Names.WINDCATCHER_COLORS, () -> DataComponentType.<WindcatcherColors>builder()
		.persistent(WindcatcherColors.CODEC)
		.networkSynchronized(WindcatcherColors.STREAM_CODEC)
		.build());
	
	// blocks and items! the order they're defined here affects the creative tab
	// group them by categories and then roughly alphabetical order within categories
	public static final DeferredHolder<Block, SolderingTableBlock> SOLDERING_TABLE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.SOLDERING_TABLE,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.5F).noOcclusion(),
		SolderingTableBlock::new);
	public static final DeferredHolder<Block, PlateBlock> STONE_PLATE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.STONE_PLATE,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD),
		PlateBlock::new);
	
	// logic plates
	public static final DeferredHolder<Block, AlternatorBlock> ALTERNATOR_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.ALTERNATOR,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(0)
			.sound(SoundType.WOOD)
			.noOcclusion(),
		AlternatorBlock::new);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> AND_GATE_BLOCK = registerLogicGateType(BLOCKS, ITEMS, Names.AND_GATE, LogicFunctions.AND, LogicFunctionPlateBlock.THREE_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> DIODE = registerLogicGateType(BLOCKS, ITEMS, Names.DIODE, LogicFunctions.INPUT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
	public static final DeferredHolder<Block, LatchBlock> LATCH_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.LATCH,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD),
		LatchBlock::new);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> MULTIPLEXER = registerLogicGateType(BLOCKS, ITEMS, Names.MULTIPLEXER, LogicFunctions.MULTIPLEX, LogicFunctionPlateBlock.THREE_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> NAND_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.NAND_GATE, LogicFunctions.NAND, LogicFunctionPlateBlock.THREE_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> NOR_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.NOR_GATE, LogicFunctions.NOR, LogicFunctionPlateBlock.THREE_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> NOT_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.NOT_GATE, LogicFunctions.NOT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> OR_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.OR_GATE, LogicFunctions.OR, LogicFunctionPlateBlock.THREE_INPUTS);
	public static final DeferredHolder<Block, PulseGateBlock> PULSE_GATE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.PULSE_GATE,
		() ->BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD),
		PulseGateBlock::new);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> TWO_INPUT_AND_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.TWO_INPUT_AND_GATE, LogicFunctions.AND_2, LogicFunctionPlateBlock.T_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> TWO_INPUT_NAND_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.TWO_INPUT_NAND_GATE, LogicFunctions.NAND_2, LogicFunctionPlateBlock.T_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> XNOR_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.XNOR_GATE, LogicFunctions.XNOR_AC, LogicFunctionPlateBlock.T_INPUTS);
	public static final DeferredHolder<Block, LogicFunctionPlateBlock> XOR_GATE = registerLogicGateType(BLOCKS, ITEMS, Names.XOR_GATE, LogicFunctions.XOR_AC, LogicFunctionPlateBlock.T_INPUTS);
	
	// bitwise gates
	static
	{
		// bitwise logic gates store state in a TE instead of block properties
		// they don't need to have their properties defined on construction but they do need to be registered to the TE type they use
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, SingleInputBitwiseGateBlock> singleInput = SingleInputBitwiseGateBlock::new;
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, TwoInputBitwiseGateBlock> twoInputs = TwoInputBitwiseGateBlock::new;
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, ThreeInputBitwiseGateBlock> threeInputs = ThreeInputBitwiseGateBlock::new;
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_AND_GATE, BitwiseLogicFunctions.AND_2, twoInputs);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_DIODE, BitwiseLogicFunctions.INPUT_B, singleInput);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_MULTIPLEXER, BitwiseLogicFunctions.MULTIPLEX, threeInputs);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_NOT_GATE, BitwiseLogicFunctions.NOT_B, singleInput);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_OR_GATE, BitwiseLogicFunctions.OR, twoInputs);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_XNOR_GATE, BitwiseLogicFunctions.XNOR_AC, twoInputs);
		registerBitwiseLogicGateType(BLOCKS, ITEMS, Names.BITWISE_XOR_GATE, BitwiseLogicFunctions.XOR_AC, twoInputs);
	}
	
	// cable posts
	public static final DeferredHolder<Block, WirePostPlateBlock> REDWIRE_JUNCTION_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.REDWIRE_JUNCTION,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(2F, 5F)
			.forceSolidOn(),
		properties -> new WirePostPlateBlock(properties, WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
	public static final DeferredHolder<Block, WirePostBlock> REDWIRE_POST_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.REDWIRE_POST,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(2F, 5F)
			.forceSolidOn(),
		WirePostBlock::new);
	public static final DeferredHolder<Block, WirePostPlateBlock> REDWIRE_RELAY_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.REDWIRE_RELAY,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(2F, 5F)
			.forceSolidOn(),
		properties -> new WirePostPlateBlock(properties, WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
	public static final DeferredHolder<Block, CableJunctionBlock> CABLE_JUNCTION_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.CABLE_JUNCTION,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLUE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(2F, 5F)
			.forceSolidOn(),
		CableJunctionBlock::new);
	public static final DeferredHolder<Block, CableRelayBlock> CABLE_RELAY_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.CABLE_RELAY,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLUE)
			.instrument(NoteBlockInstrument.BASEDRUM)
			.strength(2F, 5F)
			.forceSolidOn(),
		CableRelayBlock::new);
	
	// uncategorized redstone blocks
	public static final DeferredHolder<Block, HexidecrubrometerBlock> HEXIDECRUBROMETER_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.HEXIDECRUBROMETER,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F),
		HexidecrubrometerBlock::new);
	
	// wires and cables
	public static final DeferredHolder<Block, PoweredWireBlock> RED_ALLOY_WIRE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.RED_ALLOY_WIRE, 
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).noCollision().instabreak(),
		PoweredWireBlock::createRedAlloyWireBlock,
		Item.Properties::new,
		WireBlockItem::new);
	public static final Map<DyeColor, DeferredHolder<Block, PoweredWireBlock>> COLORED_CABLE_BLOCKS = Util.make(new LinkedHashMap<>(), map -> {
		for (DyeColor color : DyeColor.values())
		{
			map.put(color, registerBlockItem(BLOCKS, ITEMS, Names.COLORED_CABLES_BY_COLOR[color.ordinal()],
				() -> BlockBehaviour.Properties.of().mapColor(color).pushReaction(PushReaction.DESTROY).noCollision().instabreak(),
				properties -> PoweredWireBlock.createColoredCableBlock(properties, color),
				Item.Properties::new,
				WireBlockItem::new));
		}
	});
	public static final DeferredHolder<Block, BundledCableBlock> BUNDLED_CABLE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.BUNDLED_CABLE,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).pushReaction(PushReaction.DESTROY).noCollision().instabreak(),
		BundledCableBlock::new,
		Item.Properties::new,
		WireBlockItem::new);

	// tubes
	public static final DeferredHolder<Block, TubeBlock> TUBE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.TUBE,
		() -> BlockBehaviour.Properties.of()
			.instrument(NoteBlockInstrument.DIDGERIDOO)
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(0.4F)
			.sound(SoundType.METAL),
		properties -> new TubeBlock(id("block/tube"), properties));
	public static final DeferredHolder<Block, RedstoneTubeBlock> REDSTONE_TUBE_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.REDSTONE_TUBE,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.GOLD)
		.strength(0.4F)
		.sound(SoundType.METAL),
		properties -> new RedstoneTubeBlock(id("block/tube"), properties));
	public static final Map<DyeColor, DeferredHolder<Block, ColoredTubeBlock>> COLORED_TUBE_BLOCKS = Util.make(new LinkedHashMap<>(), map -> {
		for (DyeColor color : DyeColor.values()) {
			String name = Names.COLORED_TUBE_NAMES[color.ordinal()];
			DeferredHolder<Block, ColoredTubeBlock> block = registerBlockItem(BLOCKS, ITEMS, name,
				() -> BlockBehaviour.Properties.of()
				.mapColor(color)
				.instrument(NoteBlockInstrument.DIDGERIDOO)
				.strength(0.4F)
				.sound(SoundType.METAL),
				properties -> new ColoredTubeBlock(id("block/" + name),	color, properties));
			map.put(color, block);
		}
	});
	
	// logistics blocks
	public static final DeferredHolder<Block, DistributorBlock> DISTRIBUTOR_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.DISTRIBUTOR,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.WOOD)
		.strength(2F, 6F)
		.sound(SoundType.METAL),
		DistributorBlock::new);
	public static final DeferredHolder<Block, ExtractorBlock> EXTRACTOR_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.EXTRACTOR,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.WOOD)
		.strength(2F, 6F)
		.noOcclusion()
        .isRedstoneConductor(NEVER_STATE_PREDICATE)
        .isSuffocating(NEVER_STATE_PREDICATE)
        .isViewBlocking(NEVER_STATE_PREDICATE)
		.sound(SoundType.WOOD),
		ExtractorBlock::new);
	public static final DeferredHolder<Block, FilterBlock> FILTER_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.FILTER,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.TERRACOTTA_YELLOW)
		.strength(2F, 6F)
		.sound(SoundType.METAL),
		FilterBlock::new);
	public static final DeferredHolder<Block, LoaderBlock> LOADER_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.LOADER,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(2F, 6F)
			.sound(SoundType.METAL),
		LoaderBlock::new);
	public static final DeferredHolder<Block, MultiFilterBlock> MULTI_FILTER_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.MULTIFILTER,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.STONE)
		.strength(2F, 6F)
		.sound(SoundType.METAL),
		MultiFilterBlock::new);
	public static final DeferredHolder<Block, OsmosisFilterBlock> OSMOSIS_FILTER_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.OSMOSIS_FILTER,
		() -> BlockBehaviour.Properties.of()
		.mapColor(MapColor.GRASS)
		.strength(2F, 6F)
		.sound(SoundType.METAL),
		OsmosisFilterBlock::new);
	public static final DeferredHolder<Block, OsmosisSlimeBlock> OSMOSIS_SLIME_BLOCK = registerBlock(BLOCKS, Names.OSMOSIS_SLIME, BlockBehaviour.Properties::of, OsmosisSlimeBlock::new);
	public static final DeferredHolder<Block, ShuntBlock> SHUNT_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.SHUNT,
		() -> BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(2F, 6F)
			.sound(SoundType.METAL),
		ShuntBlock::new);
	
	// mechanisms
	public static final DeferredHolder<Block, AirFoilBlock> AIRFOIL_BLOCK = registerBlock(BLOCKS, Names.AIRFOIL, () -> Block.Properties.of()
		.air()
		.noCollision()
		.noOcclusion()
		.noLootTable()
		.isSuffocating(NEVER_STATE_PREDICATE),
		AirFoilBlock::new);
	public static final Map<String, DeferredHolder<Block, AxleBlock>> AXLE_BLOCKS = registerWoodSetBlocks((woodName, woodSet) -> registerBlockItem(BLOCKS, ITEMS, woodName + "_" + Names.AXLE,
		() -> Block.Properties.ofFullCopy(woodSet.strippedLog()).noOcclusion(),
		AxleBlock::new));
	// cheat to register clutch blocks before GEAR_BLOCKS is defined
	private static final GearBlock getGearBlock(String woodName)
	{
		return GEAR_BLOCKS.get(woodName).get();
	}
	public static final Map<String, DeferredHolder<Block, ClutchBlock>> CLUTCH_BLOCKS = registerWoodSetBlocks((woodName, woodSet) -> registerBlockItem(BLOCKS, ITEMS, woodName + "_" + Names.CLUTCH,
		() -> Block.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.5F)
			.noOcclusion()
	        .isRedstoneConductor(NEVER_STATE_PREDICATE)
	        .isSuffocating(NEVER_STATE_PREDICATE)
	        .isViewBlocking(NEVER_STATE_PREDICATE),
		props -> new ClutchBlock(props, Suppliers.memoize(() -> new ItemStack(getGearBlock(woodName))))));
	public static final Map<String, DeferredHolder<Block, GearBlock>> GEAR_BLOCKS = registerWoodSetBlocks((woodName, woodSet) -> registerBlockItem(BLOCKS, ITEMS, woodName + "_" + Names.GEAR,
		() -> woodSet.finagleProps(Block.Properties.of()
			.mapColor(woodSet.mapColor())
			.strength(2F)
			.sound(woodSet.soundType())
			.noOcclusion()),
		GearBlock::new,
		Item.Properties::new,
		GearBlockItem::new));
	public static final DeferredHolder<Block, GearsBlock> GEARS_BLOCK = registerBlock(BLOCKS, Names.GEARS, () -> Block.Properties.of()
		.mapColor(MapColor.WOOD)
		.strength(2F)
		.sound(SoundType.WOOD)
		.noOcclusion(),
		GearsBlock::new);
	public static final Map<String, DeferredHolder<Block, GearshifterBlock>> GEARSHIFTER_BLOCKS = registerWoodSetBlocks((woodName, woodSet) -> registerBlockItem(BLOCKS, ITEMS, woodName + "_" + Names.GEARSHIFTER,
		() -> woodSet.finagleProps(Block.Properties.of()
			.mapColor(woodSet.mapColor())
			.strength(2F)
			.sound(woodSet.soundType())
			.noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor(NEVER_STATE_PREDICATE)
            .isSuffocating(NEVER_STATE_PREDICATE)
            .isViewBlocking(NEVER_STATE_PREDICATE)),
		GearshifterBlock::new));
	public static final DeferredHolder<Block, StonemillBlock> STONEMILL_BLOCK = registerBlockItem(BLOCKS, ITEMS, Names.STONEMILL, () -> Block.Properties.of()
		.mapColor(MapColor.COLOR_BLACK)
		.strength(2F, 6F)
		.noOcclusion(),
		StonemillBlock::new);
	public static final Map<String, DeferredHolder<Block, WindcatcherBlock>> WINDCATCHER_BLOCKS = registerWoodSetBlocks((woodName, woodSet) -> registerBlockItem(BLOCKS, ITEMS, woodName + "_" + Names.WINDCATCHER,
		() -> Block.Properties.of()
			.mapColor(woodSet.mapColor())
			.strength(2F)
			.sound(woodSet.soundType())
			.ignitedByLava()
			.noOcclusion()
			.randomTicks()
	        .isValidSpawn(Blocks::never)
	        .isRedstoneConductor(NEVER_STATE_PREDICATE)
	        .isSuffocating(NEVER_STATE_PREDICATE)
	        .isViewBlocking(NEVER_STATE_PREDICATE),
		WindcatcherBlock::new,
		Item.Properties::new,
		WindCatcherBlockItem::new
	));

	// misc items
	public static final DeferredHolder<Item, Item> BUNDLED_CABLE_SPOOL_ITEM = registerItem(ITEMS, Names.BUNDLED_CABLE_SPOOL, () -> new Item.Properties().durability(64), properties -> new WireSpoolItem(properties, MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS));
	public static final DeferredHolder<Item, WireSpoolItem> REDWIRE_SPOOL_ITEM = registerItem(ITEMS, Names.REDWIRE_SPOOL, () -> new Item.Properties().durability(64), properties -> new WireSpoolItem(properties, MoreRed.Tags.Blocks.REDWIRE_POSTS));
	public static final DeferredHolder<Item, Item> RED_ALLOY_INGOT_ITEM = registerItem(ITEMS, Names.RED_ALLOY_INGOT, Item.Properties::new, Item::new);
	public static final DeferredHolder<Item, PliersItem> TUBING_PLIERS = registerItem(ITEMS, Names.PLIERS, () -> new Item.Properties().durability(128), PliersItem::new);
	

	// blockentity types
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> ALTERNATOR_BLOCK_ENTITY = GenericBlockEntity.builder()
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, TwentyFourBlock::normalizeMachineWithAttachmentNode, TwentyFourBlock::denormalizeMachineWithAttachmentNode)
		.register(BLOCK_ENTITY_TYPES, Names.ALTERNATOR, ALTERNATOR_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> AXLE_BLOCK_ENTITY = GenericBlockEntity.builder()
		.register(BLOCK_ENTITY_TYPES, Names.AXLE, AXLE_BLOCKS.values());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CablePostBlockEntity>> CABLE_POST_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.CABLE_RELAY,
		() -> new BlockEntityType<>(CablePostBlockEntity::new,
			CABLE_RELAY_BLOCK.get(),
			CABLE_JUNCTION_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> CLUTCH_BLOCK_ENTITY = GenericBlockEntity.builder()
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, TwentyFourBlock::normalizeMachineWithAttachmentNode, TwentyFourBlock::denormalizeMachineWithAttachmentNode)
		.register(BLOCK_ENTITY_TYPES, Names.CLUTCH, CLUTCH_BLOCKS.values());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistributorBlockEntity>> DISTRIBUTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.DISTRIBUTOR,
		() -> new BlockEntityType<>(DistributorBlockEntity::new, DISTRIBUTOR_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> EXTRACTOR_BLOCK_ENTITY = GenericBlockEntity.builder()
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, ExtractorBlock::normalizeMachine, ExtractorBlock::denormalizeMachine)
		.register(BLOCK_ENTITY_TYPES, Names.EXTRACTOR, EXTRACTOR_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilterBlockEntity>> FILTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.FILTER,
		() -> new BlockEntityType<>(FilterBlockEntity::new, FILTER_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> GEAR_BLOCK_ENTITY = GenericBlockEntity.builder()
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, TwentyFourBlock::normalizeMachineWithAttachmentNode, TwentyFourBlock::denormalizeMachineWithAttachmentNode)
		.register(BLOCK_ENTITY_TYPES, Names.GEAR, GEAR_BLOCKS.values());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> GEARS_BLOCK_ENTITY = GenericBlockEntity.builder()
		.syncedData(GEARS_DATA_COMPONENT)
		.dataTransformer(GEARS_DATA_COMPONENT, GearsBlock::normalizeGears, GearsBlock::denormalizeGears)
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, EightGroup::normalizeMachine, EightGroup::denormalizeMachine)
		.register(BLOCK_ENTITY_TYPES, Names.GEARS, GEARS_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> GEARSHIFTER_BLOCK_ENTITY = GenericBlockEntity.builder()
		.transformAttachment(MechanicalNodeStates.HOLDER, MechanicalNodeStates.MAP_CODEC, GearshifterBlock::normalizeMachine, GearshifterBlock::denormalizeMachine)
		.register(BLOCK_ENTITY_TYPES, Names.GEARSHIFTER, GEARSHIFTER_BLOCKS.values());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiFilterBlockEntity>> MULTIFILTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.MULTIFILTER,
		() -> new BlockEntityType<>(MultiFilterBlockEntity::new, MULTI_FILTER_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OsmosisFilterBlockEntity>> OSMOSIS_FILTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.OSMOSIS_FILTER,
		() -> new BlockEntityType<>(OsmosisFilterBlockEntity::new, OSMOSIS_FILTER_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredWireBlockEntity>> POWERED_WIRE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.POWERED_WIRE,
		() -> new BlockEntityType<>(PoweredWireBlockEntity::new, Util.make(new HashSet<>(), set -> {
			set.add(RED_ALLOY_WIRE_BLOCK.get());
			for (var block : COLORED_CABLE_BLOCKS.values())
			{
				set.add(block.get());
			}
		})));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneTubeBlockEntity>> REDSTONE_TUBE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.REDSTONE_TUBE,
		() -> new BlockEntityType<>(RedstoneTubeBlockEntity::new, REDSTONE_TUBE_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuntBlockEntity>> SHUNT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.SHUNT,
		() -> new BlockEntityType<>(ShuntBlockEntity::new, SHUNT_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleInputBitwiseGateBlockEntity>> SINGLE_INPUT_BITWISE_GATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.SINGLE_INPUT_BITWISE_GATE,
		() -> new BlockEntityType<>(SingleInputBitwiseGateBlockEntity::create, BITWISE_LOGIC_PLATES.values().stream()
			.map(DeferredHolder::get)
			.filter(block -> block instanceof SingleInputBitwiseGateBlock)
			.collect(Collectors.toSet())));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> STONEMILL_BLOCK_ENTITY = GenericBlockEntity.builder()
		.serverData(
			STONEMILL_DATA_COMPONENT,	// holds the progress counters
			() -> DataComponents.CONTAINER) // holds the inventory
		.preRemoveSideEffects(StonemillBlock::preRemoveSideEffects)
		.register(BLOCK_ENTITY_TYPES, Names.STONEMILL, STONEMILL_BLOCK);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThreeInputBitwiseGateBlockEntity>> THREE_INPUT_BITWISE_GATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.THREE_INPUT_BITWISE_GATE,
		() -> new BlockEntityType<>(ThreeInputBitwiseGateBlockEntity::create, BITWISE_LOGIC_PLATES.values().stream()
			.map(DeferredHolder::get)
			.filter(block -> block instanceof ThreeInputBitwiseGateBlock)
			.collect(Collectors.toSet())));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TubeBlockEntity>> TUBE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.TUBE,
		() -> new BlockEntityType<>(
			TubeBlockEntity::new,
			Util.make(new HashSet<>(), set -> {
				set.add(TUBE_BLOCK.get());
				for (var block : COLORED_TUBE_BLOCKS.values())
				{
					set.add(block.get());
				}
			})));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputBitwiseGateBlockEntity>> TWO_INPUT_BITWISE_GATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.TWO_INPUT_BITWISE_GATE,
		() -> new BlockEntityType<>(TwoInputBitwiseGateBlockEntity::create, BITWISE_LOGIC_PLATES.values().stream()
			.map(DeferredHolder::get)
			.filter(block -> block instanceof TwoInputBitwiseGateBlock)
			.collect(Collectors.toSet())));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireBlockEntity>> WIRE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.WIRE,
		() -> new BlockEntityType<>(WireBlockEntity::new,
			BUNDLED_CABLE_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirePostBlockEntity>> WIRE_POST_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(Names.WIRE_POST,
		() -> new BlockEntityType<>(WirePostBlockEntity::new,
			REDWIRE_POST_BLOCK.get(),
			REDWIRE_RELAY_BLOCK.get(),
			REDWIRE_JUNCTION_BLOCK.get()));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GenericBlockEntity>> WINDCATCHER_BLOCK_ENTITY = GenericBlockEntity.builder()
		.itemData(WINDCATCHER_COLORS_DATA_COMPONENT)
		.register(BLOCK_ENTITY_TYPES, Names.WINDCATCHER, WINDCATCHER_BLOCKS.values());

	// menu types
	public static final DeferredHolder<MenuType<?>, MenuType<FilterMenu>> FILTER_MENU = MENU_TYPES.register(Names.FILTER, () -> new MenuType<>(FilterMenu::createClientMenu, FeatureFlags.VANILLA_SET));
	public static final DeferredHolder<MenuType<?>, MenuType<LoaderMenu>> LOADER_MENU = MENU_TYPES.register(Names.LOADER, () -> new MenuType<>(LoaderMenu::new, FeatureFlags.VANILLA_SET));
	public static final DeferredHolder<MenuType<?>, MenuType<MultiFilterMenu>> MULTI_FILTER_MENU = MENU_TYPES.register(Names.MULTIFILTER, () -> new MenuType<>(MultiFilterMenu::clientMenu, FeatureFlags.VANILLA_SET));
	public static final DeferredHolder<MenuType<?>, MenuType<SolderingMenu>> SOLDERING_MENU = MENU_TYPES.register(Names.SOLDERING_TABLE,
		() -> new MenuType<>(SolderingMenu::getClientContainer, FeatureFlags.VANILLA_SET));
	public static final DeferredHolder<MenuType<?>, MenuType<StonemillMenu>> STONEMILL_MENU = MENU_TYPES.register(Names.STONEMILL,
		() -> new MenuType<>(StonemillMenu::clientMenu, FeatureFlags.VANILLA_SET));
	
	// recipe things
	public static final DeferredHolder<RecipeType<?>, RecipeType<SolderingRecipe>> SOLDERING_RECIPE_TYPE = RECIPE_TYPES.register(Names.SOLDERING_RECIPE, () -> RecipeType.simple(id(Names.SOLDERING_RECIPE)));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolderingRecipe>> SOLDERING_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(Names.SOLDERING_RECIPE,
		() -> new SimpleRecipeSerializer<SolderingRecipe>(SolderingRecipe.CODEC, SolderingRecipe.STREAM_CODEC));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WindcatcherRecipe>> WINDCATCHER_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(Names.WINDCATCHER,
		() -> new SimpleRecipeSerializer<WindcatcherRecipe>(WindcatcherRecipe.CODEC, WindcatcherRecipe.STREAM_CODEC));
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WindcatcherDyeRecipe>> WINDCATCHER_DYE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(Names.WINDCATCHER_DYE,
		() -> new SimpleRecipeSerializer<WindcatcherDyeRecipe>(WindcatcherDyeRecipe.CODEC, WindcatcherDyeRecipe.STREAM_CODEC));
	
	// loot things
	public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> GEARS_LOOT_ENTRY = LOOT_ENTRIES.register(Names.GEARS, () -> new LootPoolEntryType(GearsLootEntry.CODEC));
	public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<WireCountLootFunction>> WIRE_COUNT_LOOT_FUNCTION = LOOT_FUNCTIONS.register(Names.WIRE_COUNT, () -> new LootItemFunctionType<>(WireCountLootFunction.CODEC));

	// creative tabs
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = TABS.register(MoreRed.MODID, () -> CreativeModeTab.builder()
		.icon(() -> new ItemStack(LOGIC_PLATES.get(id(Names.NOR_GATE)).get()))
		.title(Component.translatable("itemGroup.morered"))
		.displayItems((params, output) -> output.acceptAll(ITEMS.getEntries().stream().map(rob -> new ItemStack(rob.get())).toList()))
		.build());

	// attachment types
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> POSTS_IN_CHUNK_ATTACHMENT = ATTACHMENT_TYPES.register(Names.POSTS_IN_CHUNK, () -> AttachmentType.<Set<BlockPos>>builder(() -> new HashSet<>())
		.serialize(MoreCodecs.POSITIONS_MAP_CODEC)
		.build());
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> TUBES_IN_CHUNK_ATTACHMENT = ATTACHMENT_TYPES.register(Names.TUBES_IN_CHUNK, () -> AttachmentType.<Set<BlockPos>>builder(() -> new HashSet<>())
		.serialize(MoreCodecs.POSITIONS_MAP_CODEC)
		.build());
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<BlockPos, VoxelShape>>> VOXEL_CACHE_ATTACHMENT = ATTACHMENT_TYPES.register(Names.VOXEL_CACHE, () -> AttachmentType.<Map<BlockPos,VoxelShape>>builder(() -> new HashMap<>())
		.build());
	
	//signal components
	static
	{
		BiConsumer<ResourceKey<MapCodec<? extends SignalComponent>>, MapCodec<? extends SignalComponent>> registerSignalComponent = (key,codec) -> SIGNAL_COMPONENT_TYPES.register(key.location().getPath(), () -> codec);
		registerSignalComponent.accept(WireSignalComponent.RESOURCE_KEY, WireSignalComponent.CODEC);
		registerSignalComponent.accept(WirePostSignalComponent.RESOURCE_KEY, WirePostSignalComponent.CODEC);
		registerSignalComponent.accept(BitwiseGateSignalComponent.RESOURCE_KEY, BitwiseGateSignalComponent.CODEC);
	}
	
	// data maps (which are registered elsewhere)
	public static final DataMapType<DimensionType, Wind> WIND_DATA_MAP_TYPE = DataMapType.builder(
			id(Names.WIND),
			Registries.DIMENSION_TYPE,
			Wind.CODEC)
		.synced(Wind.CODEC, true)
		.build();
	
	public MoreRed(IEventBus modBus)
	{
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		
		modBus.addListener(this::onRegisterPackets);
		modBus.addListener(this::onRegisterCapabilities);
		modBus.addListener(this::onRegisterDataMapTypes);
		
		forgeBus.addListener(EventPriority.LOW, this::onUseItemOnBlock);
		forgeBus.addListener(EventPriority.LOW, this::onLeftClickBlock);
		forgeBus.addListener(this::onUseItemOnBlock);
		forgeBus.addListener(this::onChunkWatch);
		forgeBus.addListener(this::onDataPackSyncEvent);
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
			
			public static final TagKey<Block> CLUTCHES = tag(Names.CLUTCHES);
			public static final TagKey<Block> WOODEN_AXLES = tag(Names.WOODEN_AXLES);
			public static final TagKey<Block> WOODEN_GEARS = tag(Names.WOODEN_GEARS);
			public static final TagKey<Block> WOODEN_GEARSHIFTERS = tag(Names.WOODEN_GEARSHIFTERS);
			public static final TagKey<Block> WINDCATCHERS = tag(Names.WINDCATCHERS);
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
			public static final TagKey<Item> AXLES = tag(Names.AXLES);
			public static final TagKey<Item> GEARS = tag(Names.GEARS);
			public static final TagKey<Item> GEARSHIFTERS = tag(Names.GEARSHIFTERS);
			public static final TagKey<Item> WINDCATCHERS = tag(Names.WINDCATCHERS);
			public static final TagKey<Item> DYEABLE_WINDCATCHERS = tag(Names.DYEABLE_WINDCATCHERS);
			public static final TagKey<Item> SUPPORTED_STRIPPED_LOGS = tag(Names.SUPPORTED_STRIPPED_LOGS);
		}
	}
	
	private void onRegisterPackets(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar r = event.registrar("8.0.0");
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
		event.registerBlockEntity(Capabilities.Item.BLOCK, DISTRIBUTOR_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, FILTER_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, MULTIFILTER_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, OSMOSIS_FILTER_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, REDSTONE_TUBE_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, SHUNT_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		event.registerBlockEntity(Capabilities.Item.BLOCK, TUBE_BLOCK_ENTITY.get(), (be,side) -> be.getItemHandler(side));
		
		event.registerBlock(Capabilities.Item.BLOCK, StonemillBlock::getItemHandler, STONEMILL_BLOCK.get());
	}
	
	private void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
		event.register(WIND_DATA_MAP_TYPE);
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
					Set<BlockPos> posts = chunk.getData(POSTS_IN_CHUNK_ATTACHMENT.get());
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
									else if (level.isClientSide())
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
								else if (level.isClientSide())
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
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (block instanceof AbstractWireBlock wireBlock)
		{
			wireBlock.handleLeftClickBlock(event, level, pos, state);
		}
		else if (block instanceof GearsBlock gearsBlock)
		{
			gearsBlock.handleLeftClickBlock(event, level, pos, state);
		}
	}
	
	// fired on the server just after vanilla chunk data is sent to the client
	private void onChunkWatch(ChunkWatchEvent.Watch event)
	{
		ServerPlayer player = event.getPlayer();
		LevelChunk chunk = event.getChunk();
		ChunkPos pos = chunk.getPos();
		Set<BlockPos> postsInChunk = chunk.getData(POSTS_IN_CHUNK_ATTACHMENT.get());
		if (postsInChunk != null && !postsInChunk.isEmpty())
		{
			PacketDistributor.sendToPlayer(player, new SyncPostsInChunkPacket(pos, postsInChunk));
		}
		PacketDistributor.sendToPlayer(player, new SyncTubesInChunkPacket(pos, Set.copyOf(TubesInChunk.getTubesInChunk(chunk))));
	}
	
	private void onDataPackSyncEvent(OnDatapackSyncEvent event)
	{
		event.sendRecipes(MoreRed.SOLDERING_RECIPE_TYPE.get());
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
	
	public static DeferredHolder<Block, LogicFunctionPlateBlock> registerLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		LogicFunctionPlateBlockFactory factory)
	{
		DeferredHolder<Block, LogicFunctionPlateBlock> blockGetter = registerBlockItem(blocks, items, name,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.instrument(NoteBlockInstrument.BASEDRUM)
				.strength(0F)
				.sound(SoundType.WOOD)
				.forceSolidOn(),
			properties -> factory.makeBlock(function, properties));
		LOGIC_PLATES.put(blockGetter.getId(), blockGetter);
		return blockGetter;
	}
	
	public static <B extends BitwiseGateBlock> DeferredHolder<Block, B> registerBitwiseLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		BitwiseLogicFunction function,
		BiFunction<BlockBehaviour.Properties, BitwiseLogicFunction, B> blockFactory)
	{
		DeferredHolder<Block, B> rob = registerBlockItem(blocks, items, name,
			() -> BlockBehaviour.Properties.of()
				.mapColor(MapColor.QUARTZ)
				.instrument(NoteBlockInstrument.BASEDRUM)
				.strength(0F)
				.sound(SoundType.WOOD)
				.forceSolidOn(),
			properties -> blockFactory.apply(properties, function));
		BITWISE_LOGIC_PLATES.put(rob.getId(), rob);
		return rob;
	}
	
	public static <T> DeferredRegister<T> defreg(ResourceKey<Registry<T>> registryKey)
	{
		var reg = DeferredRegister.create(registryKey, MODID);
		reg.register(ModList.get().getModContainerById(MODID).get().getEventBus());
		return reg;
	}
	
	public static <BLOCK extends Block> Map<String, DeferredHolder<Block, BLOCK>> registerWoodSetBlocks(BiFunction<String, WoodSet, DeferredHolder<Block,BLOCK>> registerer)
	{
		Map<String, DeferredHolder<Block, BLOCK>> map = new HashMap<>();
		for (var entry : WoodSets.LOOKUP.entrySet())
		{
			String woodName = entry.getKey();
			map.put(woodName, registerer.apply(woodName, entry.getValue()));
		}
		return map;
	}
}
