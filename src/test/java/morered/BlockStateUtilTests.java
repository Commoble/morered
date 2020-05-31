package morered;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.commoble.morered.util.BlockStateUtil;

import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;

class BlockStateUtilTests
{
	public static final Direction DOWN = Direction.DOWN;
	public static final Direction UP = Direction.UP;
	public static final Direction NORTH = Direction.NORTH;
	public static final Direction SOUTH = Direction.SOUTH;
	public static final Direction WEST = Direction.WEST;
	public static final Direction EAST = Direction.EAST;
	
	// the values here are as rationalized in BlockStateUtil
	public static final Direction[][] DIRECTION_MATRIX = {
		{NORTH, EAST, SOUTH, WEST},
		{NORTH, WEST, SOUTH, EAST},
		{UP, EAST, DOWN, WEST},
		{UP, WEST, DOWN, EAST},
		{UP, NORTH, DOWN, SOUTH},
		{UP, SOUTH, DOWN, NORTH}
	};
	
	public static final Direction[] DR = {DOWN, DOWN, DOWN, DOWN}; // "down rotations"
	public static final Direction[] UR = {UP, UP, UP, UP};
	public static final Direction[] NR = {NORTH, EAST, SOUTH, WEST};
	public static final Direction[] SR = {SOUTH, WEST, NORTH, EAST};
	public static final Direction[] WR = {WEST, NORTH, EAST, SOUTH};
	public static final Direction[] ER = {EAST, SOUTH, WEST, NORTH};
	public static final Direction[][] ROTATIONS = {DR, UR, NR, SR, WR, ER};

	@Test
	void testRotations()
	{
		Direction[] directions = Direction.values();
		int[] rotationIndexes = {0,1,2,3};
		Rotation[] rotationTransforms = Rotation.values();
		
		for (Direction attachmentDirection : directions)
		{
			for (int oldRotationIndex : rotationIndexes)
			{
				for (Rotation rotationTransform : rotationTransforms)
				{
					int newRotationIndex = BlockStateUtil.getRotatedRotation(attachmentDirection, oldRotationIndex, rotationTransform);
					Direction newAttachmentDirection = rotationTransform.rotate(attachmentDirection);
					int directionMatrixIndex = DIRECTION_MATRIX[attachmentDirection.ordinal()][oldRotationIndex].ordinal();
					Direction expectedOutputDirection = ROTATIONS[directionMatrixIndex][rotationTransform.ordinal()];
					Direction actualOutputDirection = BlockStateUtil.getOutputDirection(newAttachmentDirection, newRotationIndex);
					Assertions.assertEquals(expectedOutputDirection, actualOutputDirection);
				}
			}
		}
	}
	
	// the mirror enum is NONE, NORTH/SOUTH, WEST/EAST
	public static final Direction[] DM = {DOWN, DOWN, DOWN};	// down mirrors
	public static final Direction[] UM = {UP, UP, UP};
	public static final Direction[] NM = {NORTH, SOUTH, NORTH};
	public static final Direction[] SM = {SOUTH, NORTH, SOUTH};
	public static final Direction[] WM = {WEST, WEST, EAST};
	public static final Direction[] EM = {EAST, EAST, WEST};
	public static final Direction[][] MIRRORS = {DM, UM, NM, SM, WM, EM};
	
	@Test
	void testMirrors()
	{
		Direction[] directions = Direction.values();
		int[] rotationIndexes = {0,1,2,3};
		Mirror[] mirrorTransfroms = Mirror.values();
		
		for (Direction attachmentDirection : directions)
		{
			for (int oldRotationIndex : rotationIndexes)
			{
				for (Mirror mirrorTransform : mirrorTransfroms)
				{
					int newRotationIndex = BlockStateUtil.getMirroredRotation(attachmentDirection, oldRotationIndex, mirrorTransform);
					Direction newAttachmentDirection = mirrorTransform.mirror(attachmentDirection);
					int directionMatrixIndex = DIRECTION_MATRIX[attachmentDirection.ordinal()][oldRotationIndex].ordinal();
					Direction expectedOutputDirection = MIRRORS[directionMatrixIndex][mirrorTransform.ordinal()];
					Direction actualOutputDirection = BlockStateUtil.getOutputDirection(newAttachmentDirection, newRotationIndex);
					Assertions.assertEquals(expectedOutputDirection, actualOutputDirection);
				}
			}
		}
	}

}
