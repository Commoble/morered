package net.commoble.morered.wire_post;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BundledCablePostBlockEntity extends WirePostBlockEntity
{
	
	public BundledCablePostBlockEntity(BlockEntityType<? extends BundledCablePostBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public BundledCablePostBlockEntity(BlockPos pos, BlockState state)
	{
		super(MoreRed.get().bundledCablePostBeType.get(), pos, state);
	}

}
