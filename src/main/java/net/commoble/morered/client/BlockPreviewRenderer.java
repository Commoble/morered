package net.commoble.morered.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.commoble.morered.TwentyFourBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
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
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;

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

	public static BlockPreviewRenderState extractBlockPreview(Level level, BlockPos pos, BlockState state)
	{
		BlockRenderDispatcher blockDispatcher = Minecraft.getInstance().getBlockRenderer();
		BlockPreviewRenderer renderer = getInstance(blockDispatcher.getModelRenderer());
		List<BlockModelPart> parts = blockDispatcher
			.getBlockModel(state)
			.collectParts(level, pos, state, RandomSource.create(state.getSeed(pos)));
		MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
        movingBlockRenderState.randomSeedPos = pos;
        movingBlockRenderState.blockPos = pos;
        movingBlockRenderState.blockState = state;
        movingBlockRenderState.biome = level.getBiome(pos);
        movingBlockRenderState.level = level;
		return new BlockPreviewRenderState(renderer, parts, movingBlockRenderState);
	}
	
	public static record BlockPreviewRenderState(
		BlockPreviewRenderer renderer,
		List<BlockModelPart> parts,
		MovingBlockRenderState movingBlockRenderState
		) implements CustomBlockOutlineRenderer
	{

		@Override
		public boolean render(BlockOutlineRenderState renderState, BufferSource buffer, PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState)
		{
			BlockPos pos = this.movingBlockRenderState.blockPos;
			Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
			poseStack.pushPose();
			
			// the current position of the matrix stack is the position of the player's
			// viewport (the head, essentially)
			// we want to move it to the correct position to render the block at
			double offsetX = pos.getX() - cameraPos.x();
			double offsetY = pos.getY() - cameraPos.y();
			double offsetZ = pos.getZ() - cameraPos.z();
			poseStack.translate(offsetX, offsetY, offsetZ);
			renderer.tesselateBlock(
				this.movingBlockRenderState,
				parts,
				this.movingBlockRenderState.blockState,
				pos,
				poseStack,
				renderType -> buffer.getBuffer(Sheets.translucentItemSheet()),
				false,
				OverlayTexture.NO_OVERLAY);
		
			poseStack.popPose();
			return false;
		}
		
	}
	
	public static HeldItemPreviewRenderState extractHeldItemPreview(Level level, BlockPos placePos, BlockState stateForPlacement, ItemStack stack, LocalPlayer player)
	{
		Minecraft mc = Minecraft.getInstance();
		ItemModelResolver resolver = mc.getItemModelResolver();
		ItemStackRenderState itemState = new ItemStackRenderState();
		resolver.updateForTopItem(itemState, stack, ItemDisplayContext.NONE, level, player, player.getId());
		int packedLight = ModelBlockRenderer.CACHE.get().getLightColor(stateForPlacement, level, placePos);
		return new HeldItemPreviewRenderState(itemState, placePos, stateForPlacement, packedLight);
	}
	
	public static record HeldItemPreviewRenderState(
		ItemStackRenderState itemState,
		BlockPos placePos,
		BlockState stateForPlacement, // state of blockitem being placed
		int packedLight
		) implements CustomBlockOutlineRenderer
	{

		@Override
		public boolean render(BlockOutlineRenderState renderState, BufferSource buffer, PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState)
		{
			// what ItemRenderer#renderItem does but we bypass some things to add alpha to the verts
			VertexConsumer vertexConsumer = buffer.getBuffer(Sheets.translucentItemSheet());
			BlockPos pos = this.placePos;
			Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
			
			poseStack.pushPose();
		
			// the current position of the matrix stack is the position of the player's
			// viewport (the head, essentially)
			// we want to move it to the correct position to render the block at
			double offsetX = pos.getX() - cameraPos.x();
			double offsetY = pos.getY() - cameraPos.y();
			double offsetZ = pos.getZ() - cameraPos.z();
			poseStack.translate(offsetX, offsetY, offsetZ);
			if (stateForPlacement.hasProperty(TwentyFourBlock.ATTACHMENT_DIRECTION) && stateForPlacement.hasProperty(TwentyFourBlock.ROTATION))
			{
				Direction attachDir = stateForPlacement.getValue(TwentyFourBlock.ATTACHMENT_DIRECTION);
				int rotations = stateForPlacement.getValue(TwentyFourBlock.ROTATION);
				// item renderer renders the center of the item model at 0,0,0 in the cube
				poseStack.translate(0.5D,0.5D,0.5D); 
				RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, attachDir, rotations);
				// for whatever reason, we need to untranslate after we rotate the block...
				// BER doesn't need this, why do we need it here?
				poseStack.translate(-0.5D,-0.5D,-0.5D);
			}
			for (ItemStackRenderState.LayerRenderState layerRenderState : this.itemState.layers)
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
			return false;
		}
		
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
