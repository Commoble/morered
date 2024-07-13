package commoble.morered.bitwise_logic;

import commoble.morered.MoreRed;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.plate_blocks.PlateBlock;
import commoble.morered.plate_blocks.PlateBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ChanneledPowerStorageBlockEntity extends BlockEntity
{
	public static final String POWER = "power";
	
	protected byte[] power = new byte[16];

	public static ChanneledPowerStorageBlockEntity create(BlockPos pos, BlockState state)
	{
		return new ChanneledPowerStorageBlockEntity(MoreRed.get().bitwiseLogicGateBeType.get(), pos, state);
	}
	
	public ChanneledPowerStorageBlockEntity(BlockEntityType<? extends ChanneledPowerStorageBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public ChanneledPowerStorageBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().bitwiseLogicGateBeType.get(), pos, state);
	}
	
	public ChanneledPowerSupplier getChanneledPower(Direction side)
	{
		return side == PlateBlockStateProperties.getOutputDirection(this.getBlockState())
			? this::getPowerOnChannel
			: null;
	}

	public int getPowerOnChannel(Level level, BlockPos wirePos, BlockState wireState, Direction wireFace, int channel)
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
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		compound.putByteArray(POWER, this.power.clone());
	}
	
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
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
