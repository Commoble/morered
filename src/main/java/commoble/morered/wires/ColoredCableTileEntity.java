package commoble.morered.wires;

import java.util.EnumMap;
import java.util.Map;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.DirectionHelper;
import commoble.moreredapi.ChanneledPowerSupplier;
import commoble.moreredapi.MoreRedAPI;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class ColoredCableTileEntity extends WireTileEntity {
    protected Map<Direction, LazyOptional<ChanneledPowerSupplier>> sidedPowerSuppliers =
            Util.make(new EnumMap<>(Direction.class), map ->
            {
                for (int i = 0; i < 6; i++) {
                    Direction dir = Direction.from3DDataValue(i);
                    map.put(dir, LazyOptional.of(() -> new SidedPowerSupplier(dir)));
                }
            });

    public ColoredCableTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityRegistrar.COLORED_NETWORK_CABLE.get(), pos, state);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.sidedPowerSuppliers.forEach((dir, holder) -> holder.invalidate());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY && side != null)
            return (LazyOptional<T>) this.sidedPowerSuppliers.get(side);
        return super.getCapability(cap, side);
    }

    protected class SidedPowerSupplier implements ChanneledPowerSupplier {
        private final Direction side;

        public SidedPowerSupplier(Direction side) {
            this.side = side;
        }

        @Override
        public int getPowerOnChannel(LevelReader world, BlockPos wirePos, BlockState wireState, Direction wireFace,
                                     int channel) {
            ColoredCableTileEntity cable = ColoredCableTileEntity.this;
            BlockState state = cable.getBlockState();
            Block block = state.getBlock();
            if (!(block instanceof ColoredCableBlock))
                return 0;

            if (channel != ((ColoredCableBlock) block).getDyeColor().ordinal())
                return 0;

            // check the power of the wire attached on the capability side first
            int sideIndex = this.side.ordinal();
            if (state.getValue(AbstractWireBlock.INTERIOR_FACES[sideIndex]))
                return cable.getPower(sideIndex);

            // otherwise, if the querier needs a specific wire face, get that power
            if (wireFace != null)
                return cable.getPower(wireFace.ordinal());

            // otherwise, get the strongest wire power among the four subwires adjacent to the capability side
            int output = 0;
            for (int subSide = 0; subSide < 4; subSide++) {
                int actualSubSide = DirectionHelper.uncompressSecondSide(sideIndex, subSide);
                if (state.getValue(AbstractWireBlock.INTERIOR_FACES[actualSubSide]))
                    output = Math.max(output, cable.getPower(actualSubSide));
            }

            return output;
        }

    }

}
