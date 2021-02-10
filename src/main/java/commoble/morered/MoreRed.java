package commoble.morered;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.client.ClientEvents;
import commoble.morered.client.ClientProxy;
import commoble.morered.gatecrafting_plinth.GatecraftingRecipeButtonPacket;
import commoble.morered.plate_blocks.LogicGateType;
import commoble.morered.wire_post.IPostsInChunk;
import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wire_post.PostsInChunkCapability;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.SyncPostsInChunkPacket;
import commoble.morered.wire_post.WireBreakPacket;
import commoble.morered.wire_post.WirePostTileEntity;
import commoble.morered.wires.WireBlock;
import commoble.morered.wires.WireConnectors;
import commoble.morered.wires.WireCountLootFunction;
import commoble.morered.wires.WireUpdateBuffer;
import commoble.morered.wires.WireUpdatePacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	public static final Logger LOGGER = LogManager.getLogger();
	public static MoreRed INSTANCE;
	
	public static Optional<ClientProxy> CLIENT_PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::makeClientProxy, () -> () -> Optional.empty());
	
	// the network channel we'll use for sending packets associated with this mod
	public static final String CHANNEL_PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MoreRed.MODID, "main"),
		() -> CHANNEL_PROTOCOL_VERSION,
		CHANNEL_PROTOCOL_VERSION::equals,
		CHANNEL_PROTOCOL_VERSION::equals);
	
	private Map<Block, WireConnector> wireConnectabilities = new ConcurrentHashMap<>();
	public Map<Block, WireConnector> getWireConnectabilities() { return this.wireConnectabilities; }
	
	public static ResourceLocation getModRL(String name)
	{
		return new ResourceLocation(MODID, name);
	}
	
	public MoreRed()
	{
		INSTANCE = this;
		
		
		
		ModLoadingContext modContext = ModLoadingContext.get();
		FMLJavaModLoadingContext fmlContext = FMLJavaModLoadingContext.get();
		IEventBus modBus = fmlContext.getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		ServerConfig.initServerConfig(modContext, fmlContext);
		
		MoreRed.addModListeners(modBus);
		MoreRed.addForgeListeners(forgeBus);
		
		// add layer of separation to client stuff so we don't break servers
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientEvents.addClientListeners(modContext, fmlContext, modBus, forgeBus);
		}
		
		// blocks and blockitems for the logic gates are registered here
		LogicGateType.registerLogicGateTypes(BlockRegistrar.BLOCKS, ItemRegistrar.ITEMS);
	}
	
	public static void addModListeners(IEventBus modBus)
	{
		subscribedDeferredRegisters(modBus,
			BlockRegistrar.BLOCKS,
			ItemRegistrar.ITEMS,
			TileEntityRegistrar.TILES,
			ContainerRegistrar.CONTAINER_TYPES,
			RecipeRegistrar.RECIPE_SERIALIZERS);
		
		modBus.addGenericListener(IRecipeSerializer.class, MoreRed::onRegisterRecipeStuff);
		modBus.addListener(EventPriority.HIGH, MoreRed::onHighPriorityCommonSetup);
		modBus.addListener(MoreRed::onCommonSetup);
		modBus.addListener(MoreRed.INSTANCE::onLoadComplete);
	}
	
	private static void onRegisterRecipeStuff(RegistryEvent.Register<IRecipeSerializer<?>> event)
	{
		// forge registers ingredient serializers here for some reason, might as well do it here too
		CraftingHelper.register(new ResourceLocation("morered:tag_stack"), TagStackIngredient.SERIALIZER);
	}
	
	public static void subscribedDeferredRegisters(IEventBus modBus, DeferredRegister<?>... registers)
	{
		for(DeferredRegister<?> register : registers)
		{
			register.register(modBus);
		}
	}
	
	public static void onHighPriorityCommonSetup(FMLCommonSetupEvent event)
	{
		Map<Block, WireConnector> wireConnectors = MoreRedAPI.getWireConnectabilityRegistry();
		
		// add behaviour for vanilla objects
		wireConnectors.put(Blocks.REDSTONE_WIRE, WireConnectors::isRedstoneWireConnectable);
		
		// add behaviour for More Red objects
		MoreRedAPI.getWireConnectabilityRegistry().put(BlockRegistrar.RED_ALLOY_WIRE.get(), BlockRegistrar.RED_ALLOY_WIRE.get());
	}
	
	public static void onCommonSetup(FMLCommonSetupEvent event)
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
		
		// register capabilities
		CapabilityManager.INSTANCE.register(IPostsInChunk.class, new PostsInChunkCapability.Storage(), () -> new PostsInChunk(null));
		
		event.enqueueWork(MoreRed::afterCommonSetup);
	}
	
	static void afterCommonSetup()
	{
		Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(MODID, ObjectNames.WIRE_COUNT), WireCountLootFunction.TYPE);
	}

	void onLoadComplete(FMLLoadCompleteEvent event)
	{
		ImmutableMap.Builder<Block, WireConnector> builder = ImmutableMap.builder();
		this.wireConnectabilities.forEach(builder::put);
		this.wireConnectabilities = builder.build();
	}
	
	public static void addForgeListeners(IEventBus forgeBus)
	{
		forgeBus.addGenericListener(Chunk.class, MoreRed::onAttachChunkCapabilities);
		forgeBus.addListener(EventPriority.LOW, MoreRed::onEntityPlaceBlock);
		forgeBus.addListener(EventPriority.LOW, MoreRed::onLeftClickBlock);
		forgeBus.addListener(MoreRed::onWorldTick);
	}
	
	public static void onAttachChunkCapabilities(AttachCapabilitiesEvent<Chunk> event)
	{
		PostsInChunk cap = new PostsInChunk(event.getObject());
		event.addCapability(getModRL(ObjectNames.POSTS_IN_CHUNK), cap);
		event.addListener(cap::onCapabilityInvalidated);
	}
	
	// catch and deny block placements on the server if they weren't caught on the client
	public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event)
	{
		BlockPos pos = event.getPos();
		IWorld iworld = event.getWorld();
		BlockState state = event.getState();
		if (iworld instanceof World && !iworld.isRemote())
		{
			World world = (World)iworld;
			
			Set<ChunkPos> chunkPositions = PostsInChunk.getRelevantChunkPositionsNearPos(pos);
			
			for (ChunkPos chunkPos : chunkPositions)
			{
				if (world.isBlockLoaded(chunkPos.asBlockPos()))
				{
					Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
					chunk.getCapability(PostsInChunkCapability.INSTANCE).ifPresent(posts ->
					{
						Set<BlockPos> checkedPostPositions = new HashSet<BlockPos>();
						for (BlockPos postPos : posts.getPositions())
						{
							TileEntity te = world.getTileEntity(postPos);
							if (te instanceof WirePostTileEntity)
							{
								Vector3d hit = SlackInterpolator.doesBlockStateIntersectAnyWireOfPost(world, postPos, pos, state, ((WirePostTileEntity)te).getRemoteConnectionBoxes(), checkedPostPositions);
								if (hit != null)
								{
									event.setCanceled(true);
									Entity entity = event.getEntity();
									if (entity instanceof ServerPlayerEntity)
									{
										ServerPlayerEntity serverPlayer = (ServerPlayerEntity)entity;
										serverPlayer.connection.sendPacket(new SEntityEquipmentPacket(serverPlayer.getEntityId(), Lists.newArrayList(Pair.of(EquipmentSlotType.MAINHAND, serverPlayer.getHeldItem(Hand.MAIN_HAND)))));
//										((ServerWorld)world).spawnParticle(serverPlayer, RedstoneParticleData.REDSTONE_DUST, false, hit.x, hit.y, hit.z, 5, .05, .05, .05, 0);
//										serverPlayer.playSound(SoundEvents.ENTITY_WANDERING_TRADER_HURT, SoundCategory.BLOCKS, 0.5F, 2F);
									}
									return;
								}
								else
								{
									checkedPostPositions.add(postPos);
								}
							}
						}
					});
				}
			}
		}
	}
	
	public static void onLeftClickBlock(LeftClickBlock event)
	{
		World world = event.getWorld();
		if (!(world instanceof ServerWorld))
			return;
		BlockPos pos = event.getPos();
		BlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (!(block instanceof WireBlock))
			return;
		PlayerEntity player = event.getPlayer();
		if (!(player instanceof ServerPlayerEntity))
			return;
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
		boolean blockActionRestricted = player.blockActionRestricted(world, pos, serverPlayer.interactionManager.getGameType());
		boolean isCreative = player.isCreative();
		if (!isCreative && (event.getUseItem() == Event.Result.DENY || blockActionRestricted))
			return;
		ServerWorld serverWorld = (ServerWorld)world;
		if (pos.getY() > serverWorld.getServer().getBuildLimit())
			return;
		if (!world.isBlockModifiable(player, pos))
			return;
		
		double dx = player.getPosX() - (pos.getX() + 0.5D);
		double dy = player.getPosY() - (pos.getY() + 0.5D) + 1.5D;
		double dz = player.getPosZ() - (pos.getZ() + 0.5D);
		double distSquared = dx * dx + dy * dy + dz * dz;
		double reachDistance = player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue() + 1;
		if (distSquared > reachDistance*reachDistance)
			return;
		
		WireBlock wireBlock = (WireBlock) block;
		
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
			state.onBlockClicked(world, pos, player);
		}

		int exp = net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(world, serverPlayer.interactionManager.getGameType(), serverPlayer, pos);
		if (exp == -1)
			return;
		
		if (player.getHeldItemMainhand().onBlockStartBreak(pos, player))
            return;
		
		// checking blockActionRestricted again looks weird, but it's for parity with normal logic
		// if the player is in creative, they're allowed to fire the block break event and onBlockStartBreak first
		// then this gets checked again even if they are in creative
		if (blockActionRestricted)
			return;
		
		// don't drop items if creative
		boolean dropItems = !isCreative;
		wireBlock.destroyClickedSegment(state, serverWorld, pos, serverPlayer, destroySide, dropItems);
	}
	
	static void onWorldTick(WorldTickEvent event)
	{
		World world = event.world;
		if (event.phase == TickEvent.Phase.END && world instanceof ServerWorld)
		{
			ServerWorld serverWorld = (ServerWorld)world;
			WireUpdateBuffer.get(serverWorld).sendPackets(serverWorld);
		}
	}
}
