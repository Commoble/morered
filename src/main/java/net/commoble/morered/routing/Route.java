package net.commoble.morered.routing;

import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** This class has a natural ordering that is inconsistent with equals() **/
public class Route implements Comparable<Route>
{
	public Queue<Direction> sequenceOfMoves;
	public Endpoint destination;
	public int length;	// this isn't the same as the size of the sequence because tubes could have unusual length
	

	public Route(Endpoint destination, int length, Queue<Direction> sequenceOfMoves)
	{
		this.destination = destination;
		this.length = length;
		this.sequenceOfMoves = sequenceOfMoves;
	}
	
	// is a route's endpoint valid for an item being inserted into the network,
	// given the position of the tube the item was inserted into,
	// the side of that tube the item was inserted into,
	// and the item itself?
	public boolean isRouteDestinationValid(Level world, BlockPos startPos, Direction insertionSide, ItemStack stack, TransactionContext context)
	{
		// if the route's endpoint was the position/face the item was inserted from, this route is not valid
		if (this.destination.pos.equals(startPos.relative(insertionSide)) && this.destination.face.getOpposite().equals(insertionSide))
		{
			return false;
		}
		
		// otherwise, return whether the item is valid for the route's endpoint
		return this.destination.canInsertItem(world, stack, context);
	}

	@Override
	public int compareTo(Route other)
	{
		if (this.length != other.length)
		{
			return this.length - other.length;
		}
		else
		{
			BlockPos thisEnd = this.destination.pos;
			BlockPos otherEnd = other.destination.pos;
			if (thisEnd.getY() != otherEnd.getY())
			{
				return thisEnd.getY() - otherEnd.getY();
			}
			else if (thisEnd.getZ() != otherEnd.getZ())
			{
				return thisEnd.getZ() - otherEnd.getZ();
			}
			else
			{
				return thisEnd.getX() - otherEnd.getX();
			}
		}
	}
	
	
	public String toStringFrom(BlockPos startPos)
	{
		LinkedList<String> moveStrings = new LinkedList<String>();
		moveStrings.add(startPos.toString());
		
		for (Direction face : this.sequenceOfMoves)
		{
			if (face == null)
			{
				moveStrings.add("null");
			}
			else
			{
				moveStrings.add(face.toString());
			}
		}
		
		return String.join(", ", moveStrings);
	}
}
