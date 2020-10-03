package commoble.morered.util;

import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.Vec3d;

public class BlockStateUtil
{
	
	// if we want to make a block that can be attached to the six faces of a block,
	// and then, from there, be rotated to the four edges of that face,
	// let's start with the base state first
	// if we're attached to the top of a block (attachment direction = down, first direction index),
	// then we want the unrotated direction (r0) to be pointing north (first horizontal direction index)
	// Rotation increases clockwise around the y-axis (AXIS A), so the 0/1/2/3 states are north,east,south,west
	
	// this is simple enough, but what if the attachment face is rotated?
	
	// self-note: if x-rotation-0 is north, x-rotation-90 is down
	
				// IMPLEMENTATION A) (not used, skip to IMPLEMENTATION B for relevant information)
				// let's say that if we rotate the attachment face on some axis (AXIS B)
				// then we rotate the r0 direction in the same manner
				// and from there we always rotate clockwise (looking towards the direction of attachment)
				// attachment face // r0 // r1 // r2 // r3
				// DOWN		N/E/S/W
				// UP		S/E/N/W	
				// NORTH	U/E/D/W
				// SOUTH	D/E/U/W			
				// WEST		N/D/S/U
				// EAST		N/U/S/D
				
				// if the blockstate is rotated around the y-axis, what do we do with the rotation index r?
					// we just add the additional rotation to r and this works in all cases
				
				// we'll need to determine what to do with the rotation index r if someone mirrors the blockstate horizontally
					//	mirror N/S
					//		if face is D/U, r+2 if r is even
					//		if face is N/S, r+2 always
					//		if face is W/E, r+2 if r is even
					//		R+2 IF FACE IS N/S OR R IS EVEN
					//	mirror W/E
					//		if face is D/U, r+2 if r is odd
					//		if face is N/S, r+2 if r is odd
					//		if face is W/E, r+2 if r is odd
					//		R+2 IF R IS ODD
	
	
	// IMPLEMENTATION B)
	// if attachment face is any direction except north or south, then the r0 direction is north
	// if attachment face is north or south, then the r0 direction is down
	// and from there we always rotate clockwise, looking towards the direction of attachment
	// attachment face // r0 // r1 // r2 // r3
	// DOWN		N/E/S/W	
	// UP		N/W/S/E
	// NORTH	U/E/D/W
	// SOUTH	U/W/D/E		
	// WEST		U/N/D/S
	// EAST		U/S/D/N
	
	// if someone rotates the blockstate about the y-axis, what happens to rotation index r?
		// if attachment face is down, we add the rotation to r
		// if attachment face is up, we subtract the rotation from r
		// if attachment face is horizontal, we preserve r
	// if someone mirrors the blockstate horizontally, what happens to rotation index r?
		//	mirror NONE
		//		r = r + r
		//	mirror N/S
		//		if face is D/U, r+2 if r is even
		//		if face is N/S, r+2 if r is odd
		//		if face is W/E, r+2 if r is odd
		//	mirror W/E
		//		if face is D/U, r+2 if r is odd
		//		if face is N/S, r+2 if r is odd
		//		if face is W/E, r+2 if r is odd
		//	therefore if mirror is not NONE
		//		let boolean specialCase = (mirror is N/S and face is vertical)
		//		if ((specialCase and r is even) or (!specialCase and r is not even)) r = (r+2)%4, else r=r
		
	
	// IMPLEMENTATION C) model-rotation-aware
	// Can we define the rotations in a manner that makes the blockstate json the most simple and intuitive?
			// (spoiler: not without more math than I want to put into this)
	// Block models can only be rotated on the x- and y-axes
	// with rotations in increments of 90 degrees, that means that there are only 16 possible rotations per 1 model
	// since we need 24 different rotations, we will *require* at least two models per block
	// let's think about all the different states we could rotate our block from the ground state
	// x-rotation	// y-rotation	// attachment direction	// output direction
	// 0			0				DOWN					NORTH
	// 0			90				DOWN					EAST
	// 0			180				DOWN					SOUTH
	// 0			270				DOWN					WEST
	// 90			0				SOUTH					DOWN
	// 90			90				WEST					DOWN
	// 90			180				NORTH					DOWN
	// 90			270				EAST					DOWN
	// 180			0				UP						SOUTH
	// 180			90				UP						WEST
	// 180			180				UP						NORTH
	// 180			270				UP						EAST
	// 270			0				NORTH					UP
	// 270			90				EAST					UP
	// 270			180				SOUTH					UP
	// 270			270				WEST					UP
	// so now there are eight orientations that require a second model:
	// 0			0				EAST					NORTH
	// 0			90				SOUTH					EAST
	// 0			180				WEST					SOUTH
	// 0			270				NORTH					WEST
	// 180			0				EAST					SOUTH
	// 180			90				SOUTH					WEST
	// 180			180				WEST					NORTH
	// 180			270				NORTH					EAST
	
