package net.commoble.morered.mechanisms;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// dummy blocks which surround a windcatcher
public class AirFoilBlock extends Block
{
	// 0 => north, increasing clockwise
	public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 7);
	private static final int[] SEGMENT_X = {0, 1, 1, 1, 0, -1, -1, -1};
	private static final int[] SEGMENT_Z = {-1, -1, 0, 1, 1, 1, 0, -1};

	public AirFoilBlock(Properties props)
	{
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(SEGMENT, 0));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(SEGMENT);
	}
	
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return Shapes.empty();
	}

	@Override
	protected BlockState updateShape(BlockState thisState, LevelReader level, ScheduledTickAccess ticks, BlockPos thisPos, Direction directionToNeighbor, BlockPos neighborPos,
		BlockState neighborState, RandomSource rand)
	{
		ticks.scheduleTick(thisPos, this, 1);
		return super.updateShape(thisState, level, ticks, thisPos, directionToNeighbor, neighborPos, neighborState, rand);
	}

	@Override
	protected void tick(BlockState thisState, ServerLevel level, BlockPos pos, RandomSource rand)
	{
		super.tick(thisState, level, pos, rand);
		BlockPos corePos = getCorePos(thisState.getValue(SEGMENT), pos);
		if (!level.getBlockState(corePos).is(MoreRed.Tags.Blocks.WINDCATCHERS))
		{
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState thisState, ServerLevel level, BlockPos pos, boolean moveByPiston)
	{
		super.affectNeighborsAfterRemoval(thisState,level,pos,moveByPiston);
		BlockPos corePos = getCorePos(thisState.getValue(SEGMENT), pos);
		if (level.getBlockState(corePos).is(MoreRed.Tags.Blocks.WINDCATCHERS))
		{
			level.destroyBlock(corePos, true);
		}
	}

	public static BlockPos getCorePos(int segment, BlockPos airFoilPos)
	{
		int x = SEGMENT_X[segment];
		int z = SEGMENT_Z[segment];
		return airFoilPos.offset(x,0,z);
	}
	
	public static BlockPos getAirFoilPos(int segment, BlockPos corePos)
	{
		int x = -SEGMENT_X[segment];
		int z = -SEGMENT_Z[segment];
		return corePos.offset(x,0,z);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror)
	{
		return switch (mirror)
		{
			case NONE -> state;
			
			// flip Z
			case LEFT_RIGHT -> state.setValue(SEGMENT, 7 - ((state.getValue(SEGMENT) + 3) % 8));
			
			// flip X
			case FRONT_BACK -> state.setValue(SEGMENT, 7 - ((state.getValue(SEGMENT) + 7) % 8));
		};
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rot)
	{
		return switch(rot)
		{
			case NONE -> state;
			case CLOCKWISE_90 -> state.setValue(SEGMENT, (state.getValue(SEGMENT) + 2) % 8);
			case CLOCKWISE_180 -> state.setValue(SEGMENT, (state.getValue(SEGMENT) + 4) % 8);
			case COUNTERCLOCKWISE_90 -> state.setValue(SEGMENT, (state.getValue(SEGMENT) + 6) % 8);
		};
		
	}

	@Override
	protected RenderShape getRenderShape(BlockState p_60550_)
	{
		return RenderShape.INVISIBLE;
	}
}
