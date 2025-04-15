package net.commoble.morered.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.mechanisms.GearshifterBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record OscillatorBlockEntityRenderer(ItemRenderer itemRenderer, ItemStack axleModel) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static OscillatorBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.ITEM_MODEL, MoreRed.id("oscillator_axle"));
		return new OscillatorBlockEntityRenderer(context.getItemRenderer(), stack);
	}

	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, Vec3 Camera)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction attachDir = state.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		int faceRotations = state.getValue(GearshifterBlock.ROTATION);
		float radiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(attachDir), MechanicalState.ZERO).angularVelocity();
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, faceRotations);
		// for whatever reason, if we point in a positive direction, we have to spin backward
		Axis axis = attachDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.YN : Axis.YP;
		poseStack.mulPose(axis.rotation(radians));
		poseStack.scale(1.001F, 1.001F, 1.001F); // fix a z-fighting between the axle and the static model
		this.itemRenderer.renderStatic(axleModel, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		
		poseStack.popPose();
	}
	
	
}
