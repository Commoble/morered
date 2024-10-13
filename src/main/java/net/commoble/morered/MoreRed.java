package net.commoble.morered;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import net.commoble.morered.bitwise_logic.BitewiseGateBlock;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.SingleInputBitwiseGateBlockEntity;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlock;
import net.commoble.morered.bitwise_logic.TwoInputBitwiseGateBlockEntity;
import net.commoble.morered.client.ClientProxy;
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
import net.commoble.morered.wire_post.CableJunctionBlock;
import net.commoble.morered.wire_post.CableRelayBlock;
import net.commoble.morered.wire_post.CablePostBlockEntity;
import net.commoble.morered.wire_post.FakeStateLevel;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
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
	public static final Logger LOGGER = LogManager.getLogger();
	private static MoreRed instance;
	public static MoreRed get() { return instance; }
	
	public static ResourceLocation getModRL(String name)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, name);
	}

	public final Map<ResourceLocation, DeferredHolder<Block, ? extends LogicFunctionPlateBlock>> logicPlates = new HashMap<>();
	public final Map<ResourceLocation, DeferredHolder<Block, ? extends BitewiseGateBlock>> bitwiseLogicPlates = new HashMap<>();
	
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

	public final DeferredHolder<Item, WireSpoolItem> redwireSpoolItem;
	public final DeferredHolder<Item, Item> bundledCableSpoolItem;
	public final DeferredHolder<Item, Item> redAlloyIngotItem;
	
	public final DeferredHolder<CreativeModeTab, CreativeModeTab> tab;

	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirePostBlockEntity>> wirePostBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<CablePostBlockEntity>> cablePostBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireBlockEntity>> wireBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<PoweredWireBlockEntity>> poweredWireBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleInputBitwiseGateBlockEntity>> singleInputBitwiseGateBeType;
	public final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputBitwiseGateBlockEntity>> twoInputBitwiseGateBeType;

	public final DeferredHolder<MenuType<?>, MenuType<SolderingMenu>> solderingMenuType;
	public final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolderingRecipe>> solderingSerializer;
	public final DeferredHolder<RecipeType<?>, RecipeType<SolderingRecipe>> solderingRecipeType;
	
	public final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<WireCountLootFunction>> wireCountLootFunction;
	
	public final DeferredHolder<AttachmentType<?>, AttachmentType<Set<BlockPos>>> postsInChunkAttachment;
	public final DeferredHolder<AttachmentType<?>, AttachmentType<Map<BlockPos, VoxelShape>>> voxelCacheAttachment;
	
	public final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> spooledPostComponent;
	
	@SuppressWarnings("unchecked")
	public MoreRed(IEventBus modBus)
	{
		instance = this;
		
		IEventBus forgeBus = NeoForge.EVENT_BUS;

		DeferredRegister<Block> blocks = createDeferredRegister(modBus, Registries.BLOCK);
		DeferredRegister<Item> items = createDeferredRegister(modBus, Registries.ITEM);
		DeferredRegister<CreativeModeTab> tabs = createDeferredRegister(modBus, Registries.CREATIVE_MODE_TAB);
		DeferredRegister<BlockEntityType<?>> blockEntityTypes = createDeferredRegister(modBus, Registries.BLOCK_ENTITY_TYPE);
		DeferredRegister<MenuType<?>> menuTypes = createDeferredRegister(modBus, Registries.MENU);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = createDeferredRegister(modBus, Registries.RECIPE_SERIALIZER);
		DeferredRegister<RecipeType<?>> recipeTypes = createDeferredRegister(modBus, Registries.RECIPE_TYPE);
		DeferredRegister<LootItemFunctionType<?>> lootFunctions = createDeferredRegister(modBus, Registries.LOOT_FUNCTION_TYPE);
		DeferredRegister<AttachmentType<?>> attachmentTypes = createDeferredRegister(modBus, NeoForgeRegistries.Keys.ATTACHMENT_TYPES);
		DeferredRegister<DataComponentType<?>> dataComponentTypes = createDeferredRegister(modBus, Registries.DATA_COMPONENT_TYPE);
		
		solderingTableBlock = registerBlockItem(blocks, items, ObjectNames.SOLDERING_TABLE,
			() -> new SolderingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.5F).noOcclusion()));
		stonePlateBlock = registerBlockItem(blocks, items, ObjectNames.STONE_PLATE,
			() -> new PlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD)));
		latchBlock = registerBlockItem(blocks, items, ObjectNames.LATCH,
			() -> new LatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD)));
		pulseGateBlock = registerBlockItem(blocks, items, ObjectNames.PULSE_GATE,
			() -> new PulseGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD)));
		redwirePostBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_POST,
			() -> new WirePostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		redwireRelayBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_RELAY,
			() -> new WirePostPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
		redwireJunctionBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_JUNCTION,
			() -> new WirePostPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
		hexidecrubrometerBlock = registerBlockItem(blocks, items, ObjectNames.HEXIDECRUBROMETER,
			() -> new HexidecrubrometerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		cableRelayBlock = registerBlockItem(blocks, items, ObjectNames.CABLE_RELAY,
			() -> new CableRelayBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		cableJunctionBlock = registerBlockItem(blocks, items, ObjectNames.CABLE_JUNCTION,
			() -> new CableJunctionBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		
		redAlloyWireBlock = registerBlockItem(blocks, items, ObjectNames.RED_ALLOY_WIRE, 
			() -> PoweredWireBlock.createRedAlloyWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).noCollission().instabreak()),
			block -> new WireBlockItem(block, new Item.Properties()));
		coloredCableBlocks = Util.make((DeferredHolder<Block, PoweredWireBlock>[])new DeferredHolder[16], array ->
			Arrays.setAll(array, i -> registerBlockItem(blocks, items, ObjectNames.COLORED_CABLES_BY_COLOR[i],
				() -> PoweredWireBlock.createColoredCableBlock(BlockBehaviour.Properties.of().mapColor(DyeColor.values()[i]).pushReaction(PushReaction.DESTROY).noCollission().instabreak(), DyeColor.values()[i]),
				block -> new WireBlockItem(block, new Item.Properties()))));
		bundledCableBlock = registerBlockItem(blocks, items, ObjectNames.BUNDLED_CABLE,
			() -> new BundledCableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).pushReaction(PushReaction.DESTROY).noCollission().instabreak()),
			block -> new WireBlockItem(block, new Item.Properties()));
		
		registerLogicGateType(blocks, items, ObjectNames.DIODE, LogicFunctions.INPUT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(blocks, items, ObjectNames.NOT_GATE, LogicFunctions.NOT_B, LogicFunctionPlateBlock.LINEAR_INPUT);
		registerLogicGateType(blocks, items, ObjectNames.NOR_GATE, LogicFunctions.NOR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.NAND_GATE, LogicFunctions.NAND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.OR_GATE, LogicFunctions.OR, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.AND_GATE, LogicFunctions.AND, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.XOR_GATE, LogicFunctions.XOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.XNOR_GATE, LogicFunctions.XNOR_AC, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.MULTIPLEXER, LogicFunctions.MULTIPLEX, LogicFunctionPlateBlock.THREE_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.TWO_INPUT_AND_GATE, LogicFunctions.AND_2, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.TWO_INPUT_NAND_GATE, LogicFunctions.NAND_2, LogicFunctionPlateBlock.T_INPUTS);
		
		// bitwise logic gates store state in a TE instead of block properties
		// they don't need to have their properties defined on construction but they do need to be registered to the TE type they use
		BiFunction<BlockBehaviour.Properties, LogicFunction, SingleInputBitwiseGateBlock> singleInput = SingleInputBitwiseGateBlock::new;
		BiFunction<BlockBehaviour.Properties, LogicFunction, TwoInputBitwiseGateBlock> twoInputs = TwoInputBitwiseGateBlock::new;
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_DIODE, LogicFunctions.INPUT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_NOT_GATE, LogicFunctions.NOT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_OR_GATE, LogicFunctions.OR, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_AND_GATE, LogicFunctions.AND_2, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_XOR_GATE, LogicFunctions.XOR_AC, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_XNOR_GATE, LogicFunctions.XNOR_AC, twoInputs);

		redwireSpoolItem = items.register(ObjectNames.REDWIRE_SPOOL, () -> new WireSpoolItem(new Item.Properties().durability(64), MoreRed.Tags.Blocks.REDWIRE_POSTS));
		bundledCableSpoolItem = items.register(ObjectNames.BUNDLED_CABLE_SPOOL, () -> new WireSpoolItem(new Item.Properties().durability(64), MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS));
		redAlloyIngotItem = items.register(ObjectNames.RED_ALLOY_INGOT, () -> new Item(new Item.Properties()));

		ResourceLocation tabIconId = getModRL(ObjectNames.NOR_GATE);
		this.tab = tabs.register(MoreRed.MODID, () -> CreativeModeTab.builder()
			.icon(() -> new ItemStack(this.logicPlates.get(tabIconId).get()))
			.title(Component.translatable("itemGroup.morered"))
			.displayItems((params, output) -> output.acceptAll(items.getEntries().stream().map(rob -> new ItemStack(rob.get())).toList()))
			.build()
			);
		
		wirePostBeType = blockEntityTypes.register(ObjectNames.WIRE_POST,
			() -> BlockEntityType.Builder.of(WirePostBlockEntity::new,
				redwirePostBlock.get(),
				redwireRelayBlock.get(),
				redwireJunctionBlock.get())
			.build(null));
		cablePostBeType = blockEntityTypes.register(ObjectNames.CABLE_RELAY,
			() -> BlockEntityType.Builder.of(CablePostBlockEntity::new,
				cableRelayBlock.get(),
				cableJunctionBlock.get())
			.build(null));
		wireBeType = blockEntityTypes.register(ObjectNames.WIRE,
			() -> BlockEntityType.Builder.of(WireBlockEntity::new,
				bundledCableBlock.get())
			.build(null));
		poweredWireBeType = blockEntityTypes.register(ObjectNames.POWERED_WIRE,
			() -> BlockEntityType.Builder.of(PoweredWireBlockEntity::new,
				Stream.concat(
					Stream.of(redAlloyWireBlock),
					Arrays.stream(coloredCableBlocks))
				.map(DeferredHolder::get)
				.toArray(Block[]::new))
			.build(null));
		singleInputBitwiseGateBeType = blockEntityTypes.register(ObjectNames.SINGLE_INPUT_BITWISE_GATE,
			() -> BlockEntityType.Builder.of(SingleInputBitwiseGateBlockEntity::create,
				Util.make(() ->
				{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
					return this.bitwiseLogicPlates.values().stream()
						.map(rob -> rob.get())
						.toArray(Block[]::new);
				}))
			.build(null));
		twoInputBitwiseGateBeType = blockEntityTypes.register(ObjectNames.TWO_INPUT_BITWISE_GATE,
			() -> BlockEntityType.Builder.of(TwoInputBitwiseGateBlockEntity::create,
				Util.make(() ->
				{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
					return this.bitwiseLogicPlates.values().stream()
						.map(rob -> rob.get())
						.toArray(Block[]::new);
				}))
			.build(null));

		solderingMenuType = menuTypes.register(ObjectNames.SOLDERING_TABLE,
			() -> new MenuType<>(SolderingMenu::getClientContainer, FeatureFlags.VANILLA_SET));
		solderingSerializer = recipeSerializers.register(ObjectNames.SOLDERING_RECIPE,
			() -> new SimpleRecipeSerializer<>(SolderingRecipe.CODEC, SolderingRecipe.STREAM_CODEC));
		solderingRecipeType = recipeTypes.register(ObjectNames.SOLDERING_RECIPE, () -> RecipeType.simple(getModRL(ObjectNames.SOLDERING_RECIPE)));
		
		wireCountLootFunction = lootFunctions.register(ObjectNames.WIRE_COUNT, () -> new LootItemFunctionType<>(WireCountLootFunction.CODEC));
		
		postsInChunkAttachment = attachmentTypes.register(ObjectNames.POSTS_IN_CHUNK, () -> AttachmentType.<Set<BlockPos>>builder(() -> new HashSet<>())
			.serialize(BlockPos.CODEC.listOf().xmap(HashSet::new, List::copyOf))
			.build());
		
		voxelCacheAttachment = attachmentTypes.register(ObjectNames.VOXEL_CACHE, () -> AttachmentType.<Map<BlockPos,VoxelShape>>builder(() -> new HashMap<>())
			.build());
		
		spooledPostComponent = dataComponentTypes.register(ObjectNames.SPOOLED_POST, () -> DataComponentType.<BlockPos>builder()
			.networkSynchronized(BlockPos.STREAM_CODEC)
			.build());
		
		ServerConfig.initServerConfig();
		
		modBus.addListener(this::onRegisterPackets);
		
		forgeBus.addListener(EventPriority.LOW, this::onUseItemOnBlock);
		forgeBus.addListener(EventPriority.LOW, this::onLeftClickBlock);
		forgeBus.addListener(this::onUseItemOnBlock);
		forgeBus.addListener(this::onChunkWatch);
		
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
			private static TagKey<Block> tag(String name) { return TagKey.create(Registries.BLOCK, getModRL(name)); }
			
			public static final TagKey<Block> IGNORE_VANILLA_SIGNAL = tag("ignore_vanilla_signal");
			public static final TagKey<Block> BUNDLED_CABLE_POSTS = tag(ObjectNames.CABLE_POSTS);
			public static final TagKey<Block> COLORED_CABLES = tag(ObjectNames.COLORED_CABLES);
			public static final TagKey<Block> REDWIRE_POSTS = tag(ObjectNames.REDWIRE_POSTS);
			public static final TagKey<Block> BITWISE_GATES = tag(ObjectNames.BITWISE_GATES);
			
			public static final TagKey<Block> WIRABLE_CUBE = tag("wirable/cube");
			public static final TagKey<Block> WIRABLE_FLOOR = tag("wirable/floor");
			public static final TagKey<Block> WIRABLE_INVERTED_WALL_FLOOR_CEILING = tag("wirable/inverted_wall_floor_ceiling");
			public static final TagKey<Block> WIRABLE_WIRE_POSTS = tag("wirable/wire_posts");
			public static final TagKey<Block> WIRABLE = tag("wirable/wires");
			public static final TagKey<Block> WIRABLE_BITWISE_GATES = tag("wirable/bitwise_gates");
		}
		
		public static class Items
		{
			private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, getModRL(name)); }
			
			public static final TagKey<Item> BUNDLED_CABLES = tag(ObjectNames.BUNDLED_CABLES);
			public static final TagKey<Item> COLORED_CABLES = tag(ObjectNames.COLORED_CABLES);
			public static final TagKey<Item> CABLES = tag(ObjectNames.CABLES);
			public static final TagKey<Item> RED_ALLOY_WIRES = tag(ObjectNames.RED_ALLOY_WIRES);
			public static final TagKey<Item> RED_ALLOYABLE_INGOTS = tag(ObjectNames.RED_ALLOYABLE_INGOTS);
		}
	}
	
	private void onRegisterPackets(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar r = event.registrar("6.0.0");
		r.playToServer(SolderingRecipeButtonPacket.TYPE, SolderingRecipeButtonPacket.STREAM_CODEC, SolderingRecipeButtonPacket::handle);
		r.playToClient(WireBreakPacket.TYPE, WireBreakPacket.STREAM_CODEC, WireBreakPacket::handle);
		r.playToClient(SyncPostsInChunkPacket.TYPE, SyncPostsInChunkPacket.STREAM_CODEC, SyncPostsInChunkPacket::handle);
		r.playToClient(WireUpdatePacket.TYPE, WireUpdatePacket.STREAM_CODEC, WireUpdatePacket::handle);
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
			double range = ServerConfig.INSTANCE.maxWirePostConnectionRange().get();
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
					Set<BlockPos> posts = chunk.getData(this.postsInChunkAttachment.get());
					if (posts != null) {
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
										serverLevel.sendParticles(serverPlayer, DustParticleOptions.REDSTONE, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
									}
									else if (level.isClientSide)
									{
										level.addParticle(DustParticleOptions.REDSTONE, hit.x, hit.y, hit.z, 0.05D, 0.05D, 0.05D);
									}
									
									if (player != null)
									{
										player.playNotifySound(SoundEvents.WANDERING_TRADER_HURT, SoundSource.BLOCKS, 0.5F, 2F);
									}
									event.cancelWithResult(ItemInteractionResult.SUCCESS);
									return;
								}
								else
								{
									checkedPostPositions.add(postPos.immutable());
								}
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
			|| (pos.getY() >= serverLevel.getMaxBuildHeight() || pos.getY() < serverLevel.getMinBuildHeight())
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
	}
	
	private static <T> DeferredRegister<T> createDeferredRegister(IEventBus modBus, ResourceKey<Registry<T>> registryKey)
	{
		var reg = DeferredRegister.create(registryKey, MODID);
		reg.register(modBus);
		return reg;
	}
	
	private static <BLOCK extends Block, ITEM extends BlockItem> DeferredHolder<Block, BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<? extends BLOCK> blockFactory,
		Function<? super BLOCK, ? extends ITEM> itemFactory)
	{
		DeferredHolder<Block, BLOCK> block = blocks.register(name, blockFactory);
		items.register(name, () -> itemFactory.apply(block.get()));
		return block;
	}
	
	private static <BLOCK extends Block> DeferredHolder<Block, BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<? extends BLOCK> blockFactory)
	{
		return registerBlockItem(blocks, items, name, blockFactory, block -> new BlockItem(block, new Item.Properties()));
	}
	
	public DeferredHolder<Block, LogicFunctionPlateBlock> registerLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		LogicFunctionPlateBlockFactory factory)
	{
		Supplier<LogicFunctionPlateBlock> blockFactory = () -> factory.makeBlock(function,
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD));
		DeferredHolder<Block, LogicFunctionPlateBlock> blockGetter = registerBlockItem(blocks, items, name, blockFactory);
		logicPlates.put(blockGetter.getId(), blockGetter);
		return blockGetter;
	}
	
	public <B extends BitewiseGateBlock> DeferredHolder<Block, B> registerBitwiseLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		BiFunction<BlockBehaviour.Properties, LogicFunction, B> blockFactory)
	{
		Supplier<B> actualBlockFactory = () -> blockFactory.apply(
			BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD), function);
		DeferredHolder<Block, B> rob = registerBlockItem(blocks, items, name, actualBlockFactory);
		bitwiseLogicPlates.put(rob.getId(), rob);
		return rob;
	}
}
