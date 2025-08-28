package net.commoble.morered.mechanisms;

import java.util.Map;

import javax.annotation.Nullable;

import net.commoble.morered.MoreRed;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GearBlock extends Block implements EntityBlock, SimpleWaterloggedBlock
{
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
	public static final Map<Direction,VoxelShape> SHAPES = Util.makeEnumMap(Direction.class, dir -> {
		double minX = dir == Direction.EAST ? 14D : 0D;
		double maxX = dir == Direction.WEST ? 2D : 16D;
		double minY = dir == Direction.UP ? 14D : 0D;
		double maxY = dir == Direction.DOWN ? 2D : 16D;
		double minZ = dir == Direction.SOUTH ? 14D : 0D;
		double maxZ = dir == Direction.NORTH ? 2D : 16D;
		return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
	});
	
	public GearBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(WATERLOGGED, false));
	}
	
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.GEAR_BLOCK_ENTITY.get().create(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES.get(state.getValue(FACING));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return super.getStateForPlacement(context)
			.setValue(FACING, context.getClickedFace().getOpposite())
			.setValue(WATERLOGGED,
				Boolean.valueOf(context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER));
	}

	@Override
	protected BlockState updateShape(BlockState thisState, LevelReader level, ScheduledTickAccess ticks, BlockPos thisPos, Direction directionToNeighbor, BlockPos neighborPos,
		BlockState neighborState, RandomSource rand)
	{
		if (thisState.getValue(WATERLOGGED))
		{
			ticks.scheduleTick(thisPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}

		return super.updateShape(thisState, level, ticks, thisPos, directionToNeighbor, neighborPos, neighborState, rand);
	}
	
	@Override
	protected FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@Deprecated
	protected BlockState mirror(BlockState state, Mirror rotation)
	{
		return state.rotate(rotation.getRotation(state.getValue(FACING)));
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
	{
		return state.getValue(WATERLOGGED) || !state.ignitedByLava()
			? 0
			: 5; // same as stripped logs
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
	{
		return state.getValue(WATERLOGGED) || !state.ignitedByLava()
			? 0
			: 5; // same as stripped logs
	}
}
