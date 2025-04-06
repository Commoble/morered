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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record AxleBlockEntityRenderer(ItemRenderer itemRenderer, Map<Block, ItemStack> stackCache) implements BlockEntityRenderer<GenericBlockEntity>
{
	
	public static AxleBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new AxleBlockEntityRenderer(context.getItemRenderer(), new HashMap<>());
	}

	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, Vec3 camera)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofCube(), MechanicalState.ZERO);
		float radiansPerSecond = (float)mechanicalState.angularVelocity();
		ItemStack stack = this.stackCache.computeIfAbsent(be.getBlockState().getBlock(), ItemStack::new);
		Level level = be.getLevel();
		// reduce rounding errors at high game times, but we'll get a visual artifact every five minutes. Configurable?
		// this magic number because (6030+1F) / 20F / (2pi) is very close to a whole number
		long gameTimeTicks = level.getGameTime() % ClientProxy.CLIENTCONFIG.machineRenderCycleTicks().get();
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		BlockState state = be.getBlockState();
		Direction.Axis axis = state.getValue(AxleBlock.AXIS);
		if (axis == Direction.Axis.X)
		{
			poseStack.mulPose(Axis.YP.rotationDegrees(90F));
		}
		else if (axis == Direction.Axis.Y)
		{
			poseStack.mulPose(Axis.XN.rotationDegrees(90F));
		}
		
		poseStack.mulPose(Axis.ZP.rotation(radians));
		this.itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
	}

}
