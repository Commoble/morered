package commoble.morered.wires;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.math.OctahedralGroup;

import commoble.morered.MoreRed;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.util.DirectionHelper;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BundledCableBlockEntity extends BlockEntity
{
	public static final String POWER = "power";
	public static final String CHANNEL_FLAGS = "channel_flags";
	public static final String POWER_BYTES = "power_bytes";
	
	/**
	 * Table of power values
	 * The first index is the direction ordinal of the face of the wire where the power is stored (dunswe order)
	 * The second index is the power channel ordinal (dye color order)
	 */
	protected byte[][] power = new byte[6][16];
	
	protected Map<Direction, ChanneledPowerSupplier> sidedPowerSuppliers = Util.make(new EnumMap<>(Direction.class), map ->
	{
		for (int i=0; i<6; i++)
		{
			Direction dir = Direction.from3DDataValue(i);
			map.put(dir,  new SidedPowerSupplier(dir));
		}
	});
	
	public BundledCableBlockEntity(BlockEntityType<? extends BundledCableBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}

	public BundledCableBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().bundledNetworkCableBeType.get(), pos, state);
	}

	public int getPower(int side, int channel)
	{
		return this.power[side][channel];
	}
	
	/**
	 * Returns the power for all channels on the given side
	 * @param side Directional ordinal
	 * @return The power array for all channels on the given side (be sure to clone before storing)
	 */
	public byte[] getPowerChannels(int side)
	{
		return this.power[side];
	}
	
	/**
	 * Sets the power value and marks the block and TE updated if the power changed on the server
	 * @param side The attachment side to set the power of, in the range [0,5]
	 * @param channel The channel to set the power of, in the range [0,15]
	 * @param newPower The power value to set the power of, in the range [0,31]
	 * @return True if the power changed, false otherwise
	 */
	public boolean setPower(int side, int channel, int newPower)
	{
		int oldPower = this.power[side][channel];
		this.power[side][channel] = (byte)newPower;
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
	
	public void setPowerRaw(byte[][] newPower)
	{
		this.power= newPower;
	}

	public ChanneledPowerSupplier getChanneledPower(Direction side)
	{
		return side == null ? null : this.sidedPowerSuppliers.get(side);
	}

	// called when TE is serialized to hard drive or whatever
	// defaults to this.writeInternal()
	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.saveAdditional(compound, registries);
		this.writeServerData(compound);
	}
	
	// reads the data written by write, called when TE is loaded from hard drive
	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries)
	{
		super.loadAdditional(compound, registries);
		this.readServerData(compound);
	}
	
	public CompoundTag writeServerData(CompoundTag compound)
	{
		this.writeAllPowerData(compound);
		return compound;
	}
	
	public void readServerData(CompoundTag compound)
	{
		this.readUpdatedPowerData(compound);
	}
	
	public CompoundTag writeAllPowerData(CompoundTag compound)
	{
		CompoundTag powerData = new CompoundTag();
		boolean wrotePower = false;
		// writes all positive power values
		for (byte sideIndex=0; sideIndex<6; sideIndex++)
		{
			OctahedralGroup normalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM).inverse();
			Direction denormalSide = Direction.from3DDataValue(sideIndex);
			Direction normalizedSide = normalizer.rotate(denormalSide);
			ByteArrayList bytes = new ByteArrayList();
			short channelFlags = 0;
			for (byte channel = 0; channel < 16; channel++)
			{
				int powerValue = this.getPower(sideIndex, channel);
				if (powerValue > 0)
				{
					bytes.add((byte)powerValue);
					channelFlags += (1 << channel);
				}
			}
			if (bytes.size() > 0)
			{
				CompoundTag sidedPower = new CompoundTag();
				sidedPower.putShort(CHANNEL_FLAGS, channelFlags);
				sidedPower.putByteArray(POWER_BYTES, bytes.toByteArray());
				powerData.put(normalizedSide.getName(), sidedPower);
				wrotePower = true;
			}
		}
		
		if (wrotePower)
			compound.put(POWER, powerData);

		return compound;
		
	}
	
	public void readUpdatedPowerData(CompoundTag compound)
	{
		CompoundTag powerData = compound.getCompound(POWER);
		if (powerData == null)
			return;
		
		for (int sideIndex=0; sideIndex<6; sideIndex++)
		{

			OctahedralGroup denormalizer = this.getBlockState().getValue(AbstractWireBlock.TRANSFORM);
			Direction normalSide = Direction.from3DDataValue(sideIndex);
			Direction denormalizedSide = denormalizer.rotate(normalSide);
			int denormalizedSideIndex = denormalizedSide.ordinal();
			
			CompoundTag sidedPower = powerData.getCompound(normalSide.getName());
			if (sidedPower == null)
				continue;

			int channels = sidedPower.getShort(CHANNEL_FLAGS);
			byte[] powerValues = sidedPower.getByteArray(POWER_BYTES);
			int powerByteCount = powerValues.length;
			int powerByteIndex = 0;
			
			channelLoop:
			for (int channel = 0; channel < 16; channel++)
			{
				// safety rail to avoid index out of bounds crashes
				if (powerByteIndex >= powerByteCount)
					break channelLoop;
				
				int channelFlag = (1 << channel);
				if ((channels & channelFlag) != 0)
				{
					this.power[denormalizedSideIndex][channel] = powerValues[powerByteIndex++];
				}
			}
		}
	}
	
	protected class SidedPowerSupplier implements ChanneledPowerSupplier
	{
		private final Direction side;
		
		public SidedPowerSupplier(Direction side)
		{
			this.side = side;
		}

		@Override
		public int getPowerOnChannel(Level world, BlockPos wirePos, BlockState wireState, @Nullable Direction wireFace, int channel)
		{
			BundledCableBlockEntity cable = BundledCableBlockEntity.this;
			BlockState state = cable.getBlockState();
			Block block = state.getBlock();
			if (!(block instanceof BundledCableBlock))
				return 0;

			// check the power of the wire attached on the capability side first
			int sideIndex = this.side.ordinal();
			if (state.getValue(AbstractWireBlock.INTERIOR_FACES[sideIndex]))
				return cable.getPower(sideIndex, channel);
			
			// otherwise, if the querier needs a specific wire face, get that power
			if (wireFace != null)
				return cable.getPower(wireFace.ordinal(), channel);
			
			// otherwise, get the strongest wire power among the four subwires adjacent to the capability side
			int output = 0;
			for (int subSide = 0; subSide < 4; subSide++)
			{
				int actualSubSide = DirectionHelper.uncompressSecondSide(sideIndex, subSide);
				if (state.getValue(AbstractWireBlock.INTERIOR_FACES[actualSubSide]))
					output = Math.max(output, cable.power[actualSubSide][channel]);
			}
			
			return output;
		}
		
	}

}
