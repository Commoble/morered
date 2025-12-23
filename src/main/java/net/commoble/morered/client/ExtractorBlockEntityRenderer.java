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
import net.commoble.morered.client.ExtractorBlockEntityRenderer.ExtractorRenderState;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ExtractorBlockEntityRenderer(
	ItemModelResolver resolver,
	ItemStack pump,
	ItemStack axle,
	ItemStack bag) implements BlockEntityRenderer<GenericBlockEntity, ExtractorRenderState>
{
	public static class ExtractorRenderState extends BlockEntityRenderState
	{
		public final ItemStackRenderState pumpItemState = new ItemStackRenderState();
		public final ItemStackRenderState axleItemState = new ItemStackRenderState();
		public final ItemStackRenderState bagItemState = new ItemStackRenderState();
		public float cosInputRadians = 0F;
		public float axleRadians = 0F;
		public float reverseAxleRadians = 0F;
	}
	
	public static ExtractorBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		Identifier blockId = MoreRed.EXTRACTOR_BLOCK.getId();
		Identifier pumpModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_pump");
		Identifier axleModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
		Identifier bagModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_bag");
		ItemStack pump = new ItemStack(Items.STICK);
		pump.set(DataComponents.ITEM_MODEL, pumpModel);
		ItemStack axle = new ItemStack(Items.STICK);
		axle.set(DataComponents.ITEM_MODEL, axleModel);
		ItemStack bag = new ItemStack(Items.STICK);
		bag.set(DataComponents.ITEM_MODEL, bagModel);
		return new ExtractorBlockEntityRenderer(context.itemModelResolver(), pump, axle, bag);
	}

	@Override
	public ExtractorRenderState createRenderState()
	{
		return new ExtractorRenderState();
	}

	@Override
	public void extractRenderState(GenericBlockEntity be, ExtractorRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(be, renderState, partialTicks, camera, overlay);

		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
		Direction inputDir = attachDir.getOpposite();
		Direction axleDir = PlateBlockStateProperties.getOutputDirection(state);
		Direction reverseAxleDir = axleDir.getOpposite();
		float inputRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(inputDir), MechanicalState.ZERO).angularVelocity();
		float axleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(axleDir), MechanicalState.ZERO).angularVelocity();
		float reverseAxleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(reverseAxleDir), MechanicalState.ZERO).angularVelocity();
		Level level = Minecraft.getInstance().level;
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float inputRadians = inputRadiansPerSecond * seconds;
		renderState.axleRadians = axleRadiansPerSecond * seconds;
		renderState.reverseAxleRadians = reverseAxleRadiansPerSecond * seconds;
		renderState.cosInputRadians = Mth.cos(inputRadians);
		int renderSeed = (int)(be.getBlockPos().asLong());
		resolver.updateForTopItem(renderState.pumpItemState, this.pump, ItemDisplayContext.NONE, level, null, renderSeed);
		resolver.updateForTopItem(renderState.axleItemState, this.axle, ItemDisplayContext.NONE, level, null, renderSeed);
		resolver.updateForTopItem(renderState.bagItemState, this.bag, ItemDisplayContext.NONE, level, null, renderSeed);
	}

	@Override
	public void submit(ExtractorRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		BlockState state = renderState.blockState;
		Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
		Direction axleDir = PlateBlockStateProperties.getOutputDirection(state);
		Direction reverseAxleDir = axleDir.getOpposite();
		int faceRotations = state.getValue(TwentyFourBlock.ROTATION);
		Axis axleAxis = axleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		Axis reverseAxleAxis = reverseAxleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		float cosInputPlus1 = renderState.cosInputRadians + 1F;
		float cosInputMinus1 = renderState.cosInputRadians - 1F;
		
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, faceRotations);
		
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
		renderState.pumpItemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
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
		renderState.bagItemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		
		// render the axles
		poseStack.pushPose();
		poseStack.mulPose(axleAxis.rotation(renderState.axleRadians));
		renderState.axleItemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		
		poseStack.pushPose();
		poseStack.mulPose(Axis.YN.rotationDegrees(180F));
		poseStack.mulPose(reverseAxleAxis.rotation(renderState.reverseAxleRadians));
		renderState.axleItemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		
		poseStack.popPose();
		
		poseStack.popPose();
	}
}
