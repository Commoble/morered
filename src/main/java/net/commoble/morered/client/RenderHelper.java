package net.commoble.morered.client;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RenderHelper
{

	public static void rotateTwentyFourBlockPoseStack(PoseStack poseStack, Direction attachDir, int rotations)
	{
		// rotate the whole model based on the state
		// first, apply rotation for axis direction
		switch(attachDir)
		{
			// down = default for gearshifter, ignore
			case UP:
				poseStack.mulPose(Axis.XP.rotationDegrees(180));
				poseStack.mulPose(Axis.YN.rotationDegrees(180)); // finaglement: secondary direction is always NORTH or UP by default
				break;
			case NORTH: poseStack.mulPose(Axis.XP.rotationDegrees(90));break;
			case SOUTH:
				poseStack.mulPose(Axis.XP.rotationDegrees(270));
				poseStack.mulPose(Axis.YN.rotationDegrees(180));
				break;
			case WEST:
				poseStack.mulPose(Axis.ZN.rotationDegrees(90));
				poseStack.mulPose(Axis.YN.rotationDegrees(270));
				break;
			case EAST:
				poseStack.mulPose(Axis.ZP.rotationDegrees(90));
				poseStack.mulPose(Axis.YN.rotationDegrees(90));
				break;
			default:
		}
		// apply secondary rotation
		poseStack.mulPose(Axis.YN.rotationDegrees(90 * rotations));
	}

	/**
	 * We use itemstacks with models to easily render json models from blockentityrenderers.
	 * However, in 26.1, BERs construct before itemstacks are constructable.
	 * We could just use itemstacktemplates instead, but then we'd be allocating new itemstacks each render frame,
	 * which defeats the purpose of constructing them on BER construction in the first place.
	 * So instead we'll memoize the itemstacks.
	 * @param modelId
	 * @return Supplier of memoized itemstack
	 */
	public static Supplier<ItemStack> memoizeStackModel(Identifier modelId)
	{
		return Suppliers.memoize(() -> {
			ItemStack stack = new ItemStack(Items.STICK);
			stack.set(DataComponents.ITEM_MODEL, modelId);
			return stack;
		});
	}
}
