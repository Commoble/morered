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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record ClutchBlockEntityRenderer(ItemModelResolver resolver) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{
	public static ClutchBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new ClutchBlockEntityRenderer(context.itemModelResolver());
	}

	@Override
	public MechanicalItemBlockEntityRenderState createRenderState()
	{
		return new MechanicalItemBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(GenericBlockEntity be, MechanicalItemBlockEntityRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(be, renderState, partialTicks, camera, overlay);
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
			renderState.update(resolver, level, stack, radians);
		}
		else
		{
			renderState.clear();
		}
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		Direction facing = renderState.blockState.getValue(ClutchBlock.FACING);
		poseStack.pushPose();
		if (!renderState.blockState.getValue(ClutchBlock.EXTENDED))
		{
			double offset = -0.3125D; // 5/16ths
			poseStack.translate(offset * facing.getStepX(), offset * facing.getStepY(), offset * facing.getStepZ());
		}
		GearBlockEntityRenderer.renderGear(renderState.itemState, poseStack, collector, renderState.lightCoords, renderState.blockPos, facing, renderState.radians);
		poseStack.popPose();
	}

}
