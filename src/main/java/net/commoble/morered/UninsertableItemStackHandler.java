package net.commoble.morered;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class UninsertableItemStackHandler extends ItemStackHandler
{
	public UninsertableItemStackHandler(int size)
	{
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return false;
	}
}
