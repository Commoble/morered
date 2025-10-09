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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record AlternatorBlockEntityRenderer(ItemModelResolver resolver, ItemStack axleModel) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{
	
	public static AlternatorBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		ItemStack stack = new ItemStack(Items.STICK);
		stack.set(DataComponents.ITEM_MODEL, MoreRed.id("alternator_axle"));
		return new AlternatorBlockEntityRenderer(context.itemModelResolver(), stack);
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
		BlockState state = be.getBlockState();
		Direction attachDir = state.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		float radiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(attachDir), MechanicalState.ZERO).angularVelocity();
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		renderState.update(resolver, be.getLevel(), this.axleModel, radians);
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		poseStack.pushPose();
		
		Direction attachDir = renderState.blockState.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		int faceRotations = renderState.blockState.getValue(GearshifterBlock.ROTATION);
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, faceRotations);
		// for whatever reason, if we point in a positive direction, we have to spin backward
		Axis axis = attachDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.YN : Axis.YP;
		poseStack.mulPose(axis.rotation(renderState.radians));
		poseStack.scale(1.001F, 1.001F, 1.001F); // fix a z-fighting between the axle and the static model
		renderState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		
		poseStack.popPose();
	}
	
}
