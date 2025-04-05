package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.mechanisms.GearshifterBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record GearshifterBlockEntityRenderer(ItemRenderer itemRenderer, Map<Block, GearshifterModels> modelCache) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static GearshifterBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new GearshifterBlockEntityRenderer(context.getItemRenderer(), new HashMap<>());
	}

	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction bigDir = state.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		Direction smallDir = PlateBlockStateProperties.getOutputDirection(state);
		int faceRotations = state.getValue(GearshifterBlock.ROTATION);
		float bigRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(bigDir), MechanicalState.ZERO).angularVelocity();
		float smallRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(smallDir), MechanicalState.ZERO).angularVelocity();
		GearshifterModels models = this.modelCache.computeIfAbsent(state.getBlock(), GearshifterModels::of);
		Level level = be.getLevel();
		// reduce rounding errors at high game times
		long gameTimeTicks = level.getGameTime() % ClientProxy.CLIENTCONFIG.machineRenderCycleTicks().get();
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float bigRadians = bigRadiansPerSecond * seconds;
		float smallRadians = smallRadiansPerSecond * seconds;
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		
		// rotate the whole model based on the state
		// first, apply rotation for axis direction
		switch(bigDir)
		{
			// down = default for gearshifter, ignore
			case UP:
				poseStack.mulPose(Axis.XP.rotationDegrees(180));
				poseStack.mulPose(Axis.YN.rotationDegrees(180)); // finaglement: secondary direction is always NORTH or UP by default
				break;
			case NORTH: poseStack.mulPose(Axis.XP.rotationDegrees(90));break;
			case SOUTH:
				poseStack.mulPose(Axis.XP.rotationDegrees(270));
				poseStack.mulPose(Axis.YN.rotationDegrees(180));
				break;
			case WEST:
				poseStack.mulPose(Axis.ZN.rotationDegrees(90));
				poseStack.mulPose(Axis.YN.rotationDegrees(270));
				break;
			case EAST:
				poseStack.mulPose(Axis.ZP.rotationDegrees(90));
				poseStack.mulPose(Axis.YN.rotationDegrees(90));
				break;
			default:
		}
		// apply secondary rotation
		poseStack.mulPose(Axis.YN.rotationDegrees(90 * faceRotations));
		// render the big gear
		// for whatever reason, if we point in a positive direction, we have to spin backward
		Axis bigAxis = bigDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.YN : Axis.YP;
		Axis smallAxis = smallDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		poseStack.pushPose();
		poseStack.mulPose(bigAxis.rotation(bigRadians));
		this.itemRenderer.renderStatic(models.gear, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		// render the small gear + axle
		poseStack.pushPose();
		poseStack.mulPose(smallAxis.rotation(smallRadians));
		this.itemRenderer.renderStatic(models.axle, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		poseStack.popPose();
	}

	private static record GearshifterModels(ItemStack gear, ItemStack axle)
	{
		public static GearshifterModels of(Block block)
		{
			ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
			ResourceLocation gearModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_gear");
			ResourceLocation axleModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
			ItemStack gear = new ItemStack(Items.STICK);
			gear.set(DataComponents.ITEM_MODEL, gearModel);
			ItemStack axle = new ItemStack(Items.STICK);
			axle.set(DataComponents.ITEM_MODEL, axleModel);
			return new GearshifterModels(gear,axle);
		}
	}
}
