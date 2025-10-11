package net.commoble.morered.transportation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.commoble.morered.routing.Route;
import net.commoble.morered.util.SnapshotStack;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TubeInventoryHandler implements ResourceHandler<ItemResource>
{
	private static record TubeEnqueuement(ItemStack stack, Route route) {}
	
	private final TubeBlockEntity tube;
	private final Direction face;	// face of the tube an item is being inserted into (there shall be one handler for each side)
	private final SnapshotStack<List<TubeEnqueuement>> enqueuements = SnapshotStack.of(
		new ArrayList<>(),
		ArrayList::new,
		(oldList, newList) -> this.flushEnqueuements(newList));

	public TubeInventoryHandler(TubeBlockEntity tube, Direction face)
	{
		this.tube = tube;
		this.face = face;
	}

	@Override
	public int size()
	{
		return 1;
	}

	@Override
	public ItemResource getResource(int index)
	{
		return ItemResource.EMPTY;
	}

	@Override
	public long getAmountAsLong(int index)
	{
		return 0;
	}

	@Override
	public long getCapacityAsLong(int index, ItemResource resource)
	{
		return resource.getMaxStackSize();
	}

	@Override
	public boolean isValid(int index, ItemResource resource)
	{
		return true;
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		amount = Math.min(resource.getMaxStackSize(), amount);
		if (amount <= 0)
			return amount;
		ItemStack stack = resource.toStack(amount);
		@Nullable Route route = this.tube.getRouteForStack(stack, this.face, transaction);
		if (route == null) // could not find route to enqueue itemstack with
		{
			return 0;
		}
		this.enqueuements.updateAndTakeSnapshot(list -> list.add(new TubeEnqueuement(stack, route)), transaction);
		return amount;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0;
	}

	private void flushEnqueuements(List<TubeEnqueuement> enqueuements)
	{
		for (TubeEnqueuement enqueuement : enqueuements)
		{
			this.tube.enqueueItemStack(enqueuement.stack, this.face, enqueuement.route);
		}
		// don't need to call setChanged, the tube does this on tick
		this.enqueuements.set(new ArrayList<>());
	}
}
