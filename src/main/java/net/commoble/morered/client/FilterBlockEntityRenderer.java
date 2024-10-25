package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.commoble.morered.transportation.FilterBlock;
import net.commoble.morered.transportation.FilterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FilterBlockEntityRenderer implements BlockEntityRenderer<FilterBlockEntity>
{
	public FilterBlockEntityRenderer(BlockEntityRendererProvider.Context context)
	{
	}

	@Override
	public void render(FilterBlockEntity filter, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int intA, int intB)
	{
		if (filter.filterStack.getCount() > 0)
		{
			this.renderItem(filter.getLevel(), filter.filterStack, filter.getBlockState().getValue(FilterBlock.FACING), matrix, buffer, intA, (int)filter.getBlockPos().asLong());
		}
	}

	private void renderItem(Level level, ItemStack stack, Direction facing, PoseStack matrix, MultiBufferSource buffer, int intA, int renderSeed)
	{
		ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();

		matrix.pushPose();

		matrix.translate(0.501D, 0.502D, 0.503D);
		matrix.scale(0.9F, 0.9F, 0.9F);
		if (facing.getAxis() == Axis.X)
		{
			matrix.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90F));	// rotate 90 degrees about y-axis
		}

		renderer.renderStatic(stack, ItemDisplayContext.FIXED, intA, OverlayTexture.NO_OVERLAY, matrix, buffer, level, renderSeed);
		
		

		matrix.popPose();
	}
}
