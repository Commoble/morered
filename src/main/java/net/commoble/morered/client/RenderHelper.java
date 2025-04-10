package net.commoble.morered.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

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

}
