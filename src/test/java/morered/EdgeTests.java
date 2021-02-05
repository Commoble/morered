package morered;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import commoble.morered.client.EdgeRotation;
import commoble.morered.redwire.Edge;
import net.minecraft.client.renderer.model.ModelRotation;

public class EdgeTests
{
	// ensure that rotating the down-west edge by the EdgeRotations rotations
	// results in each rotation matching the edge of the same ordinal
	@Test
	void testEdgeRotations()
	{
		Edge baseEdge = Edge.DOWN_WEST;
		Edge[] expected = Edge.values();
		Edge[] actual = new Edge[12];
		for (int i=0; i<12; i++)
		{
			ModelRotation rotation = EdgeRotation.EDGE_ROTATIONS[i];
			actual[i] = EdgeRotation.getRotatedEdge(baseEdge, rotation.getRotation().getMatrix());
		}
		
		Assertions.assertArrayEquals(expected, actual);
	}
}
