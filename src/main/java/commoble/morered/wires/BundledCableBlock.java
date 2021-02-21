package commoble.morered.wires;

import java.util.EnumSet;

import com.google.common.cache.LoadingCache;

import commoble.morered.api.MoreRedAPI;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BundledCableBlock extends AbstractWireBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = AbstractWireBlock.makeNodeShapes(3, 4);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = AbstractWireBlock.makeRaytraceBackboards(4);
	public static final VoxelShape[] LINE_SHAPES = AbstractWireBlock.makeLineShapes(3, 4);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public BundledCableBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false);
	}

	@Override
	protected void updatePower(World world, BlockPos wirePos, BlockState wireState)
	{
	}

	@Override
	protected void notifyNeighbors(World world, BlockPos wirePos, BlockState newState, EnumSet<Direction> updateDirections, boolean doConductedPowerUpdates)
	{
	}

	@Override
	protected boolean canAdjacentBlockConnectToFace(IBlockReader world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
	{
		return MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
			.canConnectToAdjacentWire(world, thisPos, thisState, attachmentDirection, directionToWire, neighborPos, neighborState);
	}

}
