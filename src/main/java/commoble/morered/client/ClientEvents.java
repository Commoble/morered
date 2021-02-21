package commoble.morered.client;

import java.util.Map;

import javax.annotation.Nullable;

import commoble.morered.BlockRegistrar;
import commoble.morered.ContainerRegistrar;
import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.TileEntityRegistrar;
import commoble.morered.mixin.ClientPlayerControllerAccess;
import commoble.morered.plate_blocks.LogicGateType;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.VoxelCache;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.client.event.InputEvent.ClickInputEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientEvents
{
	public static void addClientListeners(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext, IEventBus modBus, IEventBus forgeBus)
	{
		ClientConfig.initClientConfig(modContext, fmlContext);
		
		modBus.addListener(ClientEvents::onClientSetup);
		modBus.addListener(ClientEvents::onRegisterModelLoaders);
		modBus.addListener(ClientEvents::onRegisterBlockColors);
		modBus.addListener(ClientEvents::onRegisterItemColors);
		modBus.addListener(ClientEvents::onModelBakeEvent);
		
		forgeBus.addListener(ClientEvents::onClientLogIn);
		forgeBus.addListener(ClientEvents::onClientLogOut);
		forgeBus.addListener(ClientEvents::onHighlightBlock);
		forgeBus.addListener(ClientEvents::onClickInput);
	}
	
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		LogicGateType.TYPES.values().forEach(ClientEvents::setLogicGateRenderLayer);
		RenderTypeLookup.setRenderLayer(BlockRegistrar.LATCH.get(), RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(BlockRegistrar.REDWIRE_POST_PLATE.get(), RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get(), RenderType.getCutout());

		ScreenManager.registerFactory(ContainerRegistrar.GATECRAFTING.get(), GatecraftingScreen::new);
		ClientRegistry.bindTileEntityRenderer(TileEntityRegistrar.REDWIRE_POST.get(), WirePostRenderer::new);
	}
	
	public static void setLogicGateRenderLayer(LogicGateType type)
	{
		RenderTypeLookup.setRenderLayer(type.blockGetter.get(), RenderType.getCutout());
	}
	
	public static void onRegisterModelLoaders(ModelRegistryEvent event)
	{
		ModelLoaderRegistry.registerLoader(new ResourceLocation(MoreRed.MODID, ObjectNames.WIRE_PARTS), WirePartModelLoader.INSTANCE);
		ModelLoaderRegistry.registerLoader(new ResourceLocation(MoreRed.MODID, ObjectNames.ROTATE_TINTS), TintRotatingModelLoader.INSTANCE);
	}
	
	public static void onRegisterBlockColors(ColorHandlerEvent.Block event)
	{
		BlockColors colors = event.getBlockColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockTint, type.blockGetter.get()));
		colors.register(ColorHandlers::getLatchBlockTint, BlockRegistrar.LATCH.get());
		colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST.get());
		colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST_PLATE.get());
		colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get());
		colors.register(ColorHandlers::getRedAlloyWireBlockTint, BlockRegistrar.RED_ALLOY_WIRE.get());
	}
	
	public static void onRegisterItemColors(ColorHandlerEvent.Item event)
	{
		ItemColors colors = event.getItemColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockItemTint, type.itemGetter.get()));
		colors.register(ColorHandlers::getLatchItemTint, ItemRegistrar.LATCH.get());
		colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST.get());
		colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST_PLATE.get());
		colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST_RELAY_PLATE.get());
	}
	
	public static void onModelBakeEvent(ModelBakeEvent event)
	{
		// replace multipart models for wires and cables with wire part models so they can process the wire data correctly
		// TODO forge added a new model loader hook for multiparts at some point, see if that's useable here
		Map<ResourceLocation, IBakedModel> registry = event.getModelRegistry();
		setWirePartModelLoader(registry, BlockRegistrar.RED_ALLOY_WIRE.get());
		setWirePartModelLoader(registry, BlockRegistrar.BUNDLED_NETWORK_CABLE.get());
		for (int i=0; i<16; i++)
		{
			setWirePartModelLoader(registry, BlockRegistrar.NETWORK_CABLES[i].get());
		}
	}
	
	static void setWirePartModelLoader(Map<ResourceLocation, IBakedModel> registry, Block block)
	{
		block.getStateContainer().getValidStates().forEach(state ->
		{
			ModelResourceLocation mrl = BlockModelShapes.getModelLocation(state);
			IBakedModel model = registry.get(mrl);
			if (model != null)
			{
				registry.put(mrl, new WirePartModelLoader.WireBlockModel(model));
			}
		});
	}
	
	public static void onClientLogIn(ClientPlayerNetworkEvent.LoggedInEvent event)
	{
		// clean up static data on the client
		MoreRed.CLIENT_PROXY = ClientProxy.makeClientProxy();
		VoxelCache.clearClientCache();
	}
	
	public static void onClientLogOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
		// clean up static data on the client
		MoreRed.CLIENT_PROXY = ClientProxy.makeClientProxy();
		VoxelCache.clearClientCache();
	}
	
	public static void onHighlightBlock(DrawHighlightEvent.HighlightBlock event)
	{
		if (ClientConfig.INSTANCE.showPlacementPreview.get())
		{
			@SuppressWarnings("resource")
			ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player != null && player.world != null)
			{
				Hand hand = player.getActiveHand();
				Item item = player.getHeldItem(hand == null ? Hand.MAIN_HAND : hand).getItem();
				if (item instanceof BlockItem)
				{
					Block block = ((BlockItem)item).getBlock();
					if (block instanceof PlateBlock)
					{
						World world = player.world;
						BlockRayTraceResult rayTrace = event.getTarget();
						Direction directionAwayFromTargetedBlock = rayTrace.getFace();
						BlockPos placePos = rayTrace.getPos().offset(directionAwayFromTargetedBlock);
						
						BlockState existingState = world.getBlockState(placePos);
						if (existingState.isAir(world, placePos) || existingState.getMaterial().isReplaceable())
						{
							// only render the preview if we know it would make sense for the block to be placed where we expect it to be
							Vector3d hitVec = rayTrace.getHitVec();
							
							Direction attachmentDirection = directionAwayFromTargetedBlock.getOpposite();
							Vector3d relativeHitVec = hitVec.subtract(Vector3d.copy(placePos));
							
							Direction outputDirection = BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec, attachmentDirection);
							BlockStateUtil.getRotationIndexForDirection(attachmentDirection, outputDirection);
							BlockState state = PlateBlockStateProperties.getStateForPlacedGatePlate(block.getDefaultState(), placePos, attachmentDirection, relativeHitVec);
							
							BlockPreviewRenderer.renderBlockPreview(placePos, state, world, event.getInfo().getProjectedView(), event.getMatrix(), event.getBuffers());
							
						}
					}
				}
			}
		}
	}
	
	public static void onClickInput(ClickInputEvent event)
	{
		// when the player clicks a wire block,
		// we want to handle the block in a manner that causes the digging packet to be sent
		// with the interior side of the wire block as the face value
		// (we can't do this perfectly with the raytrace shape normal overrider)
		// but we can do this by sending our own digging packet and bypassing the vanilla behaviour
		// (it helps that wire blocks are instant-break blocks)
		Minecraft mc = Minecraft.getInstance();
		RayTraceResult rayTraceResult = mc.objectMouseOver;
		ClientWorld world = mc.world;
		ClientPlayerEntity player = mc.player;
		if (rayTraceResult != null && rayTraceResult.getHitVec() != null && world != null && player != null && event.isAttack() && rayTraceResult.getType() == RayTraceResult.Type.BLOCK)
		{
			BlockRayTraceResult blockResult = (BlockRayTraceResult) rayTraceResult;
			BlockPos pos = blockResult.getPos();
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof AbstractWireBlock)
			{
				// we'll be taking over the event from here onward
				event.setCanceled(true);
				
				AbstractWireBlock wireBlock = (AbstractWireBlock)block;
				PlayerController controller = mc.playerController;
				ClientPlayerControllerAccess controllerAccess = (ClientPlayerControllerAccess)controller;
				GameType gameType = controller.getCurrentGameType();
				// now run over all the permissions checking that would normally happen here
				if (player.blockActionRestricted(world, pos, gameType))
					return;
				if (!world.getWorldBorder().contains(pos))
					return;
				@Nullable Direction faceToBreak = wireBlock.getInteriorFaceToBreak(state, pos, player, blockResult, mc.getRenderPartialTicks());
				if (faceToBreak == null)
					return;
				Direction hitNormal = faceToBreak.getOpposite();
				if (gameType.isCreative())
				{
					controllerAccess.callSendDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, pos, hitNormal);
					// TODO do stuff from onPlayerDestroyBlock here
					if (!net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, pos, hitNormal).isCanceled())
					{
						destroyClickedWireBlock(wireBlock, state, world, pos, player, controllerAccess, faceToBreak);
					}
					controllerAccess.setBlockHitDelay(5);
				}
				else if (!controller.getIsHittingBlock() || !controllerAccess.callIsHittingPosition(pos))
				{
					if (controller.getIsHittingBlock())
					{
						controllerAccess.callSendDiggingPacket(CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, controllerAccess.getCurrentBlock(), blockResult.getFace());
					}
					PlayerInteractEvent.LeftClickBlock leftClickBlockEvent = net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player,pos,hitNormal);

					controllerAccess.callSendDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, pos, hitNormal);
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
	private static void destroyClickedWireBlock(AbstractWireBlock block, BlockState state, ClientWorld world, BlockPos pos, ClientPlayerEntity player, ClientPlayerControllerAccess controllerAccess, Direction interiorSide)
	{
		ItemStack heldItemStack = player.getHeldItemMainhand();
		if (heldItemStack.onBlockStartBreak(pos, player))
			return;
		if (!heldItemStack.getItem().canPlayerBreakBlockWhileHolding(state, world, pos, player))
            return;
		controllerAccess.setIsHittingBlock(false);
		block.destroyClickedSegment(state, world, pos, player, interiorSide, false);
		controllerAccess.setCurBlockDamageMP(0F);
        controllerAccess.setStepSoundTickCounter(0F);
		
	}
	
}
