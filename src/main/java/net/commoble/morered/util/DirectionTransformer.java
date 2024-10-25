package net.commoble.morered.util;

import java.util.EnumSet;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;

public class DirectionTransformer
{

	/**
	 * Array of arrays of orthagonal directions, by direction index.
	 * Dimension of the array is [6][4].
	 */
	public static final Map<Direction, Direction[]> ORTHAGONALS = Maps.toMap(Lists.newArrayList(Direction.values()),
		dir -> EnumSet.complementOf(EnumSet.of(dir, dir.getOpposite())).toArray(new Direction[4]));
	
	public static final Direction D = Direction.DOWN;
	public static final Direction U = Direction.UP;
	public static final Direction N = Direction.NORTH;
	public static final Direction S = Direction.SOUTH;
	public static final Direction W = Direction.WEST;
	public static final Direction E = Direction.EAST;
	
	public static final Direction[] SAMES ={D,U,N,S,W,E};
	public static final Direction[] OPPOSITES = {U,D,S,N,E,W};
	public static final Direction[] ROTATE_X_DNUS = {N,S,U,D,W,E};
	public static final Direction[] ROTATE_X_DSUN = {S,N,D,U,W,E};
	public static final Direction[] ROTATE_Y_NESW = {D,U,E,W,N,S};
	public static final Direction[] ROTATE_Y_NWSE = {D,U,W,E,S,N};
	public static final Direction[] ROTATE_Z_DWUE = {W,E,N,S,U,D};
	public static final Direction[] ROTATE_Z_DEUW = {E,W,N,S,D,U};
	
	public static final Axis[][] ORTHAGONAL_AXES =
	{
		{Axis.Y,Axis.X},
		{Axis.X,Axis.Z},
		{Axis.Y,Axis.Z}
	};
	
	public static final Direction[][] ORTHAGONAL_ROTATION_TABLE =
	{
		{N,E,S,W},
		{S,E,N,W},
		{U,E,D,W},
		{D,E,U,W},
		{D,S,U,N},
		{U,S,D,N}
	};
	
	/** Indices are direction indices: [from][to][toRotate] **/
	public static final Direction[][][] ROTATION_TABLE =
	{
		// from = down
		{
			SAMES, // to = down
			OPPOSITES, // to = up
			ROTATE_X_DNUS, // down to north
			ROTATE_X_DSUN, // down to south
			ROTATE_Z_DWUE, // down to west
			ROTATE_Z_DEUW // down to east
		},
		// from = up
		{
			OPPOSITES, // up to down
			SAMES, // up to up
			ROTATE_X_DSUN, // up to north
			ROTATE_X_DNUS, // up to south
			ROTATE_Z_DEUW, // up to west
			ROTATE_Z_DWUE, // up to east
		},
		// from = north
		{
			ROTATE_X_DSUN, // north to down
			ROTATE_X_DNUS, // north to up
			SAMES,
			OPPOSITES,
			ROTATE_Y_NWSE, // north to west
			ROTATE_Y_NESW // north to east
		},
		// from = south
		{
			ROTATE_X_DNUS, // south to down
			ROTATE_X_DSUN, // south to up
			OPPOSITES,
			SAMES,
			ROTATE_Y_NESW,
			ROTATE_Y_NWSE
		},
		// from = west
		{
			ROTATE_Z_DEUW, // west to down
			ROTATE_Z_DWUE, // west to up
			ROTATE_Y_NESW, // west to north
			ROTATE_Y_NWSE, // west to south
			SAMES,
			OPPOSITES
		},
		// from = east
		{
			ROTATE_Z_DWUE,
			ROTATE_Z_DEUW,
			ROTATE_Y_NWSE,
			ROTATE_Y_NESW,
			OPPOSITES,
			SAMES
		}
	};
	
	/**
	 * Given two directions and a third, applies the rotation-of-the-first-direction-to-the-second
	 * to the third direction
	 * @param from
	 * @param to
	 * @param toRotate
	 * @return
	 */
	public static Direction getRotatedDirection(Direction from, Direction to, Direction toRotate)
	{
		return ROTATION_TABLE[from.ordinal()][to.ordinal()][toRotate.ordinal()];
	}
	
	/**
	 * 
	 * @param startCenter
	 * @param startSide
	 * @param endSide
	 * @return A Vector3d[4][2], an array of four pairs of vertices in local coordinate space.
	 * array[x][0] are relative to the starting block's center, array[x][1] are relative to the ending block's center
	 */
	public static Vec3[][] getVertexPairs(Direction startSide, Direction endSide)
	{
		Vec3i startSideVec = startSide.getNormal();
		Vec3i endSideVec = endSide.getNormal();
		Direction[] startEdgeDirections = ORTHAGONAL_ROTATION_TABLE[startSide.ordinal()];
		double tubeSideOffset = 4F/16F;	// distance from center of tube to center of side
		double tubeEdgeOffset = 2F/16F; // distance from center of side of tube to center of edge of hole
		double tubeCornerOffset = 2F/16F; // distance from center of edge of tube hole to corner of hole
		Vec3[][] result = new Vec3[4][2];
		
		for (int i=0; i<4; i++)
		{
			// determine the vertices of the origin side
			Direction firstOrthagonal = startEdgeDirections[i];
			Vec3i firstOrthagonalSideVec = firstOrthagonal.getNormal();
			Direction secondOrthagonal = startEdgeDirections[(i+1) % 4];;
			Vec3i secondOrthagonalSideVec = secondOrthagonal.getNormal();
			result[i][0] = getVertexOffset(startSideVec, firstOrthagonalSideVec, secondOrthagonalSideVec, tubeSideOffset, tubeEdgeOffset, tubeCornerOffset);
				
			// now we do the end side
			// each vertex on the origin side has a corresponding vertex on the end side
			// how do we determine these?
			// if the end face is the opposite face of the original, use the same orthagonals (but with the end side face offset)
			
			if (endSide == startSide.getOpposite())
			{
				result[i][1] = getVertexOffset(endSideVec, firstOrthagonalSideVec, secondOrthagonalSideVec, tubeSideOffset, tubeEdgeOffset, tubeCornerOffset);
			}
			else
			{

				
				// if we're rotating from, say, up to west
				// face side is endSide
				// first orthagonal rotates in the opposite direction (west to up)
				// second orthagonal also rotates in the opposite direction
				Direction firstEndOrthagonal = getRotatedDirection(endSide, startSide, firstOrthagonal);
				Direction secondEndOrthagonal = getRotatedDirection(endSide, startSide, secondOrthagonal);
				Vec3i firstEndSideVec = firstEndOrthagonal.getNormal();
				Vec3i secondEndSideVec = secondEndOrthagonal.getNormal();
				result[i][1] = getVertexOffset(endSideVec, firstEndSideVec, secondEndSideVec, tubeSideOffset, tubeEdgeOffset, tubeCornerOffset);
			}
		}
		
		return result;
	}
	
	public static Vec3 getVertexOffset(Vec3i sideVec, Vec3i orthagonalA, Vec3i orthagonalB, double side, double edge, double corner)
	{
		return new Vec3(
			sideVec.getX() * side + orthagonalA.getX() * edge + orthagonalB.getX() * corner,
			sideVec.getY() * side + orthagonalA.getY() * edge + orthagonalB.getY() * corner,
			sideVec.getZ() * side + orthagonalA.getZ() * edge + orthagonalB.getZ() * corner
			);
	}
}