	// let's take the above and reformat it by blockstate properties
	// turns out both A and B above look pretty ugly when converted to model rotations
	// let's use IMPLEMENTATION B since we don't have to do as much math

	// attachment direction	// rotation index	// output direction	// model	// x-rotation	// y-rotation
	// DOWN					0					NORTH				standard	0				0
	// DOWN					1					EAST				standard	0				90
	// DOWN					2					SOUTH				standard	0				180
	// DOWN					3					WEST				standard	0				270
	// UP					0					NORTH				standard	180				180
	// UP					1					WEST				standard	180				90
	// UP					2					SOUTH				standard	180				0
	// UP					3					EAST				standard	180				270
	// NORTH				0					UP					standard	270				0
	// NORTH				1					EAST				alt			180				270
	// NORTH				2					DOWN				standard	90				180
	// NORTH				3					WEST				alt			0				270
	// SOUTH				0					UP					standard	270				180
	// SOUTH				1					WEST				alt			180				90
	// SOUTH				2					DOWN				standard	90				0
	// SOUTH				3					EAST				alt			0				90
	// WEST					0					UP					standard	270				270
	// WEST					1					NORTH				alt			180				180
	// WEST					2					DOWN				standard	90				90
	// WEST					3					SOUTH				alt			0				180
	// EAST					0					UP					standard	270				90
	// EAST					1					SOUTH				alt			180				0
	// EAST					2					DOWN				standard	90				270
	// EAST					3					NORTH				alt			0				0
	
	public static final Direction[][] OUTPUT_TABLE = {
		{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST},
		{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST},
		{Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST},
		{Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST},
		{Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH},
		{Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH}
	};
	
	public static Direction getOutputDirection(Direction attachmentDirection, int rotationIndex)
	{
		return OUTPUT_TABLE[attachmentDirection.ordinal()][rotationIndex];
	}
	
	public static Direction getInputDirection(Direction attachmentDirection, int outputRotationIndex, int rotationsFromOutput)
	{
		return getOutputDirection(attachmentDirection, (outputRotationIndex + rotationsFromOutput) % 4);
	}
	
	/** Get the rotation index for a gate plate state after rotating the blockstate **/
	public static int getRotatedRotation(Direction attachmentFace, int rotationIndex, Rotation rotation)
	{
		if (attachmentFace == Direction.DOWN)
		{
			return (rotationIndex + rotation.ordinal()) % 4;
		}
		else if (attachmentFace == Direction.UP)
		{
			return (rotationIndex + 4 - rotation.ordinal()) % 4;
		}
		else
		{
			return rotationIndex;
		}
	}
	
	/** Get the rotation index for a gate plate state after mirroring the blockstate **/
	public static int getMirroredRotation(Direction attachmentFace, int rotationIndex, Mirror mirror)
	{
		if (mirror == Mirror.NONE)
		{
			return rotationIndex;
		}
		
		boolean specialCase = (mirror == Mirror.LEFT_RIGHT && attachmentFace.getAxis() == Axis.Y);
		boolean rotationIsEven = (rotationIndex % 2 == 0);
		if ((specialCase && rotationIsEven) || (!specialCase && !rotationIsEven))
		{
			return (rotationIndex+2) % 4;
		}
		else
		{
			return rotationIndex;
		}
	}
	
	public static int getRotationIndexForDirection(Direction attachmentFace, Direction outputDirection)
	{		
		// now using the lookup table above, find the index matching our directions
		Direction[] rotatedOutputs = OUTPUT_TABLE[attachmentFace.ordinal()];
		int size = rotatedOutputs.length;
		for (int i=0; i<size; i++)
		{
			if (rotatedOutputs[i] == outputDirection)
			{
				return i;
			}
		}
		
		return 0;
	}
	
	public static Direction getOutputDirectionFromRelativeHitVec(Vec3d hitVec, Direction directionTowardBlockAttachedTo)
	{
		// we have the relative hit vector, where 0,0,0 is the bottom-left corner of the cube we are placing into
		// and 1,1,1 is the top-right
		// how do we convert this into a direction?
		// we want to ignore the attachment direction and its opposite
		// Direction has a method that converts a vector to a direction
		// we could "flatten" the hit vec's value on the axis of attachment
		// Direction::getFacingFromVector uses 0,0,0 as the center and 1,1,1 and -1,-1,-1 as corners,
		// so we'll want to map our hitvec accordingly
		
		Axis axis = directionTowardBlockAttachedTo.getAxis();
		float x = (float) (axis == Axis.X ? 0F : hitVec.x*2 - 1);
		float y = (float) (axis == Axis.Y ? 0F : hitVec.y*2 - 1);
		float z = (float) (axis == Axis.Z ? 0F : hitVec.z*2 - 1);
		
		return Direction.getFacingFromVector(x, y, z);
	}
}
