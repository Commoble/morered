package net.commoble.morered.wires;

import com.mojang.math.OctahedralGroup;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WireBlockEntity extends BlockEntity
{
	public static final String POWER = "power";
	protected int[] power = new int[6];
	
	public WireBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().wireBeType.get(), pos, state);
	}
	
	public WireBlockEntity(BlockEntityType<? extends WireBlockEntity> type, BlockPos pos, BlockState state)
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
		int[] normalizedPower = compound.getIntArray(POWER); // returns 0-length array if field not present
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
	}

	// called on server to get the data to send to clients when chunk is loaded for client
	// defaults to this.writeInternal()
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries)
	{
		CompoundTag compound = super.getUpdateTag(registries); // empty tag
		this.saveAdditional(compound, registries);
		return compound;
	}

	// called on server when world.notifyBlockUpdate is invoked at this TE's position
	// generates the packet to send to nearby clients
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket()
	{
		return ClientboundBlockEntityDataPacket.create(this); // calls getUpdateTag
	}

	// called on client to read the packet sent by getUpdatePacket
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries)
	{
		super.onDataPacket(net, pkt, registries);	// calls load()
		BlockState state = this.getBlockState();
		this.level.sendBlockUpdated(this.worldPosition, state, state, 8);
	}
}
