package commoble.morered;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

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
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
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
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;

@Mod(MoreRed.MODID)
public class MoreRed
{
	public static final String MODID = "morered";
	
	public static final Optional<ClientProxy> CLIENT_PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::makeClientProxy, () -> () -> Optional.empty());
	
	// the network channel we'll use for sending packets associated with this mod
	public static final String CHANNEL_PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MoreRed.MODID, "main"),
		() -> CHANNEL_PROTOCOL_VERSION,
		CHANNEL_PROTOCOL_VERSION::equals,
		CHANNEL_PROTOCOL_VERSION::equals);
	
	public static ResourceLocation getModRL(String name)
	{
		return new ResourceLocation(MODID, name);
	}
	
	public MoreRed()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		ServerConfig.initServerConfig();
		
		MoreRed.addModListeners(modBus);
		MoreRed.addForgeListeners(forgeBus);
		
		// add layer of separation to client stuff so we don't break servers
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientEvents.addClientListeners(modBus, forgeBus);
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
		modBus.addListener(MoreRed::onCommonSetup);
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
		
		// register capabilities
		CapabilityManager.INSTANCE.register(IPostsInChunk.class, new PostsInChunkCapability.Storage(), () -> new PostsInChunk(null));
	}
	
	public static void addForgeListeners(IEventBus forgeBus)
	{
		forgeBus.addGenericListener(Chunk.class, MoreRed::onAttachChunkCapabilities);
		forgeBus.addListener(EventPriority.LOW, MoreRed::onEntityPlaceBlock);
		forgeBus.addListener(MoreRed::onPlayerStartWatchingChunk);
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
	
	// sync redwire post positions to clients when a chunk needs to be loaded on the client
	public static void onPlayerStartWatchingChunk(ChunkWatchEvent.Watch event)
	{

		ServerWorld world = event.getWorld();
		ChunkPos pos = event.getPos();
		Chunk chunk = world.getChunkAt(pos.asBlockPos());
		chunk.getCapability(PostsInChunkCapability.INSTANCE).ifPresent(cap -> 
			CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), new SyncPostsInChunkPacket(pos, cap.getPositions()))
		);
	}
}
