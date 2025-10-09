package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.commoble.morered.mechanisms.WindcatcherColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record WindcatcherBlockEntityRenderer(ItemModelResolver resolver, Map<WindcatcherKey, ItemStack> stackCache) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{
	static record WindcatcherKey(Block block, WindcatcherColors colors) {}
	
	public static WindcatcherBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new WindcatcherBlockEntityRenderer(context.itemModelResolver(), new HashMap<>());
	}

	@Override
	public AABB getRenderBoundingBox(GenericBlockEntity blockEntity)
	{
		return new AABB(blockEntity.getBlockPos()).inflate(1D,0D,1D);
	}

	@Override
	public boolean shouldRenderOffScreen()
	{
		return true;
	}

	@Override
	public MechanicalItemBlockEntityRenderState createRenderState()
	{
		return new MechanicalItemBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(GenericBlockEntity be, MechanicalItemBlockEntityRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(be, renderState, partialTicks, camera, overlay);
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		MechanicalState mechanicalState = states.getOrDefault(NodeShape.ofCube(), MechanicalState.ZERO);
		float radiansPerSecond = (float)mechanicalState.angularVelocity();
		var colorsComponent = MoreRed.WINDCATCHER_COLORS_DATA_COMPONENT.get();
		WindcatcherColors colors = Objects.requireNonNullElse(be.get(colorsComponent), WindcatcherColors.DEFAULT);
		ItemStack stack = this.stackCache.computeIfAbsent(new WindcatcherKey(be.getBlockState().getBlock(), colors), key -> {
			ItemStack newStack = new ItemStack(key.block);
			newStack.set(colorsComponent, key.colors);
			return newStack;
		});
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = radiansPerSecond * seconds;
		renderState.update(resolver, level, stack, radians);
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube		
		poseStack.mulPose(Axis.YP.rotation(renderState.radians));
		renderState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}
}
