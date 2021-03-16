package commoble.morered.wire_post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import commoble.morered.TileEntityRegistrar;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BundledCablePostTileEntity extends WirePostTileEntity
{
	public static final String POWER = "power";
	
	protected byte[] power = new byte[16];

	public static Optional<BundledCablePostTileEntity> getCablePost(IBlockReader world, BlockPos pos)
	{
		TileEntity te = world.getBlockEntity(pos);
		return Optional.ofNullable(te instanceof BundledCablePostTileEntity ? (BundledCablePostTileEntity)te : null);
	}
	
	public BundledCablePostTileEntity(TileEntityType<? extends BundledCablePostTileEntity> type)
	{
		super(type);
	}
	
	public BundledCablePostTileEntity()
	{
		super(TileEntityRegistrar.BUNDLED_CABLE_POST.get());
	}
	
	public int getPower(int channel)
	{
		return this.power[channel];
	}
	
	/**
	 * 
	 * @param newPower An array of 16 values in the range 0-31. This will be copied.
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
	 * @param side The attachment side to set the power of, in the range [0,5]
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
	public CompoundNBT save(CompoundNBT compound)
	{
		super.save(compound);
		compound.putByteArray(POWER, this.power.clone());
		return compound;
	}
	
	@Override
	protected void readCommonData(CompoundNBT compound)
	{
		super.readCommonData(compound);
		byte[] newPower = compound.getByteArray(POWER);
		if (newPower.length == 16)
			this.power = newPower.clone();
	}
	
	public void updatePower()
	{
		World world = this.getLevel();
		Set<BlockPos> remoteConnectionPositions = this.getRemoteConnections();
		List<BundledCablePostTileEntity> remoteConnections = new ArrayList<>();
		for (BlockPos pos : remoteConnectionPositions)
		{
			BundledCablePostTileEntity.getCablePost(world, pos).ifPresent(te -> remoteConnections.add(te));
		}
		byte[] powers = this.getStrongestNeighborPower();
		for (int channel=0; channel<16; channel++)
		{
			for (BundledCablePostTileEntity te : remoteConnections)
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
