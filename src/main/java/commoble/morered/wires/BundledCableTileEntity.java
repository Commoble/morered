package commoble.morered.wires;

import java.util.EnumMap;
import java.util.Map;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.DirectionHelper;
import commoble.moreredapi.ChanneledPowerSupplier;
import commoble.moreredapi.MoreRedAPI;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class BundledCableTileEntity extends BlockEntity {
    public static final String POWER = "power";
    public static final String CHANNEL_FLAGS = "channel_flags";
    public static final String POWER_BYTES = "power_bytes";

    /**
     * Table of power values
     * The first index is the direction ordinal of the face of the wire where the power is stored (dunswe order)
     * The second index is the power channel ordinal (dye color order)
     */
    protected byte[][] power = new byte[6][16];

    protected Map<Direction, LazyOptional<ChanneledPowerSupplier>> sidedPowerSuppliers =
            Util.make(new EnumMap<>(Direction.class), map ->
            {
                for (int i = 0; i < 6; i++) {
                    Direction dir = Direction.from3DDataValue(i);
                    map.put(dir, LazyOptional.of(() -> new SidedPowerSupplier(dir)));
                }
            });

//	public BundledCableTileEntity() {
//		super(TileEntityRegistrar.BUNDLED_NETWORK_CABLE.get());
//	}

    public BundledCableTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityRegistrar.BUNDLED_NETWORK_CABLE.get(), pos, state);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.sidedPowerSuppliers.forEach((dir, holder) -> holder.invalidate());
    }

    public int getPower(int side, int channel) {
        return this.power[side][channel];
    }

    /**
     * Returns the power for all channels on the given side
     *
     * @param side Directional ordinal
     * @return The power array for all channels on the given side (be sure to clone before storing)
     */
    public byte[] getPowerChannels(int side) {
        return this.power[side];
    }

    /**
     * Sets the power value and marks the block and TE updated if the power changed on the server
     *
     * @param side     The attachment side to set the power of, in the range [0,5]
     * @param channel  The channel to set the power of, in the range [0,15]
     * @param newPower The power value to set the power of, in the range [0,31]
     * @return True if the power changed, false otherwise
     */
    public boolean setPower(int side, int channel, int newPower) {
        int oldPower = this.power[side][channel];
        this.power[side][channel] = (byte) newPower;
        if (oldPower != newPower) {
            if (!this.level.isClientSide) {
                this.setChanged();
            }
            return true;
        }
        return false;
    }

    public void setPowerRaw(byte[][] newPower) {
        this.power = newPower;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY && side != null)
            return (LazyOptional<T>) this.sidedPowerSuppliers.get(side);
        return super.getCapability(cap, side);
    }

    // called when TE is serialized to hard drive or whatever
    // defaults to this.writeInternal()
    @Override
    public void saveAdditional(CompoundTag compound) {
        this.writeServerData(compound);
    }

    // reads the data written by write, called when TE is loaded from hard drive
    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        this.readServerData(compound);
    }

    public CompoundTag writeServerData(CompoundTag compound) {
        this.writeAllPowerData(compound);
        return compound;
    }

    public void readServerData(CompoundTag compound) {
        this.readUpdatedPowerData(compound);
    }

    public CompoundTag writeAllPowerData(CompoundTag compound) {
        CompoundTag powerData = new CompoundTag();
        boolean wrotePower = false;
        // writes all positive power values
        for (byte side = 0; side < 6; side++) {
            ByteArrayList bytes = new ByteArrayList();
            short channelFlags = 0;
            for (byte channel = 0; channel < 16; channel++) {
                int powerValue = this.getPower(side, channel);
                if (powerValue > 0) {
                    bytes.add((byte) powerValue);
                    channelFlags += (1 << channel);
                }
            }
            if (bytes.size() > 0) {
                CompoundTag sidedPower = new CompoundTag();
                sidedPower.putShort(CHANNEL_FLAGS, channelFlags);
                sidedPower.putByteArray(POWER_BYTES, bytes.toByteArray());
                powerData.put(Direction.from3DDataValue(side).getName(), sidedPower);
                wrotePower = true;
            }
        }

        if (wrotePower)
            compound.put(POWER, powerData);

        return compound;

    }

    public void readUpdatedPowerData(CompoundTag compound) {
        CompoundTag powerData = compound.getCompound(POWER);
        if (powerData == null)
            return;

        for (int side = 0; side < 6; side++) {
            CompoundTag sidedPower = powerData.getCompound(Direction.from3DDataValue(side).getName());
            if (sidedPower == null)
                continue;


            int channels = sidedPower.getShort(CHANNEL_FLAGS);
            byte[] powerValues = sidedPower.getByteArray(POWER_BYTES);
            int powerByteCount = powerValues.length;
            int powerByteIndex = 0;

            channelLoop:
            for (int channel = 0; channel < 16; channel++) {
                // safety rail to avoid index out of bounds crashes
                if (powerByteIndex >= powerByteCount)
                    break channelLoop;

                int channelFlag = (1 << channel);
                if ((channels & channelFlag) != 0) {
                    this.power[side][channel] = powerValues[powerByteIndex++];
                }
            }
        }
    }

    protected class SidedPowerSupplier implements ChanneledPowerSupplier {
        private final Direction side;

        public SidedPowerSupplier(Direction side) {
            this.side = side;
        }

        @Override
        public int getPowerOnChannel(LevelReader world, BlockPos wirePos, BlockState wireState, Direction wireFace,
                                     int channel) {
            BundledCableTileEntity cable = BundledCableTileEntity.this;
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
            for (int subSide = 0; subSide < 4; subSide++) {
                int actualSubSide = DirectionHelper.uncompressSecondSide(sideIndex, subSide);
                if (state.getValue(AbstractWireBlock.INTERIOR_FACES[actualSubSide]))
                    output = Math.max(output, cable.power[actualSubSide][channel]);
            }

            return output;
        }

    }

}
