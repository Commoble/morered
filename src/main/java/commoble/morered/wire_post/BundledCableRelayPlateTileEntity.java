package commoble.morered.wire_post;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.util.DirectionHelper;
import commoble.moreredapi.ChanneledPowerSupplier;
import commoble.moreredapi.MoreRedAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class BundledCableRelayPlateTileEntity extends BundledCablePostTileEntity implements ChanneledPowerSupplier {
    protected final LazyOptional<ChanneledPowerSupplier> powerHolder = LazyOptional.of(() -> this);


    public BundledCableRelayPlateTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get(), pos, state);
    }

    @Override
    public int getPowerOnChannel(@NotNull LevelReader world, @NotNull BlockPos wirePos, BlockState wireState,
                                 Direction wireFace, int channel) {
        BlockState thisState = this.getBlockState();
        return wireFace == null || (thisState.getBlock() instanceof AbstractPostBlock && wireFace == thisState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
                ? this.getPower(channel)
                : 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY)
            return (LazyOptional<T>) this.powerHolder;
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.powerHolder.invalidate();
    }

    @Override
    public byte[] getStrongestNeighborPower() {
        BlockState state = this.getBlockState();
        Block block = state.getBlock();
        if (!(block instanceof AbstractPostBlock))
            return new byte[16];

        BlockPos pos = this.getBlockPos();
        Direction attachmentDirection = state.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);

        byte[] result = new byte[16];
        for (int orthagonal = 0; orthagonal < 4; orthagonal++) {
            int secondarySide = DirectionHelper.uncompressSecondSide(attachmentDirection.ordinal(), orthagonal);
            Direction orthagonalDirection = Direction.from3DDataValue(secondarySide);
            BlockPos neighborPos = pos.relative(orthagonalDirection);
            BlockEntity te = this.level.getBlockEntity(neighborPos);
            if (te == null)
                continue;
            te.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, orthagonalDirection.getOpposite()).ifPresent(power ->
            {
                for (int channel = 0; channel < 16; channel++) {
                    result[channel] = (byte) Math.max(result[channel], power.getPowerOnChannel(this.level, pos, state,
                            attachmentDirection, channel) - 1);
                }
            });
        }
        return result;
    }

}
