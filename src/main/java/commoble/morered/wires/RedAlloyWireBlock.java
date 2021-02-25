package commoble.morered.wires;

import com.google.common.cache.LoadingCache;

import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.internal.WireVoxelHelpers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

public class RedAlloyWireBlock extends PoweredWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = WireVoxelHelpers.makeNodeShapes(1,2);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = WireVoxelHelpers.makeRaytraceBackboards(2);
	public static final VoxelShape[] LINE_SHAPES = WireVoxelHelpers.makeLineShapes(1,2);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public RedAlloyWireBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, true);
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(IBlockReader world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		return MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultWireConnector())
			.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState);
	}
}
