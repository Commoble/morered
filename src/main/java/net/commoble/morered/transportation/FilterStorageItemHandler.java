package net.commoble.morered.transportation;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class FilterStorageItemHandler extends ItemStacksResourceHandler
{
	protected FilterBlockEntity filter;
	
	public FilterStorageItemHandler(FilterBlockEntity filter)
	{
		super(1);
		this.filter = filter;
	}

	@Override
	protected int getCapacity(int index, ItemResource resource)
	{
		return 1;
	}

	@Override
	protected void onContentsChanged(int index, ItemStack previousContents)
	{
		super.onContentsChanged(index, previousContents);
		this.filter.saveAndSync();
	}
}
