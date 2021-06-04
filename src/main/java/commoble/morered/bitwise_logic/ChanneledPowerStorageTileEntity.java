package commoble.morered.bitwise_logic;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class ChanneledPowerStorageTileEntity extends TileEntity implements ChanneledPowerSupplier
{
	public static final String POWER = "power";
	
	protected byte[] power = new byte[16];
	protected LazyOptional<ChanneledPowerSupplier> powerHolder = LazyOptional.of(() -> this);

	public ChanneledPowerStorageTileEntity()
	{
		super(TileEntityRegistrar.BITWISE_LOGIC_PLATE.get());
	}
	
	public ChanneledPowerStorageTileEntity(TileEntityType<? extends ChanneledPowerStorageTileEntity> type)
	{
		super(type);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY)
			return side == PlateBlockStateProperties.getOutputDirection(this.getBlockState()) ? (LazyOptional<T>) this.powerHolder : LazyOptional.empty();
		return super.getCapability(cap, side);
	}
	
	@Override
	protected void invalidateCaps()
	{
		super.invalidateCaps();
		this.powerHolder.invalidate();
	}
	
	// invalidate and replace capabilities with fresh ones
	// needed if we change the blockstate to one with a different output side
	public void resetCapabilities()
	{
		LazyOptional<ChanneledPowerSupplier> oldPowerHolder = this.powerHolder;
		this.powerHolder = LazyOptional.of(() -> this);
		oldPowerHolder.invalidate();
	}

	@Override
	public int getPowerOnChannel(World world, BlockPos wirePos, BlockState wireState, Direction wireFace, int channel)
	{
		BlockState thisState = this.getBlockState();
		return wireFace == null || (thisState.getBlock() instanceof PlateBlock && wireFace == thisState.getValue(PlateBlock.ATTACHMENT_DIRECTION))
			? this.getPower(channel)
			: 0;
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
	public void load(BlockState state, CompoundNBT compound)
	{
		super.load(state, compound);
		byte[] newPower = compound.getByteArray(POWER);
		if (newPower.length == 16)
			this.power = newPower.clone();
	}
	
	public void updatePower()
	{
		byte[] powers = this.getStrongestNeighborPower();
		this.setPower(powers);
	}

	public byte[] getStrongestNeighborPower()
	{
		return new byte[16];
	}

}
