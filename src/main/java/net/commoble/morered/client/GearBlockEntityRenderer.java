package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.mechanisms.GearBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record GearBlockEntityRenderer(ItemModelResolver resolver, Map<Block, ItemStack> stackCache) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{	
	public static GearBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new GearBlockEntityRenderer(context.itemModelResolver(), new HashMap<>());
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
		Direction facing = state.getValue(GearBlock.FACING);
		MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofSide(facing), MechanicalState.ZERO);
		float radiansPerSecond = (float)mechanicalState.angularVelocity();
		ItemStack stack = this.stackCache.computeIfAbsent(be.getBlockState().getBlock(), ItemStack::new);
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		renderState.update(resolver, level, stack, radians);
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		renderGear(renderState.itemState, poseStack, collector, renderState.lightCoords, renderState.blockPos, renderState.blockState.getValue(GearBlock.FACING), renderState.radians);
	}

	public static void renderGear(ItemStackRenderState itemState, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, BlockPos pos, Direction facing, float radians)
	{
		int manhattanOffset = (int)((long)pos.getX() + (long)pos.getY() + (long)pos.getZ() % 6L);
		if (manhattanOffset < 0)
			manhattanOffset = manhattanOffset + 6;
		double zFightOffset = manhattanOffset * 0.0001D + facing.ordinal() * 0.00001D;
		
		poseStack.pushPose();
		// item renderer renders the center of the item model at 0,0,0 in the cube
		double offset = 0.5D - zFightOffset;
		poseStack.translate(offset, offset, offset);
		switch(facing)
		{
			case DOWN: poseStack.mulPose(Axis.XN.rotationDegrees(90F));break;
			case UP: poseStack.mulPose(Axis.XP.rotationDegrees(90));break;
			case SOUTH: poseStack.mulPose(Axis.YP.rotationDegrees(180F));break;
			case WEST: poseStack.mulPose(Axis.YP.rotationDegrees(90F));break;
			case EAST: poseStack.mulPose(Axis.YP.rotationDegrees(270F));break;
			default:
		}
		// keep gears from being in exactly the same angle
		radians += Mth.PI * 0.0625F * (manhattanOffset + facing.ordinal() / 6F);
		// because of the rotation above, the positive directions (up,south,east) are now about to rotate in the wrong direction
		// invert the rotation if needed
		float parityFix = facing.ordinal() % 2 == 0 ? 1F : -1F;
		poseStack.mulPose(Axis.ZP.rotation(radians * parityFix));
		itemState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
