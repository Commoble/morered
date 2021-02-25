package commoble.morered.api.internal;

import net.minecraft.block.Block;
import net.minecraft.util.math.shapes.VoxelShape;

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
			Block.makeCuboidShape(minWidth, min, minWidth, maxWidth, minPlusHeight, maxWidth),
			Block.makeCuboidShape(minWidth, maxMinusHeight, minWidth, maxWidth, max, maxWidth),
			Block.makeCuboidShape(minWidth, minWidth, min, maxWidth, maxWidth, minPlusHeight),
			Block.makeCuboidShape(minWidth, minWidth, maxMinusHeight, maxWidth, maxWidth, max),
			Block.makeCuboidShape(min, minWidth, minWidth, minPlusHeight, maxWidth, maxWidth),
			Block.makeCuboidShape(maxMinusHeight, minWidth, minWidth, max, maxWidth, maxWidth)
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
			Block.makeCuboidShape(min,min,min,max,minPlusHeight,max),
			Block.makeCuboidShape(min,maxMinusHeight,min,max,max,max),
			Block.makeCuboidShape(min,min,min,max,max,minPlusHeight),
			Block.makeCuboidShape(min,min,maxMinusHeight,max,max,max),
			Block.makeCuboidShape(min,min,min,minPlusHeight,max,max),
			Block.makeCuboidShape(maxMinusHeight,min,min,max,max,max)
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
			Block.makeCuboidShape(minWidth, min, min, maxWidth, minPlusHeight, minWidth), // down-north
			Block.makeCuboidShape(minWidth, min, maxWidth, maxWidth, minPlusHeight, max), // down-south
			Block.makeCuboidShape(min, min, minWidth, minWidth, minPlusHeight, maxWidth), // down-west
			Block.makeCuboidShape(maxWidth, min, minWidth, max, minPlusHeight, maxWidth), // down-east
			Block.makeCuboidShape(minWidth, maxMinusHeight, min, maxWidth, max, minWidth), // up-north
			Block.makeCuboidShape(minWidth, maxMinusHeight, maxWidth, maxWidth, max, max), // up-south
			Block.makeCuboidShape(min, maxMinusHeight, minWidth, minWidth, max, maxWidth), // up-west
			Block.makeCuboidShape(maxWidth, maxMinusHeight, minWidth, max, max, maxWidth), // up-east
			Block.makeCuboidShape(minWidth, min, min, maxWidth, minWidth, minPlusHeight), // north-down
			Block.makeCuboidShape(minWidth, maxWidth, min, maxWidth, max, minPlusHeight), // north-up
			Block.makeCuboidShape(min, minWidth, min, minWidth, maxWidth, minPlusHeight), //north-west
			Block.makeCuboidShape(maxWidth, minWidth, min, max, maxWidth, minPlusHeight), // north-east
			Block.makeCuboidShape(minWidth, min, maxMinusHeight, maxWidth, minWidth, max), // south-down
			Block.makeCuboidShape(minWidth, maxWidth, maxMinusHeight, maxWidth, max, max), // south-up
			Block.makeCuboidShape(min, minWidth, maxMinusHeight, minWidth, maxWidth, max), // south-west
			Block.makeCuboidShape(maxWidth, minWidth, maxMinusHeight, max, maxWidth, max), // south-east
			Block.makeCuboidShape(min, min, minWidth, minPlusHeight, minWidth, maxWidth), // west-down
			Block.makeCuboidShape(min, maxWidth, minWidth, minPlusHeight, max, maxWidth), // west-up
			Block.makeCuboidShape(min, minWidth, min, minPlusHeight, maxWidth, minWidth), // west-north
			Block.makeCuboidShape(min, minWidth, maxWidth, minPlusHeight, maxWidth, max), // west-south
			Block.makeCuboidShape(maxMinusHeight, min, minWidth, max, minWidth, maxWidth), // east-down
			Block.makeCuboidShape(maxMinusHeight, maxWidth, minWidth, max, max, maxWidth), // east-up
			Block.makeCuboidShape(maxMinusHeight, minWidth, min, max, maxWidth, minWidth), // east-north
			Block.makeCuboidShape(maxMinusHeight, minWidth, maxWidth, max, maxWidth, max) // east-south
		};
		
		return result;
	}

}
