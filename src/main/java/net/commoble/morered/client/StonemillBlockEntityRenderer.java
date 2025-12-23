package net.commoble.morered.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record StonemillBlockEntityRenderer(ItemModelResolver resolver, ItemStack axle) implements BlockEntityRenderer<GenericBlockEntity, MechanicalItemBlockEntityRenderState>
{
	public static StonemillBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		Identifier blockId = MoreRed.STONEMILL_BLOCK.getId();
		Identifier axleModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
		ItemStack axle = new ItemStack(Items.STICK);
		axle.set(DataComponents.ITEM_MODEL, axleModel);
		return new StonemillBlockEntityRenderer(context.itemModelResolver(), axle);
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
		float axleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(Direction.UP), MechanicalState.ZERO).angularVelocity();
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float radians = axleRadiansPerSecond * seconds;
		renderState.update(resolver, level, axle, radians);
	}

	@Override
	public void submit(MechanicalItemBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		Axis axleAxis = Axis.YP;
		
		// render the axles
		poseStack.pushPose();
		poseStack.mulPose(axleAxis.rotation(renderState.radians));
		renderState.itemState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		
		poseStack.popPose();
	}
}
