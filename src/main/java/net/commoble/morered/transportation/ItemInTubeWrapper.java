package net.commoble.morered.transportation;

import java.util.LinkedList;
import java.util.Queue;

import com.mojang.math.OctahedralGroup;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.item.ItemStack;

/** Wrapper for the itemstacks being routed for the tubes
 * Tracks an itemstack as well as which tubes the stack has been through
 * @author Joseph
 *
 */
public class ItemInTubeWrapper
{
	public ItemStack stack;
	public LinkedList<Direction> remainingMoves;
	public int maximumDurationInTube;	// the amount of ticks that will be spent in the current tube
	public int ticksElapsed;
	public boolean freshlyInserted = false;	// if true, was just inserted into a tube network --
		// freshlyInserted==true implies an "extra move" is necessary -- this will be the first
		// move in the move list, but the renderer will handle it differently
	
	public static final String MOVES_REMAINING_TAG = "moves";
	public static final String TICKS_REMAINING_TAG = "ticks_remaining";
	public static final String TICKS_DURATION_TAG = "max_duration";
	public static final String IS_FRESHLY_INSERTED = "fresh";
	public static final String ITEM = "item";
	
	/** It would be a good idea to supply this constructor with a copy of a list when using an existing list **/
	public ItemInTubeWrapper(ItemStack stack, Queue<Direction> moves, int ticksToTravel)
	{
		this.stack = stack.copy();
		this.remainingMoves = new LinkedList<Direction>();
		for (Direction dir : moves)	// copy original list so so changes don't affect the old list
		{
			this.remainingMoves.add(dir);
		}
		this.ticksElapsed = 0;
		this.maximumDurationInTube = ticksToTravel;
	}
	
	/** Constructor to use when freshly inserting a wrapper into the network **/
	public ItemInTubeWrapper(ItemStack stack, Queue<Direction> moves, int ticksToTravel, Direction firstMove)
	{
		this(stack, moves, ticksToTravel);
		if (firstMove != null)
		{
			this.remainingMoves.addFirst(firstMove);
			this.freshlyInserted = true;
		}
	}
	
	public static ItemInTubeWrapper readFromNBT(CompoundTag compound, OctahedralGroup group, HolderLookup.Provider registries)
	{
		ItemStack stack = ItemStack.parse(registries, compound.getCompoundOrEmpty(ITEM)).orElse(ItemStack.EMPTY);
		int[] moveBuffer = compound.getIntArray(MOVES_REMAINING_TAG).orElse(new int[0]);
		int ticksElapsed = compound.getIntOr(TICKS_REMAINING_TAG, 0);
		int maxDuration = compound.getIntOr(TICKS_DURATION_TAG, 10);
		boolean isFreshlyInserted = compound.getBooleanOr(IS_FRESHLY_INSERTED, false);

		ItemInTubeWrapper wrapper = new ItemInTubeWrapper(stack, decompressMoveList(moveBuffer, group), maxDuration);
		wrapper.ticksElapsed = ticksElapsed;
		wrapper.freshlyInserted = isFreshlyInserted;
		return wrapper;
	}
	
	public CompoundTag writeToNBT(CompoundTag compound, OctahedralGroup group, HolderLookup.Provider registries)
	{
		compound.put(MOVES_REMAINING_TAG, compressMoveList(this.remainingMoves, group));
		compound.putInt(TICKS_REMAINING_TAG, this.ticksElapsed);
		compound.putInt(TICKS_DURATION_TAG, this.maximumDurationInTube);
		compound.putBoolean(IS_FRESHLY_INSERTED, this.freshlyInserted);
		compound.put(ITEM, this.stack.save(registries, compound));
		
		return compound;
	}
	
	// compress the move list into an intarray NBT
	// where the intarray is of the form (dir0, count0, dir1, count1, . . . dirN, countN)
	// i.e. consisting of pairs of Direction indexes and how many times to move in that direction
	// group is the orientation of the tube (which may have been rotated by a structure piece or similar)
	// we need to "normalize" the moves to an unrotated state
	public static IntArrayTag compressMoveList(Queue<Direction> moves, OctahedralGroup group)
	{
		if (moves == null || moves.isEmpty())
			return new IntArrayTag(new int[0]);
		
		OctahedralGroup normalizer = group.inverse();
		int moveIndex = 0;
		IntArrayList buffer = new IntArrayList();
		Direction currentMove = moves.peek();
		buffer.add(normalizer.rotate(currentMove).ordinal());
		buffer.add(0);
		
		for (Direction dir : moves)
		{
			if (!dir.equals(currentMove))
			{
				buffer.add(normalizer.rotate(dir).ordinal());
				buffer.add(1);
				currentMove = dir;
				moveIndex += 2;
			}
			else
			{
				buffer.set(moveIndex+1, buffer.getInt(moveIndex+1)+1);
			}
		}
		
		IntArrayTag nbt = new IntArrayTag(buffer.toIntArray());

		return nbt;
	}
	
	// group is the orientation of the tube (if it's been rotated by structures etc)
	// data has previously been "normalized" to the unrotated orientation
	// now we want to denormalize it
	public static Queue<Direction> decompressMoveList(int[] buffer, OctahedralGroup group)
	{
		Queue<Direction> moves = new LinkedList<Direction>();
		int size = buffer.length;
		if (size % 2 != 0)
		{
			return moves;	// array should have an even size
		}
		// below this line, size of array is guaranteed to be even
		int pairCount = size / 2;
		
		for (int i=0; i<pairCount; i++)
		{
			Direction dir = group.rotate(Direction.from3DDataValue(buffer[i*2]));
			int moveCount = buffer[i*2+1];
			for (int count=0; count<moveCount; count++)
			{
				moves.add(dir);	// add this direction that many times
			}
		}
		
		return moves;
	}
}
