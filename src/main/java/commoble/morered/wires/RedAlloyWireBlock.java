package commoble.morered.wires;

import com.google.common.cache.LoadingCache;

import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.internal.WireVoxelHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

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
	protected boolean canAdjacentBlockConnectToFace(BlockGetter world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		return MoreRedAPI.getWireConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultWireConnector())
			.canConnectToAdjacentWire(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire);
	}
}
