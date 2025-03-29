package net.commoble.morered.mechanisms;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
		if (level.dimensionType().hasCeiling())
		{
			return this.defaultBlockState();
		}
		// lerp windiness from 25% to 40% of world depth (configurable)
		// TODO add xz noise?
		int y = context.getClickedPos().getY();
		int minY = level.getMinY();
		int maxY = level.getMaxY();
		double yDepthPerc = Mth.inverseLerp(y, minY, maxY);
		double minDepthPerc = MoreRed.SERVERCONFIG.minWindDepth().getAsDouble();
		double maxDepthPerc = MoreRed.SERVERCONFIG.maxWindDepth().getAsDouble();
		if (yDepthPerc <= minDepthPerc)
		{
			return this.defaultBlockState();
		}
		if (yDepthPerc >= maxDepthPerc)
		{
			return this.defaultBlockState().setValue(WIND, 8);
		}
		double relerpedPerc = Mth.inverseLerp(yDepthPerc, minDepthPerc, maxDepthPerc);
		int wind = Mth.lerpInt((float)relerpedPerc, 0, 8);
		return this.defaultBlockState().setValue(WIND, wind);
	}
	
	

}
