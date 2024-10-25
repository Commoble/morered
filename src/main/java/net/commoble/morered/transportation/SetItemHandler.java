package net.commoble.morered.transportation;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Itemhandler that acts as a Set of items.
 * All slots have a max stack size of 1, and it can store at most one stack of any given Item.
 */
public class SetItemHandler extends ItemStackHandler
{
	private Set<Item> cachedSet = null;
	
	public SetItemHandler(int size)
	{
		super(size);
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 1;
	}

	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
	{
		// we allow at most one stack of a given item to be in the inventory
		if (this.getSet().contains(stack.getItem()))
		{
			return stack;
		}
		return super.insertItem(slot, stack, simulate);
	}
	
	public Set<Item> getSet()
	{
		if (this.cachedSet == null)
		{
			Set<Item> items = new HashSet<>();
			int slots = this.getSlots();
			for (int i=0; i<slots; i++)
			{
				ItemStack stack = this.getStackInSlot(i);
				if (!stack.isEmpty())
				{
					items.add(stack.getItem());
				}
			}
			this.cachedSet = Set.copyOf(items);
		}
		return this.cachedSet;
	}

	@Override
	protected void onContentsChanged(int slot)
	{
		super.onContentsChanged(slot);
		this.cachedSet = null;
	}
}
