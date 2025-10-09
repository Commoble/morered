package net.commoble.morered.client;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.client.FilterBlockEntityRenderer.FilterBlockEntityRenderState;
import net.commoble.morered.transportation.FilterBlock;
import net.commoble.morered.transportation.FilterBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class FilterBlockEntityRenderer<S extends FilterBlockEntityRenderState> implements BlockEntityRenderer<FilterBlockEntity, S>
{
	protected ItemModelResolver resolver;
	protected Supplier<S> stateFactory;
	
	public static class FilterBlockEntityRenderState extends BlockEntityRenderState
	{
		public final ItemStackRenderState itemState = new ItemStackRenderState();
		public Direction facing = Direction.NORTH;
	}
	
	public static FilterBlockEntityRenderer<FilterBlockEntityRenderState> createFilterRenderer(BlockEntityRendererProvider.Context context)
	{
		return new FilterBlockEntityRenderer<>(context, FilterBlockEntityRenderState::new);
	}
	
	public FilterBlockEntityRenderer (BlockEntityRendererProvider.Context context, Supplier<S> stateFactory)
	{
		this.resolver = context.itemModelResolver();
		this.stateFactory = stateFactory;
	}

	@Override
	public S createRenderState()
	{
		return this.stateFactory.get();
	}

	@Override
	public void extractRenderState(FilterBlockEntity filter, S renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(filter, renderState, partialTicks, camera, overlay);
		this.resolver.updateForTopItem(renderState.itemState, filter.filterStack, ItemDisplayContext.FIXED, filter.getLevel(), null, (int)filter.getBlockPos().asLong());
		renderState.facing = filter.getBlockState().getValue(FilterBlock.FACING);
	}

	@Override
	public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		if (!renderState.itemState.isEmpty())
		{
			poseStack.pushPose();

			poseStack.translate(0.501D, 0.502D, 0.503D);
			poseStack.scale(0.9F, 0.9F, 0.9F);
			if (renderState.facing.getAxis() == Axis.X)
			{
				poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90F));	// rotate 90 degrees about y-axis
			}
			
			renderState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);

			poseStack.popPose();
		}
	}
}
