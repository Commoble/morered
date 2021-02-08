package commoble.morered.client;

import commoble.morered.util.DirectionHelper;
import commoble.morered.wires.Edge;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;

public class EdgeRotation
{
	/**
	 * Rotating the down-west edge by EDGE_ROTATIONS[i] results in Edge.values()[i]
	 */
	public static final ModelRotation[] EDGE_ROTATIONS =
	{
		ModelRotation.X0_Y90,
		ModelRotation.X0_Y270,
		ModelRotation.X0_Y0,
		ModelRotation.X0_Y180,
		
		ModelRotation.X180_Y90,
		ModelRotation.X180_Y270,
		ModelRotation.X180_Y0,
		ModelRotation.X180_Y180,
		
		ModelRotation.X90_Y90,
		ModelRotation.X90_Y180,
		ModelRotation.X90_Y0,
		ModelRotation.X90_Y270
	};
	
	public static Edge getRotatedEdge(Edge oldEdge, Matrix4f rotation)
	{
		Direction sideA = oldEdge.sideA;
		Direction sideB = oldEdge.sideB;
		Direction newSideA = Direction.rotateFace(rotation, sideA);
		Direction newSideB = Direction.rotateFace(rotation, sideB);
		if (newSideA == newSideB || newSideA == newSideB.getOpposite())
			return oldEdge;
		
		int compressedB = DirectionHelper.getCompressedSecondSide(newSideA.ordinal(), newSideB.ordinal());
		return Edge.EDGES_BY_RELEVANT_DIRECTION[newSideA.ordinal()][compressedB];
	}
}
