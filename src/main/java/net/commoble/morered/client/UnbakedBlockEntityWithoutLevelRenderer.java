package net.commoble.morered.client;

import java.util.Set;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

public record UnbakedBlockEntityWithoutLevelRenderer(Block block, BlockEntityType<?> blockEntityType) implements SpecialModelRenderer.Unbaked
{
	// block specialrenderers don't actually use the codec
	public static final MapCodec<UnbakedBlockEntityWithoutLevelRenderer> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
		BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(UnbakedBlockEntityWithoutLevelRenderer::block),
		BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec().fieldOf("block_entity_type").forGetter(UnbakedBlockEntityWithoutLevelRenderer::blockEntityType)
	).apply(builder, UnbakedBlockEntityWithoutLevelRenderer::new));

	@Override
	public SpecialModelRenderer<?> bake(EntityModelSet entityModelSet)
	{
		return new ItemRenderingSpecialModelRenderer(this.blockEntityType.create(BlockPos.ZERO, this.block.defaultBlockState()));
	}

	@Override
	public MapCodec<? extends Unbaked> type()
	{
		return CODEC;
	}

	public static record ItemRenderingSpecialModelRenderer(BlockEntity blockEntity) implements SpecialModelRenderer<Void>
	{

		@Override
		public void render(Void no, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, int overlay, boolean foil)
		{
			Minecraft mc = Minecraft.getInstance();
			float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
			Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
			// bypass the dispatcher a bit so we can skip a blockentity-has-level check, which lets us use the provided lightmap
	        getRenderer(this.blockEntity, mc.getBlockEntityRenderDispatcher()).render(this.blockEntity, partialTicks, poseStack, multiBufferSource, packedLight, overlay, camera);
		}
		
		private static <T extends BlockEntity> BlockEntityRenderer<T> getRenderer(T be, BlockEntityRenderDispatcher dispatcher)
		{
			return dispatcher.getRenderer(be);
		}

		@Override
		public Void extractArgument(ItemStack stack)
		{
			// this isn't used when SpecialModelRenderers are used to render blocks
			// instead, null is passed as the T for the first argument in render
			return null;
		}

		@Override
		public void getExtents(Set<Vector3f> points)
		{
			// *pretty* sure this isn't used when rendering blocks
		}
		
	}
}
