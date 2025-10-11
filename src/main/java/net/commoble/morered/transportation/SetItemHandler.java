package net.commoble.morered.transportation;

import java.util.HashSet;
import java.util.Set;

import net.commoble.morered.util.SnapshotStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Itemhandler that acts as a Set of items.
 * All slots have a max stack size of 1, and it can store at most one stack of any given Item.
 */
public class SetItemHandler extends ItemStacksResourceHandler
{
	private final SnapshotStack<Set<Item>> setStack = SnapshotStack.of(
		new HashSet<>(),
		HashSet::new);
	
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
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		int extracted = super.extract(index, resource, amount, transaction);
		if (extracted > 0)
		{
			this.setStack.applyAndTakeSnapshot(set -> set.remove(resource.getItem()), transaction);
		}
		return extracted;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		// we allow at most one stack of a given item to be in the inventory
		if (this.setStack.get().contains(resource.getItem()))
		{
			return 0;
		}
		int inserted = super.insert(index, resource, amount, transaction);
		if (inserted > 0)
		{
			this.setStack.applyAndTakeSnapshot(set -> set.add(resource.getItem()), transaction);
		}
		return inserted;
	}

	@Override
	public void deserialize(ValueInput input)
	{
		super.deserialize(input);
		Set<Item> newSet = new HashSet<>();
		for (ItemStack stack : this.stacks)
		{
			if (!stack.isEmpty())
			{
				newSet.add(stack.getItem());
			}
		}
		this.setStack.set(newSet);
	}

	@Override
	public void set(int index, ItemResource resource, int amount)
	{
		super.set(index, resource, amount);
		this.setStack.apply(set -> set.add(resource.getItem()));
	}

	@Override
	public boolean isValid(int index, ItemResource resource)
	{
		return super.isValid(index, resource)
			&& !this.setStack.get().contains(resource.getItem());
	}

	public Set<Item> getSet()
	{
		return this.setStack.get();
	}
}
