package net.commoble.morered.mechanisms;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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
		return MoreRed.get().windcatcherBlockEntity.get().create(pos,state);
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
		int wind = getHeightWind(level,pos);
		if (wind <= 0)
			return wind;
		for (int radius=2; radius<=5; radius++)
		{
			int blockedCount = 0;
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
					blockedCount++;
					if (blockedCount >= radius)
					{
						wind--;
						if (wind <= 0)
							return 0;
					}
				}
				else
				{
					blockedCount = 0;
				}
			}
		}
		return wind;
	}
	
	public static int getHeightWind(LevelReader level, BlockPos pos)
	{
		if (level.dimensionType().hasCeiling())
		{
			return 0;
		}
		// lerp windiness from 25% to 40% of world depth (configurable)
		// TODO add xz noise?
		int y = pos.getY();
		int minY = level.getMinY();
		int maxY = level.getMaxY();
		double yDepthPerc = Mth.inverseLerp(y, minY, maxY);
		double minDepthPerc = MoreRed.SERVERCONFIG.minWindDepth().getAsDouble();
		double maxDepthPerc = MoreRed.SERVERCONFIG.maxWindDepth().getAsDouble();
		if (yDepthPerc <= minDepthPerc)
		{
			return 0;
		}
		if (yDepthPerc >= maxDepthPerc)
		{
			return 8;
		}
		double relerpedPerc = Mth.inverseLerp(yDepthPerc, minDepthPerc, maxDepthPerc);
		return Mth.lerpInt((float)relerpedPerc, 0, 8); 
	}
}
