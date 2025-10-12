package net.commoble.morered.transportation;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ExtractOnlyItemStacksResourceHandler extends ItemStacksResourceHandler
{
	public ExtractOnlyItemStacksResourceHandler(int size)
	{
		super(size);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0;
	}

	@Override
	public boolean isValid(int index, ItemResource resource)
	{
		return false;
	}
}
