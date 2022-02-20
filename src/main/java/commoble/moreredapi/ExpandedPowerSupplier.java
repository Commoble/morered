package commoble.moreredapi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

public interface ExpandedPowerSupplier {

    /**
     * Gets the "expanded power" of the block this wire connector is assigned to.
     * Internally, wire blocks use 0-31 instead of 0-15 for power storage (outputting half of this value to non-wires)
     * letting them transmit power twice as far
     *
     * @param world           The world we're doing power queries in
     * @param thisPos         The position of this neighbor block we're querying for power
     * @param thisState       The position of this neighbor state we're querying for power
     * @param wirePos         The position of a neighboring wire block (not this block)
     * @param wireState       The blockstate of the wire block (not this block)
     * @param wireFace        The attachment face of the subwire we're supplying power to
     * @param directionToThis The direction from wirePos to neighborPos (e.g. if wirepos is west of neighborpos, this
     *                        is EAST)
     * @return a power value in the range [0, 31]
     */
    int getExpandedPower(@Nonnull Level world, @Nonnull BlockPos thisPos, @Nonnull BlockState thisState,
                         @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nonnull Direction wireFace
            , @Nonnull Direction directionToThis);
}
