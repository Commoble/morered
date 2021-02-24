package commoble.morered.wires;

import commoble.morered.TileEntityRegistrar;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

public class WireTileEntity extends TileEntity
{
	public static final String POWER = "power";
	protected int[] power = new int[6];
	
	public WireTileEntity()
	{
		this(TileEntityRegistrar.WIRE.get());
	}
	
	public WireTileEntity(TileEntityType<? extends WireTileEntity> type)
	{
		super(type);
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
			this.markDirty();
			BlockState state = this.getBlockState();
			// what do the block flags do here?
			// client world: only flag 8 (bit 4) is checked (rerender on main thread)
			// server world: none of the flags are checked
			this.world.notifyBlockUpdate(this.pos, state, state, 0);
			return true;
		}
		return false;
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

	// reads the data from getUpdateTag on client, defaults to #read
//	@Override
//	public void handleUpdateTag(BlockState state, CompoundNBT compound)
//	{
//		super.handleUpdateTag(state, compound);
//		this.readCommonData(compound);
//	}

	// called on server when world.notifyBlockUpdate is invoked at this TE's position
	// generates the packet to send to nearby clients
	@Override
	public SUpdateTileEntityPacket getUpdatePacket()
	{
		CompoundNBT compound = new CompoundNBT();
		this.writeCommonData(compound);
		return new SUpdateTileEntityPacket(this.pos, 0, compound); // non-positive ID indicates non-vanilla packet
	}

	// called on client to read the packet sent by getUpdatePacket
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
	{
		CompoundNBT compound = pkt.getNbtCompound();
		this.readCommonData(compound);
		BlockState state = this.getBlockState();
		this.world.notifyBlockUpdate(this.pos, state, state, 8);
		super.onDataPacket(net, pkt);
	}
	
	public CompoundNBT writeCommonData(CompoundNBT compound)
	{
		compound.putIntArray(POWER, this.power);
		return compound;
	}
	
	public void readCommonData(CompoundNBT compound)
	{
		int[] newPower = compound.getIntArray(POWER); // returns 0-length array if field not present
		if (newPower.length == 6)
			this.power = newPower;
	}
}
