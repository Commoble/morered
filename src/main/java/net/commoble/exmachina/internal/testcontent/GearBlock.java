package net.commoble.exmachina.internal.testcontent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GearBlock extends Block implements EntityBlock
{
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), map -> {
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    }));
	public static final EnumMap<Direction,VoxelShape> SHAPES = Util.makeEnumMap(Direction.class, dir -> {
		double minX = dir == Direction.EAST ? 15D : 0D;
		double maxX = dir == Direction.WEST ? 1D : 16D;
		double minY = dir == Direction.UP ? 15D : 0D;
		double maxY = dir == Direction.DOWN ? 1D : 16D;
		double minZ = dir == Direction.SOUTH ? 15D : 0D;
		double maxZ = dir == Direction.NORTH ? 1D : 16D;
		return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
	});
	public static final Map<Integer, VoxelShape> SHAPES_BY_BITFLAG = new HashMap<>();
	
	public static VoxelShape makeShape(int bitflag)
	{
		VoxelShape shape = Shapes.empty();
		for (int bit=0; bit<6; bit++)
		{
			if ((bitflag & (1 << bit)) > 0)
			{
				Direction dir = Direction.values()[bit];
				VoxelShape baseShape = SHAPES.get(dir);
				shape = Shapes.or(shape, baseShape);
			}
		}
		return shape;
	}

	public GearBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState()
			.setValue(DOWN, false)
			.setValue(UP, false)
			.setValue(NORTH, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false)
			.setValue(EAST, false));
	}
	
	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(DOWN,UP,NORTH,SOUTH,WEST,EAST);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return TestContent.GEAR_BLOCKENTITY.get().create(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		int bitflag = 0;
		for (int i=0; i<6; i++)
		{
			Direction dir = Direction.values()[i];
			if (state.getValue(PROPERTY_BY_DIRECTION.get(dir)))
			{
				bitflag |= (1 << i);
			}
		}
		return SHAPES_BY_BITFLAG.computeIfAbsent(bitflag, GearBlock::makeShape);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Direction facing = context.getNearestLookingDirection();
		return this.defaultBlockState().setValue(PROPERTY_BY_DIRECTION.get(facing), true);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult result)
	{
		if (stack.getItem() == TestContent.GEAR_ITEM.get())
		{
			for (Direction facing : Direction.orderedByNearest(player))
			{
				BooleanProperty prop = PROPERTY_BY_DIRECTION.get(facing);
				if (!state.getValue(prop))
				{
					level.setBlock(pos, state.setValue(prop, true), Block.UPDATE_ALL);
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.useItemOn(stack, state, level, pos, player, hand, result);
	}
	
	
}
