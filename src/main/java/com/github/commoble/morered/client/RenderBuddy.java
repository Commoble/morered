package com.github.commoble.morered.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RenderBuddy
{

	public static void renderBlockPreview(BlockPos pos, BlockState state, World world, Vec3d currentRenderPos, MatrixStack matrix, IRenderTypeBuffer renderTypeBuffer)
	{
		matrix.push();

		// the current position of the matrix stack is the position of the player's
		// viewport (the head, essentially)
		// we want to move it to the correct position to render the block at
		double offsetX = pos.getX() - currentRenderPos.getX();
		double offsetY = pos.getY() - currentRenderPos.getY();
		double offsetZ = pos.getZ() - currentRenderPos.getZ();
		matrix.translate(offsetX, offsetY, offsetZ);

		BlockRendererDispatcher blockDispatcher = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockModelRenderer renderer = BlockPreviewRenderer.getInstance(blockDispatcher.getBlockModelRenderer());
		renderer.renderModel(
			world,
			blockDispatcher.getModelForState(state),
			state,
			pos,
			matrix,
			renderTypeBuffer.getBuffer(RenderType.getTranslucent()),
			false,
			world.rand,
			state.getPositionRandom(pos),
			OverlayTexture.NO_OVERLAY,
			net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);

		matrix.pop();
	}

}
