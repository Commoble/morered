package commoble.morered.wire_post;

import commoble.morered.TileEntityRegistrar;
import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import commoble.morered.util.DirectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class BundledCableRelayPlateTileEntity extends BundledCablePostTileEntity implements ChanneledPowerSupplier
{
	protected final LazyOptional<ChanneledPowerSupplier> powerHolder = LazyOptional.of(() -> this);
	
	public BundledCableRelayPlateTileEntity(TileEntityType<? extends BundledCableRelayPlateTileEntity> type)
	{
		super(type);
	}
	
	public BundledCableRelayPlateTileEntity()
	{
		super(TileEntityRegistrar.BUNDLED_CABLE_RELAY_PLATE.get());
	}

	@Override
	public int getPowerOnChannel(World world, BlockPos wirePos, BlockState wireState, Direction wireFace, int channel)
	{
		BlockState thisState = this.getBlockState();
		return wireFace == null || (thisState.getBlock() instanceof AbstractPostBlock && wireFace == thisState.get(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
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
	protected void invalidateCaps()
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
		
		BlockPos pos = this.getPos();
		Direction attachmentDirection = state.get(AbstractPostBlock.DIRECTION_OF_ATTACHMENT);

		byte[] result = new byte[16];
		for (int orthagonal = 0; orthagonal < 4; orthagonal++)
		{
			int secondarySide = DirectionHelper.uncompressSecondSide(attachmentDirection.ordinal(), orthagonal);
			Direction orthagonalDirection = Direction.byIndex(secondarySide);
			BlockPos neighborPos = pos.offset(orthagonalDirection);
			TileEntity te = this.world.getTileEntity(neighborPos);
			if (te == null)
				continue;
			te.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, orthagonalDirection.getOpposite()).ifPresent(power ->
			{
				for (int channel=0; channel<16; channel++)
				{
					result[channel] = (byte)Math.max(result[channel], power.getPowerOnChannel(this.world, pos, state, attachmentDirection, channel)-1);
				}
			});
		}
		return result;
	}
	
}
