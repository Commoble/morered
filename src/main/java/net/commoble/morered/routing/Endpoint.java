package net.commoble.morered.routing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class Endpoint
{	
	public final BlockPos pos;	// TEs can become invalidated or replaced, so get new ones when needed (not sure why this comment is here)
	public final Direction face;	// the face of the block at this blockpos that represents the endpoint
	
	public Endpoint(BlockPos tePos, Direction blockFace)
	{
		this.pos = tePos;
		this.face = blockFace;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (this == other)	// same instance, must be equal
		{
			return true;
		}
		else if (other instanceof Endpoint)
		{	// if other object is an endpoint,
			// this is equivalent to the other endpoint if and only if
			// both blockpos are equivalent and both endpoints are equivalent
			Endpoint otherEndpoint = (Endpoint) other;
			return this.pos.equals(otherEndpoint.pos) && this.face.equals(otherEndpoint.face);
		}
		else
		{
			return false;	// not an endpoint, can't be equal
		}
	}
	
	/**
	 * Returns TRUE if the TE at this endpoint has an item handler and any portion
	 * of the given itemstack can be inserted into that item handler
	 * 
	 * Return FALSE if the handler cannot take the stack or if either the
	 * handler or the TE do not exist
	 * 
	 * This only simulates the insertion and does not affect the state of
	 * any itemstacks or inventories
	 * 
	 * @param level The world this endpoint lies in
	 * @param stack The stack to attempt to insert
	 * @return true or false as described above
	 */
	public boolean canInsertItem(Level level, ItemStack stack, TransactionContext context)
	{
		ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, this.pos, this.face);
		return handler != null && canInsertItem(handler, stack, context);
	}
	
	// helper function used for the above method
	// the itemstack is the one passed into the above method, the item handler is assumed to exist
	public static boolean canInsertItem(ResourceHandler<ItemResource> handler, ItemStack stack, TransactionContext context)
	{
		try(Transaction simulator = Transaction.open(context))
		{
			int slots = handler.size();
			ItemResource resource = ItemResource.of(stack);
			for (int i=0; i<slots; i++)
			{
				// for each slot, if the itemstack can be inserted into the slot
					// (i.e. if the type of that item is valid for that slot AND
					// if there is room to put at least part of that stack into the slot)
				// then the inventory at this endpoint can receive the stack, so return true
				if (handler.isValid(i, resource) && handler.insert(i, resource, stack.getCount(), simulator) > 0)
				{
					return true;
				}
			}
			
			// return false if no acceptable slot is found
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return this.pos.hashCode() ^ this.face.hashCode();
	}
	
	@Override
	public String toString()
	{
		return this.pos + ";    " + this.face;
	}
}
