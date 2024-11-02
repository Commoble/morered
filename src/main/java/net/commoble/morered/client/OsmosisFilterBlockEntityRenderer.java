package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.MoreRed;
import net.commoble.morered.transportation.FilterBlockEntity;
import net.commoble.morered.transportation.OsmosisFilterBlock;
import net.commoble.morered.transportation.OsmosisSlimeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class OsmosisFilterBlockEntityRenderer extends FilterBlockEntityRenderer
{	
	public OsmosisFilterBlockEntityRenderer(BlockEntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public void render(FilterBlockEntity filter, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int intA, int intB)
	{
		super.render(filter, partialTicks, matrix, buffer, intA, intB);
		this.renderSlime(filter, partialTicks, matrix, buffer, intA, intB);
	}

	private void renderSlime(FilterBlockEntity filter, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int intA, int intB)
	{
		BlockPos blockpos = filter.getBlockPos();
		BlockState filterState = filter.getBlockState();
		Direction dir = filterState.getValue(OsmosisFilterBlock.FACING);
		BlockState renderState = MoreRed.get().osmosisSlimeBlock.get().defaultBlockState().setValue(OsmosisSlimeBlock.FACING, dir);
		long transferhash = blockpos.hashCode();
		int rate = MoreRed.SERVERCONFIG.osmosisFilterTransferRate().get();
		double ticks = (double)filter.getLevel().getGameTime() + (double)transferhash + (double)partialTicks; // casting to doubles fixes a weird rounding error that was causing choppy animation
		double minScale = 0.25D;
		double lengthScale = minScale + (filter.getBlockState().getValue(OsmosisFilterBlock.TRANSFERRING_ITEMS)
			? (-Math.cos(2 * Math.PI * ticks / rate) + 1D) * 0.25D
			: 0D);
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
		
		matrix.pushPose();
		matrix.translate(translateX, translateY, translateZ);
		matrix.scale(scaleX, scaleY, scaleZ);
		
		Minecraft mc = Minecraft.getInstance();
		var blockRenderer = mc.getBlockRenderer();
		RenderType bufferType = Sheets.translucentItemSheet();
		RenderType renderType = RenderType.translucent();
		BakedModel blockModel = blockRenderer.getBlockModel(renderState);
		blockRenderer.getModelRenderer().tesselateWithAO(
			filter.getLevel(),
			blockModel,
			renderState,
			blockpos,
			matrix,
			Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(bufferType),
			false,
			RandomSource.create(),
			blockpos.asLong(),
			OverlayTexture.NO_OVERLAY,
			ModelData.EMPTY,
			renderType);
		matrix.popPose();
	}
}
