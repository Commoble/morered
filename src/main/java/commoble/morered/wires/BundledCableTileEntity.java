package commoble.morered.wires;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.util.DirectionHelper;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.common.util.LazyOptional;

public class BundledCableTileEntity extends TileEntity
{
	public static final String POWER = "power";
	public static final String CHANNEL_FLAGS = "channel_flags";
	public static final String POWER_BYTES = "power_bytes";
	
	protected static byte[][] makeEmptyPowerData()
	{
		return new byte[6][];
	}
	
	/**
	 * Table of power values
	 * The first index is the direction ordinal of the face of the wire where the power is stored (dunswe order)
	 * The second index is the power channel ordinal (dye color order)
	 */
	protected byte[][] power = new byte[6][16];
	
	/**
	 * The update packet is built as data is updated.
	 * When it's time to send the update packet, the built nbt is sent, and is replaced in this field with a new compound.
	 * This minimizes the size of packets that need to be sent to clients.
	 */
	protected byte[][] powerUpdateBuffer = makeEmptyPowerData();
	
	protected Map<Direction, LazyOptional<ChanneledPowerSupplier>> sidedPowerSuppliers = Util.make(new EnumMap<>(Direction.class), map ->
	{
		for (int i=0; i<6; i++)
		{
			Direction dir = Direction.byIndex(i);
			map.put(dir, LazyOptional.of(() -> new SidedPowerSupplier(dir)));
		}
	});

	public BundledCableTileEntity()
	{
		super(TileEntityRegistrar.BUNDLED_NETWORK_CABLE.get());
	}
	
	@Override
	protected void invalidateCaps()
	{
		super.invalidateCaps();
		this.sidedPowerSuppliers.forEach((dir, holder) -> holder.invalidate());
	}

	protected byte[][] getAndClearUpdateBuffer()
	{
		byte[][] buffer = this.powerUpdateBuffer;
		this.powerUpdateBuffer = makeEmptyPowerData();
		return buffer;
	}

