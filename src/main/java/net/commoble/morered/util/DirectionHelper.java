package net.commoble.morered.util;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class DirectionHelper
{
	
	/**
	 * Returns the relative secondary side index given two side indices.
	 * Some definitions:
	 * -- let a side index be the ordinal of a Direction
	 * -- let a relative secondary side index be an integer in the range [0,3], representing what the ordinal of a direction would be
	 * if the primary side and its opposite didn't exist
	 * e.g. if the primary side is NORTH, the secondary indices would represent down, up, west, east, respectively
	 * @param primary side index, the side index representing the interior face a wire is attached to
	 * @param secondary side index representing which direction a wire line is pointing from the center of the face,
	 * must *not* be the same axis as the primary side or an incorrect result will be returned
	 * @return the relative secondary side index given two side indices
	 */
	public static int getCompressedSecondSide(int primary, int secondary)
	{
		return secondary < primary
			? secondary
			: secondary - 2;
	}
	
	/**
	 * Inverts the getCompressedSecondSide operation, converting a compressed secondary ordinal back to the [0,5] range.
	 * @param primarySide A direction ordinal in the range [0,5]
	 * @param secondarySide A compressed direction ordinal relative to the first, in the range [0,3]
	 * @return A direction ordinal in the range [0,5]
	 */
	public static int uncompressSecondSide(int primarySide, int secondarySide)
	{
		return (secondarySide/2 < primarySide/2) ? secondarySide : secondarySide + 2;
	}
	
	/**
	 * 
	 * @param connections long connections bitflags
	 * @param primarySide int index of a wire attachment side
	 * @param secondarySide int "compressed" index of the direction from an attachment side node to the neighbor the line connects to
	 * @return true if a line exists, false otherwise
	 */
	public static boolean hasLineConnection(long connections, int primarySide, int secondarySide)
	{
		return (connections & (1 << (primarySide*4 + secondarySide + 6))) != 0L;
	}
	
	// returns null if the two positions are not orthagonally adjacent
	// otherwise, returns the direction from from to to
	public static @Nullable Direction getDirectionToNeighborPos(BlockPos from, BlockPos to)
	{
		Direction[] dirs = Direction.values();
		int directionCount = dirs.length;
		BlockPos offset = to.subtract(from);
		for (int i = 0; i < directionCount; i++)
		{
			Direction dir = dirs[i];
			if (dir.getUnitVec3i().equals(offset))
			{
				return dir;
			}
		}

		return null;
	}
}
