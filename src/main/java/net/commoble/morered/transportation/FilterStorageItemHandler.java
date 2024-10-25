package net.commoble.morered.transportation;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class FilterStorageItemHandler implements IItemHandler
{
	protected FilterBlockEntity filter;
	
	public FilterStorageItemHandler(FilterBlockEntity filter)
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
		return filter.filterStack;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (this.filter.filterStack.getCount() <= 0) // filterstack is empty
		{
			if (stack.getCount() > 0)
			{
				ItemStack reducedStack = stack.copy();
				ItemStack filterStack = reducedStack.split(1);
				if (!simulate)
				{
					this.filter.setFilterStackAndSaveAndSync(filterStack);
				}
				return this.filter.shuntingHandler.insertItem(0, reducedStack, simulate);
			}
			else
			{
				return ItemStack.EMPTY;
			}
		}
		else
		{
			return stack.copy();
		}
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		ItemStack extractedStack = this.filter.filterStack.copy();
		if (!simulate)
		{
			this.filter.setFilterStackAndSaveAndSync(ItemStack.EMPTY);
		}
		return extractedStack;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return true;
	}

}
