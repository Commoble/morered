package commoble.morered.api;

import javax.annotation.Nonnull;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

@FunctionalInterface
public interface WireConnector
{
	/**
	 * Behaviour interface that determines whether a given blockstate can connect to a specific internal face of a WireBlock.
	 * This may be called on either the server or client threads.
	 * @param world The world
	 * @param wirePos The position of a wire block
	 * @param wireState The blockstate of the wire block at the wirePos position
	 * @param wireFace The interior face the wire in question is attached to (e.g. DOWN indicates a wire attached to the floor)
	 * @param directionToWire The direction from the neighbor block to the wire block. Always orthagonal to wireFace
	 * (e.g. if wireFace is DOWN, directionToWire will be NORTH, SOUTH, WEST, or EAST)
	 * @param thisNeighborPos A position adjacent to the wire block, containing the blockstate we are wanting to be connecting the wire to
	 * @param thisNeighborState The neighboring blockstate at thisNeighborPos we are wanting to connect the attached wire to.
	 * If this WireConnector has been registered to a specific Block instance, neighborState will be a blockstate belonging to that block.
	 * @return Whether the wire in the given face at the given position can be connected to the given neighboring block
	 */
	public boolean canConnectToAdjacentWire(@Nonnull IBlockReader world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire, @Nonnull BlockPos thisNeighborPos, @Nonnull BlockState thisNeighborState);
	
	/**
	 * Gets the "expanded power" of the block this wire connector is assigned to.
	 * Internally, wire blocks use 0-31 instead of 0-15 for power storage (outputting half of this value to non-wires)
	 * letting them transmit power twice as far 
	 * @param world The world we're doing power queries in
	 * @param wirePos The position of a wire block
	 * @param wireState The blockstate of the wire block
	 * @param wireFace The attachment face of the subwire we're supplying power to
	 * @param directionToThis The direction from wirePos to neighborPos (e.g. if wirepos is west of neighborpos, this is EAST)
	 * @param thisNeighborPos The position of this neighbor block we're querying for power
	 * @param thisNeighborState The position of this neighbor state we're querying for power
	 * @return a power value in the range [0, 31]
	 */
	public default int getExpandedPower(@Nonnull World world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToThis, @Nonnull BlockPos thisNeighborPos, @Nonnull BlockState thisNeighborState)
	{
		return thisNeighborState.getWeakPower(world, thisNeighborPos, directionToThis) * 2;
	}
}
