package net.commoble.morered.transportation;

import net.commoble.morered.MoreRed;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneTubeBlockEntity extends TubeBlockEntity
{
	public static final BlockEntityTicker<RedstoneTubeBlockEntity> TICKER = (level,state,pos,tube) -> tube.tick();
	
	public RedstoneTubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
	{
		super(type,pos,state);
	}
		
	public RedstoneTubeBlockEntity(BlockPos pos, BlockState state)
	{
		this(MoreRed.REDSTONE_TUBE_BLOCK_ENTITY.get(), pos, state);
	}

	@Override
	protected void tick()
	{ // block is powered while there are items moving through it
		// change state when contents of inventory change from nothing to something or
		// from something to nothing
		super.tick();
		if (!this.level.isClientSide())
		{
			boolean hasItems = this.inventory.size() > 0;
			boolean isPowered = this.getBlockState().getValue(RedstoneTubeBlock.POWERED);
			if (hasItems != isPowered)
			{
				BlockState newState = this.getBlockState().setValue(RedstoneTubeBlock.POWERED, Boolean.valueOf(hasItems));
				this.level.setBlockAndUpdate(this.worldPosition, newState);
				this.level.playSound(null, this.worldPosition,
						hasItems ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
						SoundSource.BLOCKS, 0.3F, hasItems ? 0.6F : 0.5F);
				this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
			}
		}
	}
}
