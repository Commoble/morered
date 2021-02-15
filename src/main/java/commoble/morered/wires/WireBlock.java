package commoble.morered.wires;

import com.google.common.cache.LoadingCache;

import net.minecraft.block.Block;
import net.minecraft.util.Util;
import net.minecraft.util.math.shapes.VoxelShape;

public class WireBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE =
	{
		Block.makeCuboidShape(7, 0 ,7, 9, 2, 9),
		Block.makeCuboidShape(7, 14, 7, 9, 16, 9),
		Block.makeCuboidShape(7, 7, 0, 9, 9, 2),
		Block.makeCuboidShape(7, 7, 14, 9, 9, 16),
		Block.makeCuboidShape(0, 7, 7, 2, 9, 9),
		Block.makeCuboidShape(14, 7, 7, 16, 9, 9)
	};
	
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = 
	{
		Block.makeCuboidShape(0,0,0,16,2,16),
		Block.makeCuboidShape(0,14,0,16,16,16),
		Block.makeCuboidShape(0,0,0,16,16,2),
		Block.makeCuboidShape(0,0,14,16,16,16),
		Block.makeCuboidShape(0,0,0,2,16,16),
		Block.makeCuboidShape(14,0,0,16,16,16)
	};
	
	public static final VoxelShape[] LINE_SHAPES = Util.make(() ->
	{		
		double min = 0;
		double max = 16;
		double minPlus = 2;
		double maxMinus = 14;
		double innerMin = 7;
		double innerMax = 9;
		
		VoxelShape[] result =
		{
			Block.makeCuboidShape(innerMin, min, min, innerMax, minPlus, innerMin), // down-north
			Block.makeCuboidShape(innerMin, min, innerMax, innerMax, minPlus, max), // down-south
			Block.makeCuboidShape(min, min, innerMin, innerMin, minPlus, innerMax), // down-west
			Block.makeCuboidShape(innerMax, min, innerMin, max, minPlus, innerMax), // down-east
			Block.makeCuboidShape(innerMin, maxMinus, min, innerMax, max, innerMin), // up-north
			Block.makeCuboidShape(innerMin, maxMinus, innerMax, innerMax, max, max), // up-south
			Block.makeCuboidShape(min, maxMinus, innerMin, innerMin, max, innerMax), // up-west
			Block.makeCuboidShape(innerMax, maxMinus, innerMin, max, max, innerMax), // up-east
			Block.makeCuboidShape(innerMin, min, min, innerMax, innerMin, minPlus), // north-down
			Block.makeCuboidShape(innerMin, innerMax, min, innerMax, max, minPlus), // north-up
			Block.makeCuboidShape(min, innerMin, min, innerMin, innerMax, minPlus), //north-west
			Block.makeCuboidShape(innerMax, innerMin, min, max, innerMax, minPlus), // north-east
			Block.makeCuboidShape(innerMin, min, maxMinus, innerMax, innerMin, max), // south-down
			Block.makeCuboidShape(innerMin, innerMax, maxMinus, innerMax, max, max), // south-up
			Block.makeCuboidShape(min, innerMin, maxMinus, innerMin, innerMax, max), // south-west
			Block.makeCuboidShape(innerMax, innerMin, maxMinus, max, innerMax, max), // south-east
			Block.makeCuboidShape(min, min, innerMin, minPlus, innerMin, innerMax), // west-down
			Block.makeCuboidShape(min, innerMax, innerMin, minPlus, max, innerMax), // west-up
			Block.makeCuboidShape(min, innerMin, min, minPlus, innerMax, innerMin), // west-north
			Block.makeCuboidShape(min, innerMin, innerMax, minPlus, innerMax, max), // west-south
			Block.makeCuboidShape(maxMinus, min, innerMin, max, innerMin, innerMax), // east-down
			Block.makeCuboidShape(maxMinus, innerMax, innerMin, max, max, innerMax), // east-up
			Block.makeCuboidShape(maxMinus, innerMin, min, max, innerMax, innerMin), // east-north
			Block.makeCuboidShape(maxMinus, innerMin, innerMax, max, innerMax, max) // east-south
		};
		
		return result;
	});
	
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public WireBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, true);
	}
}
