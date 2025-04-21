package net.commoble.morered.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.mechanisms.ClutchBlock;
import net.commoble.morered.mechanisms.GearBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ClutchBlockEntityRenderer(ItemRenderer itemRenderer) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static ClutchBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new ClutchBlockEntityRenderer(context.getItemRenderer());
	}

	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, Vec3 Camera)
	{
		BlockState state = be.getBlockState();
		if (state.getBlock() instanceof ClutchBlock clutchBlock)
		{
			ItemStack stack = clutchBlock.getGearItem();
			Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
			Direction facing = state.getValue(GearBlock.FACING);
			MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofSide(facing), MechanicalState.ZERO);
			float radiansPerSecond = (float)mechanicalState.angularVelocity();
			@SuppressWarnings("resource")
			Level level = Minecraft.getInstance().level;
			int gameTimeTicks = MechanicalState.getMachineTicks(level);
			float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
			float radians = radiansPerSecond * seconds;
			BlockPos pos = be.getBlockPos();
			poseStack.pushPose();
			if (!state.getValue(ClutchBlock.EXTENDED))
			{
				double offset = -0.3125D; // 5/16ths
				poseStack.translate(offset * facing.getStepX(), offset * facing.getStepY(), offset * facing.getStepZ());
			}
			GearBlockEntityRenderer.renderGear(this.itemRenderer, level, pos, stack, facing, radians, poseStack, bufferSource, packedLight, overlay);
			poseStack.popPose();
		}
	}

}
