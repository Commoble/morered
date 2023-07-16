package commoble.morered.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.mixin.MultiPlayerGameModeAccess;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.wire_post.PostsInChunk;
import commoble.morered.wire_post.SlackInterpolator;
import commoble.morered.wire_post.WireBreakPacket;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.VoxelCache;
import commoble.morered.wires.WireUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.RegistryObject;

public class ClientProxy
{	
	private static final Set<BlockPos> NO_POSTS = ImmutableSet.of();
	
	// block positions are in absolute world coordinates, not local chunk coords
	private static Map<ChunkPos, Set<BlockPos>> clientPostsInChunk = new HashMap<>();
	
	public static void clear()
	{
		clientPostsInChunk = new HashMap<>();
	}
	
	public static void updatePostsInChunk(ChunkPos pos, Set<BlockPos> posts)
	{
		@SuppressWarnings("resource")
		Level level = Minecraft.getInstance().level;
		if (level == null)
			return;
		
		LevelChunk chunk = level.getChunk(pos.x, pos.z);
		if (chunk == null)
			return;
		
		chunk.getCapability(PostsInChunk.CAPABILITY).ifPresent(cap -> cap.setPositions(Set.copyOf(posts)));
	}
	
	@Nonnull
	public static Set<BlockPos> getPostsInChunk(ChunkPos pos)
	{
		return clientPostsInChunk.getOrDefault(pos, NO_POSTS);
	}

	public static void initializeAbstractWireBlockClient(AbstractWireBlock block, Consumer<IClientBlockExtensions> consumer)
	{
		consumer.accept(new IClientBlockExtensions() {

			@Override
			public boolean addHitEffects(BlockState state, Level Level, HitResult target, ParticleEngine manager)
			{
				// if we have no wires here, we have no shape, so return true to disable particles
				// (otherwise the particle manager crashes because it performs an unsupported operation on the empty shape)
				return block.getWireCount(state) == 0;
			}
			
		});
	}

	public static void addClientListeners(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext, IEventBus modBus, IEventBus forgeBus)
	{
		ClientConfig.initClientConfig();
		
		modBus.addListener(ClientProxy::onClientSetup);
		modBus.addListener(ClientProxy::onRegisterModelLoaders);
		modBus.addListener(ClientProxy::onRegisterBlockColors);
		modBus.addListener(ClientProxy::onRegisterItemColors);
		modBus.addListener(ClientProxy::onRegisterRenderers);
		modBus.addListener(ClientProxy::onModifyBakedModels);
		
		forgeBus.addListener(ClientProxy::onClientLogIn);
		forgeBus.addListener(ClientProxy::onClientLogOut);
		forgeBus.addListener(ClientProxy::onHighlightBlock);
		forgeBus.addListener(ClientProxy::onInteract);
	}

	@SuppressWarnings("deprecation")
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		// render layer setting is synchronized, safe to do during multithreading
		MoreRed.get().logicPlates.values().forEach(rob -> ItemBlockRenderTypes.setRenderLayer(rob.get(), RenderType.cutout()));
		MoreRed.get().bitwiseLogicPlates.values()
			.forEach(rob -> ItemBlockRenderTypes.setRenderLayer(rob.get(), RenderType.cutout()));
		ItemBlockRenderTypes.setRenderLayer(MoreRed.get().latchBlock.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(MoreRed.get().pulseGateBlock.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(MoreRed.get().redwirePostPlateBlock.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(MoreRed.get().redwirePostRelayPlateBlock.get(), RenderType.cutout());
		ItemBlockRenderTypes.setRenderLayer(MoreRed.get().bundledCableRelayPlateBlock.get(), RenderType.cutout());
		
		event.enqueueWork(ClientProxy::afterClientSetup);
	}

	static void afterClientSetup()
	{
		// not threadsafe, do this on main thread
		MenuScreens.register(MoreRed.get().gatecraftingMenuType.get(), GatecraftingScreen::new);
	}

	public static void onRegisterModelLoaders(ModelEvent.RegisterGeometryLoaders event)
	{
		event.register(ObjectNames.WIRE_PARTS, WirePartModelLoader.INSTANCE);
		event.register(ObjectNames.ROTATE_TINTS, TintRotatingModelLoader.INSTANCE);
	}

	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event)
	{
		MoreRed.get().logicPlates.values().forEach(rob -> event.register(ColorHandlers::getLogicFunctionBlockTint, rob.get()));
		event.register(ColorHandlers::getLatchBlockTint, MoreRed.get().latchBlock.get());
		event.register(ColorHandlers::getPulseGateBlockTint, MoreRed.get().pulseGateBlock.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.get().redwirePostBlock.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.get().redwirePostPlateBlock.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.get().redwirePostRelayPlateBlock.get());
		event.register(ColorHandlers::getRedAlloyWireBlockTint, MoreRed.get().redAlloyWireBlock.get());
	}

	public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event)
	{
		MoreRed.get().logicPlates.values().forEach(rob -> event.register(ColorHandlers::getLogicFunctionBlockItemTint, rob.get().asItem()));
		event.register(ColorHandlers::getLatchItemTint, MoreRed.get().latchBlock.get().asItem());
		event.register(ColorHandlers::getPulseGateItemTint, MoreRed.get().pulseGateBlock.get().asItem());
		event.register(ColorHandlers::getRedwirePostItemTint, MoreRed.get().redwirePostBlock.get().asItem());
		event.register(ColorHandlers::getRedwirePostItemTint, MoreRed.get().redwirePostPlateBlock.get().asItem());
		event.register(ColorHandlers::getRedwirePostItemTint, MoreRed.get().redwirePostRelayPlateBlock.get().asItem());
		event.register(ColorHandlers::getRedAlloyWireItemTint, MoreRed.get().redAlloyWireBlock.get().asItem());
	}

