package net.commoble.morered.transportation;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class TubeInventoryHandler extends ItemStackHandler
{
	private final TubeBlockEntity tube;
	private final Direction face;	// face of the tube an item is being inserted into (there shall be one handler for each side)

	public TubeInventoryHandler(TubeBlockEntity tube, Direction face)
	{
		super(1);
		this.tube = tube;
		this.face = face;
	}

	// the return value is the portion of the stack that was NOT inserted
	// if "simulate" is true, do not insert the item, but return the same value that
	// would be returned if it was a real insertion
	// beware, beware! Using the handler to insert an item into the tube generates a new route
	// if the route is already known, use tubetilentity.enqueueItemStack(stack, moves)
	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
	{
		if (stack.getCount() <= 0)
		{
			return ItemStack.EMPTY;
		}
		if (!simulate)
		{
			this.tube.setChanged();
		}
		return this.tube.enqueueItemStack(stack.copy(), this.face, simulate);
	}
}