	public int getPower(int side, int channel)
	{
		return this.power[side][channel];
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
			if (!this.world.isRemote)
			{
				byte[] sidedPower = this.getOrCreateUpdateBufferForSide(side);
				sidedPower[channel] = (byte)newPower;
				BlockState state = this.getBlockState();
				this.world.notifyBlockUpdate(this.pos, state, state, BlockFlags.DEFAULT);
				this.markDirty();
			}
			return true;
		}
		return false;
	}
	
	protected byte[] getOrCreateUpdateBufferForSide(int side)
	{
		byte[] sidedPower = this.powerUpdateBuffer[side];
		if (sidedPower == null)
		{
			sidedPower = new byte[16];
			this.powerUpdateBuffer[side] = sidedPower;
		}
		return sidedPower;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY)
			return (LazyOptional<T>) this.sidedPowerSuppliers.get(side);
		return super.getCapability(cap, side);
	}

	// called when TE is serialized to hard drive or whatever
	// defaults to this.writeInternal()
	@Override
	public CompoundNBT write(CompoundNBT compound)
	{
		super.write(compound);
		this.writeCommonData(compound);
		return compound;
	}
	
	// reads the data written by write, called when TE is loaded from hard drive
	@Override
	public void read(BlockState state, CompoundNBT compound)
	{
		super.read(state, compound);
		this.readCommonData(compound);
	}

	// called on server to get the data to send to clients when chunk is loaded for client
	// defaults to this.writeInternal()
	@Override
	public CompoundNBT getUpdateTag()
	{
		CompoundNBT compound = super.getUpdateTag();
		this.writeCommonData(compound);
		return compound;
	}

	// called on server when world.notifyBlockUpdate is invoked at this TE's position
	// generates the packet to send to nearby clients
	@Override
	public SUpdateTileEntityPacket getUpdatePacket()
	{
		CompoundNBT compound = new CompoundNBT();
		this.writeUpdatedPowerData(compound);
		return new SUpdateTileEntityPacket(this.pos, 0, compound); // non-positive ID indicates non-vanilla packet
	}

	// called on client to read the packet sent by getUpdatePacket
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
	{
		CompoundNBT compound = pkt.getNbtCompound();
		this.readUpdatedPowerData(compound);
		BlockState state = this.getBlockState();
		this.world.notifyBlockUpdate(this.pos, state, state, 8);
		super.onDataPacket(net, pkt);
	}
	
	public CompoundNBT writeCommonData(CompoundNBT compound)
	{
		this.writeAllPowerData(compound);
		return compound;
	}
	
	public void readCommonData(CompoundNBT compound)
	{
		this.readUpdatedPowerData(compound);
	}
	
	public CompoundNBT writeAllPowerData(CompoundNBT compound)
	{
		CompoundNBT powerData = new CompoundNBT();
		boolean wrotePower = false;
		// writes all positive power values
		for (byte side=0; side<6; side++)
		{
			ByteArrayList bytes = new ByteArrayList();
			short channelFlags = 0;
			for (byte channel = 0; channel < 16; channel++)
			{
				int powerValue = this.getPower(side, channel);
				if (powerValue > 0)
				{
					bytes.add((byte)powerValue);
					channelFlags += (1 << channel);
				}
			}
			if (bytes.size() > 0)
			{
				CompoundNBT sidedPower = new CompoundNBT();
				sidedPower.putShort(CHANNEL_FLAGS, channelFlags);
				sidedPower.putByteArray(POWER_BYTES, bytes.toByteArray());
				powerData.put(Direction.byIndex(side).getName2(), sidedPower);
				wrotePower = true;
			}
		}
		
		if (wrotePower)
			compound.put(POWER, powerData);

		return compound;
		
	}
	
	public CompoundNBT writeUpdatedPowerData(CompoundNBT compound)
	{
		CompoundNBT powerData = new CompoundNBT();
		byte[][] buffer = this.getAndClearUpdateBuffer();
		boolean wrotePower = false;
		for (int side=0; side<6; side++)
		{
			byte[] sidedPower = buffer[side];
			if (sidedPower == null)
				continue;
			
			CompoundNBT sidedPowerTag = new CompoundNBT();
			boolean wroteSidedPower = false;
			int channelFlags = 0;
			ByteList bytes = new ByteArrayList(16);
			for (int channel = 0; channel < 16; channel++)
			{
				byte channelPower = sidedPower[channel];
				if (channelPower > 0)
				{
					channelFlags |= (1 << channel);
					bytes.add(channelPower);
					wroteSidedPower = true;
				}
			}
			if (wroteSidedPower)
			{
				sidedPowerTag.putShort(CHANNEL_FLAGS, (short)channelFlags);
				sidedPowerTag.putByteArray(POWER_BYTES, bytes.toByteArray());
				powerData.put(Direction.byIndex(side).getName2(), sidedPowerTag);
				wrotePower = true;
			}
		}
		
		if (wrotePower)
			compound.put(POWER, powerData);
		
		return compound;
	}
	
	public void readUpdatedPowerData(CompoundNBT compound)
	{
		CompoundNBT powerData = compound.getCompound(POWER);
		if (powerData == null)
			return;
		
		for (int side=0; side<6; side++)
		{
			CompoundNBT sidedPower = powerData.getCompound(Direction.byIndex(side).getName2());
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
				if ((channels | channelFlag) != 0)
				{
					this.power[side][channel] = powerValues[powerByteIndex++];
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
		public int getPowerOnChannel(World world, BlockPos wirePos, BlockState wireState, @Nullable Direction wireFace, int channel)
		{
			BundledCableTileEntity cable = BundledCableTileEntity.this;
			BlockState state = cable.getBlockState();
			Block block = state.getBlock();
			if (!(block instanceof BundledCableBlock))
				return 0;

			// check the power of the wire attached on the capability side first
			int sideIndex = this.side.ordinal();
			if (state.get(AbstractWireBlock.INTERIOR_FACES[sideIndex]))
				return cable.getPower(sideIndex, channel);
			
			// otherwise, if the querier needs a specific wire face, get that power
			if (wireFace != null)
				return cable.getPower(wireFace.ordinal(), channel);
			
			// otherwise, get the strongest wire power among the four subwires adjacent to the capability side
			int output = 0;
			for (int subSide = 0; subSide < 4; subSide++)
			{
				int actualSubSide = DirectionHelper.uncompressSecondSide(sideIndex, subSide);
				if (state.get(AbstractWireBlock.INTERIOR_FACES[actualSubSide]))
					output = Math.max(output, cable.power[actualSubSide][channel]);
			}
			
			return output;
		}
		
	}

}
