package net.commoble.morered.api;

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

@FunctionalInterface
public interface WireConnector
{
	/**
	 * Behaviour interface that determines whether a given blockstate can connect to a specific internal face of a WireBlock.
	 * This may be called on either the server or client threads.
	 * @param world The world
	 * @param thisPos A position adjacent to the wire block, containing the blockstate we are wanting to be connecting the wire to
	 * @param thisState The blockstate at thisPos we are wanting to connect the attached wire to.
	 * If this WireConnector has been registered to a specific Block instance, neighborState will be a blockstate belonging to that block.
	 * @param wirePos The position of a wire block
	 * @param wireState The blockstate of the wire block at the wirePos position
	 * @param wireFace The interior face the wire in question is attached to (e.g. DOWN indicates a wire attached to the floor)
	 * @param directionToWire The direction from the neighbor block to the wire block.
	 * @return Whether the wire in the given face at the given position can be connected to the given neighboring block
	 */
	public boolean canConnectToAdjacentWire(@Nonnull BlockGetter world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace, @Nonnull Direction directionToWire);

}
