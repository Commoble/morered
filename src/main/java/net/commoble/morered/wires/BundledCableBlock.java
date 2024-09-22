package net.commoble.morered.wires;

import java.util.Map;

import com.google.common.cache.LoadingCache;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.MoreRedAPI;
import net.commoble.morered.api.internal.WireVoxelHelpers;
import net.commoble.morered.future.Channel;
import net.commoble.morered.future.SignalStrength;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BundledCableBlock extends AbstractWireBlock implements EntityBlock
{
	public static final VoxelShape[] NODE_SHAPES_DUNSWE = WireVoxelHelpers.makeNodeShapes(3, 4);
	public static final VoxelShape[] RAYTRACE_BACKBOARDS = WireVoxelHelpers.makeRaytraceBackboards(4);
	public static final VoxelShape[] LINE_SHAPES = WireVoxelHelpers.makeLineShapes(3, 4);
	public static final VoxelShape[] SHAPES_BY_STATE_INDEX = AbstractWireBlock.makeVoxelShapes(NODE_SHAPES_DUNSWE, LINE_SHAPES);
	public static final LoadingCache<Long, VoxelShape> VOXEL_CACHE = AbstractWireBlock.makeVoxelCache(SHAPES_BY_STATE_INDEX, LINE_SHAPES);

	public BundledCableBlock(Properties properties)
	{
		super(properties, SHAPES_BY_STATE_INDEX, RAYTRACE_BACKBOARDS, VOXEL_CACHE, false, false, false, Channel.SIXTEEN_COLORS);
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().wireBeType.get().create(pos, state);
	}

//	@Override
//	protected boolean canAdjacentBlockConnectToFace(BlockGetter world, BlockPos thisPos, BlockState thisState, Block neighborBlock, Direction attachmentDirection, Direction directionToWire, BlockPos neighborPos, BlockState neighborState)
//	{
//		return MoreRedAPI.getCableConnectabilityRegistry().getOrDefault(neighborBlock, MoreRedAPI.getDefaultCableConnector())
//			.canConnectToAdjacentWire(world, neighborPos, neighborState, thisPos, thisState, attachmentDirection, directionToWire);
//	}

	@Override
	protected Map<Direction, SignalStrength> onReceivePower(LevelAccessor level, BlockPos pos, BlockState state, Direction attachmentSide, int power, Channel channel)
	{
		return Map.of();
	}

}
