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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record GearBlockEntityRenderer(ItemRenderer itemRenderer, Map<Block, ItemStack> stackCache) implements BlockEntityRenderer<GenericBlockEntity>
{	
	public static GearBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new GearBlockEntityRenderer(context.getItemRenderer(), new HashMap<>());
	}
	
	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(GearBlock.FACING);
		MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofSide(facing), MechanicalState.ZERO);
		float radiansPerSecond = (float)mechanicalState.angularVelocity();
		ItemStack stack = this.stackCache.computeIfAbsent(be.getBlockState().getBlock(), ItemStack::new);
		Level level = be.getLevel();
		// reduce rounding errors at high game times
		long gameTimeTicks = level.getGameTime() % ClientProxy.CLIENTCONFIG.machineRenderCycleTicks().get();
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		BlockPos pos = be.getBlockPos();
		int manhattanOffset = (int)((long)pos.getX() + (long)pos.getY() + (long)pos.getZ() % 6L);
		if (manhattanOffset < 0)
			manhattanOffset = manhattanOffset + 6;
		poseStack.pushPose();
		double zFightOffset = manhattanOffset * 0.0001D + facing.ordinal() * 0.00001D;
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
		this.itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
	}

}
