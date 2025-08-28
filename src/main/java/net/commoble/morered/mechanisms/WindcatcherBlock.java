package net.commoble.morered.mechanisms;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WindcatcherBlock extends Block implements EntityBlock
{
	public static final IntegerProperty WIND = IntegerProperty.create("wind", 0, 8);
	
	public WindcatcherBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(WIND, 0));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(WIND);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.WINDCATCHER_BLOCK_ENTITY.get().create(pos,state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		Level level = context.getLevel();
		BlockState state = this.defaultBlockState();
		// state desync doesn't matter, the block has no clientside visuals dependent on the placed state
		return level.isClientSide ? state : state.setValue(WIND, getWind(context.getLevel(), context.getClickedPos()));
	}
	
	@Override
	protected BlockState updateShape(
		BlockState thisState,
		LevelReader level,
		ScheduledTickAccess ticks,
		BlockPos thisPos,
		Direction directionToNeighbor,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random)
	{
		return thisState.setValue(WIND, getWind(level,thisPos));
	}

	
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		super.randomTick(state, level, pos, random);
		BlockState updatedState = state.setValue(WIND, getWind(level,pos));
		if (updatedState != state)
		{
			level.setBlock(pos, updatedState, UPDATE_ALL);
		}
	}

	@SuppressWarnings("deprecation")
	public static int getWind(LevelReader level, BlockPos pos)
	{
		int wind = Wind.getWind(level, pos);
		if (wind <= 0)
			return wind;
		for (int radius=2; radius<=5; radius++)
		{
			int freebies = radius-1;
			float increment = Mth.PI/radius;
			int increments = radius*2;
			for (int i=0; i<increments; i++)
			{
				int x = (int) (radius * Mth.sin(i*increment));
				int z = (int) (radius * Mth.cos(i*increment));
				BlockState occludingState = level.getBlockState(pos.offset(x,0,z));
				// subtract a level if we find too many occlusions in a row
				if (occludingState.isSolid())
				{
					if (freebies > 0)
						freebies--;
					else if (wind > 0)
						wind--;
					
					if (wind <= 0)
						return 0;
				}
			}
		}
		return wind;
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.empty();
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos)
	{
		return 1.0F;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state)
	{
		return true;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
	{
		return !state.ignitedByLava()
			? 0
			: 5; // same as stripped logs
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
	{
		return !state.ignitedByLava()
			? 0
			: 5; // same as stripped logs
	}
}
