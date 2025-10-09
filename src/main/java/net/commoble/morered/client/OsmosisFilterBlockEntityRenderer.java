package net.commoble.morered.client;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.MoreRed;
import net.commoble.morered.client.FilterBlockEntityRenderer.FilterBlockEntityRenderState;
import net.commoble.morered.client.OsmosisFilterBlockEntityRenderer.OsmosisFilterRenderState;
import net.commoble.morered.transportation.FilterBlockEntity;
import net.commoble.morered.transportation.OsmosisFilterBlock;
import net.commoble.morered.transportation.OsmosisSlimeBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class OsmosisFilterBlockEntityRenderer extends FilterBlockEntityRenderer<OsmosisFilterRenderState>
{	
	public OsmosisFilterBlockEntityRenderer(BlockEntityRendererProvider.Context context, Supplier<OsmosisFilterRenderState> stateFactory)
	{
		super(context, stateFactory);
	}

	public static OsmosisFilterBlockEntityRenderer createOsmosisFilterrRenderer(BlockEntityRendererProvider.Context context)
	{
		return new OsmosisFilterBlockEntityRenderer(context, OsmosisFilterRenderState::new);
	}
	
	public static class OsmosisFilterRenderState extends FilterBlockEntityRenderState
	{
		public double lengthScale = 0;
	}
	
	@Override
	public void extractRenderState(FilterBlockEntity filter, OsmosisFilterRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		super.extractRenderState(filter, renderState, partialTicks, camera, overlay);

		long transferhash = filter.getBlockPos().hashCode();
		double time = (double)filter.getLevel().getGameTime() + (double)transferhash + (double)partialTicks; // casting to doubles fixes a weird rounding error that was causing choppy animation

		int rate = MoreRed.SERVERCONFIG.osmosisFilterTransferRate().get();
		double minScale = 0.25D;
		renderState.lengthScale = minScale + (filter.getBlockState().getValue(OsmosisFilterBlock.TRANSFERRING_ITEMS)
			? (-Math.cos(2 * Math.PI * time / rate) + 1D) * 0.25D
			: 0D);
	}



	@Override
	public void submit(OsmosisFilterRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		super.submit(renderState, poseStack, collector, camera);
		BlockState filterState = renderState.blockState;
		Direction dir = filterState.getValue(OsmosisFilterBlock.FACING);
		BlockState slimeState = MoreRed.OSMOSIS_SLIME_BLOCK.get().defaultBlockState().setValue(OsmosisSlimeBlock.FACING, dir);
		double lengthScale = renderState.lengthScale;
		double lengthTranslateFactor = 1D - lengthScale;

		double zFightFix = 0.9999D;
		
		int dirOffsetX = dir.getStepX();
		int dirOffsetY = dir.getStepY();
		int dirOffsetZ = dir.getStepZ();
		
		float scaleX = (float) (dirOffsetX == 0 ? zFightFix : lengthScale);
		float scaleY = (float) (dirOffsetY == 0 ? zFightFix : lengthScale);
		float scaleZ = (float) (dirOffsetZ == 0 ? zFightFix : lengthScale);
		
		int translateFactorX = dirOffsetX == 0 ? 0 : 1;
		int translateFactorY = dirOffsetY == 0 ? 0 : 1;
		int translateFactorZ = dirOffsetZ == 0 ? 0 : 1;
		
		double tX = dirOffsetX < 0 ? 1D : 0D;
		double tY = dirOffsetY < 0 ? 1D : 0D;
		double tZ = dirOffsetZ < 0 ? 1D : 0D;
		
		double translateX = translateFactorX * (tX * lengthTranslateFactor + 0.125D*dirOffsetX);
		double translateY = translateFactorY * (tY * lengthTranslateFactor + 0.125D*dirOffsetY);
		double translateZ = translateFactorZ * (tZ * lengthTranslateFactor + 0.125D*dirOffsetZ);
		
		poseStack.pushPose();
		poseStack.translate(translateX, translateY, translateZ);
		poseStack.scale(scaleX, scaleY, scaleZ);
		
		collector.submitBlock(poseStack, slimeState, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
