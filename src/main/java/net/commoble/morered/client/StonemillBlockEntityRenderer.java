package net.commoble.morered.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.commoble.exmachina.api.MechanicalNodeStates;
import net.commoble.exmachina.api.MechanicalState;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.morered.GenericBlockEntity;
import net.commoble.morered.MoreRed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record StonemillBlockEntityRenderer(ItemRenderer itemRenderer, ItemStack axle) implements BlockEntityRenderer<GenericBlockEntity>
{
	public static StonemillBlockEntityRenderer create(BlockEntityRendererProvider.Context context)
	{
		ResourceLocation blockId = MoreRed.STONEMILL_BLOCK.getId();
		ResourceLocation axleModel = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), blockId.getPath() + "_axle");
		ItemStack axle = new ItemStack(Items.STICK);
		axle.set(DataComponents.ITEM_MODEL, axleModel);
		return new StonemillBlockEntityRenderer(context.getItemRenderer(), axle);
	}
	
	@Override
	public void render(GenericBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int overlay, Vec3 Camera)
	{
		Map<NodeShape, MechanicalState> states = be.getData(MechanicalNodeStates.HOLDER.get());
		float axleRadiansPerSecond = (float) states.getOrDefault(NodeShape.ofSide(Direction.UP), MechanicalState.ZERO).angularVelocity();
		@SuppressWarnings("resource")
		Level level = Minecraft.getInstance().level;
		int gameTimeTicks = MechanicalState.getMachineTicks(level);
		float seconds = (gameTimeTicks + partialTicks) * 0.05F; // in seconds
		float axleRadians = axleRadiansPerSecond * seconds;
		
		poseStack.pushPose();
		poseStack.translate(0.5D,0.5D,0.5D); // item renderer renders the center of the item model at 0,0,0 in the cube
		Axis axleAxis = Axis.YP;
		
		// render the axles
		poseStack.pushPose();
		poseStack.mulPose(axleAxis.rotation(axleRadians));
		this.itemRenderer.renderStatic(this.axle, ItemDisplayContext.NONE, packedLight, overlay, poseStack, bufferSource, level, 0);
		poseStack.popPose();
		
		poseStack.popPose();
	}

}
