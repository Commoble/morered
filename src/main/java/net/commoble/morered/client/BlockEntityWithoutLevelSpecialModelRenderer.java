package net.commoble.morered.client;

import java.util.function.Consumer;

import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public record UnbakedBlockEntityWithoutLevelRenderer(BlockState blockState, BlockEntityType<?> blockEntityType) implements NoDataSpecialModelRenderer.Unbaked
{
	// block specialrenderers don't actually use the codec
	public static final MapCodec<UnbakedBlockEntityWithoutLevelRenderer> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
		BlockState.CODEC.fieldOf("block_state").forGetter(UnbakedBlockEntityWithoutLevelRenderer::blockState),
		BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec().fieldOf("block_entity_type").forGetter(UnbakedBlockEntityWithoutLevelRenderer::blockEntityType)
	).apply(builder, UnbakedBlockEntityWithoutLevelRenderer::new));

	@Override
	public SpecialModelRenderer<@Nullable Void> bake(BakingContext context)
	{
		return new ItemRenderingSpecialModelRenderer(this.blockEntityType.create(BlockPos.ZERO, this.blockState));
	}

	@Override
	public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type()
	{
		return CODEC;
	}

	public static record ItemRenderingSpecialModelRenderer(BlockEntity blockEntity) implements NoDataSpecialModelRenderer
	{

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int overlayCoords, boolean hasFoil, int outlineColor)
		{
			renderBlockEntity(this.blockEntity, poseStack, collector, Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState, packedLight);
		}
		
		private static <T extends BlockEntity> void renderBlockEntity(T be, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, int lightCoords)
		{
			renderBlockEntity(be, Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be), poseStack, collector, cameraRenderState, lightCoords);
		}
		
		private static <T extends BlockEntity, S extends BlockEntityRenderState> void renderBlockEntity(T be, BlockEntityRenderer<T,S> renderer, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, int lightCoords)
		{
			Minecraft mc = Minecraft.getInstance();
			float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
	        S renderState = renderer.createRenderState();
	        renderer.extractRenderState(be, renderState, partialTicks, cameraRenderState.pos, null);
	        renderState.lightCoords = lightCoords;
	        renderer.submit(renderState, poseStack, collector, cameraRenderState);
		}

		@Override
		public Void extractArgument(ItemStack stack)
		{
			// this isn't used when SpecialModelRenderers are used to render blocks
			// instead, null is passed as the T for the first argument in render
			return null;
		}

		@Override
		public void getExtents(Consumer<Vector3fc> points)
		{
			// *pretty* sure this isn't used when rendering blocks
		}
		
	}
}
