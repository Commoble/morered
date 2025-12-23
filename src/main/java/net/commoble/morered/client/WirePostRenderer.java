package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.wire_post.SlackInterpolator;
import net.commoble.morered.wire_post.WirePostBlockEntity;
import net.commoble.morered.wire_post.WirePostBlockEntity.WirePostConnectionRenderInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WirePostRenderer implements BlockEntityRenderer<WirePostBlockEntity, WirePostBlockEntityRenderState>
{
	@Override
	public WirePostBlockEntityRenderState createRenderState()
	{
		return new WirePostBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(WirePostBlockEntity post, WirePostBlockEntityRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(post, renderState, partialTicks, camera, overlay);
		renderState.updateForRed(post, partialTicks);
	}

	@Override
	public void submit(WirePostBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		for (WirePostConnectionRenderInfo info : renderState.connectionStates)
		{
			this.renderConnection(renderState, info, poseStack, collector);
		}
		for (WirePostConnectionRenderInfo info : renderState.playerConnections.values())
		{
			this.renderConnection(renderState, info, poseStack, collector);
		}
	}

	public WirePostRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	private void renderConnection(
		WirePostBlockEntityRenderState postState,
		WirePostConnectionRenderInfo connectionState,
		PoseStack poseStack,
		SubmitNodeCollector collector)
	{
		Vec3 startVec = postState.startVec;
		Vec3 endVec = connectionState.endVec;
		int startRed = postState.red;
		int endRed = connectionState.red;
		poseStack.pushPose();

		boolean translateSwap = false;
		if (startVec.y() > endVec.y())
		{
			Vec3 swap = startVec;
			startVec = endVec;
			endVec = swap;
			int swapRed = startRed;
			startRed = endRed;
			endRed = swapRed;
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
			float maxLerpFactor = lines;
			float lineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
			
			poseStack.pushPose();
			for (int line = 0; line < lines; line++)
			{
				float lerpFactor = line / maxLerpFactor;
				int red = Mth.lerpDiscrete(lerpFactor, startRed, endRed);
				Vec3 firstPoint = pointList[line];
				Vec3 secondPoint = pointList[line+1];
				collector.submitCustomGeometry(poseStack, RenderTypes.LINES, (pose, vertexBuilder) -> {
					vertexBuilder.addVertex(pose, (float)firstPoint.x(), (float)firstPoint.y(), (float)firstPoint.z())
						.setColor(red, 0, 0, 255)
						.setNormal((float)firstPoint.x(), (float)firstPoint.y(), (float)firstPoint.z())
						.setLineWidth(lineWidth);
					vertexBuilder.addVertex(pose, (float)secondPoint.x(), (float)secondPoint.y(), (float)secondPoint.z())
						.setColor(red, 0, 0, 255)
						.setNormal((float)secondPoint.x(), (float)secondPoint.y(), (float)secondPoint.z())
						.setLineWidth(lineWidth);
				});
			}
			poseStack.popPose();
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
}
