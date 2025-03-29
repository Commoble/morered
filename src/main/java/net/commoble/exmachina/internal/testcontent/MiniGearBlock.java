package net.commoble.exmachina.internal.testcontent;

import java.util.EnumMap;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MiniGearBlock extends Block implements EntityBlock
{

	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final EnumMap<Direction,VoxelShape> SHAPES = Util.makeEnumMap(Direction.class, dir -> {
		double minX = dir == Direction.EAST ? 15D : 0D;
		double maxX = dir == Direction.WEST ? 1D : 16D;
		double minY = dir == Direction.UP ? 15D : 0D;
		double maxY = dir == Direction.DOWN ? 1D : 16D;
		double minZ = dir == Direction.SOUTH ? 15D : 0D;
		double maxZ = dir == Direction.NORTH ? 1D : 16D;
		return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
	});

	public MiniGearBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return TestContent.MINIGEAR_BLOCKENTITY.get().create(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@Deprecated
	protected BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
