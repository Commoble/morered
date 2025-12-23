package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.mechanisms.AxleBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record AxleBlockEntityRenderer(ItemModelResolver resolver, Map<Block, ItemStack> stackCache) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{
	
	public static AxleBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new AxleBlockEntityRenderer(context.itemModelResolver(), new HashMap<>());
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
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofCube(), MechanicalState.ZERO);
		float radiansPerSecond = (float)mechanicalState.angularVelocity();
		ItemStack stack = this.stackCache.computeIfAbsent(be.getBlockState().getBlock(), ItemStack::new);
		Level level = Minecraft.getInstance().level;
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		renderState.update(this.resolver, be.getLevel(), stack, radians);
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		BlockState state = renderState.blockState;
		Direction.Axis axis = state.getValue(AxleBlock.AXIS);
		if (axis == Direction.Axis.X)
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(90F));
		}
		else if (axis == Direction.Axis.Y)
		{
			poseStack.mulPose(Axis.XN.rotationDegrees(90F));
		}
		poseStack.mulPose(Axis.ZP.rotation(renderState.radians));
		renderState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

}
