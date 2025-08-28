package net.commoble.morered.mechanisms;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class WindCatcherBlockItem extends BlockItem
{

	public WindCatcherBlockItem(Block block, Properties props)
	{
		super(block, props);
	}

	@Override
	protected boolean canPlace(BlockPlaceContext context, BlockState state)
	{
		Level level = context.getLevel();
		BlockPos placePos = context.getClickedPos();
		for (int segment=0; segment<8; segment++)
		{
			BlockPos airFoilPos = AirFoilBlock.getAirFoilPos(segment, placePos);
			if (!level.getBlockState(airFoilPos).canBeReplaced(context))
			{
				return false;
			}
		}
		return super.canPlace(context, state);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state)
	{
		// airfoil dummy blocks check for existence of windcatcher on updateshape
		// place windcatcher first, then place airfoils
		boolean success = super.placeBlock(context, state);
		if (!success)
			return false;
		Level level = context.getLevel();
		if (!level.isClientSide)
		{
			BlockPos corePos = context.getClickedPos();
			for (int segment=0; segment<8; segment++)
			{
				BlockPos airFoilPos = AirFoilBlock.getAirFoilPos(segment, corePos);
				level.setBlock(airFoilPos, MoreRed.AIRFOIL_BLOCK.get().defaultBlockState().setValue(AirFoilBlock.SEGMENT, segment), Block.UPDATE_ALL);
			}
		}
		return true;
	}
}
