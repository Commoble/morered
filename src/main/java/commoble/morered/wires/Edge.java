package commoble.morered.wires;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A cube has 12 edges.
 * If there are two wire block wires attached to two exterior faces of a given cube,
 * and if the block diagonally adjacent to that cube (touching the attached wire blocks' cubespaces) is empty or a
 * wire block
 * then we want to make sure a wire block exists in that space, and have it render a small segment of wire going
 * around the convex edge.
 * <p>
 * A wire block should render an edge if and only if both of the adjacent blocks in the two given directions are wire
 * blocks,
 * and if both wire blocks have a wire attached to the relevant face of the central cube.
 */
public enum Edge {

    DOWN_NORTH(Direction.DOWN, Direction.NORTH),
    DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
    DOWN_WEST(Direction.DOWN, Direction.WEST),
    DOWN_EAST(Direction.DOWN, Direction.EAST),
    UP_NORTH(Direction.UP, Direction.NORTH),
    UP_SOUTH(Direction.UP, Direction.SOUTH),
    UP_WEST(Direction.UP, Direction.WEST),
    UP_EAST(Direction.UP, Direction.EAST),
    NORTH_WEST(Direction.NORTH, Direction.WEST),
    NORTH_EAST(Direction.NORTH, Direction.EAST),
    SOUTH_WEST(Direction.SOUTH, Direction.WEST),
    SOUTH_EAST(Direction.SOUTH, Direction.EAST);

    public static final Edge[][] EDGES_BY_RELEVANT_DIRECTION =
            {
                    {DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST}, // edges with down
                    {UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST}, // up
                    {DOWN_NORTH, UP_NORTH, NORTH_WEST, NORTH_EAST}, // north
                    {DOWN_SOUTH, UP_SOUTH, SOUTH_WEST, SOUTH_EAST}, // south
                    {DOWN_WEST, UP_WEST, NORTH_WEST, SOUTH_WEST}, // west
                    {DOWN_EAST, UP_EAST, NORTH_EAST, SOUTH_EAST} // east
            };

    public final Direction sideA;
    public final Direction sideB;

    Edge(Direction sideA, Direction sideB) {
        this.sideA = sideA;
        this.sideB = sideB;
    }

    /**
     * @param world           world
     * @param pos             The position of a wire or air block
     * @param centerWireBlock The wire block we want to render an edge for (connecting two neighbor wires)
     * @return true if this edge should be rendered for this block
     */
    public boolean shouldEdgeRender(BlockGetter world, BlockPos pos, AbstractWireBlock centerWireBlock) {
        // suppose we're checking the edge for down-north
        // we want to render this edge if the block below us is a wire block attached to north,
        // and if the block to our north is a wire block attached to down
        // we also want to make sure we don't render an edge if THIS wire block is attached on either of those faces
        // can we assume this is true?
        // if both of the neighboring blocks are wire blocks, then they have non-solid faces, so this is true
        BooleanProperty propB = AbstractWireBlock.INTERIOR_FACES[this.sideB.ordinal()];
        BlockState neighborStateA = world.getBlockState(pos.relative(this.sideA));
        if (neighborStateA.getBlock() == centerWireBlock && neighborStateA.getValue(propB)) {
            BooleanProperty propA = AbstractWireBlock.INTERIOR_FACES[this.sideA.ordinal()];
            BlockState neighborStateB = world.getBlockState(pos.relative(this.sideB));
            return neighborStateB.getBlock() == centerWireBlock && neighborStateB.getValue(propA);
        }
        return false;
    }
}
