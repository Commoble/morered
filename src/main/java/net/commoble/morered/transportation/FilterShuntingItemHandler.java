package net.commoble.morered.transportation;

import java.util.ArrayList;
import java.util.List;

import net.commoble.morered.util.SnapshotStack;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class FilterShuntingItemHandler implements ResourceHandler<ItemResource>
{
	private final AbstractFilterBlockEntity filter;
	private boolean shunting = false; // true while retreiving insertion result from neighbor, averts infinite loops
	private final SnapshotStack<List<ItemStack>> ejector = SnapshotStack.of(
		new ArrayList<>(),
		ArrayList::new,
		(oldList, newList) -> this.ejectItems(newList));
	
	public FilterShuntingItemHandler (AbstractFilterBlockEntity filter)
	{
		this.filter = filter;
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
		return !resource.isEmpty() && this.filter.canItemPassThroughFilter(resource.getItem());
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		if (!this.filter.getBlockState().hasProperty(DirectionalBlock.FACING)
			|| this.shunting // refuse inserts while inserting to avoid infinite shunt loops
			|| !this.isValid(index, resource))
			return 0;
		
		// make sure we don't eject millions of items
		amount = Math.min(amount, resource.getMaxStackSize());

		// attempt to insert item
		BlockPos pos = this.filter.getBlockPos();
		Direction outputDir = this.filter.getBlockState().getValue(DirectionalBlock.FACING);
		BlockPos outputPos = pos.relative(outputDir);
		
		this.shunting = true;
		ResourceHandler<ItemResource> outputHandler = this.filter.getLevel().getCapability(Capabilities.Item.BLOCK, outputPos, outputDir.getOpposite());
		
		int inserted = outputHandler == null
			? 0
			: ResourceHandlerUtil.insertStacking(outputHandler, resource, amount, transaction);
		
		this.shunting = false;
		
		int remaining = amount - inserted;
		
		if (remaining > 0)
		{
			ItemStack ejectedStack = resource.toStack(remaining);
			this.ejector.updateAndTakeSnapshot(list -> list.add(ejectedStack), transaction);
		}
		
		return amount;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0; // cannot extract from this handler
	}

	private void ejectItems(List<ItemStack> newList)
	{
		Level level = this.filter.getLevel();
		BlockPos shuntPos = this.filter.getBlockPos();
		Direction outputDir = this.filter.getBlockState().getValue(ShuntBlock.FACING);
		for (ItemStack stack : newList)
		{
			WorldHelper.ejectItemstack(level, shuntPos, outputDir, stack);
		}
		this.ejector.set(new ArrayList<>());
	}
}
