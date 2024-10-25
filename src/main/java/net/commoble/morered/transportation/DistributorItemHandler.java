package net.commoble.morered.transportation;

import net.commoble.morered.util.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class DistributorItemHandler implements IItemHandler
{
	protected final DistributorBlockEntity distributor;
	private final Direction inputFace; // face of the distributor block that items are inserted into
	
	private Direction nextDirection = Direction.DOWN; 
	private boolean shunting = false;
	
	public DistributorItemHandler(DistributorBlockEntity distributor, Direction inputFace)
	{
		this.distributor = distributor;
		this.inputFace = inputFace;
	}
	
	public int getNextDirectionIndex()
	{
		return this.nextDirection.ordinal();
	}
	
	public void setNextDirectionIndex(int index)
	{
		this.nextDirection = Direction.from3DDataValue(index);
		this.distributor.setChanged();
	}
	
	@Override
	public int getSlots()
	{
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (this.shunting)
			return stack.copy();
		
		if (!simulate && !stack.isEmpty())
		{
			// starting with the index of the next direction to check,
			// check all directions EXCEPT the input direction.
			// when a valid item handler to send the item to is found,
			// send as much of the remaining stack as possible in that direction.
			// Stop looking when remaining stack is empty, or when all directions have been checked.
			// Afterward, set the next direction to check to the next direction after the last checked direction.
			// Return empty stack if successfully sent item onward, return the entire stack if failure to do so.
			int startCheckIndex = this.nextDirection.ordinal();
			int checkIndex = 0;
			Direction checkDirection;
			ItemStack remainingStack = stack;
			
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
				final ItemStack stackForNextInsertion = remainingStack.copy();
				
				this.shunting = true;
				IItemHandler outputHandler = this.distributor.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, outputPos, checkDirection.getOpposite());
				remainingStack = outputHandler == null
					? remainingStack
					: WorldHelper.disperseItemToHandler(stackForNextInsertion, outputHandler, simulate);
				this.shunting = false;
				
				if (remainingStack.isEmpty())
				{
					break;
				}
				
			}
			
			if (!remainingStack.isEmpty())
			{
				WorldHelper.ejectItemstack(this.distributor.getLevel(), this.distributor.getBlockPos(), this.inputFace.getOpposite(), remainingStack);
			}
			
			this.setNextDirectionIndex((checkIndex + 1) % 6);
		}
		
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate)
	{
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot)
	{
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack)
	{
		return true;
	}

}
