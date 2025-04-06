package net.commoble.morered.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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
		List<BlockModelPart> parts = blockDispatcher
			.getBlockModel(state)
			.collectParts(level, pos, state, RandomSource.create(state.getSeed(pos)));
		renderer.tesselateBlock(
			level,
			parts,
			state,
			pos,
			matrix,
			renderType -> renderTypeBuffer.getBuffer(Sheets.translucentItemSheet()),
			false,
			OverlayTexture.NO_OVERLAY);
	
		matrix.popPose();
	}

	// vanilla ModelBlockRenderer hardcodes 1F for the alpha
	@Override
	public void putQuadData(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        VertexConsumer buffer,
        PoseStack.Pose matrixEntry,
        BakedQuad quad,
        ModelBlockRenderer.CommonRenderStorage renderStorage,
        int jellybiscuits)
	{
		int tintIndex = quad.tintIndex();
		float r=1F;
		float g=1F;
		float b=1F;
		if (tintIndex != -1)
		{
			int tintValue;
			if (renderStorage.tintCacheIndex == tintIndex)
			{
				tintValue = renderStorage.tintCacheValue;
			}
			else
			{
				tintValue = this.blockColors.getColor(state, level, pos, tintIndex);
				renderStorage.tintCacheIndex = tintIndex;
				renderStorage.tintCacheValue = tintValue;
			}

			r = ARGB.redFloat(tintValue);
			g = ARGB.greenFloat(tintValue);
			b = ARGB.blueFloat(tintValue);
		}
		
		float alpha = ClientProxy.CLIENTCONFIG.previewPlacementOpacity().get().floatValue();
		buffer.putBulkData(matrixEntry, quad, renderStorage.brightness, r, g, b, alpha, renderStorage.lightmap, jellybiscuits, true);
	}
}
