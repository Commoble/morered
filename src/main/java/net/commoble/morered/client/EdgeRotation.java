package net.commoble.morered.client;

import org.joml.Matrix4fc;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.util.DirectionHelper;
import net.commoble.morered.wires.Edge;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;

public class EdgeRotation
{
	/**
	 * Rotating the down-west edge by EDGE_ROTATIONS[i] results in Edge.values()[i]
	 */
	public static final BlockModelRotation[] EDGE_ROTATIONS =
	{
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_90),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_270),
		BlockModelRotation.IDENTITY,
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_180),
		
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_90.compose(OctahedralGroup.BLOCK_ROT_Z_90)),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_270.compose(OctahedralGroup.BLOCK_ROT_Z_90)),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_X_180),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_X_180.compose(OctahedralGroup.BLOCK_ROT_Y_180)),
		
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_X_270),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_90.compose(OctahedralGroup.BLOCK_ROT_X_270)),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_X_90),
		BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_270.compose(OctahedralGroup.BLOCK_ROT_X_90))
	};
	
	public static Edge getRotatedEdge(Edge oldEdge, Matrix4fc rotation)
	{
		Direction sideA = oldEdge.sideA;
		Direction sideB = oldEdge.sideB;
		Direction newSideA = Direction.rotate(rotation, sideA);
		Direction newSideB = Direction.rotate(rotation, sideB);
		if (newSideA == newSideB || newSideA == newSideB.getOpposite())
			return oldEdge;
		
		int compressedB = DirectionHelper.getCompressedSecondSide(newSideA.ordinal(), newSideB.ordinal());
		return Edge.EDGES_BY_RELEVANT_DIRECTION[newSideA.ordinal()][compressedB];
	}
}
