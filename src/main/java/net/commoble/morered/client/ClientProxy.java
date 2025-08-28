package net.commoble.morered.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.commoble.morered.FaceSegmentBlock;
import net.commoble.morered.IsWasSprintPacket;
import net.commoble.morered.MoreRed;
import net.commoble.morered.Names;
import net.commoble.morered.TwentyFourBlock;
import net.commoble.morered.mechanisms.GearsBlock;
import net.commoble.morered.mixin.MultiPlayerGameModeAccess;
import net.commoble.morered.soldering.SolderingRecipe.SolderingRecipeHolder;
import net.commoble.morered.transportation.FilterMenu;
import net.commoble.morered.transportation.RaytraceHelper;
import net.commoble.morered.transportation.TubeBreakPacket;
import net.commoble.morered.util.BlockStateUtil;
import net.commoble.morered.util.ConfigHelper;
import net.commoble.morered.wire_post.SlackInterpolator;
import net.commoble.morered.wire_post.WireBreakPacket;
import net.commoble.morered.wires.AbstractWireBlock;
import net.commoble.morered.wires.VoxelCache;
import net.commoble.morered.wires.WireUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialBlockModelRendererEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value=MoreRed.MODID, dist=Dist.CLIENT)
public class ClientProxy
{
	public static final ClientConfig CLIENTCONFIG = ConfigHelper.register(MoreRed.MODID, ModConfig.Type.CLIENT, ClientConfig::create);
	
	// block positions are in absolute world coordinates, not local chunk coords
	private static Map<ChunkPos, Set<BlockPos>> clientPostsInChunk = new HashMap<>();
	private static Map<ChunkPos, Set<BlockPos>> clientTubesInChunk = new HashMap<>();
	public static RecipeMap recipeMap = RecipeMap.create(List.of());
	private static List<SolderingRecipeHolder> solderingRecipes = new ArrayList<>();
	private static boolean isHoldingSprint = false;

	public ClientProxy(IEventBus modBus)
	{
		IEventBus gameBus = NeoForge.EVENT_BUS;
		
		modBus.addListener(ClientProxy::onClientSetup);
		modBus.addListener(ClientProxy::onRegisterModelLoaders);
		modBus.addListener(ClientProxy::onRegisterBlockStateModels);
		modBus.addListener(ClientProxy::onRegisterBlockColors);
		modBus.addListener(ClientProxy::onRegisterItemModels);
		modBus.addListener(ClientProxy::onRegisterRenderers);
		modBus.addListener(ClientProxy::onRegisterSpecialBlockModelRenderers);
		modBus.addListener(ClientProxy::onRegisterScreens);
		modBus.addListener(ClientProxy::onRegisterClientExtensions);
		
		gameBus.addListener(ClientProxy::onClientLogIn);
		gameBus.addListener(ClientProxy::onClientLogOut);
		gameBus.addListener(ClientProxy::onHighlightBlock);
		gameBus.addListener(ClientProxy::onInteract);
		gameBus.addListener(ClientProxy::onClientTick);
		gameBus.addListener(ClientProxy::onRecipesReceived);
	}
	
	public static void clear()
	{
		clientPostsInChunk = new HashMap<>();
		clientTubesInChunk = new HashMap<>();
		recipeMap = RecipeMap.create(List.of());
		solderingRecipes = new ArrayList<>();
		isHoldingSprint = false;
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
		
		chunk.setData(MoreRed.POSTS_IN_CHUNK_ATTACHMENT.get(), Set.copyOf(posts));
	}
	
	@Nonnull
	public static Set<BlockPos> getPostsInChunk(ChunkPos pos)
	{
		return clientPostsInChunk.getOrDefault(pos, Set.of());
	}
	
	@SuppressWarnings("resource")
	public static boolean getSprintingIfClientPlayer(Player player)
	{
		if (player == Minecraft.getInstance().player)
		{
			return getWasSprinting();
		}
		else
		{
			return false;
		}
	}
	
	public static boolean getWasSprinting()
	{
		return isHoldingSprint;
	}
	
	public static void setIsSprintingAndNotifyServer(boolean isSprinting)
	{
		// mark the capability on the client and send a packet to the server to do the same
		isHoldingSprint = isSprinting;
		Minecraft.getInstance().getConnection().send(new IsWasSprintPacket(isSprinting));
	}
	
