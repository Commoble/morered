package commoble.morered.wires;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.wire_post.BundledCablePostTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WireTileEntity extends BlockEntity {
    public static final String POWER = "power";
    protected int[] power = new int[6];

    public WireTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityRegistrar.WIRE.get(), pos, state);
    }

    public WireTileEntity(BlockEntityType<? extends WireTileEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Sets the power on a given interior face
     *
     * @param side     The ordinal of the attachment direction of the subwire to set the power of
     * @param newPower The power value to set the subwire's power to
     * @return True if the power changed, false if it didn't
     */
    public boolean setPower(int side, int newPower) {
        int oldPower = this.power[side];
        this.power[side] = newPower;
        if (oldPower != newPower) {
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
     *
     * @param power array of power values (be wary of reference-type problems)
     */
    public void setPowerRaw(int[] power) {
        this.power = power;
    }

    /**
     * Sets the power on a given interior face
     *
     * @param direction The attachment direction of the subwire to set the power of
     * @param newPower  The power value to set the subwire's power to
     * @return True if the power changed, false if it didn't
     */
    public boolean setPower(Direction direction, int newPower) {
        return this.setPower(direction.ordinal(), newPower);
    }

    public int getPower(Direction direction) {
        return this.getPower(direction.ordinal());
    }

    public int getPower(int side) {
        return this.power[side];
    }

    // called when TE is serialized to hard drive or whatever
    // defaults to this.writeInternal()
    @Override
    public void saveAdditional(CompoundTag compound) {
        this.writeCommonData(compound);
    }

    // reads the data written by write, called when TE is loaded from hard drive
    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        this.readCommonData(compound);
    }

    // called on server to get the data to send to clients when chunk is loaded for client
    // defaults to this.writeInternal()
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag compound = super.getUpdateTag();
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
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // called on client to read the packet sent by getUpdatePacket
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag compound = pkt.getTag();
        this.readCommonData(compound == null ? new CompoundTag() : compound);
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 8);
        super.onDataPacket(net, pkt);
    }

    public CompoundTag writeCommonData(CompoundTag compound) {
        compound.putIntArray(POWER, this.power.clone());
        return compound;
    }

    public void readCommonData(CompoundTag compound) {
        int[] newPower = compound.getIntArray(POWER); // returns 0-length array if field not present
        if (newPower.length == 6)
            this.power = newPower.clone();
    }
}
