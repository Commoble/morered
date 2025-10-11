package net.commoble.morered.transportation;

import java.util.function.UnaryOperator;

import net.commoble.morered.util.SnapshotStack;
import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class DistributorItemHandler implements ResourceHandler<ItemResource>
{
	protected final DistributorBlockEntity distributor;
	private final Direction inputFace; // face of the distributor block that items are inserted into
	private final SnapshotStack<Direction> nextDirectionStack = SnapshotStack.of(
		Direction.DOWN,
		UnaryOperator.identity(),
		(oldValue, newValue) -> this.distributor().setChanged());

	private boolean shunting = false;
	
	public DistributorItemHandler(DistributorBlockEntity distributor, Direction inputFace)
	{
		this.distributor = distributor;
		this.inputFace = inputFace;
	}
	
	private DistributorBlockEntity distributor()
	{
		return this.distributor;
	}
	
	public int getNextDirectionIndex()
	{
		return this.nextDirectionStack.get().ordinal();
	}
	
	public void setNextDirectionIndex(int index)
	{
		this.nextDirectionStack.set(Direction.from3DDataValue(index));
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
		if (this.shunting)
			return 0; // nothing inserted

		// starting with the index of the next direction to check,
		// check all directions EXCEPT the input direction.
		// when a valid item handler to send the item to is found,
		// send as much of the remaining stack as possible in that direction.
		// Stop looking when remaining stack is empty, or when all directions have been checked.
		// Afterward, set the next direction to check to the next direction after the last checked direction.
		// Return empty stack if successfully sent item onward, return the entire stack if failure to do so.
		int startCheckIndex = this.nextDirectionStack.get().ordinal();
		int checkIndex = 0;
		Direction checkDirection;
		int remainingAmount = amount;
		
		// avoid checking neighboring TEs during simulation to avoid infinite loops in tubes
		// this unfortunately means that we have to wholly accept any item sent into it
		// so we'll have to eject the item if there's nowhere to send it onward to
		
		for (int i=0; i<6; i++)
		{
			checkIndex = (i + startCheckIndex) % 6;
			checkDirection = Direction.from3DDataValue(checkIndex);
			if (checkDirection == this.inputFace)
				continue;
			
			BlockPos outputPos = this.distributor.getBlockPos().relative(checkDirection);
//			final ItemStack stackForNextInsertion = remainingStack.copy();
			
			this.shunting = true;
			
			ResourceHandler<ItemResource> outputHandler = this.distributor.getLevel().getCapability(Capabilities.Item.BLOCK, outputPos, checkDirection.getOpposite());
			if (outputHandler != null)
			{
				int inserted = ResourceHandlerUtil.insertStacking(outputHandler, resource, remainingAmount, transaction);
				remainingAmount = Math.max(remainingAmount - inserted, 0);
			}

			this.shunting = false;

			if (remainingAmount <= 0)
			{
				break;
			}
			
		}
		
		if (remainingAmount > 0)
		{
			WorldHelper.ejectItemResources(this.distributor.getLevel(), this.distributor.getBlockPos(), this.inputFace.getOpposite(), resource, remainingAmount);
		}
		
		this.nextDirectionStack.setAndTakeSnapshot(Direction.from3DDataValue((checkIndex + 1) % 6), transaction);
		
		return amount;
	}

	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)
	{
		return 0;
	}

}