	public static void updateTubesInChunk(ChunkPos pos, Set<BlockPos> tubes)
	{
		clientTubesInChunk.put(pos, tubes);
	}
	
	public static Set<BlockPos> getTubesInChunk(ChunkPos pos)
	{
		return clientTubesInChunk.getOrDefault(pos, Set.of());
	}

	public static List<SolderingRecipeHolder> getAllSolderingRecipes()
	{
		return solderingRecipes;
	}

	@SuppressWarnings("deprecation")
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		// render layer setting is synchronized, safe to do during multithreading
		MoreRed.LOGIC_PLATES.values().forEach(rob -> ItemBlockRenderTypes.setRenderLayer(rob.get(), ChunkSectionLayer.CUTOUT));
		MoreRed.BITWISE_LOGIC_PLATES.values()
			.forEach(rob -> ItemBlockRenderTypes.setRenderLayer(rob.get(), ChunkSectionLayer.CUTOUT));
		ItemBlockRenderTypes.setRenderLayer(MoreRed.ALTERNATOR_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.LATCH_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.PULSE_GATE_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.REDWIRE_RELAY_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.REDWIRE_JUNCTION_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.CABLE_JUNCTION_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.EXTRACTOR_BLOCK.get(), ChunkSectionLayer.CUTOUT);
		ItemBlockRenderTypes.setRenderLayer(MoreRed.STONEMILL_BLOCK.get(), ChunkSectionLayer.CUTOUT);
	}
	
	static void onRegisterScreens(RegisterMenuScreensEvent event)
	{
		event.register(MoreRed.SOLDERING_MENU.get(), SolderingScreen::new);
		event.register(MoreRed.LOADER_MENU.get(), LoaderScreen::new);
		event.<FilterMenu, SingleSlotMenuScreen<FilterMenu>>register(MoreRed.FILTER_MENU.get(), SingleSlotMenuScreen::new);
		event.register(MoreRed.MULTI_FILTER_MENU.get(), StandardSizeContainerScreenFactory.of(
			ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png"), MoreRed.MULTI_FILTER_BLOCK.get().getDescriptionId()));
		event.register(MoreRed.STONEMILL_MENU.get(), SingleSlotMenuScreen::new);
	}
	
	private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
	{
		Consumer<AbstractWireBlock> registerWireBlock = block -> event.registerBlock(
			new IClientBlockExtensions() {
				@Override
				public boolean addHitEffects(BlockState state, Level Level, HitResult target, ParticleEngine manager)
				{
					// if we have no wires here, we have no shape, so return true to disable particles
					// (otherwise the particle manager crashes because it performs an unsupported operation on the empty shape)
					return block.getWireCount(state) == 0;
				}
			}, block);
		registerWireBlock.accept(MoreRed.RED_ALLOY_WIRE_BLOCK.get());
		registerWireBlock.accept(MoreRed.BUNDLED_CABLE_BLOCK.get());
		for (var block : MoreRed.COLORED_CABLE_BLOCKS.values())
		{
			registerWireBlock.accept(block.get());
		}
	}
	
	private static void onRegisterItemModels(RegisterItemModelsEvent event)
	{
		event.register(MoreRed.id(Names.LOGIC_GATE), UnbakedLogicGateModel.CODEC);
		event.register(MoreRed.id(Names.WINDCATCHER), UnbakedWindcatcherModel.CODEC);
	}

	public static void onRegisterModelLoaders(ModelEvent.RegisterLoaders event)
	{
		event.register(MoreRed.id(Names.ROTATE_TINTS), TintRotatingModelLoader.INSTANCE);
	}
	
	public static void onRegisterBlockStateModels(RegisterBlockStateModels event)
	{
		event.registerModel(MoreRed.id(Names.WIRE_PARTS), UnbakedWirePartBlockStateModel.CODEC);
		event.registerModel(MoreRed.id(Names.XYZ), UnbakedXyzBlockStateModel.CODEC);
	}

	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event)
	{
		MoreRed.LOGIC_PLATES.values().forEach(rob -> event.register(ColorHandlers::getLogicFunctionBlockTint, rob.get()));
		event.register(ColorHandlers::getLatchBlockTint, MoreRed.LATCH_BLOCK.get());
		event.register(ColorHandlers::getPulseGateBlockTint, MoreRed.PULSE_GATE_BLOCK.get());
		event.register(ColorHandlers::getAlternatorBlockTint, MoreRed.ALTERNATOR_BLOCK.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.REDWIRE_POST_BLOCK.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.REDWIRE_RELAY_BLOCK.get());
		event.register(ColorHandlers::getRedwirePostBlockTint, MoreRed.REDWIRE_JUNCTION_BLOCK.get());
		event.register(ColorHandlers::getRedAlloyWireBlockTint, MoreRed.RED_ALLOY_WIRE_BLOCK.get());
	}

	static void onRegisterRenderers(RegisterRenderers event)
	{
		event.registerBlockEntityRenderer(MoreRed.ALTERNATOR_BLOCK_ENTITY.get(), AlternatorBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.WIRE_POST_BLOCK_ENTITY.get(), WirePostRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.CABLE_POST_BLOCK_ENTITY.get(), BundledCablePostRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.TUBE_BLOCK_ENTITY.get(), TubeBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.REDSTONE_TUBE_BLOCK_ENTITY.get(), TubeBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.EXTRACTOR_BLOCK_ENTITY.get(), ExtractorBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.FILTER_BLOCK_ENTITY.get(), FilterBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.OSMOSIS_FILTER_BLOCK_ENTITY.get(), OsmosisFilterBlockEntityRenderer::new);
		event.registerBlockEntityRenderer(MoreRed.AXLE_BLOCK_ENTITY.get(), AxleBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.GEAR_BLOCK_ENTITY.get(), GearBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.GEARS_BLOCK_ENTITY.get(), GearsBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.GEARSHIFTER_BLOCK_ENTITY.get(), GearshifterBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.CLUTCH_BLOCK_ENTITY.get(), ClutchBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.WINDCATCHER_BLOCK_ENTITY.get(), WindcatcherBlockEntityRenderer::create);
		event.registerBlockEntityRenderer(MoreRed.STONEMILL_BLOCK_ENTITY.get(), StonemillBlockEntityRenderer::create);
	}

	static void onRegisterSpecialBlockModelRenderers(RegisterSpecialBlockModelRendererEvent event)
	{
		BiConsumer<Holder<? extends Block>, Holder<? extends BlockEntityType<?>>> renderBlockAsItem = (block,type) -> event.register(block.value(), new UnbakedBlockEntityWithoutLevelRenderer(block.value(), type.value()));
		Function<Holder<? extends BlockEntityType<?>>, Consumer<Holder<? extends Block>>> renderBlock = type -> block -> renderBlockAsItem.accept(block, type);
		MoreRed.AXLE_BLOCKS.values().forEach(renderBlock.apply(MoreRed.AXLE_BLOCK_ENTITY));
		MoreRed.GEAR_BLOCKS.values().forEach(renderBlock.apply(MoreRed.GEAR_BLOCK_ENTITY));
		MoreRed.GEARSHIFTER_BLOCKS.values().forEach(renderBlock.apply(MoreRed.GEARSHIFTER_BLOCK_ENTITY));
		MoreRed.CLUTCH_BLOCKS.values().forEach(renderBlock.apply(MoreRed.CLUTCH_BLOCK_ENTITY));
		MoreRed.WINDCATCHER_BLOCKS.values().forEach(renderBlock.apply(MoreRed.WINDCATCHER_BLOCK_ENTITY));
		// don't bother with morered:gears because it'd just render an empty gears anyway which renders nothing
		renderBlockAsItem.accept(MoreRed.EXTRACTOR_BLOCK, MoreRed.EXTRACTOR_BLOCK_ENTITY);
		renderBlockAsItem.accept(MoreRed.ALTERNATOR_BLOCK, MoreRed.ALTERNATOR_BLOCK_ENTITY);
	}
	
	static void onClientLogIn(ClientPlayerNetworkEvent.LoggingIn event)
	{
		// clean up static data on the client
		clear();
	}

	static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event)
	{
		// clean up static data on the client
		clear();
	}

	static void onHighlightBlock(RenderHighlightEvent.Block event)
	{
		if (ClientProxy.CLIENTCONFIG.showPlacementPreview().get())
		{
			@SuppressWarnings("resource")
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && player.level() != null)
			{
				InteractionHand hand = player.getUsedItemHand();
				hand = hand == null ? InteractionHand.MAIN_HAND : hand;
				ItemStack stack = player.getItemInHand(hand);
				Item item = stack.getItem();
				if (item instanceof BlockItem blockItem)
				{
					Block block = blockItem.getBlock();
					if (block instanceof TwentyFourBlock twentyFourBlock)
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
							BlockState state = twentyFourBlock.getStateForPlacement(new BlockPlaceContext(player, hand, stack, rayTrace));
							
							if (twentyFourBlock.hasBlockStateModelsForPlacementPreview(state))
							{
								BlockPreviewRenderer.renderBlockPreview(placePos, state, world, event.getCamera().getPosition(), event.getPoseStack(), event.getMultiBufferSource());							
							}
							else
							{
								BlockPreviewRenderer.renderHeldItemAsBlockPreview(placePos, stack, state, world, event.getCamera().getPosition(), event.getPoseStack(), event.getMultiBufferSource());
							}
							
						}
					}
				}
			}
		}
	}
	
	private static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if (mc.player != null)
		{
			boolean sprintIsDown = mc.options.keySprint.isDown();
			boolean sprintWasDown = ClientProxy.getWasSprinting();
			if (sprintWasDown != sprintIsDown)	// change in sprint key detected
			{
				ClientProxy.setIsSprintingAndNotifyServer(sprintIsDown);
			}
		}
	}
	
	private static void onRecipesReceived(RecipesReceivedEvent event)
	{
		// soldering recipes are always synced and received here
		// converting them to SolderingRecipeHolder because can't deal with the RecipeHolder generics
		RecipeMap recipeMap = event.getRecipeMap();
		solderingRecipes = recipeMap.byType(MoreRed.SOLDERING_RECIPE_TYPE.get())
			.stream()
			.map(recipeHolder -> new SolderingRecipeHolder(recipeHolder.id().location(), recipeHolder.value()))
			.sorted(Comparator.comparing(holder -> I18n.get(holder.recipe().result().getItem().getDescriptionId())))
			.toList();
		ClientProxy.recipeMap = recipeMap;
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
			MultiPlayerGameMode controller = mc.gameMode;
			MultiPlayerGameModeAccess controllerAccess = (MultiPlayerGameModeAccess)controller;
			if (block instanceof AbstractWireBlock)
			{
				// we'll be taking over the event from here onward
				event.setCanceled(true);
				
				AbstractWireBlock wireBlock = (AbstractWireBlock)block;
				GameType gameType = controller.getPlayerMode();
				// now run over all the permissions checking that would normally happen here
				if (player.blockActionRestricted(world, pos, gameType))
					return;
				if (!world.getWorldBorder().isWithinBounds(pos))
					return;
				@Nullable Direction faceToBreak = wireBlock.getInteriorFaceToBreak(state, pos, player, blockResult, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), wireBlock.getRaytraceBackboards());
				if (faceToBreak == null)
					return;
				Direction hitNormal = faceToBreak.getOpposite();
				if (gameType.isCreative())
				{
					Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hitNormal));
					// TODO do stuff from onPlayerDestroyBlock here
					if (!CommonHooks.onLeftClickBlock(player, pos, hitNormal, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled())
					{
						destroyClickedSegment(wireBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
					controllerAccess.setDestroyDelay(5);
				}
				else if (!controller.isDestroying() || !controllerAccess.callSameDestroyTarget(pos))
				{
					if (controller.isDestroying())
					{
						Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, controllerAccess.getDestroyBlockPos(), blockResult.getDirection()));
					}
					PlayerInteractEvent.LeftClickBlock leftClickBlockEvent = CommonHooks.onLeftClickBlock(player,pos,hitNormal, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
	
					Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hitNormal));
					if (!leftClickBlockEvent.isCanceled() && leftClickBlockEvent.getUseItem() != TriState.FALSE)
					{
						destroyClickedSegment(wireBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
				}
			}
			else if (block instanceof GearsBlock faceSegmentBlock)
			{
				// known bug: destroy progress isn't reset when aborting destroy. Haven't been able to find what I'm missing.
				// Leaving alone for now
				GameType gameType = controller.getPlayerMode();
				if (gameType.isCreative())
				{
					event.setCanceled(true);
					// now run over all the permissions checking that would normally happen here
					if (player.blockActionRestricted(world, pos, gameType))
						return;
					if (!world.getWorldBorder().isWithinBounds(pos))
						return;
					@Nullable Direction faceToBreak = faceSegmentBlock.getInteriorFaceToBreak(state, pos, player, blockResult, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), faceSegmentBlock.getRaytraceBackboards());
					if (faceToBreak == null)
						return;
					Direction hitNormal = faceToBreak.getOpposite();
					Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hitNormal));
					// TODO do stuff from onPlayerDestroyBlock here
					if (!CommonHooks.onLeftClickBlock(player, pos, hitNormal, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK).isCanceled())
					{
						destroyClickedSegment(faceSegmentBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
					controllerAccess.setDestroyDelay(5);
				}
				else if (controllerAccess.callSameDestroyTarget(pos))
				{
					event.setCanceled(true);
					@Nullable Direction faceToBreak = faceSegmentBlock.getInteriorFaceToBreak(state, pos, player, blockResult, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), faceSegmentBlock.getRaytraceBackboards());
					if (faceToBreak == null)
						return;
					Direction hitNormal = faceToBreak.getOpposite();
					float newDestroyProgress = controllerAccess.getDestroyProgress() + state.getDestroyProgress(player, world, pos);
					controllerAccess.setDestroyProgress(newDestroyProgress);
					if (controllerAccess.getDestroyTicks() % 4.0F == 0.0F)
					{
						SoundType soundtype = state.getSoundType(world, pos, player);
						mc.getSoundManager().play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F,
							soundtype.getPitch() * 0.5F, SoundInstance.createUnseededRandom(), pos));
					}

					controllerAccess.setDestroyTicks(controllerAccess.getDestroyTicks() + 1F);
					if (net.neoforged.neoforge.common.CommonHooks.onClientMineHold(player, pos, hitNormal).getUseItem().isFalse())
						return;
					if (newDestroyProgress >= 1.0F)
					{
						PlayerInteractEvent.LeftClickBlock leftClickBlockEvent = CommonHooks.onLeftClickBlock(player,pos,hitNormal, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
						Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, hitNormal));
						if (!leftClickBlockEvent.isCanceled() && leftClickBlockEvent.getUseItem() != TriState.FALSE)
						{
							destroyClickedSegment(faceSegmentBlock, state, world, pos, player, controllerAccess, faceToBreak);
						}
						controllerAccess.setDestroyDelay(5);
					}
	                world.destroyBlockProgress(player.getId(), controllerAccess.getDestroyBlockPos(), controller.getDestroyStage());
				}
			}
		}
	}

	// more parity with existing destroy-clicked-block code
	// these checks are run after the digging packet is sent
	// the existing code checks some things that are already checked above, so we'll skip those
	static void destroyClickedSegment(FaceSegmentBlock block, BlockState state, ClientLevel world, BlockPos pos, LocalPlayer player, MultiPlayerGameModeAccess controllerAccess, Direction interiorSide)
	{
		ItemStack heldItemStack = player.getMainHandItem();
		if (!heldItemStack.canDestroyBlock(state, world, pos, player))
	        return;
		controllerAccess.setIsDestroying(false);
		block.destroyClickedSegment(state, world, pos, player, interiorSide, false);
		controllerAccess.setDestroyProgress(0F);
	    controllerAccess.setDestroyTicks(0F);
		
	}

	public static void onWireBreakPacket(WireBreakPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientLevel world = mc.level;
		
		if (world != null)
		{
			Vec3[] points = SlackInterpolator.getInterpolatedPoints(packet.start(), packet.end());
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
			packet.positions().forEach(pos -> VoxelCache.invalidate(world, pos));
		}
	}

	public static void onTubeBreakPacket(TubeBreakPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		
		if (level != null)
		{
			Vec3[] points = RaytraceHelper.getInterpolatedPoints(packet.start(), packet.end());
			ParticleEngine manager = mc.particleEngine;
			BlockState state = level.getBlockState(BlockPos.containing(packet.start()));
			
			for (Vec3 point : points)
			{
				manager.add(
					new TerrainParticle(level, point.x, point.y, point.z, 0.0D, 0.0D, 0.0D, state)
						.setPower(0.2F).scale(0.6F));
			}
		}
	}
}
