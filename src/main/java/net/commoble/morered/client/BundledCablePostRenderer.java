package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.MoreRed;
import net.commoble.morered.wire_post.SlackInterpolator;
import net.commoble.morered.wire_post.WirePostBlockEntity;
import net.commoble.morered.wire_post.WirePostBlockEntity.WirePostConnectionRenderInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record BundledCablePostRenderer(MaterialSet materials) implements BlockEntityRenderer<WirePostBlockEntity, WirePostBlockEntityRenderState>
{	
	public static final RenderType CABLE_RENDER_TYPE = ExtraRenderTypes.CABLE_RENDER_TYPE;

	@SuppressWarnings("deprecation")
	public static final Material MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS,MoreRed.id("block/bundled_network_cable")); 

	public static final float[] REDS = {0.71F, 0.31F, 0.19F};
	public static final float[] GREENS = {0.19F, 0.48F, 0.29F};
	public static final float[] BLUES = {0.12F, 0.16F, 0.66F};
	
	public static BundledCablePostRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new BundledCablePostRenderer(context.materials());
	}

	@Override
	public WirePostBlockEntityRenderState createRenderState()
	{
		return new WirePostBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(WirePostBlockEntity post, WirePostBlockEntityRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(post, renderState, partialTicks, camera, overlay);
		renderState.updateForBlockLight(post, partialTicks);
	}

	@Override
	public void submit(WirePostBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		TextureAtlasSprite sprite = this.materials.get(MATERIAL);
		float totalMinU = sprite.getU0();
		float totalMinV = sprite.getV0();
		float totalMaxU = sprite.getU1();
		float totalMaxV = sprite.getV1();
		float texWidth = totalMaxU - totalMinU;
		float texHeight = totalMaxV - totalMinV;
		float quadStartU = (11F/16F) * texWidth + totalMinU;
		float quadEndU = (13F/16F) * texWidth + totalMinU;
		float quadStartV = (6F/16F) * texHeight + totalMinV;
		float quadEndV = (12F/16F) * texHeight + totalMinV;
		for (WirePostConnectionRenderInfo connectionState : renderState.connectionStates)
		{
			this.renderConnection(renderState, connectionState, poseStack, collector, quadStartU, quadEndU, quadStartV, quadEndV);
		}
		for (WirePostConnectionRenderInfo connectionState : renderState.playerConnections.values())
		{
			this.renderConnection(renderState, connectionState, poseStack, collector, quadStartU, quadEndU, quadStartV, quadEndV);
		}
	}

	private void renderConnection(
		WirePostBlockEntityRenderState postState,
		WirePostConnectionRenderInfo connectionState,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		float minU, float maxU, float minV, float maxV)
	{
		Vec3 startVec = postState.startVec;
		Vec3 endVec = connectionState.endVec;
		int startBlockLight = postState.startBlockLight;
		int startSkyLight = postState.startSkyLight;
		int endBlockLight = connectionState.endBlockLight;
		int endSkyLight = connectionState.endSkyLight;
		poseStack.pushPose();

		boolean translateSwap = false;
		if (startVec.y() > endVec.y())
		{
			Vec3 swap = startVec;
			startVec = endVec;
			endVec = swap;
			float swapF = minU;
			minU = maxU;
			maxU = swapF;
			swapF = minV;
			minV = maxV;
			maxV = swapF;
			
			int swapI = startBlockLight;
			startBlockLight = endBlockLight;
			endBlockLight = swapI;
			swapI = startSkyLight;
			startSkyLight = endSkyLight;
			endSkyLight = swapI;
			
			translateSwap = true;
		}

		poseStack.translate(0.5D, 0.5D, 0.5D);

		double startX = startVec.x();
		double startY = startVec.y();
		double startZ = startVec.z();

		double endX = endVec.x();
		double endY = endVec.y();
		double endZ = endVec.z();
		float dx = (float) (endX - startX);
		float dy = (float) (endY - startY);
		float dz = (float) (endZ - startZ);
		if (translateSwap)
		{
			poseStack.translate(-dx, -dy, -dz);
		}

		if (startY <= endY)
		{
			Vec3[] pointList = SlackInterpolator.getInterpolatedDifferences(endVec.subtract(startVec));
			int points = pointList.length;
			int lines = points - 1;
			float cableWidth = 0.1F;
			@SuppressWarnings("deprecation")
			float offsetScale = 0.5F * cableWidth * (float)Mth.fastInvSqrt(dx * dx + dz * dz);
			float xOffset = dz * offsetScale;
			float zOffset = dx * offsetScale;
			float maxLerpFactor = lines - 1F;

			for (int segmentIndex = 0; segmentIndex < lines; ++segmentIndex)
			{
				Vec3 firstPoint = pointList[segmentIndex];
				Vec3 secondPoint = pointList[segmentIndex+1];
				float lerpFactor = segmentIndex / maxLerpFactor;
				int lerpedBlockLight = (int) Mth.lerp(lerpFactor, startBlockLight, endBlockLight);
				int lerpedSkyLight = (int) Mth.lerp(lerpFactor, startSkyLight, endSkyLight);
				int packedLight = LightTexture.pack(lerpedBlockLight, lerpedSkyLight);
				float x0 = (float) firstPoint.x;
				float y0 = (float) firstPoint.y;
				float z0 = (float) firstPoint.z;
				float x1 = (float) secondPoint.x;
				float y1 = (float) secondPoint.y;
				float z1 = (float) secondPoint.z;
				int secondSegmentIndex = segmentIndex+1;
				if (xOffset == 0F && zOffset == 0F) // completely vertical
				{
					// render the quads with a different orientation if the cable is completely vertical
					// (otherwise they have 0 width and are invisible)
					float width = (float) (cableWidth * 0.5F);
					addVertexPair(poseStack, collector, packedLight, x0, y0, z0, cableWidth, cableWidth, segmentIndex, false, width, width, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x1, y1, z1, cableWidth, cableWidth, secondSegmentIndex, true, width, width, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x0, y0, z0, cableWidth, 0F, segmentIndex, false, -width, width, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x1, y1, z1, cableWidth, 0F, secondSegmentIndex, true, -width, width, minU, maxU, minV, maxV);
				}
				else
				{
					addVertexPair(poseStack, collector, packedLight, x0, y0, z0, cableWidth, cableWidth, segmentIndex, false, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x1, y1, z1, cableWidth, cableWidth, secondSegmentIndex, true, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x0, y0, z0, cableWidth, 0F, segmentIndex, false, xOffset, zOffset, minU, maxU, minV, maxV);
					addVertexPair(poseStack, collector, packedLight, x1, y1, z1, cableWidth, 0F, secondSegmentIndex, true, xOffset, zOffset, minU, maxU, minV, maxV);
				}
			}
		}

		poseStack.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen()
	{
		return true;
	}

	@Override
	public AABB getRenderBoundingBox(WirePostBlockEntity blockEntity)
	{
		return blockEntity.getRenderBoundingBox();
	}

	public static void addVertexPair(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, float x, float y, float z, float cableWidth,
		float cableWidthOrZero, int segmentIndex, boolean secondVertexPairForQuad, float xOffset, float zOffset,
		float minU, float maxU, float minV, float maxV)
	{
		collector.submitCustomGeometry(poseStack, CABLE_RENDER_TYPE, (lastMatrix, vertexBuilder) -> {
			if (!secondVertexPairForQuad)
			{
				vertexBuilder.addVertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
					.setColor(1F, 1F, 1F, 1F)
					.setUv(minU, minV)
					.setLight(packedLight);
				vertexBuilder.addVertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
					.setColor(1F, 1F, 1F, 1F)
					.setUv(minU, maxV)
					.setLight(packedLight);
			}
			else
			{

				vertexBuilder.addVertex(lastMatrix, x - xOffset, y + cableWidthOrZero, z + zOffset)
					.setColor(1F, 1F, 1F, 1F)
					.setUv(maxU, maxV)
					.setLight(packedLight);
				vertexBuilder.addVertex(lastMatrix, x + xOffset, y + cableWidth - cableWidthOrZero, z - zOffset)
					.setColor(1F, 1F, 1F, 1F)
					.setUv(maxU, minV)
					.setLight(packedLight);
			}
		});

	}

}
