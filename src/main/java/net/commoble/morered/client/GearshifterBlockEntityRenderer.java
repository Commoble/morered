package net.commoble.morered.client;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.client.GearshifterBlockEntityRenderer.GearshifterRenderState;
import net.commoble.morered.mechanisms.GearshifterBlock;
import net.commoble.morered.plate_blocks.PlateBlockStateProperties;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record GearshifterBlockEntityRenderer(ItemModelResolver resolver, Map<Block, GearshifterModels> modelCache) implements BlockEntityRenderer<GenericBlockEntity, GearshifterRenderState>
{
	public static GearshifterBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		return new GearshifterBlockEntityRenderer(context.itemModelResolver(), new HashMap<>());
	}

	private static record GearshifterModels(ItemStack gear, ItemStack axle)
	{
		public static GearshifterModels of(Block block)
		{
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
			Identifier gearModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_gear");
			Identifier axleModel = Identifier.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
			ItemStack gear = new ItemStack(Items.STICK);
			gear.set(DataComponents.ITEM_MODEL, gearModel);
			ItemStack axle = new ItemStack(Items.STICK);
			axle.set(DataComponents.ITEM_MODEL, axleModel);
			return new GearshifterModels(gear,axle);
		}
	}
	public static class GearshifterRenderState extends BlockEntityRenderState
	{
		public final ItemStackRenderState gearState = new ItemStackRenderState();
		public final ItemStackRenderState axleState = new ItemStackRenderState();
		public float gearRadians = 0F;
		public float axleRadians = 0F;
	}

	@Override
	public GearshifterRenderState createRenderState()
	{
		return new GearshifterRenderState();
	}

	@Override
	public void extractRenderState(GenericBlockEntity be, GearshifterRenderState renderState, float partialTicks, Vec3 camera, CrumblingOverlay overlay)
	{
		BlockEntityRenderer.super.extractRenderState(be, renderState, partialTicks, camera, overlay);
		int seed = (int)(be.getBlockPos().asLong());
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		BlockState state = be.getBlockState();
		Direction bigDir = state.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		Direction smallDir = PlateBlockStateProperties.getOutputDirection(state);
		float bigRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(bigDir), MechanicalState.ZERO).angularVelocity();
		float smallRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(smallDir), MechanicalState.ZERO).angularVelocity();
		GearshifterModels models = this.modelCache.computeIfAbsent(state.getBlock(), GearshifterModels::of);
		Level level = be.getLevel();
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		renderState.gearRadians = bigRadiansPerSecond * seconds;
		renderState.axleRadians = smallRadiansPerSecond * seconds;
		this.resolver.updateForTopItem(renderState.gearState, models.gear, ItemDisplayContext.NONE, be.getLevel(), null, seed);
		this.resolver.updateForTopItem(renderState.axleState, models.axle, ItemDisplayContext.NONE, be.getLevel(), null, seed);
	}

	@Override
	public void submit(GearshifterRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
	{
		Direction bigDir = renderState.blockState.getValue(GearshifterBlock.ATTACHMENT_DIRECTION);
		Direction smallDir = PlateBlockStateProperties.getOutputDirection(renderState.blockState);
		int faceRotations = renderState.blockState.getValue(GearshifterBlock.ROTATION);
		
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		RenderHelper.rotateTwentyFourBlockPoseStack(poseStack, bigDir, faceRotations);
		// render the big gear
		// for whatever reason, if we point in a positive direction, we have to spin backward
		Axis bigAxis = bigDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.YN : Axis.YP;
		Axis smallAxis = smallDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Axis.ZN : Axis.ZP;
		poseStack.pushPose();
		poseStack.mulPose(bigAxis.rotation(renderState.gearRadians));
		renderState.gearState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		
		// render the small gear + axle
		poseStack.pushPose();
		poseStack.mulPose(smallAxis.rotation(renderState.axleRadians));
		renderState.axleState.submit(poseStack, collector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
		
		poseStack.popPose();
	}
}
