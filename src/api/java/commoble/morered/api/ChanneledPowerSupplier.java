package commoble.morered.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ChanneledPowerSupplier
{
	
	/**
	 * Gets the per-channel power of the block this wire connector is assigned to.
	 * Internally, cable blocks use 0-31 for power storage (redstone-capable cables output half of this value to non-wires).
	 * @param world The world we're doing power queries in
	 * @param wirePos The position of a wire block
	 * @param wireState The blockstate of the wire block
	 * @param wireFace The attachment face of the subwire we're supplying power to. Can be null if e.g. querier isn't a wire-like block.
	 * @param channel The channel index we're querying power for. Currently values [0,15] are supported (equivalent to dyecolor ordinals)
	 * @return a power value in the range [0, 31]
	 */
	public int getPowerOnChannel(@Nonnull World world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nullable Direction wireFace, int channel);
}
