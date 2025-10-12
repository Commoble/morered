package net.commoble.morered.transportation;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * Itemhandler that acts as a Set of items.
 * All slots have a max stack size of 1, and it can store at most one stack of any given Item.
 */
public class SetItemHandler extends ItemStacksResourceHandler
{	
	public SetItemHandler(int size)
	{
		super(size);
	}

	@Override
	public int getCapacity(int slot, ItemResource resource)
	{
		return 1;
	}

	@Override
	public void set(int index, ItemResource resource, int amount)
	{
		if (this.isValid(index, resource))
		{
			super.set(index, resource, amount);
		}
	}

	@Override
	public boolean isValid(int index, ItemResource resource)
	{
		int slots = this.size();
		for (int i=0; i<slots; i++)
		{
			ItemResource existingResource = this.getResource(i);
			if (!existingResource.isEmpty() && existingResource.getItem() == resource.getItem())
			{
				return false;
			}
		}
		return super.isValid(index, resource);
	}
}
