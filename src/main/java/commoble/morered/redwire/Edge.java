package commoble.morered.redwire;

import net.minecraft.block.BlockState;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

/**
 * A cube has 12 edges.
 * If there are two wire block wires attached to two exterior faces of a given cube,
 * and if the block diagonally adjacent to that cube (touching the attached wire blocks' cubespaces) is empty or a wire block
 * then we want to make sure a wire block exists in that space, and have it render a small segment of wire going around the convex edge.
 * 
 * A wire block should render an edge if and only if both of the adjacent blocks in the two given directions are wire blocks,
 * and if both wire blocks have a wire attached to the relevant face of the central cube.
 */
public class Edge
{
	public static final Edge[] EDGES =
	{
		new Edge(Direction.DOWN, Direction.NORTH),
		new Edge(Direction.DOWN, Direction.SOUTH),
		new Edge(Direction.DOWN, Direction.WEST),
		new Edge(Direction.DOWN, Direction.EAST),
		new Edge(Direction.UP, Direction.NORTH),
		new Edge(Direction.UP, Direction.SOUTH),
		new Edge(Direction.UP, Direction.WEST),
		new Edge(Direction.UP, Direction.EAST),
		new Edge(Direction.NORTH, Direction.WEST),
		new Edge(Direction.NORTH, Direction.EAST),
		new Edge(Direction.SOUTH, Direction.WEST),
		new Edge(Direction.SOUTH, Direction.EAST),
	};
	
//	public static final Edge[][] EDGES_BY_RELEVANT_DIRECTION =
//	{
//		{EDGES[0], EDGES[1], EDGES[2], EDGES[3]}, // edges with down
//		{EDGES[4], EDGES[5], EDGES[6], EDGES[7]}, // up
//		{EDGES[0], EDGES[4], EDGES[8], EDGES[9]}, // north
//		{EDGES[1], EDGES[5], EDGES[10], EDGES[11]}, // south
//		{EDGES[2], EDGES[6], EDGES[8], EDGES[10]}, // west
//		{EDGES[3], EDGES[7], EDGES[9], EDGES[11]}
//	};
	
	private final Direction sideA;
	private final Direction sideB;
	
	public Edge(Direction sideA, Direction sideB)
	{
		this.sideA = sideA;
		this.sideB = sideB;
	}
	
	/**
	 * @param betweenState a state from a WireBlock
	 * @param world world
	 * @param pos The position of a wire or air block
	 * @return true if this edge should be rendered for this block
	 */
	public boolean shouldEdgeRender(IBlockReader world, BlockPos pos)
	{
		// suppose we're checking the edge for down-north
		// we want to render this edge if the block below us is a wire block attached to north,
		// and if the block to our north is a wire block attached to down
		// we also want to make sure we don't render an edge if THIS wire block is attached on either of those faces
		// can we assume this is true?
		// if both of the neighboring blocks are wire blocks, then they have non-solid faces, so this is true
		BooleanProperty propB = WireBlock.INTERIOR_FACES[this.sideB.ordinal()];
		BlockState neighborStateA = world.getBlockState(pos.offset(this.sideA));
		if (neighborStateA.getBlock() instanceof WireBlock && neighborStateA.get(propB))
		{
			BooleanProperty propA = WireBlock.INTERIOR_FACES[this.sideA.ordinal()];
			BlockState neighborStateB = world.getBlockState(pos.offset(this.sideB));
			return neighborStateB.getBlock() instanceof WireBlock && neighborStateB.get(propA);
		}
		return false;
	}
}
