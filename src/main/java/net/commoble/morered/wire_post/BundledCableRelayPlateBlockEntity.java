package net.commoble.morered.wire_post;

import net.commoble.morered.MoreRed;
import net.commoble.morered.api.ChanneledPowerSupplier;
import net.commoble.morered.api.MoreRedAPI;
import net.commoble.morered.util.DirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BundledCableRelayPlateBlockEntity extends BundledCablePostBlockEntity
{	
	public BundledCableRelayPlateBlockEntity(BlockEntityType<? extends BundledCableRelayPlateBlockEntity> type, BlockPos pos, BlockState state)
	{
		super(type, pos, state);
	}
	
	public BundledCableRelayPlateBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.get().bundledCableRelayPlateBeType.get(), pos, state);
	}

	public int getPowerOnChannel(Level world, BlockPos wirePos, BlockState wireState, Direction wireFace, int channel)
	{
		BlockState thisState = this.getBlockState();
		return wireFace == null || (thisState.getBlock() instanceof AbstractPostBlock && wireFace == thisState.getValue(AbstractPostBlock.DIRECTION_OF_ATTACHMENT))
			? this.getPower(channel)
			: 0;
	}

	public ChanneledPowerSupplier getChanneledPower(Direction side)
	{
		return this::getPowerOnChannel;
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
			ChanneledPowerSupplier power = level.getCapability(MoreRedAPI.CHANNELED_POWER_CAPABILITY, neighborPos, orthagonalDirection.getOpposite());
			if (power != null) {
				for (int channel=0; channel<16; channel++)
				{
					result[channel] = (byte)Math.max(result[channel], power.getPowerOnChannel(this.level, pos, state, attachmentDirection, channel)-1);
				}
			};
		}
		return result;
	}
	
}
