package net.commoble.morered.util;

import javax.annotation.Nullable;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.transportation.TubeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class PosHelper
{
	/**
	 * Returns the direction one must travel to get from the startPos to the nextPos
	 * returns null if the blocks are not adjacent
	 * Supplying (destination, start) instead of the other way around gives the face of the destination that touches the start
	 * (helpful for item handlers)
	 * @return
	 */
	@Nullable
	public static Direction getTravelDirectionFromTo(Level world, BlockPos startPos, BlockPos nextPos)
	{
		if (world.getBlockEntity(startPos) instanceof TubeBlockEntity startTube)
		{
			Direction dir = startTube.getDirectionOfRemoteConnection(nextPos);
			if (dir != null)
				return dir;
		}
		return getTravelDirectionBetweenAdjacentPositions(startPos, nextPos);
	}
	
	@Nullable
	public static Direction getTravelDirectionBetweenAdjacentPositions(BlockPos startPos, BlockPos nextPos)
	{
		for (Direction face : Direction.values())
		{
			if (startPos.relative(face).equals(nextPos))
			{
				return face;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param pos BlockPos to rotate about the origin
	 * @param group OctahedralGroup to rotate the pos with
	 * @return BlockPos rotated by the group
	 */
	public static BlockPos transform(BlockPos pos, OctahedralGroup group)
	{
		// averts most of the logic in most cases
		if (group == OctahedralGroup.IDENTITY)
		{
			return pos;
		}
		
		BlockPos newPos = new BlockPos(0,0,0);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if (x != 0)
		{
			Direction oldDirX = x > 0 ? Direction.EAST : Direction.WEST;
			Direction newDirX = group.rotate(oldDirX);
			newPos = newPos.relative(newDirX, Math.abs(x));
		}
		if (y != 0)
		{
			Direction oldDirY = y > 0 ? Direction.UP : Direction.DOWN;
			Direction newDirY = group.rotate(oldDirY);
			newPos = newPos.relative(newDirY, Math.abs(y));
		}
		if (z != 0)
		{
			Direction oldDirZ = z > 0 ? Direction.SOUTH : Direction.NORTH;
			Direction newDirZ = group.rotate(oldDirZ);
			newPos = newPos.relative(newDirZ, Math.abs(z));
		}
		return newPos;
	}
}
