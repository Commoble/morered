package commoble.morered.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.ScreenManager;
import commoble.morered.BlockRegistrar;
import commoble.morered.ContainerRegistrar;
import commoble.morered.ItemRegistrar;
import commoble.morered.MoreRed;
import commoble.morered.ObjectNames;
import commoble.morered.TileEntityRegistrar;
import commoble.morered.foundation.config.AllConfigs;
import commoble.morered.plate_blocks.LogicGateType;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import commoble.morered.util.BlockStateUtil;
import commoble.morered.wires.AbstractWireBlock;
import commoble.morered.wires.VoxelCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.event.InputEvent.ClickInputEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientEvents {
    public static void addClientListeners(ModLoadingContext modContext, FMLJavaModLoadingContext fmlContext,
                                          IEventBus modBus, IEventBus forgeBus) {
//		ClientConfig.initClientConfig(modContext, fmlContext);

        modBus.addListener(ClientEvents::onClientSetup);
        modBus.addListener(ClientEvents::onRegisterModelLoaders);
        modBus.addListener(ClientEvents::onRegisterBlockColors);
        modBus.addListener(ClientEvents::onRegisterItemColors);

        forgeBus.addListener(ClientEvents::onClientLogIn);
        forgeBus.addListener(ClientEvents::onClientLogOut);
        forgeBus.addListener(ClientEvents::onHighlightBlock);
        forgeBus.addListener(ClientEvents::onClickInput);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        // render layer setting is synchronized, safe to do during multithreading
        LogicGateType.TYPES.values().forEach(ClientEvents::setLogicGateRenderLayer);
        LogicGateType.BITWISE_TYPES.values()
                .forEach(pair -> ItemBlockRenderTypes.setRenderLayer(pair.blockGetter.get(), RenderType.cutout()));

        ItemBlockRenderTypes.setRenderLayer(BlockRegistrar.LATCH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistrar.REDWIRE_POST_PLATE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistrar.BUNDLED_CABLE_RELAY_PLATE.get(), RenderType.cutout());

        BlockEntityRenderers.register(TileEntityRegistrar.REDWIRE_POST.get(), WirePostRenderer::new);
        BlockEntityRenderers.register(TileEntityRegistrar.BUNDLED_CABLE_POST.get(), BundledCablePostRenderer::new);
        BlockEntityRenderers.register(TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get(),
                BundledCablePostRenderer::new);

        event.enqueueWork(ClientEvents::afterClientSetup);
    }

    static void afterClientSetup() {
        // not threadsafe, do this on main thread
        MenuScreens.register(ContainerRegistrar.GATECRAFTING.get(), GatecraftingScreen::new);
    }

    public static void setLogicGateRenderLayer(LogicGateType type) {
        ItemBlockRenderTypes.setRenderLayer(type.blockGetter.get(), RenderType.cutout());
    }

    public static void onRegisterModelLoaders(ModelRegistryEvent event) {
        ModelLoaderRegistry.registerLoader(new ResourceLocation(MoreRed.MODID, ObjectNames.WIRE_PARTS),
                WirePartModelLoader.INSTANCE);
        ModelLoaderRegistry.registerLoader(new ResourceLocation(MoreRed.MODID, ObjectNames.ROTATE_TINTS),
                TintRotatingModelLoader.INSTANCE);
    }

    public static void onRegisterBlockColors(ColorHandlerEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockTint,
                type.blockGetter.get()));
        colors.register(ColorHandlers::getLatchBlockTint, BlockRegistrar.LATCH.get());
        colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST.get());
        colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST_PLATE.get());
        colors.register(ColorHandlers::getRedwirePostBlockTint, BlockRegistrar.REDWIRE_POST_RELAY_PLATE.get());
        colors.register(ColorHandlers::getRedAlloyWireBlockTint, BlockRegistrar.RED_ALLOY_WIRE.get());
    }

    public static void onRegisterItemColors(ColorHandlerEvent.Item event) {
        ItemColors colors = event.getItemColors();
        LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockItemTint,
                type.itemGetter.get()));
        colors.register(ColorHandlers::getLatchItemTint, ItemRegistrar.LATCH.get());
        colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST.get());
        colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST_PLATE.get());
        colors.register(ColorHandlers::getRedwirePostItemTint, ItemRegistrar.REDWIRE_POST_RELAY_PLATE.get());
        colors.register(ColorHandlers::getRedAlloyWireItemTint, ItemRegistrar.RED_ALLOY_WIRE.get());
    }

    public static void onClientLogIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        // clean up static data on the client
        MoreRed.CLIENT_PROXY = ClientProxy.makeClientProxy();
        VoxelCache.clearClientCache();
    }

    public static void onClientLogOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        // clean up static data on the client
        MoreRed.CLIENT_PROXY = ClientProxy.makeClientProxy();
        VoxelCache.clearClientCache();
    }

    public static void onHighlightBlock(DrawSelectionEvent.HighlightBlock event) {
        if (AllConfigs.CLIENT.showPlacementPreview.get()) {
            @SuppressWarnings("resource")
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                InteractionHand hand = player.getUsedItemHand();
                Item item = player.getItemInHand(hand).getItem();
                if (item instanceof BlockItem) {
                    Block block = ((BlockItem) item).getBlock();
                    if (block instanceof PlateBlock) {
                        Level world = player.level;
                        BlockHitResult rayTrace = event.getTarget();
                        Direction directionAwayFromTargetedBlock = rayTrace.getDirection();
                        BlockPos placePos = rayTrace.getBlockPos().relative(directionAwayFromTargetedBlock);

                        BlockState existingState = world.getBlockState(placePos);
                        if (existingState.isAir() || existingState.getMaterial().isReplaceable()) {
                            // only render the preview if we know it would make sense for the block to be placed
                            // where we expect it to be
                            Vec3 hitVec = rayTrace.getLocation();

                            Direction attachmentDirection = directionAwayFromTargetedBlock.getOpposite();
                            Vec3 relativeHitVec = hitVec.subtract(Vec3.atLowerCornerOf(placePos));

                            Direction outputDirection =
                                    BlockStateUtil.getOutputDirectionFromRelativeHitVec(relativeHitVec,
                                            attachmentDirection);
                            BlockStateUtil.getRotationIndexForDirection(attachmentDirection, outputDirection);
                            BlockState state =
                                    PlateBlockStateProperties.getStateForPlacedGatePlate(block.defaultBlockState(),
                                            placePos, attachmentDirection, relativeHitVec);

                            BlockPreviewRenderer.renderBlockPreview(placePos, state, world,
                                    event.getTarget().getLocation(), event.getPoseStack(),
                                    event.getMultiBufferSource());

                        }
                    }
                }
            }
        }
    }

    public static void onClickInput(ClickInputEvent event) {
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
        if (rayTraceResult != null && rayTraceResult.getLocation() != null && world != null && player != null && event.isAttack() && rayTraceResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockResult = (BlockHitResult) rayTraceResult;
            BlockPos pos = blockResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof AbstractWireBlock) {
                // we'll be taking over the event from here onward
                event.setCanceled(true);

                AbstractWireBlock wireBlock = (AbstractWireBlock) block;
                MultiPlayerGameMode controller = mc.gameMode;
//				ClientPlayerControllerAccess controllerAccess = (ClientPlayerControllerAccess)controller;
                GameType gameType = controller.getPlayerMode();
                // now run over all the permissions checking that would normally happen here
                if (player.blockActionRestricted(world, pos, gameType))
                    return;
                if (!world.getWorldBorder().isWithinBounds(pos))
                    return;
                @Nullable Direction faceToBreak = wireBlock.getInteriorFaceToBreak(state, pos, player, blockResult,
                        mc.getFrameTime());
                if (faceToBreak == null)
                    return;
                Direction hitNormal = faceToBreak.getOpposite();
                if (gameType.isCreative()) {
                    controller.startDestroyBlock(pos, hitNormal);
//					controllerAccess.callSendBlockAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
//					pos, hitNormal);
                    // TODO do stuff from onPlayerDestroyBlock here
                    if (!net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, pos, hitNormal).isCanceled()) {
                        destroyClickedWireBlock(wireBlock, state, world, pos, player, controller, faceToBreak);
                    }
//					controllerAccess.setDestroyDelay(5);
                } else if (!controller.isDestroying()) {
                    if (controller.isDestroying()) {
                        controller.stopDestroyBlock();
//						controllerAccess.callSendBlockAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
//						controllerAccess.getDestroyBlockPos(), blockResult.getDirection());
                    }
                    PlayerInteractEvent.LeftClickBlock leftClickBlockEvent =
                            net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, pos, hitNormal);

//					controllerAccess.callSendBlockAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
//					pos, hitNormal);
                    controller.continueDestroyBlock(pos, hitNormal);
                    if (!leftClickBlockEvent.isCanceled() && leftClickBlockEvent.getUseItem() != Event.Result.DENY) {
                        destroyClickedWireBlock(wireBlock, state, world, pos, player, controller, faceToBreak);
                    }
                }
            }
        }
    }

    // more parity with existing destroy-clicked-block code
    // these checks are run after the digging packet is sent
    // the existing code checks some things that are already checked above, so we'll skip those
    private static void destroyClickedWireBlock(AbstractWireBlock block, BlockState state, Level world, BlockPos pos, LocalPlayer player,
                                                MultiPlayerGameMode controllerAccess, Direction interiorSide) {
        ItemStack heldItemStack = player.getMainHandItem();
        if (heldItemStack.onBlockStartBreak(pos, player))
            return;
        if (!heldItemStack.getItem().canAttackBlock(state, world, pos, player))
            return;
//		controllerAccess.setIsDestroying(false);
        controllerAccess.destroyBlock(pos);
        block.destroyClickedSegment(state, world, pos, player, interiorSide, false);
//		controllerAccess.setDestroyProgress(0F);
//        controllerAccess.setDestroyTicks(0F);

    }

}
