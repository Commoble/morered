package net.commoble.morered.wire_post;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CablePostBlockEntity extends WirePostBlockEntity
{
	
	public CablePostBlockEntity(BlockEntityType<? extends CablePostBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public CablePostBlockEntity(BlockPos pos, BlockState state)
	{
		super(MoreRed.CABLE_POST_BLOCK_ENTITY.get(), pos, state);
	}

}
