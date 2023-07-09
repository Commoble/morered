package commoble.morered;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.ExpandedPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.api.internal.APIRegistries;
import commoble.morered.api.internal.DefaultWireProperties;
import commoble.morered.bitwise_logic.BitwiseLogicPlateBlock;
import commoble.morered.bitwise_logic.ChanneledPowerStorageBlockEntity;
import commoble.morered.bitwise_logic.SingleInputBitwiseLogicPlateBlock;
import commoble.morered.bitwise_logic.TwoInputBitwiseLogicPlateBlock;
import commoble.morered.client.ClientProxy;
import commoble.morered.gatecrafting_plinth.GatecraftingMenu;
import commoble.morered.gatecrafting_plinth.GatecraftingPlinthBlock;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeButtonPacket;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeSerializer;
import commoble.morered.plate_blocks.LatchBlock;
import commoble.morered.plate_blocks.LogicFunction;
import commoble.morered.plate_blocks.LogicFunctionPlateBlock;
import commoble.morered.plate_blocks.LogicFunctionPlateBlock.LogicFunctionPlateBlockFactory;
import commoble.morered.plate_blocks.LogicFunctions;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.wire_post.BundledCablePostBlock;
import commoble.morered.wire_post.BundledCablePostBlockEntity;
import commoble.morered.wire_post.BundledCableRelayPlateBlock;
import commoble.morered.wire_post.BundledCableRelayPlateBlockEntity;
import commoble.morered.wire_post.FakeStateLevel;
import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.SyncPostsInChunkPacket;
import commoble.morered.wire_post.WireBreakPacket;
import commoble.morered.wire_post.WirePostBlock;
import commoble.morered.wire_post.WirePostBlockEntity;
import commoble.morered.wire_post.WirePostPlateBlock;
import commoble.morered.wire_post.WireSpoolItem;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.BundledCableBlock;
import commoble.morered.wires.BundledCableBlockEntity;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.ColoredCableBlockEntity;
import commoble.morered.wires.RedAlloyWireBlock;
import commoble.morered.wires.WireBlockEntity;
import commoble.morered.wires.WireBlockItem;
import commoble.morered.wires.WireCountLootFunction;
import commoble.morered.wires.WireUpdateBuffer;
import commoble.morered.wires.WireUpdatePacket;
import commoble.useitemonblockevent.api.UseItemOnBlockEvent;
import commoble.useitemonblockevent.api.UseItemOnBlockEvent.UsePhase;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	public static final Logger LOGGER = LogManager.getLogger();
	private static MoreRed instance;
	public static MoreRed get() { return instance; }
	
	// the network channel we'll use for sending packets associated with this mod
	public static final String CHANNEL_PROTOCOL_VERSION = "2";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MoreRed.MODID, "main"),
		() -> CHANNEL_PROTOCOL_VERSION,
		CHANNEL_PROTOCOL_VERSION::equals,
		CHANNEL_PROTOCOL_VERSION::equals);
	
	public static ResourceLocation getModRL(String name)
	{
		return new ResourceLocation(MODID, name);
	}

	public final Map<ResourceLocation, RegistryObject<? extends LogicFunctionPlateBlock>> logicPlates = new HashMap<>();
	public final Map<ResourceLocation, RegistryObject<? extends BitwiseLogicPlateBlock>> bitwiseLogicPlates = new HashMap<>();
	
	public final RegistryObject<GatecraftingPlinthBlock> gatecraftingPlinthBlock;
	public final RegistryObject<PlateBlock> stonePlateBlock;
	public final RegistryObject<LatchBlock> latchBlock;
	public final RegistryObject<WirePostBlock> redwirePostBlock;
	public final RegistryObject<WirePostPlateBlock> redwirePostPlateBlock;
	public final RegistryObject<WirePostPlateBlock> redwirePostRelayPlateBlock;
	public final RegistryObject<HexidecrubrometerBlock> hexidecrubrometerBlock;
	public final RegistryObject<RedAlloyWireBlock> redAlloyWireBlock;
	public final RegistryObject<ColoredCableBlock>[] networkCableBlocks;
	public final RegistryObject<BundledCableBlock> bundledNetworkCableBlock;
	public final RegistryObject<BundledCablePostBlock> bundledCablePostBlock;
	public final RegistryObject<BundledCableRelayPlateBlock> bundledCableRelayPlateBlock;

	public final RegistryObject<WireSpoolItem> redwireSpoolItem;
	public final RegistryObject<Item> bundledCableSpoolItem;
	public final RegistryObject<Item> redAlloyIngotItem;
	
	public final RegistryObject<CreativeModeTab> tab;

	public final RegistryObject<BlockEntityType<WirePostBlockEntity>> redwirePostBeType;
	public final RegistryObject<BlockEntityType<WireBlockEntity>> wireBeType;
	public final RegistryObject<BlockEntityType<ColoredCableBlockEntity>> coloredNetworkCableBeType;
	public final RegistryObject<BlockEntityType<BundledCableBlockEntity>> bundledNetworkCableBeType;
	public final RegistryObject<BlockEntityType<BundledCablePostBlockEntity>> bundledCablePostBeType;
	public final RegistryObject<BlockEntityType<BundledCableRelayPlateBlockEntity>> bundledCableRelayPlateBeType;
	public final RegistryObject<BlockEntityType<ChanneledPowerStorageBlockEntity>> bitwiseLogicGateBeType;

	public final RegistryObject<MenuType<GatecraftingMenu>> gatecraftingMenuType;
	public final RegistryObject<GatecraftingRecipeSerializer> gatecraftingSerializer;
	public final RegistryObject<RecipeType<Recipe<CraftingContainer>>> gatecraftingRecipeType;
	
	public final RegistryObject<LootItemFunctionType> wireCountLootFunction;
	
	@SuppressWarnings("unchecked")
	public MoreRed()
	{
		instance = this;
		
		ModLoadingContext modContext = ModLoadingContext.get();
		FMLJavaModLoadingContext fmlContext = FMLJavaModLoadingContext.get();
		IEventBus modBus = fmlContext.getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;

		DeferredRegister<Block> blocks = createDeferredRegister(modBus, Registries.BLOCK);
		DeferredRegister<Item> items = createDeferredRegister(modBus, Registries.ITEM);
		DeferredRegister<CreativeModeTab> tabs = createDeferredRegister(modBus, Registries.CREATIVE_MODE_TAB);
		DeferredRegister<BlockEntityType<?>> blockEntityTypes = createDeferredRegister(modBus, Registries.BLOCK_ENTITY_TYPE);
		DeferredRegister<MenuType<?>> menuTypes = createDeferredRegister(modBus, Registries.MENU);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = createDeferredRegister(modBus, Registries.RECIPE_SERIALIZER);
		DeferredRegister<RecipeType<?>> recipeTypes = createDeferredRegister(modBus, Registries.RECIPE_TYPE);
		DeferredRegister<LootItemFunctionType> lootFunctions = createDeferredRegister(modBus, Registries.LOOT_FUNCTION_TYPE);
		
		gatecraftingPlinthBlock = registerBlockItem(blocks, items, ObjectNames.GATECRAFTING_PLINTH,
			() -> new GatecraftingPlinthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.5F).noOcclusion()));
		stonePlateBlock = registerBlockItem(blocks, items, ObjectNames.STONE_PLATE,
			() -> new PlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F).sound(SoundType.WOOD)));
		latchBlock = registerBlockItem(blocks, items, ObjectNames.LATCH,
			() -> new LatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0).sound(SoundType.WOOD)));
		redwirePostBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_POST,
			() -> new WirePostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F), WirePostBlock::getRedstoneConnectionDirections));
		redwirePostPlateBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_POST_PLATE,
			() -> new WirePostPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForEmptyPlate));
		redwirePostRelayPlateBlock = registerBlockItem(blocks, items, ObjectNames.REDWIRE_POST_RELAY_PLATE,
			() -> new WirePostPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F), WirePostPlateBlock::getRedstoneConnectionDirectionsForRelayPlate));
		hexidecrubrometerBlock = registerBlockItem(blocks, items, ObjectNames.HEXIDECRUBROMETER,
			() -> new HexidecrubrometerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		bundledCablePostBlock = registerBlockItem(blocks, items, ObjectNames.BUNDLED_CABLE_POST,
			() -> new BundledCablePostBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		bundledCableRelayPlateBlock = registerBlockItem(blocks, items, ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
			() -> new BundledCableRelayPlateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASEDRUM).strength(2F, 5F)));
		
		redAlloyWireBlock = registerBlockItem(blocks, items, ObjectNames.RED_ALLOY_WIRE, 
			() -> new RedAlloyWireBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).noCollission().instabreak()),
			block -> new WireBlockItem(block, new Item.Properties()));
		networkCableBlocks = Util.make((RegistryObject<ColoredCableBlock>[])new RegistryObject[16], array ->
			Arrays.setAll(array, i -> registerBlockItem(blocks, items, ObjectNames.NETWORK_CABLES[i],
				() -> new ColoredCableBlock(BlockBehaviour.Properties.of().mapColor(DyeColor.values()[i]).pushReaction(PushReaction.DESTROY).noCollission().instabreak(), DyeColor.values()[i]),
				block -> new WireBlockItem(block, new Item.Properties()))));
		bundledNetworkCableBlock = registerBlockItem(blocks, items, ObjectNames.BUNDLED_NETWORK_CABLE,
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
		registerLogicGateType(blocks, items, ObjectNames.AND_2_GATE, LogicFunctions.AND_2, LogicFunctionPlateBlock.T_INPUTS);
		registerLogicGateType(blocks, items, ObjectNames.NAND_2_GATE, LogicFunctions.NAND_2, LogicFunctionPlateBlock.T_INPUTS);
		
		// bitwise logic gates store state in a TE instead of block properties
		// they don't need to have their properties defined on construction but they do need to be registered to the TE type they use
		BiFunction<BlockBehaviour.Properties, LogicFunction, SingleInputBitwiseLogicPlateBlock> singleInput = SingleInputBitwiseLogicPlateBlock::new;
		BiFunction<BlockBehaviour.Properties, LogicFunction, TwoInputBitwiseLogicPlateBlock> twoInputs = TwoInputBitwiseLogicPlateBlock::new;
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_DIODE, LogicFunctions.INPUT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_NOT_GATE, LogicFunctions.NOT_B, singleInput);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_OR_GATE, LogicFunctions.OR, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_AND_GATE, LogicFunctions.AND_2, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_XOR_GATE, LogicFunctions.XOR_AC, twoInputs);
		registerBitwiseLogicGateType(blocks, items, ObjectNames.BITWISE_XNOR_GATE, LogicFunctions.XNOR_AC, twoInputs);

		redwireSpoolItem = items.register(ObjectNames.REDWIRE_SPOOL, () -> new WireSpoolItem(new Item.Properties().durability(64), MoreRed.Tags.Blocks.REDWIRE_POSTS));
		bundledCableSpoolItem = items.register(ObjectNames.BUNDLED_CABLE_SPOOL, () -> new WireSpoolItem(new Item.Properties().durability(64), MoreRed.Tags.Blocks.BUNDLED_CABLE_POSTS));
		redAlloyIngotItem = items.register(ObjectNames.RED_ALLOY_INGOT, () -> new Item(new Item.Properties()));

		ResourceLocation tabIconId = new ResourceLocation(MODID, ObjectNames.NOR_GATE);
		this.tab = tabs.register(MoreRed.MODID, () -> CreativeModeTab.builder()
			.icon(() -> new ItemStack(this.logicPlates.get(tabIconId).get()))
			.title(Component.translatable("itemGroup.morered"))
			.displayItems((params, output) -> output.acceptAll(items.getEntries().stream().map(rob -> new ItemStack(rob.get())).toList()))
			.build()
			);
		
		redwirePostBeType = blockEntityTypes.register(ObjectNames.REDWIRE_POST,
			() -> BlockEntityType.Builder.of(WirePostBlockEntity::new,
				redwirePostBlock.get(),
				redwirePostPlateBlock.get(),
				redwirePostRelayPlateBlock.get())
			.build(null));
		wireBeType = blockEntityTypes.register(ObjectNames.WIRE,
			() -> BlockEntityType.Builder.of(WireBlockEntity::new,
				redAlloyWireBlock.get())
			.build(null));
		coloredNetworkCableBeType = blockEntityTypes.register(ObjectNames.COLORED_NETWORK_CABLE,
			() -> BlockEntityType.Builder.of(ColoredCableBlockEntity::new,
				Arrays.stream(networkCableBlocks)
					.map(RegistryObject::get)
					.toArray(ColoredCableBlock[]::new))
			.build(null));
		bundledNetworkCableBeType = blockEntityTypes.register(ObjectNames.BUNDLED_NETWORK_CABLE,
			() -> BlockEntityType.Builder.of(BundledCableBlockEntity::new,
				bundledNetworkCableBlock.get())
			.build(null));
		bundledCablePostBeType = blockEntityTypes.register(ObjectNames.BUNDLED_CABLE_POST,
			() -> BlockEntityType.Builder.of(BundledCablePostBlockEntity::new,
				bundledCablePostBlock.get())
			.build(null));
		bundledCableRelayPlateBeType = blockEntityTypes.register(ObjectNames.BUNDLED_CABLE_RELAY_PLATE,
			() -> BlockEntityType.Builder.of(BundledCableRelayPlateBlockEntity::new,
				bundledCableRelayPlateBlock.get())
			.build(null));
		bitwiseLogicGateBeType = blockEntityTypes.register(ObjectNames.BITWISE_LOGIC_PLATE,
			() -> BlockEntityType.Builder.of(ChanneledPowerStorageBlockEntity::new,
				Util.make(() ->
				{	// valid blocks are all of the bitwise logic gate blocks registered from LogicGateType
					return this.bitwiseLogicPlates.values().stream()
						.map(rob -> rob.get())
						.toArray(Block[]::new);
				}))
			.build(null));

		gatecraftingMenuType = menuTypes.register(ObjectNames.GATECRAFTING_PLINTH,
			() -> new MenuType<>(GatecraftingMenu::getClientContainer, FeatureFlags.VANILLA_SET));
		gatecraftingSerializer = recipeSerializers.register(ObjectNames.GATECRAFTING_RECIPE,
			() -> new GatecraftingRecipeSerializer());
		gatecraftingRecipeType = recipeTypes.register(ObjectNames.GATECRAFTING_RECIPE, () -> RecipeType.simple(new ResourceLocation(MODID, ObjectNames.GATECRAFTING_RECIPE)));
		
		wireCountLootFunction = lootFunctions.register(ObjectNames.WIRE_COUNT, () -> new LootItemFunctionType(WireCountLootFunction.INSTANCE));
		
		ServerConfig.initServerConfig(modContext, fmlContext);
		
		modBus.addListener(EventPriority.HIGH, this::onHighPriorityCommonSetup);
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::onLoadComplete);
		modBus.addListener(this::onRegisterCapabilities);
		
		forgeBus.addGenericListener(LevelChunk.class, this::onAttachChunkCapabilities);
		forgeBus.addListener(EventPriority.LOW, this::onUseItemOnBlock);
		forgeBus.addListener(EventPriority.LOW, this::onLeftClickBlock);
		forgeBus.addListener(this::onLevelTick);
		forgeBus.addListener(this::onUseItemOnBlock);
		forgeBus.addListener(this::onChunkWatch);
		
		// add layer of separation to client stuff so we don't break servers
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientProxy.addClientListeners(modContext, fmlContext, modBus, forgeBus);
		}
	}
	
	public static class Tags
	{
		public static class Blocks
		{
			private static TagKey<Block> tag(String name) { return TagKey.create(Registries.BLOCK, new ResourceLocation(MODID, name)); }
			
			public static final TagKey<Block> REDWIRE_POSTS = tag(ObjectNames.REDWIRE_POSTS);
			public static final TagKey<Block> BUNDLED_CABLE_POSTS = tag(ObjectNames.BUNDLED_CABLE_POSTS);
		}
		
		public static class Items
		{
			private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, new ResourceLocation(MODID, name)); }
			
			public static final TagKey<Item> RED_ALLOY_WIRES = tag(ObjectNames.RED_ALLOY_WIRES);
			public static final TagKey<Item> COLORED_NETWORK_CABLES = tag(ObjectNames.COLORED_NETWORK_CABLES);
		}
	}
	
	public void addModListeners(IEventBus modBus)
	{
	}
	
	public static void subscribedDeferredRegisters(IEventBus modBus, DeferredRegister<?>... registers)
	{
		for(DeferredRegister<?> register : registers)
		{
			register.register(modBus);
		}
	}
	
	private void onHighPriorityCommonSetup(FMLCommonSetupEvent event)
	{
		Map<Block, WireConnector> wireConnectors = MoreRedAPI.getWireConnectabilityRegistry();
		Map<Block, ExpandedPowerSupplier> expandedPowerSuppliers = MoreRedAPI.getExpandedPowerRegistry();
		Map<Block, WireConnector> cableConnectors = MoreRedAPI.getCableConnectabilityRegistry();
		
		// add behaviour for vanilla objects
		wireConnectors.put(Blocks.REDSTONE_WIRE, DefaultWireProperties::isRedstoneWireConnectable);
		
		// add behaviour for More Red objects
		RedAlloyWireBlock redWireBlock = this.redAlloyWireBlock.get();
		wireConnectors.put(redWireBlock, AbstractWireBlock::canWireConnectToAdjacentWireOrCable);
		expandedPowerSuppliers.put(redWireBlock, redWireBlock::getExpandedPower);
		for (int i=0; i<16; i++)
		{
			ColoredCableBlock coloredCableBlock = networkCableBlocks[i].get();
			wireConnectors.put(coloredCableBlock, coloredCableBlock::canConnectToAdjacentWireOrCable);
			expandedPowerSuppliers.put(coloredCableBlock, coloredCableBlock::getExpandedPower);
			cableConnectors.put(coloredCableBlock, coloredCableBlock::canConnectToAdjacentWireOrCable);
		}
		BundledCableBlock bundledCableBlock = bundledNetworkCableBlock.get();
		cableConnectors.put(bundledCableBlock, AbstractWireBlock::canWireConnectToAdjacentWireOrCable);
		BundledCableRelayPlateBlock cablePlateBlock = bundledCableRelayPlateBlock.get();
		cableConnectors.put(cablePlateBlock, cablePlateBlock::canConnectToAdjacentCable);
		this.bitwiseLogicPlates.values().stream()
			.map(rob -> rob.get())
			// eclipse compiler allows a method reference to canConnectToAdjacentCable in the put here
			// but javac doesn't like the generics, but accepts a lambda here
			.forEach(block -> cableConnectors.put(block, (world,thisPos,thisState,wirePos,wireState,wireFace,directionToWire)->block.canConnectToAdjacentCable(world, thisPos, thisState, wirePos, wireState, wireFace, directionToWire)));
	}
	
	private void onRegisterCapabilities(RegisterCapabilitiesEvent event)
	{
		event.register(PostsInChunk.class);
		event.register(ChanneledPowerSupplier.class);
	}
	
	private void onCommonSetup(FMLCommonSetupEvent event)
	{
		// register packets
		int packetID = 0;
		MoreRed.CHANNEL.registerMessage(packetID++,
			GatecraftingRecipeButtonPacket.class,
			GatecraftingRecipeButtonPacket::write,
			GatecraftingRecipeButtonPacket::read,
			GatecraftingRecipeButtonPacket::handle);
		MoreRed.CHANNEL.registerMessage(packetID++,
			WireBreakPacket.class,
			WireBreakPacket::write,
			WireBreakPacket::read,
			WireBreakPacket::handle);
		MoreRed.CHANNEL.registerMessage(packetID++,
			SyncPostsInChunkPacket.class,
			SyncPostsInChunkPacket::write,
			SyncPostsInChunkPacket::read,
			SyncPostsInChunkPacket::handle);
		PacketTypeFactory.register(packetID++, MoreRed.CHANNEL, WireUpdatePacket.CODEC, new WireUpdatePacket(ImmutableSet.of()));
		
		// run thread-sensitive stuff on main thread
		event.enqueueWork(this::afterCommonSetup);
	}
	
	private void afterCommonSetup()
	{
		CraftingHelper.register(new ResourceLocation("morered:tag_stack"), TagStackIngredient.SERIALIZER);
	}

	private void onLoadComplete(FMLLoadCompleteEvent event)
	{
		// freeze API registries -- convert to immutable maps
		APIRegistries.freezeRegistries();
	}
	
	private void onAttachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event)
	{
		PostsInChunk cap = new PostsInChunk(event.getObject());
		event.addCapability(getModRL(ObjectNames.POSTS_IN_CHUNK), cap);
		event.addListener(cap::onCapabilityInvalidated);
	}
	
	@SuppressWarnings("deprecation")
	private void onUseItemOnBlock(UseItemOnBlockEvent event)
	{
		UseOnContext useContext = event.getUseOnContext();
		ItemStack stack = useContext.getItemInHand();
		if (event.getUsePhase() == UsePhase.POST_BLOCK && stack.getItem() instanceof BlockItem blockItem)
		{
			Level level = useContext.getLevel();
			BlockPlaceContext placeContext = new BlockPlaceContext(useContext);
			BlockPos placePos = placeContext.getClickedPos(); // getClickedPos is a misnomer, this is the position the block is placed at
			BlockState placementState = blockItem.getPlacementState(placeContext);
			if (placementState == null)
			{
				return; // placement state is null when the block couldn't be placed there anyway
			}
			Set<ChunkPos> chunkPositions = PostsInChunk.getRelevantChunkPositionsNearPos(placePos);
			
			for (ChunkPos chunkPos : chunkPositions)
			{
				if (level.hasChunkAt(chunkPos.getWorldPosition()))
				{
					LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
					chunk.getCapability(PostsInChunk.CAPABILITY).ifPresent(posts ->
					{
						Set<BlockPos> checkedPostPositions = new HashSet<BlockPos>();
						for (BlockPos postPos : posts.getPositions())
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
									event.cancelWithResult(InteractionResult.SUCCESS);
									return;
								}
								else
								{
									checkedPostPositions.add(postPos.immutable());
								}
							}
						}
					});
				}
			}
		}
	}
	
	private void onLeftClickBlock(LeftClickBlock event)
	{
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

		boolean blockActionRestricted = player.blockActionRestricted(level, pos, serverPlayer.gameMode.getGameModeForPlayer());
		boolean isCreative = player.isCreative();
		if (!isCreative && (event.getUseItem() == Event.Result.DENY || blockActionRestricted))
			return;
		if (pos.getY() > serverLevel.getMaxBuildHeight() || pos.getY() < serverLevel.getMinBuildHeight())
			return;
		if (!level.mayInteract(player, pos))
			return;

		// this was the old reach check, keep it around for a version or two so we can compare old behavior if we get buggy
//		double dx = player.getX() - (pos.getX() + 0.5D);
//		double dy = player.getY() - (pos.getY() + 0.5D) + 1.5D;
//		double dz = player.getZ() - (pos.getZ() + 0.5D);
//		double distSquared = dx * dx + dy * dy + dz * dz;
//		double reachDistance = player.getAttribute(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()).getValue() + 1;
//		if (distSquared > reachDistance*reachDistance)
//			return;
		if (!player.canReach(pos, 1.5D))
		{
			return;
		}
		
		// we have a specific enough situation to take over the event from this point forward
		event.setCanceled(true);
		// if we cancel the event, a digging response packet will be sent
		// using whatever state exists in the world *after* the event
		// so we can set our own blockstate here and it should be sent to the client correctly
		
		// we also really don't want to be sending any packets ourselves
		// each digging packet the client sent has to have a response packet for the same pos and action or we get log spam
		// the event handler will send a matching digging response for us if we cancel it
		// (it's a deny response, but it also immediately send a block update afterward, so that's fine)
		Direction hitNormal = event.getFace();
		Direction destroySide = hitNormal.getOpposite();
		if (!isCreative && (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY))
		{
			state.attack(level, pos, player);
		}

		int exp = ForgeHooks.onBlockBreakEvent(level, serverPlayer.gameMode.getGameModeForPlayer(), serverPlayer, pos);
		if (exp == -1)
			return;
		
		if (player.getMainHandItem().onBlockStartBreak(pos, player))
            return;
		
		// checking blockActionRestricted again looks weird, but it's for parity with normal logic
		// if the player is in creative, they're allowed to fire the block break event and onBlockStartBreak first
		// then this gets checked again even if they are in creative
		if (blockActionRestricted)
			return;
		
		// don't drop items if creative
		boolean dropItems = !isCreative;
		wireBlock.destroyClickedSegment(state, serverLevel, pos, serverPlayer, destroySide, dropItems);
	}
	
	private void onLevelTick(LevelTickEvent event)
	{
		Level level = event.level;
		if (event.phase == TickEvent.Phase.END && level instanceof ServerLevel serverLevel)
		{
			WireUpdateBuffer.get(serverLevel).sendPackets(serverLevel);
		}
	}
	
	// fired on the server just after vanilla chunk data is sent to the client
	private void onChunkWatch(ChunkWatchEvent.Watch event)
	{
		ServerPlayer player = event.getPlayer();
		LevelChunk chunk = event.getChunk();
		ChunkPos pos = chunk.getPos();
		chunk.getCapability(PostsInChunk.CAPABILITY).ifPresent(cap -> 
			MoreRed.CHANNEL.send(PacketDistributor.PLAYER.with(()->player), new SyncPostsInChunkPacket(pos, cap.getPositions()))
		);
	}
	
	private static <T> DeferredRegister<T> createDeferredRegister(IEventBus modBus, ResourceKey<Registry<T>> registryKey)
	{
		var reg = DeferredRegister.create(registryKey, MODID);
		reg.register(modBus);
		return reg;
	}
	
	private static <BLOCK extends Block, ITEM extends BlockItem> RegistryObject<BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<? extends BLOCK> blockFactory,
		Function<? super BLOCK, ? extends ITEM> itemFactory)
	{
		RegistryObject<BLOCK> block = blocks.register(name, blockFactory);
		items.register(name, () -> itemFactory.apply(block.get()));
		return block;
	}
	
	private static <BLOCK extends Block> RegistryObject<BLOCK> registerBlockItem(
		DeferredRegister<Block> blocks,
		DeferredRegister<Item> items,
		String name,
		Supplier<? extends BLOCK> blockFactory)
	{
		return registerBlockItem(blocks, items, name, blockFactory, block -> new BlockItem(block, new Item.Properties()));
	}

	public static Optional<Recipe<CraftingContainer>> getGatecraftingRecipe(RecipeManager manager, ResourceLocation id)
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<ResourceLocation, Recipe<CraftingContainer>> map = (Map)manager.recipes.getOrDefault(get().gatecraftingRecipeType.get(), Collections.emptyMap());
		return Optional.ofNullable(map.get(id));
	}
	
	public RegistryObject<LogicFunctionPlateBlock> registerLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		LogicFunctionPlateBlockFactory factory)
	{
		Supplier<LogicFunctionPlateBlock> blockFactory = () -> factory.makeBlock(function,
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD));
		RegistryObject<LogicFunctionPlateBlock> blockGetter = registerBlockItem(blocks, items, name, blockFactory);
		logicPlates.put(blockGetter.getId(), blockGetter);
		return blockGetter;
	}
	
	public <B extends BitwiseLogicPlateBlock> RegistryObject<B> registerBitwiseLogicGateType(DeferredRegister<Block> blocks, DeferredRegister<Item> items, String name,
		LogicFunction function,
		BiFunction<BlockBehaviour.Properties, LogicFunction, B> blockFactory)
	{
		Supplier<B> actualBlockFactory = () -> blockFactory.apply(
			BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).strength(0F).sound(SoundType.WOOD), function);
		RegistryObject<B> rob = registerBlockItem(blocks, items, name, actualBlockFactory);
		bitwiseLogicPlates.put(rob.getId(), rob);
		return rob;
	}
}
