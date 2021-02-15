package commoble.morered.wires;

import com.google.common.cache.LoadingCache;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.DyeColor;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public class ColoredCableBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE =
	{
		Block.makeCuboidShape(6, 0, 6, 10, 3, 10),
		Block.makeCuboidShape(6, 13, 6, 10, 16, 10),
		Block.makeCuboidShape(6, 6, 0, 10, 10, 3),
		Block.makeCuboidShape(6, 6, 13, 10, 10, 16),
		Block.makeCuboidShape(0, 6, 6, 3, 10, 10),
		Block.makeCuboidShape(13, 6, 6, 16, 10, 10)
	};
	
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = 
	{
		Block.makeCuboidShape(0,0,0,16,3,16),
		Block.makeCuboidShape(0,13,0,16,16,16),
		Block.makeCuboidShape(0,0,0,16,16,3),
		Block.makeCuboidShape(0,0,13,16,16,16),
		Block.makeCuboidShape(0,0,0,3,16,16),
		Block.makeCuboidShape(13,0,0,16,16,16)
	};
	
	public static final VoxelShape[] LINE_SHAPES = Util.make(() ->
	{		
		double min = 0;
		double max = 16;
		double minPlus = 3;
		double maxMinus = 13;
		double innerMin = 6;
		double innerMax = 10;
		
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
	
	private final DyeColor color; public DyeColor getDyeColor() { return this.color; }

	public ColoredCableBlock(Properties properties, DyeColor color)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
		this.color = color;
	}
	
	@Override
	public boolean canConnectToAdjacentWire(IBlockReader world, BlockPos wirePos, BlockState wireState, Direction wireFace, Direction directionToWire, BlockPos neighborPos,
		BlockState neighborState)
	{
		Block wireBlock = wireState.getBlock();
		return wireBlock instanceof ColoredCableBlock && ((ColoredCableBlock)wireBlock).getDyeColor() != this.getDyeColor()
			? false
			: super.canConnectToAdjacentWire(world, wirePos, wireState, wireFace, directionToWire, neighborPos, neighborState);
	}
	
}
