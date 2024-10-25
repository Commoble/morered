package net.commoble.morered.transportation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractFilterBlockEntity extends BlockEntity
{
	public static final String INV_KEY = "inventory";
	
	public AbstractFilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public abstract boolean canItemPassThroughFilter(ItemStack stack);
	public abstract void dropItems();
}
