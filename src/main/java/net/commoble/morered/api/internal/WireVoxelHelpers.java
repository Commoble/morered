package net.commoble.morered.api.internal;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WireVoxelHelpers
{
	/**
	 * Creates and returns an array of six voxelshapes for the wire nodes in dunswe order
	 * @param xzRadius The radius of the node shape on the axes parallel to the attachment face
	 * @param height The height of the node shape perpendicular to the attachment face
	 * @return An array of six voxelshapes in dunswe order, where the ordinal of an attachment face's direction is the respective index
	 */
	public static VoxelShape[] makeNodeShapes(int xzRadius, int height)
	{
		int min = 0;
		int max = 16;
		int minPlusHeight = min + height;
		int maxMinusHeight = max - height;
		int minWidth = 8 - xzRadius;
		int maxWidth = 8 + xzRadius;
		return new VoxelShape[]
		{
			Block.box(minWidth, min, minWidth, maxWidth, minPlusHeight, maxWidth),
			Block.box(minWidth, maxMinusHeight, minWidth, maxWidth, max, maxWidth),
			Block.box(minWidth, minWidth, min, maxWidth, maxWidth, minPlusHeight),
			Block.box(minWidth, minWidth, maxMinusHeight, maxWidth, maxWidth, max),
			Block.box(min, minWidth, minWidth, minPlusHeight, maxWidth, maxWidth),
			Block.box(maxMinusHeight, minWidth, minWidth, max, maxWidth, maxWidth)
		};
	}

	public static VoxelShape[] makeRaytraceBackboards(int height)
	{
		int min = 0;
		int max = 16;
		int minPlusHeight = min + height;
		int maxMinusHeight = max - height;
		return new VoxelShape[]
		{
			Block.box(min,min,min,max,minPlusHeight,max),
			Block.box(min,maxMinusHeight,min,max,max,max),
			Block.box(min,min,min,max,max,minPlusHeight),
			Block.box(min,min,maxMinusHeight,max,max,max),
			Block.box(min,min,min,minPlusHeight,max,max),
			Block.box(maxMinusHeight,min,min,max,max,max)
		};
	}

	public static VoxelShape[] makeLineShapes(int radius, int height)
	{
		double min = 0;
		double max = 16;
		double minPlusHeight = min + height;
		double maxMinusHeight = max - height;
		double minWidth = 8 - radius;
		double maxWidth = 8 + radius;
		
		VoxelShape[] result =
		{
			Block.box(minWidth, min, min, maxWidth, minPlusHeight, minWidth), // down-north
			Block.box(minWidth, min, maxWidth, maxWidth, minPlusHeight, max), // down-south
			Block.box(min, min, minWidth, minWidth, minPlusHeight, maxWidth), // down-west
			Block.box(maxWidth, min, minWidth, max, minPlusHeight, maxWidth), // down-east
			Block.box(minWidth, maxMinusHeight, min, maxWidth, max, minWidth), // up-north
			Block.box(minWidth, maxMinusHeight, maxWidth, maxWidth, max, max), // up-south
			Block.box(min, maxMinusHeight, minWidth, minWidth, max, maxWidth), // up-west
			Block.box(maxWidth, maxMinusHeight, minWidth, max, max, maxWidth), // up-east
			Block.box(minWidth, min, min, maxWidth, minWidth, minPlusHeight), // north-down
			Block.box(minWidth, maxWidth, min, maxWidth, max, minPlusHeight), // north-up
			Block.box(min, minWidth, min, minWidth, maxWidth, minPlusHeight), //north-west
			Block.box(maxWidth, minWidth, min, max, maxWidth, minPlusHeight), // north-east
			Block.box(minWidth, min, maxMinusHeight, maxWidth, minWidth, max), // south-down
			Block.box(minWidth, maxWidth, maxMinusHeight, maxWidth, max, max), // south-up
			Block.box(min, minWidth, maxMinusHeight, minWidth, maxWidth, max), // south-west
			Block.box(maxWidth, minWidth, maxMinusHeight, max, maxWidth, max), // south-east
			Block.box(min, min, minWidth, minPlusHeight, minWidth, maxWidth), // west-down
			Block.box(min, maxWidth, minWidth, minPlusHeight, max, maxWidth), // west-up
			Block.box(min, minWidth, min, minPlusHeight, maxWidth, minWidth), // west-north
			Block.box(min, minWidth, maxWidth, minPlusHeight, maxWidth, max), // west-south
			Block.box(maxMinusHeight, min, minWidth, max, minWidth, maxWidth), // east-down
			Block.box(maxMinusHeight, maxWidth, minWidth, max, max, maxWidth), // east-up
			Block.box(maxMinusHeight, minWidth, min, max, maxWidth, minWidth), // east-north
			Block.box(maxMinusHeight, minWidth, maxWidth, max, maxWidth, max) // east-south
		};
		
		return result;
	}

}