	static void onRegisterRenderers(RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(MoreRed.get().redwirePostBeType.get(), WirePostRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.get().bundledCablePostBeType.get(), BundledCablePostRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.get().bundledCableRelayPlateBeType.get(), BundledCablePostRenderer::new);
	}

	@Deprecated
	static void onModifyBakedModels(ModelEvent.ModifyBakingResult event)
	{
		// replace multipart models with modeldata-sensitive ones
		var models = event.getModels();
		@SuppressWarnings("deprecation")
		Consumer<RegistryObject<? extends Block>> modelCheeser = rob -> {
			for (BlockState state : rob.get().getStateDefinition().getPossibleStates())
			{
				ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
				BakedModel model = models.get(mrl);
				if (model != null)
				{
					models.put(mrl, new WirePartModelLoader.MultipartWireModel(model));
				}
			}
		};
		
		modelCheeser.accept(MoreRed.get().redAlloyWireBlock);
		modelCheeser.accept(MoreRed.get().bundledNetworkCableBlock);
		for (var rob : MoreRed.get().networkCableBlocks)
		{
			modelCheeser.accept(rob);
		}
	}

	static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event)
	{
		// clean up static data on the client
		clear();
		VoxelCache.clearClientCache();
	}

	static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event)
	{
		// clean up static data on the client
		clear();
		VoxelCache.clearClientCache();
	}

	static void onHighlightBlock(RenderHighlightEvent.Block event)
	{
		if (ClientConfig.INSTANCE.showPlacementPreview().get())
		{
			@SuppressWarnings("resource")
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && player.level() != null)
			{
				InteractionHand hand = player.getUsedItemHand();
				Item item = player.getItemInHand(hand == null ? InteractionHand.MAIN_HAND : hand).getItem();
				if (item instanceof BlockItem blockItem)
				{
					Block block = blockItem.getBlock();
					if (block instanceof PlateBlock)
					{
						Level world = player.level();
						BlockHitResult rayTrace = event.getTarget();
						Direction directionAwayFromTargetedBlock = rayTrace.getDirection();
						BlockPos placePos = rayTrace.getBlockPos().relative(directionAwayFromTargetedBlock);
						
						BlockState existingState = world.getBlockState(placePos);
						if (existingState.isAir() || existingState.canBeReplaced())
						{
							// only render the preview if we know it would make sense for the block to be placed where we expect it to be
							Vec3 hitVec = rayTrace.getLocation();
							
							Direction attachmentDirection = directionAwayFromTargetedBlock.getOpposite();
							Vec3 relativeHitVec = hitVec.subtract(Vec3.atLowerCornerOf(placePos));
							
							Direction outputDirection = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, attachmentDirection);
							BlockStateUtil.getRotationIndexForDirection(attachmentDirection, outputDirection);
							BlockState state = PlateBlockStateProperties.getStateForPlacedGatePlate(block.defaultBlockState(), placePos, attachmentDirection, relativeHitVec);
							
							BlockPreviewRenderer.renderBlockPreview(placePos, state, world, event.getCamera().getPosition(), event.getPoseStack(), event.getMultiBufferSource());
							
						}
					}
				}
			}
		}
	}

	public static void onInteract(InputEvent.InteractionKeyMappingTriggered event)
	{
		// when the player clicks a wire block,
		// we want to handle the block in a manner that causes the digging packet to be sent
		// with the interior side of the wire block as the face value
		// (we can't do this perfectly with the raytrace shape normal overrider)
		// but we can do this by sending our own digging packet and bypassing the vanilla behaviour
		// (it helps that wire blocks are instant-break blocks)
		Minecraft mc = Minecraft.getInstance();
		HitResult rayTraceResult = mc.hitResult;
		ClientLevel world = mc.level;
		LocalPlayer player = mc.player;
		if (rayTraceResult != null && rayTraceResult.getLocation() != null && world != null && event.isAttack() && player != null && rayTraceResult.getType() == HitResult.Type.BLOCK)
		{
			BlockHitResult blockResult = (BlockHitResult) rayTraceResult;
			BlockPos pos = blockResult.getBlockPos();
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof AbstractWireBlock)
			{
				// we'll be taking over the event from here onward
				event.setCanceled(true);
				
				AbstractWireBlock wireBlock = (AbstractWireBlock)block;
				MultiPlayerGameMode controller = mc.gameMode;
				MultiPlayerGameModeAccess controllerAccess = (MultiPlayerGameModeAccess)controller;
				GameType gameType = controller.getPlayerMode();
				// now run over all the permissions checking that would normally happen here
				if (player.blockActionRestricted(world, pos, gameType))
					return;
				if (!world.getWorldBorder().isWithinBounds(pos))
					return;
				@Nullable Direction faceToBreak = wireBlock.getInteriorFaceToBreak(state, pos, player, blockResult, mc.getFrameTime());
				if (faceToBreak == null)
					return;
				Direction hitNormal = faceToBreak.getOpposite();
				if (gameType.isCreative())
				{
					Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hitNormal));
					// TODO do stuff from onPlayerDestroyBlock here
					if (!net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, pos, hitNormal).isCanceled())
					{
						destroyClickedWireBlock(wireBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
					controllerAccess.setDestroyDelay(5);
				}
				else if (!controller.isDestroying() || !controllerAccess.callSameDestroyTarget(pos))
				{
					if (controller.isDestroying())
					{
						Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, controllerAccess.getDestroyBlockPos(), blockResult.getDirection()));
					}
					PlayerInteractEvent.LeftClickBlock leftClickBlockEvent = net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player,pos,hitNormal);
	
					Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hitNormal));
					if (!leftClickBlockEvent.isCanceled() && leftClickBlockEvent.getUseItem() != Event.Result.DENY)
					{
						destroyClickedWireBlock(wireBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
				}
			}
		}
	}

	// more parity with existing destroy-clicked-block code
	// these checks are run after the digging packet is sent
	// the existing code checks some things that are already checked above, so we'll skip those
	static void destroyClickedWireBlock(AbstractWireBlock block, BlockState state, ClientLevel world, BlockPos pos, LocalPlayer player, MultiPlayerGameModeAccess controllerAccess, Direction interiorSide)
	{
		ItemStack heldItemStack = player.getMainHandItem();
		if (heldItemStack.onBlockStartBreak(pos, player))
			return;
		if (!heldItemStack.getItem().canAttackBlock(state, world, pos, player))
	        return;
		controllerAccess.setIsDestroying(false);
		block.destroyClickedSegment(state, world, pos, player, interiorSide, false);
		controllerAccess.setDestroyProgress(0F);
	    controllerAccess.setDestroyTicks(0F);
		
	}

	public static void onWireBreakPacket(NetworkEvent.Context context, WireBreakPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		
		if (world != null)
		{
			Vec3[] points = SlackInterpolator.getInterpolatedPoints(packet.start, packet.end);
			ParticleEngine manager = mc.particleEngine;
			BlockState state = Blocks.REDSTONE_WIRE.defaultBlockState();
			
			for (Vec3 point : points)
			{
				manager.add(
					new TerrainParticle(world, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D, state)
						.setPower(0.2F).scale(0.6F));
			}
		}
	}

	public static void onWireUpdatePacket(WireUpdatePacket packet)
	{
		@SuppressWarnings("resource")
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null)
		{
			VoxelCache.get(world).shapesByPos.invalidateAll(packet.getPositions());
		}
	}
}
