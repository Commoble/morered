package commoble.morered.wire_post;

import commoble.morered.MoreRed;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class BundledCableRelayPlateBlockEntity extends BundledCablePostBlockEntity implements ChanneledPowerSupplier
{
	protected final LazyOptional<ChanneledPowerSupplier> powerHolder = LazyOptional.of(() -> this);
	
	public BundledCableRelayPlateBlockEntity(BlockEntityType<? extends BundledCableRelayPlateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public BundledCableRelayPlateBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().bundledCableRelayPlateBeType.get(), pos, state);
	}

	@Override
	public int getPowerOnChannel(Level world, BlockPos wirePos, BlockState wireState, Direction wireFace, int channel)
	{
		BlockState thisState = this.getBlockState();
		return wireFace == null || (thisState.getBlock() instanceof AbstractPostBlock && wireFace == thisState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
			? this.getPower(channel)
			: 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
	{
		if (cap == MoreRedAPI.CHANNELED_POWER_CAPABILITY)
			return (LazyOptional<T>) this.powerHolder;
		return super.getCapability(cap, side);
	}
	
	@Override
	public void invalidateCaps()
	{
		super.invalidateCaps();
		this.powerHolder.invalidate();;
	}
	
	@Override
	public byte[] getStrongestNeighborPower()
	{
		BlockState state = this.getBlockState();
		Block block = state.getBlock();
		if (!(block instanceof AbstractPostBlock))
			return new byte[16];
		
		BlockPos pos = this.getBlockPos();
		Direction attachmentDirection = state.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);

		byte[] result = new byte[16];
		for (int orthagonal = 0; orthagonal < 4; orthagonal++)
		{
			int secondarySide = DirectionHelper.uncompressSecondSide(attachmentDirection.ordinal(), orthagonal);
			Direction orthagonalDirection = Direction.from3DDataValue(secondarySide);
			BlockPos neighborPos = pos.relative(orthagonalDirection);
			BlockEntity te = this.level.getBlockEntity(neighborPos);
			if (te == null)
				continue;
			te.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, orthagonalDirection.getOpposite()).ifPresent(power ->
			{
				for (int channel=0; channel<16; channel++)
				{
					result[channel] = (byte)Math.max(result[channel], power.getPowerOnChannel(this.level, pos, state, attachmentDirection, channel)-1);
				}
			});
		}
		return result;
	}
	
}
