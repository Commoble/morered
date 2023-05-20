package commoble.morered.client;

import commoble.morered.util.DirectionHelper;
import commoble.morered.wires.Edge;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import com.mojang.math.Matrix4f;

public class EdgeRotation
{
	/**
	 * Rotating the down-west edge by EDGE_ROTATIONS[i] results in Edge.values()[i]
	 */
	public static final BlockModelRotation[] EDGE_ROTATIONS =
	{
		BlockModelRotation.X0_Y90,
		BlockModelRotation.X0_Y270,
		BlockModelRotation.X0_Y0,
		BlockModelRotation.X0_Y180,
		
		BlockModelRotation.X180_Y90,
		BlockModelRotation.X180_Y270,
		BlockModelRotation.X180_Y0,
		BlockModelRotation.X180_Y180,
		
		BlockModelRotation.X90_Y90,
		BlockModelRotation.X90_Y180,
		BlockModelRotation.X90_Y0,
		BlockModelRotation.X90_Y270
	};
	
	public static Edge getRotatedEdge(Edge oldEdge, Matrix4f rotation)
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
