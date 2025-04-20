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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ExtractorBlockEntityRenderer(ItemRenderer itemRenderer, ItemStack screw, ItemStack axle) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static ExtractorBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		ResourceLocation blockId = MoreRed.get().extractorBlock.getId();
		ResourceLocation screwModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_screw");
		ResourceLocation axleModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
		ItemStack screw = new ItemStack(Items.STICK);
		screw.set(DataComponents.ITEM_MODEL, screwModel);
		ItemStack axle = new ItemStack(Items.STICK);
		axle.set(DataComponents.ITEM_MODEL, axleModel);
		return new ExtractorBlockEntityRenderer(context.getItemRenderer(), screw, axle);
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
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float inputRadians = inputRadiansPerSecond * seconds;
		float axleRadians = axleRadiansPerSecond * seconds;
		float reverseAxleRadians = reverseAxleRadiansPerSecond * seconds;
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, faceRotations);
		// render the screw
		Axis screwAxis = attachDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.YN : Axis.YP;
		Axis axleAxis = axleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		Axis reverseAxleAxis = reverseAxleDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		poseStack.pushPose();
		poseStack.mulPose(screwAxis.rotation(inputRadians));
		this.itemRenderer.renderStatic(this.screw, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
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
