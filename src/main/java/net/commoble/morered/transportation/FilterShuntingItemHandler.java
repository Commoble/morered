package net.commoble.morered.transportation;

import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class FilterShuntingItemHandler implements IItemHandler
{
	private final AbstractFilterBlockEntity filter;
	private boolean shunting = false; // true while retreiving insertion result from neighbor, averts infinite loops
	
	public FilterShuntingItemHandler (AbstractFilterBlockEntity filter)
	{
		this.filter = filter;
	}

	@Override
	public int getSlots()
	{
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		// TODO Auto-generated method stub
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (!this.filter.getBlockState().hasProperty(DirectionalBlock.FACING))
			return stack;
		
		if (this.shunting || !this.isItemValid(slot, stack))
		{
			return stack.copy();
		}
		
		if (!simulate) // actually inserting an item
		{
			// attempt to insert item
			BlockPos pos = this.filter.getBlockPos();
			Direction outputDir = this.filter.getBlockState().getValue(DirectionalBlock.FACING);
			BlockPos outputPos = pos.relative(outputDir);
			
			this.shunting = true;
			IItemHandler outputHandler = this.filter.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, outputPos, outputDir.getOpposite());
			ItemStack remaining = outputHandler == null
				? stack.copy()
				: WorldHelper.disperseItemToHandler(stack, outputHandler);
			this.shunting = false;
			
			if (remaining.getCount() > 0) // we have remaining items
			{
				WorldHelper.ejectItemstack(this.filter.getLevel(), pos, outputDir, remaining);
			}
		}
		
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return stack.getCount() > 0 && this.filter.canItemPassThroughFilter(stack);
	}

}
