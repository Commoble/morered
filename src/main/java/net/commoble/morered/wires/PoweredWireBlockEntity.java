package net.commoble.morered.wires;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PoweredWireBlockEntity extends WireBlockEntity
{
	public static final String POWER = "power";
	protected int[] power = new int[6];

	public PoweredWireBlockEntity(BlockPos pos, BlockState state)
	{
		super(MoreRed.get().poweredWireBeType.get(), pos, state);
	}
	public PoweredWireBlockEntity(BlockEntityType<? extends WireBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}


	/**
	 * Sets the power on a given interior face
	 * @param side The ordinal of the attachment direction of the subwire to set the power of
	 * @param newPower The power value to set the subwire's power to
	 * @return True if the power changed, false if it didn't
	 */
	public boolean setPower(int side, int newPower)
	{
		int oldPower = this.power[side];
		this.power[side] = newPower;
		if (oldPower != newPower)
		{
			this.setChanged();
			BlockState state = this.getBlockState();
			// what do the block flags do here?
			// client world: only flag 8 (bit 4) is checked (rerender on main thread)
			// server world: none of the flags are checked
			this.level.sendBlockUpdated(this.worldPosition, state, state, 0);
			return true;
		}
		return false;
	}
	
	/**
	 * Sets a power array without causing updates
	 * @param power array of power values (be wary of reference-type problems)
	 */
	public void setPowerRaw(int[] power)
	{
		this.power = power;
	}
	
	@Override
	public void setConnectionsWithServerUpdate(long connections)
	{
		long oldConnections = this.connections;
		super.setConnectionsWithServerUpdate(connections);
		for (int i=0; i<6; i++)
		{
			if ((connections & (1 << i)) == 0)
			{
				this.setPower(i, 0);
			}
		}
		if (connections != oldConnections)
		{
			this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
		}
	}
	/**
	 * Sets the power on a given interior face
	 * @param direction The attachment direction of the subwire to set the power of
	 * @param newPower The power value to set the subwire's power to
	 * @return True if the power changed, false if it didn't
	 */
	public boolean setPower(Direction direction, int newPower)
	{
		return this.setPower(direction.ordinal(), newPower);
	}
	
	public int getPower(Direction direction)
	{
		return this.getPower(direction.ordinal());
	}
	
	public int getPower(int side)
	{
		return this.power[side];
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		// normalize power array based on structure transform
		int[] normalizedPower = new int[6];
		OctahedralGroup normalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM).inverse();
		for (int i=0; i<6; i++)
		{
			int sidedPower = this.power[i];
			int normalizedIndex = normalizer.rotate(Direction.from3DDataValue(i)).ordinal();
			normalizedPower[normalizedIndex] = sidedPower;
		}
		compound.putIntArray(POWER, normalizedPower);
	}
	
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
		compound.getIntArray(POWER).ifPresent(normalizedPower -> {
			if (normalizedPower.length == 6)
			{
				// denormalize power array based on structure transform
				int[] denormalizedPower = new int[6];
				OctahedralGroup denormalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM);
				for (int i=0; i<6; i++)
				{
					int sidedPower = normalizedPower[i];
					int denormalizedIndex = denormalizer.rotate(Direction.from3DDataValue(i)).ordinal();
					denormalizedPower[denormalizedIndex] = sidedPower;
				}
				this.power = denormalizedPower;
			}
		});
	}
}
