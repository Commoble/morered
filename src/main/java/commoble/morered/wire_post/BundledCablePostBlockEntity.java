package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BundledCablePostBlockEntity extends WirePostBlockEntity
{
	public static final String POWER = "power";
	
	protected byte[] power = new byte[16];

	public static Optional<BundledCablePostBlockEntity> getCablePost(BlockGetter world, BlockPos pos)
	{
		BlockEntity te = world.getBlockEntity(pos);
		return Optional.ofNullable(te instanceof BundledCablePostBlockEntity ? (BundledCablePostBlockEntity)te : null);
	}
	
	public BundledCablePostBlockEntity(BlockEntityType<? extends BundledCablePostBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public BundledCablePostBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().bundledCablePostBeType.get(), pos, state);
	}
	
	public int getPower(int channel)
	{
		return this.power[channel];
	}
	
	/**
	 * 
	 * @param newPowers An array of 16 values in the range 0-31. This will be copied.
	 * @return true if any values changed, false otherwise
	 */
	public boolean setPower(byte[] newPowers)
	{
		boolean updated = false;
		for (int i=0; i<16; i++)
		{
			byte newPower = newPowers[i];
			byte oldPower = this.power[i];
			if (newPower != oldPower)
			{
				this.power[i] = newPower;
				updated = true;
			}
		}
		if (updated && !this.level.isClientSide)
		{
			this.setChanged();
			this.notifyConnections();
			return true;
		}
		return false;
	}
	
	/**
	 * Sets the power value and marks the block and TE updated if the power changed on the server
	 * use setPower(int[]) where possible to minimize neighbor updates
	 * @param channel The channel to set the power of, in the range [0,15]
	 * @param newPower The power value to set the power of, in the range [0,31]
	 * @return True if the power changed, false otherwise
	 */
	public boolean setPower(int channel, int newPower)
	{
		int oldPower = this.power[channel];
		this.power[channel] = (byte)newPower;
		if (oldPower != newPower)
		{
			if (!this.level.isClientSide)
			{
				this.setChanged();
				this.notifyConnections();
			}
			return true;
		}
		return false;
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		compound.putByteArray(POWER, this.power.clone());
	}
	
	@Override
	protected void readCommonData(CompoundTag compound)
	{
		super.readCommonData(compound);
		byte[] newPower = compound.getByteArray(POWER);
		if (newPower.length == 16)
			this.power = newPower.clone();
	}
	
	public void updatePower()
	{
		Level world = this.getLevel();
		Set<BlockPos> remoteConnectionPositions = this.getRemoteConnections();
		List<BundledCablePostBlockEntity> remoteConnections = new ArrayList<>();
		for (BlockPos pos : remoteConnectionPositions)
		{
			BundledCablePostBlockEntity.getCablePost(world, pos).ifPresent(te -> remoteConnections.add(te));
		}
		byte[] powers = this.getStrongestNeighborPower();
		for (int channel=0; channel<16; channel++)
		{
			for (BundledCablePostBlockEntity te : remoteConnections)
			{
				powers[channel] = (byte) Math.max(powers[channel], te.getPower(channel)-1);
			}
		}
		this.setPower(powers);
	}

	public byte[] getStrongestNeighborPower()
	{
		return new byte[16];
	}
	
}
