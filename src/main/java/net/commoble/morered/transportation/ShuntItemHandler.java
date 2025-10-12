package net.commoble.morered.transportation;

import java.util.ArrayList;
import java.util.List;

import net.commoble.morered.util.SnapshotStack;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ShuntItemHandler implements ResourceHandler<ItemResource>
{
	public final ShuntBlockEntity shunt;
	public final boolean canTakeItems;
	private boolean shunting = false; // prevent infinite loops
	private final SnapshotStack<List<ItemStack>> ejector = SnapshotStack.of(
		new ArrayList<>(),
		ArrayList::new,
		(oldList, newList) -> this.ejectItems(newList));

	public ShuntItemHandler(ShuntBlockEntity shunt, boolean canTakeItems)
	{
		this.shunt = shunt;
		this.canTakeItems = canTakeItems;
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
		Level world = this.shunt.getLevel();
		BlockPos pos = this.shunt.getBlockPos();
		return this.canTakeItems && !world.hasNeighborSignal(pos);
	}

	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		if (!this.canTakeItems)
		{
			return 0;
		}
		
		// make sure we don't flood the world with Integer.MAX_VALUE items
		amount = Math.min(resource.getMaxStackSize(), amount);

		BlockPos shuntPos = this.shunt.getBlockPos();
		Direction outputDir = this.shunt.getBlockState().getValue(ShuntBlock.FACING);
		BlockPos outputPos = shuntPos.relative(outputDir);
		
		if (!this.shunting)
		{
			// attempt to insert item
			this.shunting = true;
			ResourceHandler<ItemResource> outputHandler = this.shunt.getLevel().getCapability(Capabilities.Item.BLOCK, outputPos, outputDir.getOpposite());
			int inserted = outputHandler == null
				? 0
				: outputHandler.insert(resource, amount, transaction);
			this.shunting = false;
			
			int remainingAmount = amount - inserted;
			
			if (remainingAmount > 0) // we have remaining items
			{
				ItemStack remainingStack = resource.toStack(remainingAmount);
				this.ejector.updateAndTakeSnapshot(list -> list.add(remainingStack), transaction);
			}
		}
		else
		{
			int ejectedAmount = amount;
			ItemStack ejectedStack = resource.toStack(ejectedAmount);
			this.ejector.updateAndTakeSnapshot(list -> list.add(ejectedStack), transaction);
		}
		
		return amount;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0;
	}

	private void ejectItems(List<ItemStack> newList)
	{
		Level level = this.shunt.getLevel();
		BlockPos shuntPos = this.shunt.getBlockPos();
		Direction outputDir = this.shunt.getBlockState().getValue(ShuntBlock.FACING);
		for (ItemStack stack : newList)
		{
			WorldHelper.ejectItemstack(level, shuntPos, outputDir, stack);
		}
		this.ejector.set(new ArrayList<>());
	}

}
