package net.commoble.morered.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.commoble.morered.TwentyFourBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
	
	public static void renderHeldItemAsBlockPreview(BlockPos pos, ItemStack stack, BlockState state, Level level, Vec3 currentRenderPos, PoseStack poseStack, MultiBufferSource renderTypeBuffer)
	{
		// what ItemRenderer#renderItem does but we bypass some things to add alpha to the verts
		VertexConsumer vertexConsumer = renderTypeBuffer.getBuffer(Sheets.translucentItemSheet());
		Minecraft mc = Minecraft.getInstance();
		ItemRenderer itemRenderer = mc.getItemRenderer();
		ItemModelResolver resolver = mc.getItemModelResolver();
		ItemStackRenderState renderState = itemRenderer.scratchItemStackRenderState;
		resolver.updateForTopItem(renderState, stack, ItemDisplayContext.NONE, level, mc.player, mc.player.getId());
		int packedLight = ModelBlockRenderer.CACHE.get().getLightColor(state, level, pos);
		poseStack.pushPose();
	
		// the current position of the matrix stack is the position of the player's
		// viewport (the head, essentially)
		// we want to move it to the correct position to render the block at
		double offsetX = pos.getX() - currentRenderPos.x();
		double offsetY = pos.getY() - currentRenderPos.y();
		double offsetZ = pos.getZ() - currentRenderPos.z();
		poseStack.translate(offsetX, offsetY, offsetZ);
		if (state.hasProperty(TwentyFourBlock.ATTACHMENT_DIRECTION) && state.hasProperty(TwentyFourBlock.ROTATION))
		{
			Direction attachDir = state.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
			int rotations = state.getValue(TwentyFourBlock.ROTATION);
			// item renderer renders the center of the item model at 0,0,0 in the cube
			poseStack.translate(0.5D,0.5D,0.5D); 
			RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, rotations);
			// for whatever reason, we need to untranslate after we rotate the block...
			// BER doesn't need this, why do we need it here?
			poseStack.translate(-0.5D,-0.5D,-0.5D);
		}
		for (ItemStackRenderState.LayerRenderState layerRenderState : renderState.layers)
		{
            poseStack.pushPose();
			renderItemQuads(
				poseStack,
				vertexConsumer,
				layerRenderState.quads,
				layerRenderState.tintLayers,
				packedLight,
				OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
		}
		poseStack.popPose();
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
	
	private static void renderItemQuads(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads, int[] tints, int packedLight, int overlay)
	{
		PoseStack.Pose posestack$pose = poseStack.last();
		for (BakedQuad bakedquad : quads)
		{
			float alpha = (float) (double) ClientProxy.CLIENTCONFIG.previewPlacementOpacity().get();
			float r = 1F;
			float g = 1F;
			float b = 1F;
			if (bakedquad.isTinted())
			{
				int tint = ItemRenderer.getLayerColorSafe(tints, bakedquad.tintIndex());
				alpha *= (ARGB.alpha(tint) / 255.0F);
				r = ARGB.red(tint) / 255.0F;
				g = ARGB.green(tint) / 255.0F;
				b = ARGB.blue(tint) / 255.0F;
			}

			buffer.putBulkData(posestack$pose, bakedquad, r, g, b, alpha, packedLight, overlay, true);
		}
	}
}
