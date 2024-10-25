package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DistributorBlock extends Block implements EntityBlock
{
	public DistributorBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return MoreRed.get().distributorEntity.get().create(pos, state);
	}
}
