package net.commoble.morered.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.TwentyFourBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ExtractorBlockEntityRenderer(
	ItemRenderer itemRenderer,
	ItemStack pump,
	ItemStack axle,
	ItemStack bag) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static ExtractorBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		ResourceLocation blockId = MoreRed.get().extractorBlock.getId();
		ResourceLocation pumpModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_pump");
		ResourceLocation axleModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
		ResourceLocation bagModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_bag");
		ItemStack pump = new ItemStack(Items.STICK);
		pump.set(DataComponents.ITEM_MODEL, pumpModel);
		ItemStack axle = new ItemStack(Items.STICK);
		axle.set(DataComponents.ITEM_MODEL, axleModel);
		ItemStack bag = new ItemStack(Items.STICK);
		bag.set(DataComponents.ITEM_MODEL, bagModel);
		return new ExtractorBlockEntityRenderer(context.getItemRenderer(), pump, axle, bag);
	}
	
	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, Vec3 Camera)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
		Direction inputDir = attachDir.getOpposite();
		Direction axleDir = PlateBlockStateProperties.getOutputDirection(state);
		Direction reverseAxleDir = axleDir.getOpposite();
		int faceRotations = state.getValue(TwentyFourBlock.ROTATION);
		float inputRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(inputDir), MechanicalState.ZERO).angularVelocity();
		float axleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(axleDir), MechanicalState.ZERO).angularVelocity();
		float reverseAxleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(reverseAxleDir), MechanicalState.ZERO).angularVelocity();
		@SuppressWarnings("resource")
		Level level = Minecraft.getInstance().level;
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float inputRadians = inputRadiansPerSecond * seconds;
		float axleRadians = axleRadiansPerSecond * seconds;
		float reverseAxleRadians = reverseAxleRadiansPerSecond * seconds;
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, faceRotations);
		Axis axleAxis = axleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		Axis reverseAxleAxis = reverseAxleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		float cosInputRadians = Mth.cos(inputRadians);
		float cosInputPlus1 = cosInputRadians + 1F;
		float cosInputMinus1 = cosInputRadians - 1F;
		// render the pump
		// this is translated toward down by some value y
		// let y0 = 0, y1 = -0.25
		// cos is 1 to -1
		// what maps {1 -> -1} to {0 -> -0.25}
		// slope first
		// m0 = -2
		// m1 = -0.25
		// that's 1/8
		// y' = 0.125 * cos(radians)
		// that gives us {0.125 -> -0.125}
		// then subtract 1/8
		// y = 0.125 * cos(radians) - 0.125
		// or y = 0.125(cos(radians) - 1)
		poseStack.pushPose();
		poseStack.translate(0D, 0.125F * cosInputMinus1, 0D);
		this.itemRenderer.renderStatic(this.pump, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		// render the bag
		// this scales on the y-axis from 0 to 1
		// use cos again
		// what maps from {1 -> -1} to {0 -> 1}
		// slope is -0.5
		// y' = -0.5F * cos(radians) = {-0.5 -> 0.5}
		// then add 0.5
		// y'' = -0.5F * cos(radians) + 0.5 = -0.5F * (cos(radians) - 1)
		// now, the scale pulls it toward the middle of the block
		// it renders at y=0.5F when scale is 0 at the near end
		// we want to push this up by 7/16
		// at the far end, we want this to be 0
		// so we need to map cos from {1,-1} to {1,0}
		// y' = 0.5cos(radians) = {0.5,-0.5}
		// y'' = 0.5cos(radians) + 0.5 = 0.5(cos(radians) + 1) = {1,0}
		// y''' = 7/16 * 0.5(cosInputPlus1) = 7/32 * cosInputPlus1 = 0.21875 * cosInputPlus1
		poseStack.pushPose();
		poseStack.translate(0D, 0.21875F*cosInputPlus1, 0D);
		poseStack.scale(1F, -0.5F * cosInputMinus1, 1F);
		this.itemRenderer.renderStatic(this.bag, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		// render the axles
		poseStack.pushPose();
		poseStack.mulPose(axleAxis.rotation(axleRadians));
		this.itemRenderer.renderStatic(this.axle, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		poseStack.pushPose();
		poseStack.mulPose(Axis.YN.rotationDegrees(180F));
		poseStack.mulPose(reverseAxleAxis.rotation(reverseAxleRadians));
		this.itemRenderer.renderStatic(this.axle, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		poseStack.popPose();
	}
}
