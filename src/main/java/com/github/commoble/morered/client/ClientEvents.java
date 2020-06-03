package com.github.commoble.morered.client;

import com.github.commoble.morered.BlockRegistrar;
import com.github.commoble.morered.ContainerRegistrar;
import com.github.commoble.morered.ItemRegistrar;
import com.github.commoble.morered.plate_blocks.LogicGateType;
import com.github.commoble.morered.plate_blocks.PlateBlock;
import com.github.commoble.morered.plate_blocks.PlateBlockStateProperties;
import com.github.commoble.morered.util.BlockStateUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEvents
{
	public static void addClientListeners(IEventBus modBus, IEventBus forgeBus)
	{
		ClientConfig.initClientConfig();
		
		modBus.addListener(ClientEvents::onClientSetup);
		modBus.addListener(ClientEvents::onRegisterBlockColors);
		modBus.addListener(ClientEvents::onRegisterItemColors);
		
		forgeBus.addListener(ClientEvents::onHighlightBlock);
	}
	
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		LogicGateType.TYPES.values().forEach(ClientEvents::setLogicGateRenderLayer);
		RenderTypeLookup.setRenderLayer(BlockRegistrar.LATCH.get(), RenderType.getCutout());

		ScreenManager.registerFactory(ContainerRegistrar.GATECRAFTING.get(), GatecraftingScreen::new);
	}
	
	public static void setLogicGateRenderLayer(LogicGateType type)
	{
		RenderTypeLookup.setRenderLayer(type.blockGetter.get(), RenderType.getCutout());
	}
	
	public static void onRegisterBlockColors(ColorHandlerEvent.Block event)
	{
		BlockColors colors = event.getBlockColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockTint, type.blockGetter.get()));
		colors.register(ColorHandlers::getLatchBlockTint, BlockRegistrar.LATCH.get());
	}
	
	public static void onRegisterItemColors(ColorHandlerEvent.Item event)
	{
		ItemColors colors = event.getItemColors();
		LogicGateType.TYPES.values().forEach(type -> colors.register(ColorHandlers::getLogicFunctionBlockItemTint, type.itemGetter.get()));
		colors.register(ColorHandlers::getLatchItemTint, ItemRegistrar.LATCH.get());
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
							Vec3d hitVec = rayTrace.getHitVec();
							
							Direction attachmentDirection = directionAwayFromTargetedBlock.getOpposite();
							Vec3d relativeHitVec = hitVec.subtract(new Vec3d(placePos));
							
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
}
