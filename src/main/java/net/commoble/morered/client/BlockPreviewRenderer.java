package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Wrapper class around the vanilla block renderer so we don't have to copy five
 * functions verbatim
 **/
public class BlockPreviewRenderer extends ModelBlockRenderer
{
	private static BlockPreviewRenderer INSTANCE;
	
	public static BlockPreviewRenderer getInstance(ModelBlockRenderer baseRenderer)
	{
		if (INSTANCE == null || INSTANCE.blockColors != baseRenderer.blockColors)
		{
			INSTANCE = new BlockPreviewRenderer(baseRenderer);
		}
		
		return INSTANCE;
	}
	public BlockPreviewRenderer(ModelBlockRenderer baseRenderer)
	{
		super(baseRenderer.blockColors);
	}

	// invoked from the DrawHighlightEvent.HighlightBlock event
	public static void renderBlockPreview(BlockPos pos, BlockState state, Level level, Vec3 currentRenderPos, PoseStack matrix, MultiBufferSource renderTypeBuffer)
	{
		matrix.pushPose();
	
		// the current position of the matrix stack is the position of the player's
		// viewport (the head, essentially)
		// we want to move it to the correct position to render the block at
		double offsetX = pos.getX() - currentRenderPos.x();
		double offsetY = pos.getY() - currentRenderPos.y();
		double offsetZ = pos.getZ() - currentRenderPos.z();
		matrix.translate(offsetX, offsetY, offsetZ);
	
		BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
		ModelBlockRenderer renderer = getInstance(blockDispatcher.getModelRenderer());
		renderer.tesselateBlock(
			level,
			blockDispatcher.getBlockModel(state),
			state,
			pos,
			matrix,
			renderTypeBuffer.getBuffer(Sheets.translucentItemSheet()),
			false,
			level.random,
			state.getSeed(pos),
			OverlayTexture.NO_OVERLAY,
			ModelData.EMPTY,
			RenderType.translucent());
	
		matrix.popPose();
	}

	// vanilla ModelBlockRenderer hardcodes 1F for the alpha
	@Override
	public void putQuadData(BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer buffer, PoseStack.Pose matrixEntry, BakedQuad quadIn,
		float tintA, float tintB, float tintC, float tintD, int brightness0, int brightness1, int brightness2, int brightness3, int combinedOverlayIn)
	{
		float r=1F;
		float g=1F;
		float b=1F;
		if (quadIn.isTinted())
		{
			int i = this.blockColors.getColor(state, level, pos, quadIn.getTintIndex());
			r = (i >> 16 & 255) / 255.0F;
			g = (i >> 8 & 255) / 255.0F;
			b = (i & 255) / 255.0F;
		}
		if (quadIn.isShade()) // better name: shouldApplyDiffuseLighting
		{
			float shade = level.getShade(quadIn.getDirection(), true);
			r *= shade;
			g *= shade;
			b *= shade;
		}

		float alpha = ClientProxy.CLIENTCONFIG.previewPlacementOpacity().get().floatValue();

		buffer.putBulkData(matrixEntry, quadIn, new float[] { tintA, tintB, tintC, tintD }, r, g, b, alpha, new int[] { brightness0, brightness1, brightness2, brightness3 },
			combinedOverlayIn, true);
	}
}
